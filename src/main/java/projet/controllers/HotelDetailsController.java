package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import projet.entites.Hotel;
import projet.entites.user;
import projet.services.ServiceService;
// Ensure this matches your package structure

import java.io.IOException;
import java.sql.SQLException;

public class HotelDetailsController {
    user connectedUser=new user("achref","souli","user");
    //user connectedUser=new user("achref","souli","user");
    // Link to FXML IDs defined in hotelDetails.fxml
    @FXML private Label lblNom;
    @FXML private Label lblLocalisation;
    @FXML private Label lblPrix;
    @FXML private Label lblEtoiles;
    @FXML private Label lblChambre;
    @FXML private Label lblCapacite;
    @FXML private Label lblStatus;
    @FXML private Label lblDescription;
    @FXML private Button btnReserver;
    @FXML
    private Button retourBtn;
    int id;
    ServiceService HotelService = new ServiceService();

    /**
     * This method is called from ServicesController to populate the view
     */
    public void setHotelData(Hotel hotel) {
        // 1. Set Basic Text
        lblNom.setText(hotel.getNom());
        lblLocalisation.setText(hotel.getLocalisation());
        lblPrix.setText(hotel.getPrix() + " TND");
        lblDescription.setText(hotel.getDescription());

        // Handle Integer conversion
        lblCapacite.setText(String.valueOf(hotel.getCapacite()) + " Personnes");

        // Handle Field Name variations (Adjust 'getTypeChambre' to your Model's getter)
        // If your model uses 'getChambre()', change line below:
        lblChambre.setText(hotel.getChambre());

        // 2. Generate Star Visuals (e.g., 5 -> ★★★★★)
        StringBuilder stars = new StringBuilder();
        int starCount = hotel.getNbEtoiles(); // Assuming getter is getNbEtoiles()
        for(int i = 0; i < starCount; i++) {
            stars.append("★");
        }
        lblEtoiles.setText(stars.toString());
        // Style the stars yellow/gold
        lblEtoiles.setStyle("-fx-text-fill: #f6c750; -fx-font-size: 16px;");

        // 3. Status Logic & Coloring
        // Assuming your model has getStatus() returning "Active", "Pending", etc.
        if (hotel.getDisponibilite()){
            String status = "disponible";
            lblStatus.setText(status);
            lblStatus.setStyle("-fx-text-fill: #4ba3a1; -fx-font-weight: bold;"); // Brand Teal
        }
        else {
            String status = "no disponible";
            lblStatus.setText(status);
            lblStatus.setStyle("-fx-text-fill: #f6c750; -fx-font-weight: bold;");
        }
        retourBtn.setOnAction(e -> {handleBack();});
        if (connectedUser.getType().equals("admin")) {
            btnReserver.setVisible(false);
        }
        btnReserver.setOnAction(e -> {handleReserver();});
        try {
            id=HotelService.getId(hotel.getNom());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            Parent root = loader.load();
            retourBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleReserver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationForm.fxml"));
            Parent root = loader.load();
            AddReservationController addReservationController = loader.getController();
            addReservationController.setIdService(id);
            retourBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}