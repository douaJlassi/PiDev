package projet.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import projet.entites.vol;

import java.io.IOException;

public class VolDetailsController {
@FXML
private Button retourBtn;
    @FXML private Label lblNom, lblNumeroVol, lblPrix, lblVilleDepart, lblVilleArrivee,
            lblDateDepart, lblDateArrivee, lblCapacite, lblStatus, lblDescription;

    public void setVolData(vol vol) {
        lblNom.setText(vol.getNom());
        lblNumeroVol.setText(vol.getNumeroVol());
        lblPrix.setText(vol.getPrix() + " TND");
        lblVilleDepart.setText(vol.getVilleDepart());
        lblVilleArrivee.setText(vol.getVilleArrivee());
        lblDateArrivee.setText(vol.getDateArrivee().toString());
        lblDateDepart.setText(vol.getDateDepart().toString());
        lblCapacite.setText(vol.getNumeroVol());
        if (vol.getDisponibilite()){
            lblStatus.setText("disponible");}
        else {
            lblStatus.setText("no disponible");}
        lblDescription.setText(vol.getDescription());
retourBtn.setOnAction(e -> {handleBack();});

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

}
