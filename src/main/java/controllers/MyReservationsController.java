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


import java.time.format.DateTimeFormatter;

public class MyReservationsController {

    @FXML private TableView<ReservationSummary> table;
    //@FXML private TableColumn<ReservationSummary, Integer> idCol;
    @FXML private TableColumn<ReservationSummary, Object> dateCol;
    @FXML private TableColumn<ReservationSummary, Object> statusCol;
    @FXML private TableColumn<ReservationSummary, String> payCol;
    @FXML private TableColumn<ReservationSummary, Object> totalCol;
    @FXML private TableColumn<ReservationSummary, Void> actionCol;

    @FXML private Label msgLbl;

    private final ReservationRepository repo = new ReservationRepository();

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

                Label lb = new Label(item.toString().toUpperCase().replace("_", " "));
                lb.getStyleClass().add("status-pill");

                String status = item.toString().toUpperCase();
                switch (status) {
                    case "CONFIRME" -> lb.getStyleClass().add("pill-success");
                    case "ENATTENTE" -> lb.getStyleClass().add("pill-warning");
                    case "ANNULE" -> lb.getStyleClass().add("pill-danger");
                    default -> lb.getStyleClass().add("pill-default");
                }

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
        var list = repo.findByClient(Session.getUserId());
        table.setItems(FXCollections.observableArrayList(list));
        msgLbl.setText(list.isEmpty() ? "No reservations found." : (list.size() + " reservation(s)."));
    }

    private void showDetails(ReservationSummary r) {
        var items = repo.findReservationItems(r.getIdReservation(), Session.getUserId());

        StringBuilder sb = new StringBuilder();
        sb.append("Reservation #").append(r.getIdReservation()).append("\n")
                .append("Status: ").append(r.getStatut()).append("\n")
                .append("Payment: ").append(r.getModePaiement()).append("\n")
                .append("Total: ").append(r.getMontantTotal()).append(" TND\n\n")
                .append("Offers:\n");

        for (var it : items) {
            sb.append("- ").append(it.getTitre())
                    .append(" (").append(it.getPrixUnitaire()).append(" TND)\n");
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Reservation Details");
        a.setHeaderText(null);
        a.setContentText(sb.toString());
        a.showAndWait();
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