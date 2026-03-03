package controllers;

import app.Session;
import entities.OffreAgency;
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

    @FXML private VBox aiAgentBox;
    @FXML private TextArea aiSearchArea;
    @FXML private Button aiSearchBtn;

    @FXML private DatePicker calendarDp;
    @FXML private TextField minPriceTf;
    @FXML private TextField maxPriceTf;

    @FXML private Label agenciesLbl;
    @FXML private ListView<OffreAgency> agenciesLv;

    private final repositories.AgencyRepository agencyRepo = new repositories.AgencyRepository();

    private OfferFilter filter = new OfferFilter();
    private Consumer<OfferFilter> onChanged;
    private Runnable onClose;

    @FXML
    public void initialize() {
        // AI visible only for client
        boolean isClient = Session.isClient();
        if (aiAgentBox != null) {
            aiAgentBox.setVisible(isClient);
            aiAgentBox.setManaged(isClient);
        }

        // agencies list only for client/admin
        boolean showAgencies = Session.isClient() || Session.isAdmin();
        if (agenciesLv != null) {
            agenciesLv.setVisible(showAgencies);
            agenciesLv.setManaged(showAgencies);
        }
        if (agenciesLbl != null) {
            agenciesLbl.setVisible(showAgencies);
            agenciesLbl.setManaged(showAgencies);
        }

        if (showAgencies && agenciesLv != null) {
            var agencies = agencyRepo.findAllValidated();
            agenciesLv.getItems().setAll(agencies);

            agenciesLv.setCellFactory(list -> new javafx.scene.control.cell.CheckBoxListCell<>(
                    (OffreAgency a) -> {
                        javafx.beans.property.BooleanProperty prop =
                                new javafx.beans.property.SimpleBooleanProperty(filter.getAgencyIds().contains(a.getIdUser()));

                        prop.addListener((obs, was, now) -> {
                            if (now) filter.getAgencyIds().add(a.getIdUser());
                            else filter.getAgencyIds().remove(a.getIdUser());
                            fireChanged();
                        });
                        return prop;
                    },
                    new javafx.util.StringConverter<>() {
                        @Override public String toString(OffreAgency a) { return a == null ? "" : a.toString(); }
                        @Override public OffreAgency fromString(String s) { return null; }
                    }
            ));
        }

        // listeners
        if (calendarDp != null) calendarDp.valueProperty().addListener((o, a, b) -> { filter.setSelectedDate(b); fireChanged(); });
        if (minPriceTf != null) minPriceTf.textProperty().addListener((o, a, b) -> { filter.setMinPrice(parseBD(b)); fireChanged(); });
        if (maxPriceTf != null) maxPriceTf.textProperty().addListener((o, a, b) -> { filter.setMaxPrice(parseBD(b)); fireChanged(); });

        // agency must stay scoped to itself
        if (Session.isAgency()) {
            filter.getAgencyIds().clear();
            filter.getAgencyIds().add(Session.getUserId());
        }
    }

    public void setFilter(OfferFilter f) {
        this.filter = (f == null) ? new OfferFilter() : f;
        if (Session.isAgency()) {
            filter.getAgencyIds().clear();
            filter.getAgencyIds().add(Session.getUserId());
        }
        syncUiFromFilter();
    }

    public void setOnChanged(Consumer<OfferFilter> onChanged) {
        this.onChanged = onChanged;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void onClose() {
        if (onClose != null) onClose.run();
    }

    @FXML
    private void onClearFilters() {
        filter.setKeyword(null);
        filter.setMinPrice(null);
        filter.setMaxPrice(null);
        filter.setSelectedDate(null);

        filter.getAgencyIds().clear();
        if (Session.isAgency()) {
            filter.getAgencyIds().add(Session.getUserId());
        }

        if (minPriceTf != null) minPriceTf.clear();
        if (maxPriceTf != null) maxPriceTf.clear();
        if (calendarDp != null) calendarDp.setValue(null);
        if (agenciesLv != null) agenciesLv.refresh();

        fireChanged();
    }

    @FXML
    private void onAISmartSearch() {
        if (!Session.isClient()) return;

        String query = aiSearchArea == null ? null : aiSearchArea.getText();
        if (query == null || query.isBlank()) return;

        aiSearchBtn.setDisable(true);
        aiSearchBtn.setText("Gemini is thinking...");

        Task<SearchCriteria> task = new Task<>() {
            @Override protected SearchCriteria call() throws Exception {
                return AISearchService.parseDeepQuery(query);
            }
        };

        task.setOnSucceeded(e -> {
            SearchCriteria sc = task.getValue();
            applyAICriteria(sc);

            aiSearchBtn.setDisable(false);
            aiSearchBtn.setText("Search with Gemini");
        });

        task.setOnFailed(e -> {
            aiSearchBtn.setDisable(false);
            aiSearchBtn.setText("Search with Gemini");
            // fail safely: do nothing
            System.err.println("AI Search Failed: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void applyAICriteria(SearchCriteria sc) {
        if (sc == null) return;

        // reset then apply
        onClearFilters();

        if (sc.destination != null) {
            filter.setKeyword(sc.destination);
        }
        if (sc.maxPrice != null) {
            filter.setMaxPrice(BigDecimal.valueOf(sc.maxPrice));
            if (maxPriceTf != null) maxPriceTf.setText(String.format("%.2f", sc.maxPrice));
        }

        fireChanged();
    }

    private void syncUiFromFilter() {
        if (calendarDp != null) calendarDp.setValue(filter.getSelectedDate());
        if (minPriceTf != null) minPriceTf.setText(filter.getMinPrice() == null ? "" : filter.getMinPrice().toString());
        if (maxPriceTf != null) maxPriceTf.setText(filter.getMaxPrice() == null ? "" : filter.getMaxPrice().toString());
        if (agenciesLv != null) agenciesLv.refresh();
    }

    private void fireChanged() {
        if (onChanged != null) onChanged.accept(filter);
    }

    private BigDecimal parseBD(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return new BigDecimal(s); } catch (Exception e) { return null; }
    }
}