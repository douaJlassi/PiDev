package controllers;

import app.Session;
import entities.ReservationSummary;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import repositories.ReservationRepository;
import java.util.List;
import java.util.Collections;


import java.time.format.DateTimeFormatter;

public class MyReservationsController {

    @FXML private TableView<ReservationSummary> table;
    //@FXML private TableColumn<ReservationSummary, Integer> idCol;
    @FXML private TableColumn<ReservationSummary, Object> dateCol;
    @FXML private TableColumn<ReservationSummary, Object> statusCol;
    @FXML private TableColumn<ReservationSummary, Object> totalCol;
    @FXML private TableColumn<ReservationSummary, String> payCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableColumn<ReservationSummary, Void> actionCol;

    @FXML private Label msgLbl;

    private final ReservationRepository repo = new ReservationRepository();
    private List<ReservationSummary> master = Collections.emptyList();
    @FXML
    public void initialize() {
        if (!Session.isClient()) {
            msgLbl.setText("Access denied.");
            table.setDisable(true);
            return;
        }

        // 1. LINK THE DATA (Crucial!)
        payCol.setCellValueFactory(new PropertyValueFactory<>("modePaiement"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("montantTotal"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statut")); // MUST HAVE THIS
        // 6) FILTER UI (search + status)
        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "ENATTENTE", "CONFIRME", "ANNULE"
        ));
        statusFilter.getSelectionModel().select("ALL");

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        // 2. DATE COLUMN FORMATTING
        dateCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else if (item instanceof java.time.LocalDateTime dt) setText(dt.format(fmt));
                else setText(item.toString());
            }
        });

        // 3. STATUS COLUMN (MODERN PILLS)
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                String rawStatus = item.toString().toUpperCase();
                String displayStatus;
                String styleClass;

                // --- MAP THE NAMES AND STYLES ---
                switch (rawStatus) {
                    case "CONFIRME" -> {
                        displayStatus = "BOOKED";
                        styleClass = "pill-success";
                    }
                    case "ENATTENTE" -> {
                        displayStatus = "PENDING AGENCY";
                        styleClass = "pill-warning";
                    }
                    case "ANNULE" -> {
                        displayStatus = "CANCELLED";
                        styleClass = "pill-danger";
                    }
                    default -> {
                        displayStatus = rawStatus.replace("_", " ");
                        styleClass = "pill-default";
                    }
                }

                Label lb = new Label(displayStatus);
                lb.getStyleClass().addAll("status-pill", styleClass);

                // Optional: Inline style if CSS isn't loading
                lb.setStyle("-fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;");

                HBox container = new HBox(lb);
                container.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(container);
            }
        });

        // 4. PAYMENT COLUMN
        payCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lb = new Label(item.toUpperCase());
                    lb.getStyleClass().add("payment-label");
                    HBox h = new HBox(lb);
                    h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(h);
                }
            }
        });

        // 5. ACTION COLUMN (VIEW BUTTON)
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Details");
            {
                btn.getStyleClass().add("btn-ghost");
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                btn.setOnAction(e -> {
                    ReservationSummary r = getTableView().getItems().get(getIndex());
                    showDetails(r);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        refresh();
    }

    @FXML private void onRefresh() { refresh(); }

    private void refresh() {
        master = repo.findByClient(Session.getUserId());
        applyFilters();
    }
    private void applyFilters() {
        String q = (searchField == null || searchField.getText() == null)
                ? ""
                : searchField.getText().trim().toLowerCase();

        String status = (statusFilter == null || statusFilter.getValue() == null)
                ? "ALL"
                : statusFilter.getValue();

        var filtered = new java.util.ArrayList<ReservationSummary>();

        for (ReservationSummary r : master) {
            // status dropdown filter
            if (!"ALL".equalsIgnoreCase(status)) {
                if (r.getStatut() == null) continue;
                if (!r.getStatut().name().equalsIgnoreCase(status)) continue;
            }

            // text search filter
            if (!q.isEmpty()) {
                String st = (r.getStatut() == null) ? "" : r.getStatut().name().toLowerCase();
                String dt = (r.getDateReservation() == null) ? "" : r.getDateReservation().toString().toLowerCase();
                String pay = (r.getModePaiement() == null) ? "" : r.getModePaiement().toLowerCase();
                String total = (r.getMontantTotal() == null) ? "" : r.getMontantTotal().toString().toLowerCase();
                String id = String.valueOf(r.getIdReservation());

                boolean match = st.contains(q) || dt.contains(q) || pay.contains(q) || total.contains(q) || id.contains(q);
                if (!match) continue;
            }

            filtered.add(r);
        }

        table.setItems(FXCollections.observableArrayList(filtered));
        msgLbl.setText(filtered.isEmpty()
                ? "No reservations found."
                : (filtered.size() + " reservation(s)."));
    }

    private void showDetails(ReservationSummary r) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/ReservationDetailsView.fxml")
            );
            javafx.scene.Parent root = loader.load();

            controllers.ReservationDetailsController ctrl = loader.getController();
            ctrl.init(r);

            Stage stage = new Stage();
            stage.setTitle("Reservation Details");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initOwner(table.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.showAndWait();

            // refresh after closing (maybe user cancelled / removed lines)
            refresh();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Cannot open details: " + ex.getMessage());
            a.showAndWait();
        }
    }
    private Runnable onBack;

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void onBack() {
        if (onBack != null) { onBack.run(); return; }
        // fallback: close window if opened standalone
        Stage stage = (Stage) table.getScene().getWindow();
        stage.close();
    }
}