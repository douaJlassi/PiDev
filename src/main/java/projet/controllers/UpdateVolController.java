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
import java.time.LocalDate; // If you have dates

public class UpdateVolController {

    @FXML
    private CheckBox cbDisponibilite;

    @FXML
    private DatePicker dpDateArrivee;

    @FXML
    private DatePicker dpDateDepart;

    @FXML
    private TextArea taDescription;

    @FXML
    private TextField tfCapacite;

    @FXML
    private TextField tfNom;

    @FXML
    private TextField tfNumeroVol;

    @FXML
    private TextField tfPrix;

    @FXML
    private TextField tfVilleArrivee;

    @FXML
    private TextField tfVilleDepart;




    private vol currentService; // To hold the ID for updating later


    public void setServiceData(vol service) {
        this.currentService = service;

        tfNom.setText(service.getNom());
        tfPrix.setText(String.valueOf(service.getPrix()));
        tfCapacite.setText(String.valueOf(service.getCapacite()));
        taDescription.setText(service.getDescription());
        cbDisponibilite.setSelected(service.getDisponibilite());
        tfNumeroVol.setText(String.valueOf(service.getNumeroVol()));
        tfVilleDepart.setText(String.valueOf(service.getVilleDepart()));
        tfVilleArrivee.setText(String.valueOf(service.getVilleArrivee()));


    }

    @FXML
    private void handleUpdate() {
        // 2. Get modified data from TextFields
        String newName = tfNom.getText();
        double newPrice = Double.parseDouble(tfPrix.getText());
        String newdescription=taDescription.getText();
        String newnumeroVol = tfNumeroVol.getText();
        int newcapacite=Integer.parseInt(tfCapacite.getText());
        boolean newdisponibilite=cbDisponibilite.isSelected();
        String newVilleDepart=tfVilleDepart.getText();
        String newVilleArrivee=tfVilleArrivee.getText();
        LocalDate localDateArrivee = dpDateArrivee.getValue();
        LocalDate localDateDepart = dpDateDepart.getValue();
        java.sql.Date sqlDateArrive = java.sql.Date.valueOf(localDateArrivee);
        java.sql.Date sqlDateDepart = java.sql.Date.valueOf(localDateDepart);
        VolService vs=new VolService();
        vol v = new vol(newName,newdescription,newPrice,newdisponibilite,newcapacite,newnumeroVol,newVilleDepart,newVilleArrivee,sqlDateDepart,sqlDateArrive,"vol");
        try {
            vs.updateOne(currentService.getNumeroVol(),v);
            Parent dashboardView = null;
            try {
                dashboardView = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

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
        }
    }
}