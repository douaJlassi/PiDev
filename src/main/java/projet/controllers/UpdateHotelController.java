package projet.controllers;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import projet.entites.Hotel;
import projet.entites.vol;
import projet.services.HotelService;
import projet.services.VolService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class UpdateHotelController {
        @FXML
        private CheckBox cbDisponibilite;
        @FXML
        private TextField tfCapacite;
        @FXML
        private TextField tfChambre;
        @FXML
        private TextField tfDescription;
        @FXML
        private TextField tfLocalisation;
        @FXML
        private TextField tfNbEtoiles;
        @FXML
        private TextField tfNom;
        @FXML
        private TextField tfPrix;
    private Hotel currentService; // To hold the ID for updating later


    public void setServiceData(Hotel service) {
        this.currentService = service;

        tfNom.setText(service.getNom());
        tfPrix.setText(String.valueOf(service.getPrix()));
        tfCapacite.setText(String.valueOf(service.getCapacite()));
        tfDescription.setText(service.getDescription());
        cbDisponibilite.setSelected(service.getDisponibilite());
        tfNbEtoiles.setText(String.valueOf(service.getNbEtoiles()));
       tfChambre.setText(String.valueOf(service.getChambre()));
       tfLocalisation.setText(String.valueOf(service.getLocalisation()));


    }

    @FXML
    private void handleUpdate() {

        String newName = tfNom.getText();
        double newPrice = Double.parseDouble(tfPrix.getText());
        String newdescription=tfDescription.getText();
        int newnbetoiles = Integer.parseInt(tfNbEtoiles.getText());
        int newcapacite=Integer.parseInt(tfCapacite.getText());
        boolean newdisponibilite=cbDisponibilite.isSelected();
String newlocalisation=tfLocalisation.getText();
String newtypechambre=tfChambre.getText();
        HotelService vs=new HotelService();
        Hotel v = new Hotel(newName,newdescription,newPrice,newdisponibilite,newcapacite,"hotel",newnbetoiles,newlocalisation,newtypechambre);
        try {
            vs.updateOne(currentService.getNom(),v);
            Parent dashboardView = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));

            // Find the main content area of the application
            // This assumes your main layout has a StackPane with fx:id="contentArea"
            StackPane contentArea = (StackPane) tfNom.getScene().lookup("#contentArea");

            if (contentArea != null) {
                // Replace the update form with the dashboard view
                contentArea.getChildren().setAll(dashboardView);
            } else {
                // Fallback if contentArea is not found (replaces the whole window)
                tfNom.getScene().setRoot(dashboardView);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    }


