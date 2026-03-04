package controllers;

import entities.Person;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import entities.reservation;
import entities.user;
import entities.vol;
import services.ReservationService;
import services.ServiceService;
import services.VolService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AddReservationController {
    String messageErrorNom="";
    String messageErrorMethode="";
    String messageErrorDate="";
    private static final String STYLE_AVAILABLE =
            "-fx-background-color: #2ecc71; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-min-width: 42px; -fx-min-height: 38px; " +
                    "-fx-background-radius: 6 6 2 2; -fx-cursor: hand;";

    private static final String STYLE_BOOKED =
            "-fx-background-color: #e74c3c; -fx-text-fill: #888888; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-min-width: 42px; -fx-min-height: 38px; " +
                    "-fx-background-radius: 6 6 2 2; -fx-opacity: 0.6;";

    private static final String STYLE_SELECTED =
            "-fx-background-color: #3498db; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-min-width: 42px; -fx-min-height: 38px; " +
                    "-fx-background-radius: 6 6 2 2; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, #3498db, 8, 0.5, 0, 0);";
    private Integer selectedSeatNumber = null;
    @FXML private TextField tfModePaiement;
    @FXML private DatePicker dpDateReservation;
    @FXML private VBox errorContainer;
    @FXML private TextField tfNom;
    @FXML private ComboBox<Integer> cbSiege;
    @FXML private Label lblSiege;
    @FXML private VBox seatMapContainer;
    @FXML private FlowPane seatGrid;
    @FXML private Label lblSelectedSeat;
    @FXML private Label lblErrorMethode;
    @FXML private Label lblErrorNom;
    @FXML private Label lblErrorDate;
    @FXML private Label lblErrorMetier;
    private int idService;
    private String Type;
    VolService volService = new VolService();
    String username;
    String type;
    void setConnected(String username, String type) {
        this.username = username;
        this.type=type;
        tfNom.setText(username);
        if (Type.equals("vol")) {
            seatMapContainer.setVisible(true);
            seatMapContainer.setManaged(true);
            try {
                vol v=volService.selectById(idService);
                loadSeatMap();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }
    }
    public void setType(String Type){
        this.Type = Type;

    }
    public void setIdService(int id){this.idService = id;}



    @FXML
    void AddReservation(ActionEvent event) {
        if (isInputValid()) {
            String nom = tfNom.getText();
            String modePaiement = tfModePaiement.getText();
            LocalDate dateReservationValue = dpDateReservation.getValue();
            java.sql.Date sqlDateArrive = java.sql.Date.valueOf(dateReservationValue);
            String statut = "en attente";
            System.out.println(idService);
            ReservationService service = new ReservationService();
            reservation reservation = "vol".equals(Type)
                    ? new reservation("en attente", sqlDateArrive, idService, modePaiement, nom, selectedSeatNumber)
                    : new reservation("en attente", sqlDateArrive, idService, modePaiement, nom, -1);
            try {
                service.validerReservation(reservation, Type);
            } catch (ReservationService.ReservationException e) {
                afficherErreurMetier(e.getMessage());
                return;
            } catch (SQLException e) {
                afficherErreurMetier("Erreur base de données : " + e.getMessage());
                return;
            }

                try {
                    service.insertOne(reservation);
                    ServiceService serviceService = new ServiceService();
                    serviceService.DecrementCapacite(idService);
                    handleBack();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }



        }
}
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mainpage.fxml"));
            Parent root = loader.load();
            tfModePaiement.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadSeatMap() {
        seatGrid.getChildren().clear();
       //selectedSeatNumber = null;
        lblSelectedSeat.setText("Aucun siège sélectionné");

        try {
            VolService volService = new VolService();
            vol v = volService.selectById(idService);
            ReservationService service = new ReservationService();
             java.util.List<Integer> ls = service.selectSeats(idService);
            int capacity = v.getCapacite()+ ls.size();
            ReservationService reservationService = new ReservationService();
            List<Integer> bookedSeats = reservationService.selectSeats(idService);

            for (int i = 1; i <= capacity; i++) {
                final int seatNumber = i;
                Button seatBtn = new Button(String.valueOf(i));
                if (bookedSeats.contains(i)) {
                    seatBtn.setStyle(STYLE_BOOKED);
                    seatBtn.setDisable(true);
                } else {
                    seatBtn.setStyle(STYLE_AVAILABLE);
                    seatBtn.setOnAction(e -> handleSeatClick(seatBtn, seatNumber));
                }

                seatGrid.getChildren().add(seatBtn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void handleSeatClick(Button clickedBtn, int seatNumber) {
        // Deselect previously selected seat
        if (selectedSeatNumber != null) {
            seatGrid.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .filter(btn -> btn.getText().equals(String.valueOf(selectedSeatNumber)))
                    .findFirst()
                    .ifPresent(btn -> btn.setStyle(STYLE_AVAILABLE));
        }

        // If clicking the same seat again → deselect
        if (seatNumber == (selectedSeatNumber != null ? selectedSeatNumber : -1)) {
            selectedSeatNumber = null;
            lblSelectedSeat.setText("Aucun siège sélectionné");
            return;
        }

        // Select the new seat
        selectedSeatNumber = seatNumber;
        clickedBtn.setStyle(STYLE_SELECTED);
        System.out.println(selectedSeatNumber);
        lblSelectedSeat.setText("Siège sélectionné : " + seatNumber);
        errorContainer.getChildren().clear(); // clear any seat error
    }
    private  boolean isNomValid() {
        if (tfNom.getText() == null || tfNom.getText().trim().isEmpty()) {
            tfNom.getStyleClass().add("error");
            messageErrorNom="Le nom ne peut pas être vide" ;
            return false;
        } else if (tfNom.getText().trim().length()<3) {
            messageErrorNom="Le nom doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isMethodeValid() {
        if (tfModePaiement.getText() == null || tfModePaiement.getText().trim().isEmpty()) {
            tfModePaiement.getStyleClass().add("error");
            messageErrorMethode="La methode ne peut pas être vide" ;
            return false;
        } else if (tfModePaiement.getText().trim().length()<3) {
            messageErrorMethode="La methode doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isDateValid() {
        if (dpDateReservation.getValue() == null) {
            dpDateReservation.getStyleClass().add("error");
            messageErrorDate="La date ne peut pas être vide" ;
            return false;
        } else if (dpDateReservation.getValue().isBefore(LocalDate.now())) {
            messageErrorDate="Date already passed" ;
            return false;
        }
        return true;
    }
    private boolean isInputValid() {
        resetStyles();
        if (isNomValid()&&isMethodeValid()&&isDateValid()) {
            lblErrorNom.setVisible(false);
            lblErrorNom.setManaged(false);
            lblErrorMethode.setVisible(false);
            lblErrorMethode.setManaged(false);
            lblErrorDate.setVisible(false);
            lblErrorDate.setManaged(false);
            return true;
        }
        else {
            if (!isNomValid()){
                lblErrorNom.setText(messageErrorNom);
                lblErrorNom.setVisible(true);
                lblErrorNom.setManaged(true);
            }
            if (!isMethodeValid()){
                lblErrorMethode.setText(messageErrorMethode);
                lblErrorMethode.setVisible(true);
                lblErrorMethode.setManaged(true);
            }
            if (!isDateValid()){
                lblErrorDate.setText(messageErrorDate);
                lblErrorDate.setVisible(true);
                lblErrorDate.setManaged(true);
            }




            return false;
        }

    }
    private void resetStyles() {
        lblErrorNom.setVisible(false);
        lblErrorNom.setManaged(false);
        lblErrorDate.setVisible(false);
        lblErrorDate.setManaged(false);
        lblErrorMethode.setVisible(false);
        lblErrorMethode.setManaged(false);
    }
    private void afficherErreurMetier(String message) {
        if (lblErrorMetier != null) {
            lblErrorMetier.setText(message);
            lblErrorMetier.setVisible(true);
            lblErrorMetier.setManaged(true);
            // Style rouge bien visible
            lblErrorMetier.setStyle(
                    "-fx-text-fill: #c0392b; -fx-font-weight: bold; " +
                            "-fx-background-color: #fde8e8; -fx-padding: 8px; " +
                            "-fx-background-radius: 6px; -fx-border-color: #e74c3c; " +
                            "-fx-border-radius: 6px; -fx-border-width: 1px;"
            );
        }
    }
    private void cacherErreurMetier() {
        if (lblErrorMetier != null) {
            lblErrorMetier.setVisible(false);
            lblErrorMetier.setManaged(false);
        }
    }

}
