package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import projet.entites.vol;
import projet.services.VolService;

import java.sql.SQLException;
import java.time.LocalDate;


public class addVol {

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

    @FXML
    void ajouterVol(ActionEvent event) {
        String nom = tfNom.getText();
        String description=taDescription.getText();
        double prix = Double.parseDouble(tfPrix.getText());
        String numeroVol = tfNumeroVol.getText();
        int capacite=Integer.parseInt(tfCapacite.getText());
        String VilleDepart=tfVilleDepart.getText();
        String VilleArrivee=tfVilleArrivee.getText();
        boolean disponibilite=cbDisponibilite.isSelected();
        LocalDate localDateArrivee = dpDateArrivee.getValue();
        LocalDate localDateDepart = dpDateDepart.getValue();

        java.sql.Date sqlDateArrive = java.sql.Date.valueOf(localDateArrivee);
        java.sql.Date sqlDateDepart = java.sql.Date.valueOf(localDateDepart);
        VolService service = new VolService();
        vol vol = new vol(nom,description,prix,disponibilite,capacite,numeroVol,VilleDepart,VilleArrivee,sqlDateDepart,sqlDateArrive,"vol");
        try {
            service.insertOne(vol);
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }

    }

}
