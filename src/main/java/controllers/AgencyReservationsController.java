package controllers;

import app.Session;
import entities.AgencyReservationLine;
import entities.AgencyStatut;
import entities.ReservationStatut;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import repositories.AgencyReservationsRepository;
import repositories.IAgencyReservationsRepository;
import services.OffreEmailService;

import java.time.format.DateTimeFormatter;

public class AgencyReservationsController {

    @FXML private TableView<AgencyReservationLine> table;

    @FXML private TableColumn<AgencyReservationLine, Number> colResId;
    @FXML private TableColumn<AgencyReservationLine, String> colClient;
    @FXML private TableColumn<AgencyReservationLine, String> colOffer;
    @FXML private TableColumn<AgencyReservationLine, String> colPrice;
    @FXML private TableColumn<AgencyReservationLine, String> colResStatus;
    @FXML private TableColumn<AgencyReservationLine, String> colAgencyStatus;
    @FXML private TableColumn<AgencyReservationLine, String> colEnds;
    @FXML private TableColumn<AgencyReservationLine, AgencyReservationLine> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableColumn<AgencyReservationLine, String> colPhone;


    private final IAgencyReservationsRepository repo = new AgencyReservationsRepository();
    private final repositories.UserRepository userRepo = new repositories.UserRepository();
    private final OffreEmailService offreEmailService = new OffreEmailService();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private java.util.List<AgencyReservationLine> master = java.util.Collections.emptyList();

    @FXML
    public void initialize() {
        if (!Session.isAgency()) {
            show(Alert.AlertType.ERROR, "Access denied", "This page is for agencies only.");
            return;
        }
        setupColumns();
        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "ENATTENTE", "APPROUVEE", "REFUSEE"
        ));
        statusFilter.getSelectionModel().select("ALL");

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    private void setupColumns() {
        colResId.setCellValueFactory(p -> new javafx.beans.property.SimpleIntegerProperty(p.getValue().getIdReservation()));
        colClient.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(p.getValue().getClientName() == null ? "Client" : p.getValue().getClientName())
        );
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
        colPhone.setCellValueFactory(p ->
                new SimpleStringProperty(p.getValue().getClientPhone() == null ? "-" : p.getValue().getClientPhone())
        );
        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button confirmBtn = new Button("Confirm");
            private final Button rejectBtn  = new Button("Reject");
            private final HBox box = new HBox(10, confirmBtn, rejectBtn);

            {
                confirmBtn.getStyleClass().add("btn-primary");
                rejectBtn.getStyleClass().add("btn-danger");

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

                            String clientName = (x.getClientName() == null || x.getClientName().isBlank())
                                    ? "Client"
                                    : x.getClientName();

                            String subject = "Rehletna - Reservation Confirmed ✅ (#" + x.getIdReservation() + ")";
                            String body = buildConfirmationEmailBody(x.getIdReservation(), x.getIdClient(), clientName);

                            offreEmailService.send(email, subject, body);
                        }

                        show(Alert.AlertType.INFORMATION, "Reservation", "Auto-confirmed ✅ Email sent.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        show(Alert.AlertType.WARNING, "Email", "Reservation confirmed, but email failed.");
                    }
                });

                rejectBtn.setOnAction(e -> {
                    AgencyReservationLine x = getTableView().getItems().get(getIndex());

                    String reason = showStyledReasonDialog(); // always show the styled dialog
                    if (reason == null) return;               // cancelled
                    if (reason.trim().isEmpty()) return;      // empty

                    boolean ok = repo.rejectLine(Session.getUserId(), x.getIdReservation(), x.getIdOffre(), reason.trim());
                    load();
                    show(Alert.AlertType.INFORMATION, "Availability", ok ? "Rejected ✅" : "Failed.");
                });
            }
            private String buildConfirmationEmailBody(int reservationId, int idClient, String clientName) {
                var rr = new repositories.ReservationRepository();
                var items = rr.findReservationItems(reservationId, idClient);

                StringBuilder sb = new StringBuilder();
                sb.append("Hello ").append(clientName == null || clientName.isBlank() ? "Client" : clientName).append(",\n\n");
                sb.append("✅ Good news! Your reservation has been confirmed.\n\n");
                sb.append("Reservation ID: ").append(reservationId).append("\n");
                sb.append("Payment method: CASH\n");

                try {
                    var total = rr.getTotal(reservationId);
                    sb.append("Total amount: ").append(total).append(" TND\n");
                } catch (Exception ignored) {}

                sb.append("\nIncluded offers:\n");
                if (items == null || items.isEmpty()) {
                    sb.append("- (No items found)\n");
                } else {
                    for (var it : items) {
                        String agency = (it.getNomAgence() == null || it.getNomAgence().isBlank()) ? "Agency" : it.getNomAgence();
                        sb.append("- ").append(it.getTitre())
                                .append(" (").append(agency).append(")")
                                .append(" — ").append(it.getPrixUnitaire()).append(" TND")
                                .append("\n");
                    }
                }

                sb.append("\nThank you for using Rehletna.\n");
                sb.append("We wish you a great experience!\n");

                return sb.toString();
            }

            private String showStyledReasonDialog() {
                Dialog<String> dialog = new Dialog<>();
                dialog.setTitle("Reject request");
                dialog.setHeaderText("Write the reason (shown to the client)");

                // buttons
                ButtonType sendBtn = new ButtonType("Use this reason", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(sendBtn, ButtonType.CANCEL);

                // layout (card-like)
                VBox root = new VBox(10);
                root.setStyle("-fx-padding: 14; -fx-background-color: white;");

                Label hint = new Label("Tip: choose a template then edit it.");
                hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

                // templates (like “rectangles”)
                HBox templates = new HBox(8);
                templates.setStyle("-fx-padding: 4 0 2 0;");

                Button t1 = new Button("No availability");
                Button t2 = new Button("Dates not possible");
                Button t3 = new Button("Offer ended");
                Button t4 = new Button("Maintenance");

                for (Button b : new Button[]{t1, t2, t3, t4}) {
                    b.setStyle("-fx-background-color: rgba(27,183,177,0.12); -fx-text-fill: #0F172A; " +
                            "-fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold;");
                }

                TextArea area = new TextArea();
                area.setPromptText("Example: Unfortunately, this offer is not available for the requested dates.");
                area.setWrapText(true);
                area.setPrefRowCount(4);
                area.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #E2E8F0;");

                // template click behavior
                t1.setOnAction(e -> area.setText("Unfortunately, this offer has no availability for this period."));
                t2.setOnAction(e -> area.setText("Unfortunately, the requested dates are not available for this offer."));
                t3.setOnAction(e -> area.setText("This offer is no longer available (ended). Please choose another option."));
                t4.setOnAction(e -> area.setText("This offer is temporarily unavailable due to maintenance."));

                templates.getChildren().addAll(t1, t2, t3, t4);

                root.getChildren().addAll(hint, templates, area);

                dialog.getDialogPane().setContent(root);

                // disable OK until text not empty
                Node okBtn = dialog.getDialogPane().lookupButton(sendBtn);
                okBtn.setDisable(true);
                area.textProperty().addListener((obs, oldV, newV) -> okBtn.setDisable(newV == null || newV.trim().isEmpty()));

                dialog.setResultConverter(btn -> btn == sendBtn ? area.getText() : null);

                return dialog.showAndWait().orElse(null);
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

    /*private void load() {
        var data = repo.findLinesForAgency(Session.getUserId());
        table.setItems(FXCollections.observableArrayList(data));
    }*/
    private void load() {
        master = repo.findLinesForAgency(Session.getUserId());
        applyFilters();
    }

    private void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    private void applyFilters() {
        String q = (searchField == null || searchField.getText() == null)
                ? ""
                : searchField.getText().trim().toLowerCase();

        String st = (statusFilter == null || statusFilter.getValue() == null)
                ? "ALL"
                : statusFilter.getValue();

        var filtered = new java.util.ArrayList<AgencyReservationLine>();

        for (AgencyReservationLine x : master) {
            // filter by agency status
            if (!"ALL".equalsIgnoreCase(st)) {
                if (x.getAgencyStatut() == null) continue;
                if (!x.getAgencyStatut().name().equalsIgnoreCase(st)) continue;
            }

            // search text
            if (!q.isEmpty()) {
                String client = (x.getClientName() == null) ? "" : x.getClientName().toLowerCase();
                String offer  = (x.getOfferTitle() == null) ? "" : x.getOfferTitle().toLowerCase();
                String phone = (x.getClientPhone() == null) ? "" : x.getClientPhone().toLowerCase();
                String reason = (x.getRefusalReason() == null) ? "" : x.getRefusalReason().toLowerCase();
                String resSt  = (x.getReservationStatut() == null) ? "" : x.getReservationStatut().name().toLowerCase();

                boolean match = client.contains(q) || offer.contains(q) || reason.contains(q) || phone.contains(q) || resSt.contains(q);
                if (!match) continue;
            }

            filtered.add(x);
        }

        table.setItems(FXCollections.observableArrayList(filtered));
    }
    @FXML
    private void onOpenAnalytics() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/AgencyAnalytics.fxml")
            );
            javafx.scene.Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Analytics");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initOwner(table.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            show(Alert.AlertType.ERROR, "Analytics", "Cannot open analytics: " + ex.getMessage());
        }
    }
}