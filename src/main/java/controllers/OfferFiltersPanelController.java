package controllers;

import app.Session;
import entities.OfferFilter;
import entities.SearchCriteria;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import services.AISearchService;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class OfferFiltersPanelController {

    @FXML
    private VBox aiAgentBox;

    @FXML
    private TextArea aiSearchArea;

    @FXML
    private Button aiSearchBtn;

    @FXML
    private DatePicker calendarDp;

    @FXML
    private TextField minPriceTf;

    @FXML
    private TextField maxPriceTf;

    @FXML
    private Label statusLbl;

    @FXML
    private ListView<String> statusLv;

    @FXML
    private Label locationsLbl;

    @FXML
    private ListView<String> locationsLv;

    private OfferFilter filter = new OfferFilter();

    private Consumer<OfferFilter> onChanged;

    private Runnable onClose;

    @FXML
    public void initialize() {

        // ------------------------------------
        // AI BOX
        // ------------------------------------

        boolean isClient = Session.isClient();

        if (aiAgentBox != null) {
            aiAgentBox.setVisible(isClient);
            aiAgentBox.setManaged(isClient);
        }

        // ------------------------------------
        // STATUS LIST
        // ------------------------------------

        if (statusLv != null) {

            statusLv.getItems().setAll(
                    "ACTIVE",
                    "ARCHIVED"
            );

            statusLv.setCellFactory(
                    list -> new javafx.scene.control.cell.CheckBoxListCell<>(
                            item -> {

                                javafx.beans.property.BooleanProperty prop =
                                        new javafx.beans.property.SimpleBooleanProperty(
                                                filter.getStatuses().contains(item)
                                        );

                                prop.addListener((obs, was, now) -> {

                                    if (now) {
                                        filter.getStatuses().add(item);
                                    } else {
                                        filter.getStatuses().remove(item);
                                    }

                                    fireChanged();
                                });

                                return prop;
                            }
                    )
            );
        }

        // ------------------------------------
        // LOCATIONS LIST
        // ------------------------------------

        if (locationsLv != null) {

            locationsLv.getItems().setAll(
                    "Tunis",
                    "Sousse",
                    "Djerba",
                    "Hammamet",
                    "Monastir"
            );

            locationsLv.setCellFactory(
                    list -> new javafx.scene.control.cell.CheckBoxListCell<>(
                            item -> {

                                javafx.beans.property.BooleanProperty prop =
                                        new javafx.beans.property.SimpleBooleanProperty(
                                                filter.getLocations().contains(item)
                                        );

                                prop.addListener((obs, was, now) -> {

                                    if (now) {
                                        filter.getLocations().add(item);
                                    } else {
                                        filter.getLocations().remove(item);
                                    }

                                    fireChanged();
                                });

                                return prop;
                            }
                    )
            );
        }

        // ------------------------------------
        // DATE FILTER
        // ------------------------------------

        if (calendarDp != null) {

            calendarDp.valueProperty().addListener((o, a, b) -> {

                filter.setSelectedDate(b);

                fireChanged();
            });
        }

        // ------------------------------------
        // PRICE FILTERS
        // ------------------------------------

        if (minPriceTf != null) {

            minPriceTf.textProperty().addListener((o, a, b) -> {

                filter.setMinPrice(parseBD(b));

                fireChanged();
            });
        }

        if (maxPriceTf != null) {

            maxPriceTf.textProperty().addListener((o, a, b) -> {

                filter.setMaxPrice(parseBD(b));

                fireChanged();
            });
        }
    }

    public void setFilter(OfferFilter f) {

        this.filter =
                (f == null)
                        ? new OfferFilter()
                        : f;

        syncUiFromFilter();
    }

    public void setOnChanged(
            Consumer<OfferFilter> onChanged
    ) {

        this.onChanged = onChanged;
    }

    public void setOnClose(
            Runnable onClose
    ) {

        this.onClose = onClose;
    }

    @FXML
    private void onClose() {

        if (onClose != null) {
            onClose.run();
        }
    }

    @FXML
    private void onClearFilters() {

        filter.setKeyword(null);

        filter.setMinPrice(null);

        filter.setMaxPrice(null);

        filter.setSelectedDate(null);

        filter.getStatuses().clear();

        filter.getLocations().clear();

        if (minPriceTf != null) {
            minPriceTf.clear();
        }

        if (maxPriceTf != null) {
            maxPriceTf.clear();
        }

        if (calendarDp != null) {
            calendarDp.setValue(null);
        }

        if (statusLv != null) {
            statusLv.refresh();
        }

        if (locationsLv != null) {
            locationsLv.refresh();
        }

        fireChanged();
    }

    @FXML
    private void onAISmartSearch() {

        if (!Session.isClient()) {
            return;
        }

        String query =
                aiSearchArea == null
                        ? null
                        : aiSearchArea.getText();

        if (query == null || query.isBlank()) {
            return;
        }

        aiSearchBtn.setDisable(true);

        aiSearchBtn.setText(
                "Gemini is thinking..."
        );

        Task<SearchCriteria> task =
                new Task<>() {

                    @Override
                    protected SearchCriteria call()
                            throws Exception {

                        return AISearchService
                                .parseDeepQuery(query);
                    }
                };

        task.setOnSucceeded(e -> {

            SearchCriteria sc =
                    task.getValue();

            applyAICriteria(sc);

            aiSearchBtn.setDisable(false);

            aiSearchBtn.setText(
                    "Search with Gemini"
            );
        });

        task.setOnFailed(e -> {

            aiSearchBtn.setDisable(false);

            aiSearchBtn.setText(
                    "Search with Gemini"
            );

            if (task.getException() != null) {

                System.err.println(
                        "AI Search Failed: "
                                + task.getException()
                                .getMessage()
                );
            }
        });

        new Thread(task).start();
    }

    private void applyAICriteria(
            SearchCriteria sc
    ) {

        if (sc == null) {
            return;
        }

        onClearFilters();

        if (sc.destination != null) {

            filter.setKeyword(
                    sc.destination
            );
        }

        if (sc.maxPrice != null) {

            filter.setMaxPrice(
                    BigDecimal.valueOf(sc.maxPrice)
            );

            if (maxPriceTf != null) {

                maxPriceTf.setText(
                        String.format(
                                "%.2f",
                                sc.maxPrice
                        )
                );
            }
        }

        fireChanged();
    }

    private void syncUiFromFilter() {

        if (calendarDp != null) {

            calendarDp.setValue(
                    filter.getSelectedDate()
            );
        }

        if (minPriceTf != null) {

            minPriceTf.setText(
                    filter.getMinPrice() == null
                            ? ""
                            : filter.getMinPrice().toString()
            );
        }

        if (maxPriceTf != null) {

            maxPriceTf.setText(
                    filter.getMaxPrice() == null
                            ? ""
                            : filter.getMaxPrice().toString()
            );
        }

        if (statusLv != null) {
            statusLv.refresh();
        }

        if (locationsLv != null) {
            locationsLv.refresh();
        }
    }

    private void fireChanged() {

        if (onChanged != null) {
            onChanged.accept(filter);
        }
    }

    private BigDecimal parseBD(
            String s
    ) {

        if (s == null) {
            return null;
        }

        s = s.trim();

        if (s.isEmpty()) {
            return null;
        }

        try {

            return new BigDecimal(s);

        } catch (Exception e) {

            return null;
        }
    }
}