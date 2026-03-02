package Controllers;

import app.Session;
import entities.AgencyReservationLine;
import entities.AgencyStatut;
import entities.ReservationStatut;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import repositories.AgencyReservationsRepository;
import repositories.IAgencyReservationsRepository;

import java.time.format.DateTimeFormatter;

public class AgencyReservationsController {

    @FXML private TableView<AgencyReservationLine> table;

    @FXML private TableColumn<AgencyReservationLine, Number> colResId;
    @FXML private TableColumn<AgencyReservationLine, Number> colClient;
    @FXML private TableColumn<AgencyReservationLine, String> colOffer;
    @FXML private TableColumn<AgencyReservationLine, String> colPrice;
    @FXML private TableColumn<AgencyReservationLine, String> colResStatus;
    @FXML private TableColumn<AgencyReservationLine, String> colAgencyStatus;
    @FXML private TableColumn<AgencyReservationLine, String> colEnds;
    @FXML private TableColumn<AgencyReservationLine, AgencyReservationLine> colActions;

    private final IAgencyReservationsRepository repo = new AgencyReservationsRepository();
    private final repositories.UserRepository userRepo = new repositories.UserRepository();
    private final Services.OffreEmailService emailService = new Services.OffreEmailService();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        if (!Session.isAgency()) {
            show(Alert.AlertType.ERROR, "Access denied", "This page is for agencies only.");
            return;
        }
        setupColumns();
        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    private void setupColumns() {
        colResId.setCellValueFactory(p -> new javafx.beans.property.SimpleIntegerProperty(p.getValue().getIdReservation()));
        colClient.setCellValueFactory(p -> new javafx.beans.property.SimpleIntegerProperty(p.getValue().getIdClient()));
        colOffer.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getOfferTitle()));

        colPrice.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getPrixFinal() == null ? "-" : (p.getValue().getPrixFinal() + " TND")
        ));

        colResStatus.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getReservationStatut() == null ? "-" : p.getValue().getReservationStatut().name()
        ));

        colAgencyStatus.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getAgencyStatut() == null ? "-" : p.getValue().getAgencyStatut().name()
        ));

        colEnds.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getAgencyDecisionAt() == null ? "-" : fmt.format(p.getValue().getAgencyDecisionAt())
        ));

        colActions.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button confirmBtn = new Button("Confirm");
            private final Button rejectBtn  = new Button("Reject");
            private final HBox box = new HBox(10, confirmBtn, rejectBtn);

            {
                confirmBtn.getStyleClass().add("btn-primary");
                rejectBtn.getStyleClass().add("btn-action-danger");

                confirmBtn.setOnAction(e -> {
                    AgencyReservationLine x = getTableView().getItems().get(getIndex());

                    // Use concrete repository method for confirmedNow
                    AgencyReservationsRepository rrepo = (AgencyReservationsRepository) repo;

                    boolean confirmedNow = rrepo.approveLineAndConfirmIfReady(
                            Session.getUserId(),
                            x.getIdReservation(),
                            x.getIdOffre()
                    );

                    // Always reload to reflect latest status
                    load();

                    if (!confirmedNow) {
                        show(Alert.AlertType.INFORMATION, "Availability", "Confirmed ✅ (waiting other agencies)");
                        return;
                    }

                    // ✅ reservation just became CONFIRME now -> send email once
                    try {
                        String email = userRepo.findEmailByUserId(x.getIdClient());
                        if (email != null && !email.isBlank()) {
                            emailService.send(
                                    email,
                                    "Reservation Confirmed #" + x.getIdReservation(),
                                    "Your reservation #" + x.getIdReservation() + " has been confirmed. Payment: CASH."
                            );
                        }
                        show(Alert.AlertType.INFORMATION, "Reservation", "Auto-confirmed ✅ Email sent.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        show(Alert.AlertType.WARNING, "Email", "Reservation confirmed, but email failed.");
                    }
                });

                rejectBtn.setOnAction(e -> {
                    AgencyReservationLine x = getTableView().getItems().get(getIndex());

                    var choices = java.util.List.of(
                            "Not available",
                            "Dates not possible",
                            "Offer ended",
                            "Maintenance",
                            "Other..."
                    );

                    ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
                    dialog.setTitle("Reject request");
                    dialog.setHeaderText("Choose a reason");
                    dialog.setContentText("Reason:");
                    var result = dialog.showAndWait();
                    if (result.isEmpty()) return;

                    String reason = result.get();
                    if ("Other...".equals(reason)) {
                        TextInputDialog t = new TextInputDialog();
                        t.setTitle("Reject request");
                        t.setHeaderText("Write the reason");
                        t.setContentText("Reason:");
                        var r2 = t.showAndWait();
                        if (r2.isEmpty() || r2.get().trim().isEmpty()) return;
                        reason = r2.get().trim();
                    }

                    boolean ok = repo.rejectLine(Session.getUserId(), x.getIdReservation(), x.getIdOffre(), reason);
                    load();
                    show(Alert.AlertType.INFORMATION, "Availability", ok ? "Rejected ✅" : "Failed.");
                });
            }

            @Override
            protected void updateItem(AgencyReservationLine x, boolean empty) {
                super.updateItem(x, empty);
                if (empty || x == null) {
                    setGraphic(null);
                    return;
                }

                boolean pending = x.getAgencyStatut() == AgencyStatut.ENATTENTE;
                boolean reservPending = x.getReservationStatut() == ReservationStatut.ENATTENTE;

                confirmBtn.setDisable(!(pending && reservPending));
                rejectBtn.setDisable(!(pending && reservPending));

                setGraphic(box);
            }
        });
    }

    private void load() {
        var data = repo.findLinesForAgency(Session.getUserId());
        table.setItems(FXCollections.observableArrayList(data));
    }

    private void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}