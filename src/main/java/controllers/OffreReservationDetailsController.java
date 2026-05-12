package controllers;

import app.Session;
import entities.CartItem;
import entities.ReservationStatut;
import entities.ReservationSummary;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import repositories.ReservationRepository;

import java.time.format.DateTimeFormatter;

public class OffreReservationDetailsController {

    @FXML
    private Label titleLbl;

    @FXML
    private Label statusLbl;

    @FXML
    private Label totalLbl;

    @FXML
    private Label payLbl;

    @FXML
    private Label dateLbl;

    @FXML
    private TableView<CartItem> itemsTable;

    @FXML
    private TableColumn<CartItem, String> colTitle;

    @FXML
    private TableColumn<CartItem, String> colPrice;

    @FXML
    private TableColumn<CartItem, Object> colAgencyStatus;

    @FXML
    private TableColumn<CartItem, Void> colAction;

    @FXML
    private TableColumn<CartItem, String> colReason;

    @FXML
    private TableColumn<CartItem, String> colDecisionAt;

    @FXML
    private Button cancelBtn;

    private final ReservationRepository repo =
            new ReservationRepository();

    private ReservationSummary reservation;

    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void init(ReservationSummary r) {

        this.reservation = r;

        titleLbl.setText(
                "Reservation #" + r.getIdReservation()
        );

        totalLbl.setText(
                "Total: " +
                        (
                                r.getMontantTotal() == null
                                        ? "-"
                                        : r.getMontantTotal() + " TND"
                        )
        );

        payLbl.setText(
                "Payment: " +
                        (
                                r.getModePaiement() == null
                                        ? "CASH"
                                        : r.getModePaiement().toUpperCase()
                        )
        );

        dateLbl.setText(
                "Date: " +
                        (
                                r.getDateReservation() == null
                                        ? "-"
                                        : r.getDateReservation().format(fmt)
                        )
        );

        applyStatusPill(r.getStatut());

        setupColumns();

        refreshItems();

        boolean canCancel =
                isPending(r.getStatut());

        if (cancelBtn != null) {
            cancelBtn.setDisable(!canCancel);
        }
    }

    private void setupColumns() {

        colTitle.setCellValueFactory(
                p -> new javafx.beans.property.SimpleStringProperty(
                        buildItemTitle(p.getValue())
                )
        );

        colPrice.setCellValueFactory(
                p -> new javafx.beans.property.SimpleStringProperty(
                        p.getValue().getPrixUnitaire() == null
                                ? "-"
                                : p.getValue().getPrixUnitaire() + " TND"
                )
        );

        colDecisionAt.setCellValueFactory(
                p -> new javafx.beans.property.SimpleStringProperty(
                        p.getValue().getAgencyDecisionAt() == null
                                ? "-"
                                : p.getValue().getAgencyDecisionAt().format(fmt)
                )
        );

        colReason.setCellValueFactory(
                p -> new javafx.beans.property.SimpleStringProperty(
                        p.getValue().getRefusalReason() == null ||
                                p.getValue().getRefusalReason().isBlank()
                                ? "-"
                                : p.getValue().getRefusalReason()
                )
        );

        colReason.setCellFactory(col -> new TableCell<>() {

            private final Label label =
                    new Label();

            {
                label.setWrapText(true);
                label.setStyle(
                        "-fx-text-fill: #475569; -fx-padding: 6 0;"
                );
                setPrefHeight(Control.USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(
                    String item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    label.setText(null);
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
            }
        });

        colAgencyStatus.setCellValueFactory(
                p -> new javafx.beans.property.SimpleObjectProperty<>(
                        p.getValue().getAgencyStatus()
                )
        );

        colAgencyStatus.setCellFactory(col -> new TableCell<>() {

            @Override
            protected void updateItem(
                    Object item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                String normalized =
                        normalizeStatus(item.toString());

                String text;
                String bg;

                switch (normalized) {

                    case "CONFIRMED" -> {
                        text = "APPROVED";
                        bg = "rgba(34,197,94,0.15)";
                    }

                    case "REJECTED" -> {
                        text = "REJECTED";
                        bg = "rgba(239,68,68,0.15)";
                    }

                    case "PENDING" -> {
                        text = "PENDING";
                        bg = "rgba(234,179,8,0.15)";
                    }

                    default -> {
                        text = normalized;
                        bg = "rgba(100,116,139,0.15)";
                    }
                }

                Label lb =
                        new Label(text);

                lb.setStyle(
                        "-fx-font-weight: bold; " +
                                "-fx-padding: 2 10; " +
                                "-fx-background-radius: 10; " +
                                "-fx-background-color: " + bg + ";"
                );

                HBox h =
                        new HBox(lb);

                h.setAlignment(
                        javafx.geometry.Pos.CENTER
                );

                setGraphic(h);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button removeBtn =
                    new Button("Remove");

            {
                removeBtn.getStyleClass().add("btn-action-danger");

                removeBtn.setStyle(
                        "-fx-font-size: 11px; -fx-padding: 4 10;"
                );

                removeBtn.setOnAction(e -> {

                    CartItem it =
                            getTableView()
                                    .getItems()
                                    .get(getIndex());

                    boolean ok =
                            repo.removeLine(
                                    reservation.getIdReservation(),
                                    Session.getUserId(),
                                    it.getIdOffre()
                            );

                    if (!ok) {
                        show(
                                Alert.AlertType.ERROR,
                                "Failed",
                                "Cannot remove this line."
                        );
                    }

                    refreshItems();
                });
            }

            @Override
            protected void updateItem(
                    Void item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                CartItem it =
                        getTableView()
                                .getItems()
                                .get(getIndex());

                boolean canEdit =
                        isPending(reservation.getStatut());

                boolean rejected =
                        it != null &&
                                isRejected(it.getAgencyStatus());

                removeBtn.setDisable(
                        !(canEdit && rejected)
                );

                setGraphic(removeBtn);

                setAlignment(
                        javafx.geometry.Pos.CENTER
                );
            }
        });
    }

    private String buildItemTitle(CartItem item) {

        if (item == null) {
            return "-";
        }

        String title =
                item.getTitre() == null
                        ? "-"
                        : item.getTitre();

        String agency =
                item.getNomAgence() == null
                        ? ""
                        : item.getNomAgence();

        if (agency.isBlank()) {
            return title;
        }

        return title + "  •  " + agency;
    }

    private void refreshItems() {

        var items =
                repo.findReservationItems(
                        reservation.getIdReservation(),
                        Session.getUserId()
                );

        itemsTable.setItems(
                FXCollections.observableArrayList(items)
        );
    }

    @FXML
    private void onCancelRequest() {

        Alert confirm =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirm.setTitle("Cancel request");

        confirm.setHeaderText(null);

        confirm.setContentText(
                "Cancel this reservation request?"
        );

        if (
                confirm.showAndWait()
                        .orElse(ButtonType.CANCEL)
                        != ButtonType.OK
        ) {
            return;
        }

        boolean ok =
                repo.cancelReservation(
                        reservation.getIdReservation(),
                        Session.getUserId()
                );

        if (ok) {

            show(
                    Alert.AlertType.INFORMATION,
                    "Cancelled",
                    "Reservation cancelled."
            );

            onBack();

        } else {

            show(
                    Alert.AlertType.ERROR,
                    "Failed",
                    "Cannot cancel this reservation."
            );
        }
    }

    @FXML
    private void onBack() {

        Stage stage =
                (Stage)
                        itemsTable
                                .getScene()
                                .getWindow();

        stage.close();
    }

    private void applyStatusPill(
            ReservationStatut statut
    ) {

        String raw =
                statut == null
                        ? "PENDING"
                        : statut.name();

        String normalized =
                normalizeStatus(raw);

        String text;
        String bg;

        switch (normalized) {

            case "CONFIRMED" -> {
                text = "CONFIRMED";
                bg = "rgba(34,197,94,0.15)";
            }

            case "PENDING" -> {
                text = "PENDING";
                bg = "rgba(234,179,8,0.15)";
            }

            case "REJECTED" -> {
                text = "REJECTED";
                bg = "rgba(239,68,68,0.15)";
            }

            case "CANCELLED" -> {
                text = "CANCELLED";
                bg = "rgba(239,68,68,0.15)";
            }

            default -> {
                text = normalized;
                bg = "rgba(100,116,139,0.15)";
            }
        }

        statusLbl.setText(text);

        statusLbl.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-padding: 2 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-background-color: " + bg + ";"
        );
    }

    private boolean isPending(
            ReservationStatut statut
    ) {

        if (statut == null) {
            return false;
        }

        return normalizeStatus(statut.name())
                .equalsIgnoreCase("PENDING");
    }

    private boolean isRejected(
            String status
    ) {

        if (status == null) {
            return false;
        }

        return normalizeStatus(status)
                .equalsIgnoreCase("REJECTED");
    }

    private String normalizeStatus(
            String status
    ) {

        if (status == null) {
            return "";
        }

        String s =
                status.trim().toUpperCase();

        return switch (s) {

            case "ENATTENTE" -> "PENDING";

            case "CONFIRME" -> "CONFIRMED";

            case "APPROUVEE" -> "CONFIRMED";

            case "ANNULE" -> "CANCELLED";

            case "REFUSEE" -> "REJECTED";

            case "PANIER" -> "PENDING";

            default -> s;
        };
    }

    private void show(
            Alert.AlertType type,
            String title,
            String msg
    ) {

        Alert a =
                new Alert(type);

        a.setTitle(title);

        a.setHeaderText(null);

        a.setContentText(msg);

        a.showAndWait();
    }

    @FXML
    private void onRefresh() {
        refreshItems();
    }
}