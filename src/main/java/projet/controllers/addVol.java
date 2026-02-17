package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import projet.entites.vol;
import projet.services.VolService;

import java.sql.SQLException;
import java.time.LocalDate;


public class addVol {
    String messageErrorNom="";
    String messageErrorDescription="";
    String messageErrorPrix="";
    String messageErrorCapacite="";
    String messageErrorNumeroVol="";
    String messageErrorVilleDepart="";
    String messageErrorVilleArrive="";
    String messageErrorDateDepart="";
    String messageErrorDateArrive="";
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
    private Label lblErrorCapacite;

    @FXML
    private Label lblErrorDateArrive;

    @FXML
    private Label lblErrorDateDepart;

    @FXML
    private Label lblErrorDescription;

    @FXML
    private Label lblErrorNom;

    @FXML
    private Label lblErrorNumeroVol;

    @FXML
    private Label lblErrorPrix;

    @FXML
    private Label lblErrorVilleArrive;

    @FXML
    private Label lblErrorVilleDepart;
    @FXML
    void ajouterVol(ActionEvent event) {
        if (isInputValid()){
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

    }}
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
    private  boolean isPrixValid() {
        if (tfPrix.getText() == null || tfPrix.getText().isEmpty()) {
            messageErrorPrix="Le prix est requis.";
            tfPrix.getStyleClass().add("error");
            return false;
        }
        else {
            try {
                double prix = Double.parseDouble(tfPrix.getText());
                if (prix <= 0) {
                    messageErrorPrix="Le prix doit être un nombre positif.";
                    tfPrix.getStyleClass().add("error");
                    return false;
                }
            } catch (NumberFormatException e) {
                messageErrorPrix="Le prix doit être un nombre valide (ex: 150.50)";
                tfPrix.getStyleClass().add("error");
                return false;
            }
        }
        return true;
    }
    private  boolean isCapaciteValid() {
        if (tfCapacite.getText() == null || tfCapacite.getText().isEmpty()) {
            messageErrorCapacite="La capacité est requise.";
            tfCapacite.getStyleClass().add("error");
            return false;
        }
        else {
            try {
                int capacite = Integer.parseInt(tfCapacite.getText());
                if (capacite <= 0) {
                    messageErrorCapacite="La capacité doit être supérieure à 0.";
                    tfCapacite.getStyleClass().add("error");
                    return false;
                }
            } catch (NumberFormatException e) {
                messageErrorCapacite="La capacité doit être un nombre entier.\n";
                tfCapacite.getStyleClass().add("error");
            }
        }
        return true;
    }
    private  boolean isDescriptionValid() {
        if (taDescription.getText() == null || taDescription.getText().isEmpty()){
            taDescription.getStyleClass().add("error");
            messageErrorDescription="La Description est requise.";
            return false;
        }
        else if (taDescription.getText().trim().length()<5) {
            messageErrorDescription="Le nom doit être au moins 5 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isNumeroVolValid() {
        if (tfNumeroVol.getText() == null || tfNumeroVol.getText().isEmpty()){
            tfNumeroVol.getStyleClass().add("error");
            messageErrorNumeroVol="numerovol est requise.";
            return false;
        }
        else if (tfNumeroVol.getText().trim().length()<3) {
            messageErrorNumeroVol="numerovol doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isVilleDepartValid() {
        if (tfVilleDepart.getText() == null || tfVilleDepart.getText().isEmpty()){
            tfVilleDepart.getStyleClass().add("error");
            messageErrorVilleDepart="ville depart est requise.";
            return false;
        }
        else if (tfVilleDepart.getText().trim().length()<3) {
            messageErrorVilleDepart="ville depart doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isVilleArriveValid() {
        if (tfVilleArrivee.getText() == null || tfVilleArrivee.getText().isEmpty()){
            tfVilleArrivee.getStyleClass().add("error");
            messageErrorVilleArrive="ville Arrive est requise.";
            return false;
        }
        else if (tfVilleArrivee.getText().trim().length()<3) {
            messageErrorVilleArrive="ville Arrive doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }

    private boolean isInputValid() {
        resetStyles();
        if (isNomValid() && isPrixValid() && isCapaciteValid() && isDescriptionValid() && isNumeroVolValid() && isVilleDepartValid() && isVilleArriveValid()) {

            lblErrorNom.setVisible(false);
            lblErrorNom.setManaged(false);
            lblErrorDescription.setVisible(false);
            lblErrorDescription.setManaged(false);
            lblErrorCapacite.setVisible(false);
            lblErrorCapacite.setManaged(false);
            lblErrorNumeroVol.setVisible(false);
            lblErrorNumeroVol.setManaged(false);
            lblErrorPrix.setVisible(false);
            lblErrorPrix.setManaged(false);
            lblErrorVilleDepart.setVisible(false);
            lblErrorVilleDepart.setManaged(false);
            lblErrorVilleArrive.setVisible(false);
            lblErrorVilleArrive.setManaged(false);

            return true;
        }
        else {
            if (!isNomValid()){
                lblErrorNom.setText(messageErrorNom);
                lblErrorNom.setVisible(true);
                lblErrorNom.setManaged(true);
            }
            if (!isPrixValid()){
                lblErrorPrix.setVisible(true);
                lblErrorPrix.setManaged(true);
                lblErrorPrix.setText(messageErrorPrix);

            }
            if (!isCapaciteValid()){  lblErrorCapacite.setText(messageErrorCapacite);
                lblErrorCapacite.setVisible(true);
                lblErrorCapacite.setManaged(true);}
            if (!isNumeroVolValid()){

                lblErrorNumeroVol.setText(messageErrorNumeroVol);
                lblErrorNumeroVol.setVisible(true);
                lblErrorNumeroVol.setManaged(true);
            }
            if (!isDescriptionValid()){
                lblErrorDescription.setText(messageErrorDescription);
                lblErrorDescription.setVisible(true);
                lblErrorDescription.setManaged(true);
            }
            if (!isVilleDepartValid()){
                lblErrorVilleDepart.setText(messageErrorVilleDepart);
                lblErrorVilleDepart.setVisible(true);
                lblErrorVilleDepart.setManaged(true);
            }
            if (!isVilleArriveValid()){
                lblErrorVilleArrive.setText(messageErrorVilleArrive);
                lblErrorVilleArrive.setVisible(true);
                lblErrorVilleArrive.setManaged(true);
            }

            return false;
        }

    }
    private void resetStyles() {
        lblErrorNom.setVisible(false);
        lblErrorNom.setManaged(false);
        lblErrorDescription.setVisible(false);
        lblErrorDescription.setManaged(false);
        lblErrorCapacite.setVisible(false);
        lblErrorCapacite.setManaged(false);

        lblErrorPrix.setVisible(false);
        lblErrorPrix.setManaged(false);
        lblErrorNumeroVol.setVisible(false);
        lblErrorNumeroVol.setManaged(false);
        lblErrorVilleDepart.setVisible(false);
        lblErrorVilleDepart.setManaged(false);
        lblErrorVilleArrive.setVisible(false);
        lblErrorVilleArrive.setManaged(false);

        tfNom.getStyleClass().remove("error");
        tfPrix.getStyleClass().remove("error");
        tfCapacite.getStyleClass().remove("error");
        taDescription.getStyleClass().remove("error");
        tfNumeroVol.getStyleClass().remove("error");
        tfVilleDepart.getStyleClass().remove("error");
        tfVilleArrivee.getStyleClass().remove("error");

    }
}
