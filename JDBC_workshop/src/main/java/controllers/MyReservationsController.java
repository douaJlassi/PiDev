package controllers;

import app.Session;
import entities.ReservationSummary;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import repositories.ReservationRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyReservationsController {

    @FXML
    private TableView<ReservationSummary> table;

    @FXML
    private TableColumn<ReservationSummary, Object> dateCol;

    @FXML
    private TableColumn<ReservationSummary, Object> statusCol;

    @FXML
    private TableColumn<ReservationSummary, Object> totalCol;

    @FXML
    private TableColumn<ReservationSummary, String> payCol;

    @FXML
    private TableColumn<ReservationSummary, Void> actionCol;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private Label msgLbl;

    private final ReservationRepository repo =
            new ReservationRepository();

    private List<ReservationSummary> master =
            Collections.emptyList();

    private Runnable onBack;

    @FXML
    public void initialize() {

        if (!Session.isClient()) {
            if (msgLbl != null) {
                msgLbl.setText("Access denied.");
            }

            if (table != null) {
                table.setDisable(true);
            }

            return;
        }

        setupColumns();

        setupFilters();

        refresh();
    }

    private void setupColumns() {

        payCol.setCellValueFactory(
                new PropertyValueFactory<>("modePaiement")
        );

        totalCol.setCellValueFactory(
                new PropertyValueFactory<>("montantTotal")
        );

        dateCol.setCellValueFactory(
                new PropertyValueFactory<>("dateReservation")
        );

        statusCol.setCellValueFactory(
                new PropertyValueFactory<>("statut")
        );

        setupDateColumn();

        setupStatusColumn();

        setupPaymentColumn();

        setupActionColumn();
    }

    private void setupFilters() {

        if (statusFilter != null) {

            statusFilter.setItems(
                    FXCollections.observableArrayList(
                            "ALL",
                            "PENDING",
                            "CONFIRMED",
                            "REJECTED",
                            "CANCELLED"
                    )
            );

            statusFilter.getSelectionModel().select("ALL");

            statusFilter.valueProperty().addListener(
                    (obs, oldV, newV) -> applyFilters()
            );
        }

        if (searchField != null) {

            searchField.textProperty().addListener(
                    (obs, oldV, newV) -> applyFilters()
            );
        }
    }

    private void setupDateColumn() {

        dateCol.setCellFactory(col -> new TableCell<>() {

            private final DateTimeFormatter fmt =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            @Override
            protected void updateItem(
                    Object item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {

                    setText(null);

                } else if (item instanceof java.time.LocalDateTime dt) {

                    setText(dt.format(fmt));

                } else {

                    setText(item.toString());
                }
            }
        });
    }

    private void setupStatusColumn() {

        statusCol.setCellFactory(col -> new TableCell<>() {

            @Override
            protected void updateItem(
                    Object item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                String rawStatus =
                        item.toString().toUpperCase();

                String normalized =
                        normalizeStatus(rawStatus);

                String displayStatus;
                String styleClass;

                switch (normalized) {

                    case "CONFIRMED" -> {
                        displayStatus = "CONFIRMED";
                        styleClass = "pill-success";
                    }

                    case "PENDING" -> {
                        displayStatus = "PENDING";
                        styleClass = "pill-warning";
                    }

                    case "REJECTED" -> {
                        displayStatus = "REJECTED";
                        styleClass = "pill-danger";
                    }

                    case "CANCELLED" -> {
                        displayStatus = "CANCELLED";
                        styleClass = "pill-danger";
                    }

                    default -> {
                        displayStatus = rawStatus.replace("_", " ");
                        styleClass = "pill-default";
                    }
                }

                Label lb =
                        new Label(displayStatus);

                lb.getStyleClass().addAll(
                        "status-pill",
                        styleClass
                );

                lb.setStyle(
                        "-fx-font-weight: bold; " +
                                "-fx-padding: 2 8; " +
                                "-fx-background-radius: 10;"
                );

                HBox container =
                        new HBox(lb);

                container.setAlignment(
                        javafx.geometry.Pos.CENTER
                );

                setGraphic(container);
                setText(null);
            }
        });
    }

    private void setupPaymentColumn() {

        payCol.setCellFactory(col -> new TableCell<>() {

            @Override
            protected void updateItem(
                    String item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {

                    setGraphic(null);
                    setText(null);

                } else {

                    Label lb =
                            new Label(item.toUpperCase());

                    lb.getStyleClass().add("payment-label");

                    HBox h =
                            new HBox(lb);

                    h.setAlignment(
                            javafx.geometry.Pos.CENTER_LEFT
                    );

                    setGraphic(h);
                    setText(null);
                }
            }
        });
    }

    private void setupActionColumn() {

        actionCol.setCellFactory(col -> new TableCell<>() {

            private final Button btn =
                    new Button("Details");

            {
                btn.getStyleClass().add("btn-ghost");

                btn.setStyle(
                        "-fx-font-size: 11px; " +
                                "-fx-padding: 4 10;"
                );

                btn.setOnAction(e -> {

                    ReservationSummary r =
                            getTableView()
                                    .getItems()
                                    .get(getIndex());

                    showDetails(r);
                });
            }

            @Override
            protected void updateItem(
                    Void item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                setGraphic(empty ? null : btn);

                setAlignment(
                        javafx.geometry.Pos.CENTER
                );
            }
        });
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {

        master =
                repo.findByClient(
                        Session.getUserId()
                );

        applyFilters();
    }

    private void applyFilters() {

        String q =
                searchField == null ||
                        searchField.getText() == null
                        ? ""
                        : searchField
                        .getText()
                        .trim()
                        .toLowerCase();

        String selectedStatus =
                statusFilter == null ||
                        statusFilter.getValue() == null
                        ? "ALL"
                        : statusFilter.getValue();

        List<ReservationSummary> filtered =
                new ArrayList<>();

        for (ReservationSummary r : master) {

            String reservationStatus =
                    r.getStatut() == null
                            ? ""
                            : normalizeStatus(
                            r.getStatut()
                                    .name()
                                    .toUpperCase()
                    );

            if (!"ALL".equalsIgnoreCase(selectedStatus)) {

                if (
                        !reservationStatus.equalsIgnoreCase(
                                selectedStatus
                        )
                ) {
                    continue;
                }
            }

            if (!q.isEmpty()) {

                String st =
                        reservationStatus.toLowerCase();

                String dt =
                        r.getDateReservation() == null
                                ? ""
                                : r.getDateReservation()
                                .toString()
                                .toLowerCase();

                String pay =
                        r.getModePaiement() == null
                                ? ""
                                : r.getModePaiement()
                                .toLowerCase();

                String total =
                        r.getMontantTotal() == null
                                ? ""
                                : r.getMontantTotal()
                                .toString()
                                .toLowerCase();

                String id =
                        String.valueOf(
                                r.getIdReservation()
                        );

                boolean match =
                        st.contains(q)
                                || dt.contains(q)
                                || pay.contains(q)
                                || total.contains(q)
                                || id.contains(q);

                if (!match) {
                    continue;
                }
            }

            filtered.add(r);
        }

        table.setItems(
                FXCollections.observableArrayList(
                        filtered
                )
        );

        if (msgLbl != null) {

            msgLbl.setText(
                    filtered.isEmpty()
                            ? "No reservations found."
                            : filtered.size()
                            + " reservation(s)."
            );
        }
    }

    private String normalizeStatus(String status) {

        if (status == null) {
            return "";
        }

        String s =
                status.trim().toUpperCase();

        return switch (s) {

            case "ENATTENTE" -> "PENDING";

            case "CONFIRME" -> "CONFIRMED";

            case "ANNULE" -> "CANCELLED";

            case "REFUSEE" -> "REJECTED";

            default -> s;
        };
    }

    private void showDetails(
            ReservationSummary r
    ) {

        try {

            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                            getClass().getResource(
                                    "/fxml/OffreReservationsDetailsView.fxml"
                            )
                    );

            javafx.scene.Parent root =
                    loader.load();

            OffreReservationDetailsController ctrl =
                    loader.getController();

            ctrl.init(r);

            Stage stage =
                    new Stage();

            stage.setTitle(
                    "Reservation Details"
            );

            stage.setScene(
                    new javafx.scene.Scene(root)
            );

            stage.initOwner(
                    table
                            .getScene()
                            .getWindow()
            );

            stage.initModality(
                    Modality.WINDOW_MODAL
            );

            stage.showAndWait();

            refresh();

        } catch (Exception ex) {

            ex.printStackTrace();

            Alert a =
                    new Alert(
                            Alert.AlertType.ERROR,
                            "Cannot open details: "
                                    + ex.getMessage()
                    );

            a.showAndWait();
        }
    }

    public void setOnBack(
            Runnable onBack
    ) {
        this.onBack = onBack;
    }

    @FXML
    private void onBack() {

        if (onBack != null) {
            onBack.run();
            return;
        }

        Stage stage =
                (Stage)
                        table
                                .getScene()
                                .getWindow();

        stage.close();
    }
}