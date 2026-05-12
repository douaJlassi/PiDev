package controllers;

import app.Session;
import entities.AgencyReservationLine;
import entities.AgencyStatut;
import entities.ReservationStatut;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import repositories.AgencyReservationsRepository;
import repositories.IAgencyReservationsRepository;
import repositories.UserRepository;
import services.OffreEmailService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgencyReservationsController {

    @FXML
    private TableView<AgencyReservationLine> table;

    @FXML
    private TableColumn<AgencyReservationLine, Number> colResId;

    @FXML
    private TableColumn<AgencyReservationLine, String> colClient;

    @FXML
    private TableColumn<AgencyReservationLine, String> colOffer;

    @FXML
    private TableColumn<AgencyReservationLine, String> colPrice;

    @FXML
    private TableColumn<AgencyReservationLine, String> colResStatus;

    @FXML
    private TableColumn<AgencyReservationLine, String> colAgencyStatus;

    @FXML
    private TableColumn<AgencyReservationLine, String> colEnds;

    @FXML
    private TableColumn<AgencyReservationLine, AgencyReservationLine> colActions;

    @FXML
    private TableColumn<AgencyReservationLine, String> colPhone;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    private final IAgencyReservationsRepository repo =
            new AgencyReservationsRepository();

    private final UserRepository userRepo =
            new UserRepository();

    private final OffreEmailService offreEmailService =
            new OffreEmailService();

    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private List<AgencyReservationLine> master =
            Collections.emptyList();

    @FXML
    public void initialize() {

        if (!Session.isAgency()) {
            show(
                    Alert.AlertType.ERROR,
                    "Access denied",
                    "This page is for agencies only."
            );
            return;
        }

        setupColumns();

        if (statusFilter != null) {
            statusFilter.setItems(
                    FXCollections.observableArrayList(
                            "ALL",
                            "PENDING",
                            "CONFIRMED",
                            "REJECTED"
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

        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    private void setupColumns() {

        colResId.setCellValueFactory(
                p -> new SimpleIntegerProperty(
                        p.getValue().getIdReservation()
                )
        );

        colClient.setCellValueFactory(
                p -> new SimpleStringProperty(
                        p.getValue().getClientName() == null
                                ? "Client"
                                : p.getValue().getClientName()
                )
        );

        colOffer.setCellValueFactory(
                p -> new SimpleStringProperty(
                        p.getValue().getOfferTitle() == null
                                ? "-"
                                : p.getValue().getOfferTitle()
                )
        );

        colPrice.setCellValueFactory(
                p -> new SimpleStringProperty(
                        p.getValue().getPrixFinal() == null
                                ? "-"
                                : p.getValue().getPrixFinal() + " TND"
                )
        );

        colResStatus.setCellValueFactory(
                p -> new SimpleStringProperty(
                        p.getValue().getReservationStatut() == null
                                ? "-"
                                : p.getValue().getReservationStatut().name()
                )
        );

        colAgencyStatus.setCellValueFactory(
                p -> new SimpleStringProperty(
                        p.getValue().getAgencyStatut() == null
                                ? "-"
                                : p.getValue().getAgencyStatut().name()
                )
        );

        colEnds.setCellValueFactory(
                p -> new SimpleStringProperty(
                        p.getValue().getAgencyDecisionAt() == null
                                ? "-"
                                : fmt.format(
                                p.getValue().getAgencyDecisionAt()
                        )
                )
        );

        if (colPhone != null) {
            colPhone.setCellValueFactory(
                    p -> new SimpleStringProperty(
                            p.getValue().getClientPhone() == null
                                    ? "-"
                                    : p.getValue().getClientPhone()
                    )
            );
        }

        colActions.setCellValueFactory(
                p -> new SimpleObjectProperty<>(
                        p.getValue()
                )
        );

        colActions.setCellFactory(
                col -> new TableCell<>() {

                    private final Button confirmBtn =
                            new Button("Confirm");

                    private final Button rejectBtn =
                            new Button("Reject");

                    private final HBox box =
                            new HBox(10, confirmBtn, rejectBtn);

                    {
                        confirmBtn.getStyleClass().add("btn-primary");
                        rejectBtn.getStyleClass().add("btn-danger");

                        confirmBtn.setOnAction(e -> {
                            AgencyReservationLine x =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());

                            AgencyReservationsRepository concreteRepo =
                                    (AgencyReservationsRepository) repo;

                            boolean confirmed =
                                    concreteRepo.approveLineAndConfirmIfReady(
                                            Session.getUserId(),
                                            x.getIdReservation(),
                                            x.getIdOffre()
                                    );

                            load();

                            if (confirmed) {
                                sendConfirmationEmailSafe(x);

                                show(
                                        Alert.AlertType.INFORMATION,
                                        "Reservation",
                                        "Reservation confirmed ✅"
                                );
                            } else {
                                show(
                                        Alert.AlertType.WARNING,
                                        "Reservation",
                                        "Could not confirm this reservation."
                                );
                            }
                        });

                        rejectBtn.setOnAction(e -> {
                            AgencyReservationLine x =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());

                            String reason =
                                    showStyledReasonDialog();

                            if (reason == null) {
                                return;
                            }

                            if (reason.trim().isEmpty()) {
                                return;
                            }

                            boolean ok =
                                    repo.rejectLine(
                                            Session.getUserId(),
                                            x.getIdReservation(),
                                            x.getIdOffre(),
                                            reason.trim()
                                    );

                            load();

                            show(
                                    ok
                                            ? Alert.AlertType.INFORMATION
                                            : Alert.AlertType.WARNING,
                                    "Reservation",
                                    ok
                                            ? "Reservation rejected ✅"
                                            : "Could not reject this reservation."
                            );
                        });
                    }

                    @Override
                    protected void updateItem(
                            AgencyReservationLine x,
                            boolean empty
                    ) {
                        super.updateItem(x, empty);

                        if (empty || x == null) {
                            setGraphic(null);
                            return;
                        }

                        boolean reservationPending =
                                isReservationPending(x);

                        confirmBtn.setDisable(!reservationPending);
                        rejectBtn.setDisable(!reservationPending);

                        setGraphic(box);
                    }
                }
        );
    }

    private boolean isReservationPending(AgencyReservationLine x) {

        if (x == null || x.getReservationStatut() == null) {
            return false;
        }

        String status =
                x.getReservationStatut().name();

        return status.equalsIgnoreCase("PENDING")
                || status.equalsIgnoreCase("ENATTENTE");
    }

    private void sendConfirmationEmailSafe(
            AgencyReservationLine x
    ) {

        try {

            String email =
                    userRepo.findEmailByUserId(
                            x.getIdClient()
                    );

            if (email == null || email.isBlank()) {
                return;
            }

            String clientName =
                    x.getClientName() == null ||
                            x.getClientName().isBlank()
                            ? "Client"
                            : x.getClientName();

            String subject =
                    "Rehletna - Reservation Confirmed ✅ (#"
                            + x.getIdReservation()
                            + ")";

            String body =
                    buildConfirmationEmailBody(
                            x,
                            clientName
                    );

            offreEmailService.send(
                    email,
                    subject,
                    body
            );

        } catch (Exception ex) {
            ex.printStackTrace();

            show(
                    Alert.AlertType.WARNING,
                    "Email",
                    "Reservation confirmed, but email failed."
            );
        }
    }

    private String buildConfirmationEmailBody(
            AgencyReservationLine x,
            String clientName
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("Hello ")
                .append(
                        clientName == null ||
                                clientName.isBlank()
                                ? "Client"
                                : clientName
                )
                .append(",\n\n");

        sb.append("✅ Good news! Your reservation has been confirmed.\n\n");

        sb.append("Reservation ID: ")
                .append(x.getIdReservation())
                .append("\n");

        sb.append("Offer: ")
                .append(
                        x.getOfferTitle() == null
                                ? "-"
                                : x.getOfferTitle()
                )
                .append("\n");

        sb.append("Total amount: ")
                .append(
                        x.getPrixFinal() == null
                                ? "-"
                                : x.getPrixFinal() + " TND"
                )
                .append("\n");

        sb.append("\nThank you for using Rehletna.\n");

        sb.append("We wish you a great experience!\n");

        return sb.toString();
    }

    private String showStyledReasonDialog() {

        Dialog<String> dialog =
                new Dialog<>();

        dialog.setTitle("Reject request");

        dialog.setHeaderText(
                "Write the reason shown to the client"
        );

        ButtonType sendBtn =
                new ButtonType(
                        "Use this reason",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        sendBtn,
                        ButtonType.CANCEL
                );

        VBox root =
                new VBox(10);

        root.setStyle(
                "-fx-padding: 14; -fx-background-color: white;"
        );

        Label hint =
                new Label(
                        "Tip: choose a template then edit it."
                );

        hint.setStyle(
                "-fx-text-fill: #64748B; -fx-font-size: 11px;"
        );

        HBox templates =
                new HBox(8);

        templates.setStyle(
                "-fx-padding: 4 0 2 0;"
        );

        Button t1 =
                new Button("No availability");

        Button t2 =
                new Button("Dates not possible");

        Button t3 =
                new Button("Offer ended");

        Button t4 =
                new Button("Maintenance");

        for (
                Button b :
                new Button[]{t1, t2, t3, t4}
        ) {

            b.setStyle(
                    "-fx-background-color: rgba(27,183,177,0.12); " +
                            "-fx-text-fill: #0F172A; " +
                            "-fx-background-radius: 10; " +
                            "-fx-cursor: hand; " +
                            "-fx-font-weight: bold;"
            );
        }

        TextArea area =
                new TextArea();

        area.setPromptText(
                "Example: Unfortunately, this offer is not available for the requested dates."
        );

        area.setWrapText(true);

        area.setPrefRowCount(4);

        area.setStyle(
                "-fx-background-radius: 10; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-color: #E2E8F0;"
        );

        t1.setOnAction(
                e -> area.setText(
                        "Unfortunately, this offer has no availability for this period."
                )
        );

        t2.setOnAction(
                e -> area.setText(
                        "Unfortunately, the requested dates are not available for this offer."
                )
        );

        t3.setOnAction(
                e -> area.setText(
                        "This offer is no longer available. Please choose another option."
                )
        );

        t4.setOnAction(
                e -> area.setText(
                        "This offer is temporarily unavailable due to maintenance."
                )
        );

        templates.getChildren()
                .addAll(t1, t2, t3, t4);

        root.getChildren()
                .addAll(hint, templates, area);

        dialog.getDialogPane()
                .setContent(root);

        Node okBtn =
                dialog.getDialogPane()
                        .lookupButton(sendBtn);

        okBtn.setDisable(true);

        area.textProperty()
                .addListener(
                        (obs, oldV, newV) ->
                                okBtn.setDisable(
                                        newV == null ||
                                                newV.trim().isEmpty()
                                )
                );

        dialog.setResultConverter(
                btn -> btn == sendBtn
                        ? area.getText()
                        : null
        );

        return dialog.showAndWait()
                .orElse(null);
    }

    private void load() {

        master =
                repo.findLinesForAgency(
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

        String st =
                statusFilter == null ||
                        statusFilter.getValue() == null
                        ? "ALL"
                        : statusFilter.getValue();

        List<AgencyReservationLine> filtered =
                new ArrayList<>();

        for (AgencyReservationLine x : master) {

            if (!"ALL".equalsIgnoreCase(st)) {

                if (x.getReservationStatut() == null) {
                    continue;
                }

                if (
                        !x.getReservationStatut()
                                .name()
                                .equalsIgnoreCase(st)
                ) {
                    continue;
                }
            }

            if (!q.isEmpty()) {

                String client =
                        x.getClientName() == null
                                ? ""
                                : x.getClientName()
                                .toLowerCase();

                String offer =
                        x.getOfferTitle() == null
                                ? ""
                                : x.getOfferTitle()
                                .toLowerCase();

                String phone =
                        x.getClientPhone() == null
                                ? ""
                                : x.getClientPhone()
                                .toLowerCase();

                String reason =
                        x.getRefusalReason() == null
                                ? ""
                                : x.getRefusalReason()
                                .toLowerCase();

                String resSt =
                        x.getReservationStatut() == null
                                ? ""
                                : x.getReservationStatut()
                                .name()
                                .toLowerCase();

                boolean match =
                        client.contains(q)
                                || offer.contains(q)
                                || reason.contains(q)
                                || phone.contains(q)
                                || resSt.contains(q);

                if (!match) {
                    continue;
                }
            }

            filtered.add(x);
        }

        table.setItems(
                FXCollections.observableArrayList(
                        filtered
                )
        );
    }

    @FXML
    private void onOpenAnalytics() {

        try {

            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                            getClass().getResource(
                                    "/fxml/AgencyAnalytics.fxml"
                            )
                    );

            javafx.scene.Parent root =
                    loader.load();

            Stage stage =
                    new Stage();

            stage.setTitle("Analytics");

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

        } catch (Exception ex) {

            ex.printStackTrace();

            show(
                    Alert.AlertType.ERROR,
                    "Analytics",
                    "Cannot open analytics: "
                            + ex.getMessage()
            );
        }
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
}