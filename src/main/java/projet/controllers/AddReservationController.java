package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import projet.entites.reservation;
import projet.entites.vol;
import projet.services.ReservationService;
import projet.services.ServiceService;
import projet.services.VolService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddReservationController {
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
    @FXML
    private TextField tfModePaiement;
    @FXML
    private DatePicker dpDateReservation;
    @FXML
    private VBox errorContainer;
    @FXML
    private TextField tfNom;
    @FXML private ComboBox<Integer> cbSiege;
    @FXML private Label lblSiege;
    @FXML private VBox seatMapContainer;
    @FXML private FlowPane seatGrid;
    @FXML private Label lblSelectedSeat;
    private int idService;
    private String Type;
    VolService volService = new VolService();
    public void setType(String Type){
        this.Type = Type;
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
    public void setIdService(int id){this.idService = id;}

@FXML
void AddReservation(ActionEvent event)
{
    String nom = tfNom.getText();
    String modePaiement = tfModePaiement.getText();
    LocalDate dateReservationValue = dpDateReservation.getValue();
    java.sql.Date sqlDateArrive = java.sql.Date.valueOf(dateReservationValue);
    String statut="non acceptee";
    System.out.println(idService);
    ReservationService service = new ReservationService();
    reservation reservation = new reservation(statut,sqlDateArrive,idService,modePaiement,nom);
    try {
        service.insertOne(reservation);
        ServiceService serviceService = new ServiceService();
        serviceService.DecrementCapacite(idService);
        handleBack();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
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
            projet.entites.vol v = volService.selectById(idService);
            int capacity = v.getCapacite();

           /* ReservationService reservationService = new ReservationService();
            List<Integer> bookedSeats = reservationService.getBookedSeats(idService);*/

            for (int i = 1; i <= capacity; i++) {
                final int seatNumber = i;
                Button seatBtn = new Button(String.valueOf(i));
              /*  if (bookedSeats.contains(i)) {
                    // Taken — red, disabled
                    seatBtn.setStyle(STYLE_BOOKED);
                    seatBtn.setDisable(true);
                }
                else {
                    // Available — green, clickable
                    seatBtn.setStyle(STYLE_AVAILABLE);
                    seatBtn.setOnAction(e -> handleSeatClick(seatBtn, seatNumber));
                }*/

                seatGrid.getChildren().add(seatBtn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*private void handleSeatClick(Button clickedBtn, int seatNumber) {
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
        lblSelectedSeat.setText("Siège sélectionné : " + seatNumber);
        errorContainer.getChildren().clear(); // clear any seat error
    }*/
}
