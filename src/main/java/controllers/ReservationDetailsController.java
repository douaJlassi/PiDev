package controllers;

import app.Session;
import entities.CartItem;
import entities.ReservationSummary;
import entities.ReservationStatut;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import repositories.ReservationRepository;

import java.time.format.DateTimeFormatter;

public class ReservationDetailsController {

    @FXML private Label titleLbl;
    @FXML private Label statusLbl;
    @FXML private Label totalLbl;
    @FXML private Label payLbl;
    @FXML private Label dateLbl;

    @FXML private TableView<CartItem> itemsTable;
    @FXML private TableColumn<CartItem, String> colTitle;
    @FXML private TableColumn<CartItem, String> colPrice;
    @FXML private TableColumn<CartItem, Object> colAgencyStatus;
    @FXML private TableColumn<CartItem, Void> colAction;

    @FXML private Button cancelBtn;

    private final ReservationRepository repo = new ReservationRepository();
    private ReservationSummary reservation;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void init(ReservationSummary r) {
        this.reservation = r;

        titleLbl.setText("Reservation #" + r.getIdReservation());
        totalLbl.setText("Total: " + r.getMontantTotal() + " TND");
        payLbl.setText("Payment: " + (r.getModePaiement() == null ? "CASH" : r.getModePaiement().toUpperCase()));
        dateLbl.setText("Date: " + (r.getDateReservation() == null ? "-" : r.getDateReservation().format(fmt)));

        applyStatusPill(r.getStatut());

        setupColumns();
        refreshItems();

        // only cancel when ENATTENTE (or PANIER if you want)
        boolean canCancel = r.getStatut() == ReservationStatut.ENATTENTE;
        cancelBtn.setDisable(!canCancel);
    }

    private void setupColumns() {
        colTitle.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getTitre()));
        colPrice.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getPrixUnitaire() == null ? "-" : (p.getValue().getPrixUnitaire() + " TND")
        ));

        // Agency status pill
        colAgencyStatus.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().getAgencyStatus()));
        colAgencyStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                String raw = item.toString().toUpperCase();
                String text;
                String bg;

                switch (raw) {
                    case "APPROUVEE" -> { text = "APPROVED"; bg = "rgba(34,197,94,0.15)"; }
                    case "REFUSEE"   -> { text = "REFUSED";  bg = "rgba(239,68,68,0.15)"; }
                    default          -> { text = "PENDING";  bg = "rgba(234,179,8,0.15)"; }
                }

                Label lb = new Label(text);
                lb.setStyle("-fx-font-weight: bold; -fx-padding: 2 10; -fx-background-radius: 10; -fx-background-color: " + bg + ";");
                HBox h = new HBox(lb);
                h.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(h);
            }
        });

        // Action column: Remove (only if REFUSEE AND reservation ENATTENTE)
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");

            {
                removeBtn.getStyleClass().add("btn-action-danger");
                removeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                removeBtn.setOnAction(e -> {
                    CartItem it = getTableView().getItems().get(getIndex());
                    boolean ok = repo.removeLine(reservation.getIdReservation(), Session.getUserId(), it.getIdOffre());
                    if (!ok) show(Alert.AlertType.ERROR, "Failed", "Cannot remove this line.");
                    refreshItems();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                CartItem it = getTableView().getItems().get(getIndex());
                boolean canEdit = reservation.getStatut() == ReservationStatut.ENATTENTE;
                boolean refused = it != null && "REFUSEE".equalsIgnoreCase(it.getAgencyStatus());

                removeBtn.setDisable(!(canEdit && refused));
                setGraphic(removeBtn);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }

    private void refreshItems() {
        var items = repo.findReservationItems(reservation.getIdReservation(), Session.getUserId());
        itemsTable.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    private void onCancelRequest() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel request");
        confirm.setHeaderText(null);
        confirm.setContentText("Cancel this reservation request?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        boolean ok = repo.cancelReservation(reservation.getIdReservation(), Session.getUserId());
        if (ok) {
            show(Alert.AlertType.INFORMATION, "Cancelled", "Reservation cancelled.");
            onBack();
        } else {
            show(Alert.AlertType.ERROR, "Failed", "Cannot cancel this reservation.");
        }
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) itemsTable.getScene().getWindow();
        stage.close();
    }

    private void applyStatusPill(ReservationStatut statut) {
        if (statut == null) statut = ReservationStatut.ENATTENTE;

        String raw = statut.name();
        String text;
        String bg;

        switch (raw) {
            case "CONFIRME" -> { text = "BOOKED"; bg = "rgba(34,197,94,0.15)"; }
            case "ENATTENTE" -> { text = "PENDING AGENCY"; bg = "rgba(234,179,8,0.15)"; }
            case "ANNULE" -> { text = "CANCELLED"; bg = "rgba(239,68,68,0.15)"; }
            case "PANIER" -> { text = "CART"; bg = "rgba(100,116,139,0.15)"; }
            default -> { text = raw; bg = "rgba(100,116,139,0.15)"; }
        }

        statusLbl.setText(text);
        statusLbl.setStyle("-fx-font-weight: bold; -fx-padding: 2 10; -fx-background-radius: 10; -fx-background-color: " + bg + ";");
    }

    private void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}