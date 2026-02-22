package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import projet.entites.Hotel;
import projet.services.HotelService;

import java.io.IOException;
import java.sql.SQLException;

public class addHotelController {
    String messageErrorNom="";
    String messageErrorDescription="";
    String messageErrorLocalisation="";
    String messageErrorNbEtoiles="";
    String messageErrorPrix="";
    String messageErrorCapacite="";
    String messageErrorChambre="";
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

    @FXML
    private Label lblErrorCapacite;

    @FXML
    private Label lblErrorChambre;

    @FXML
    private Label lblErrorDescription;

    @FXML
    private Label lblErrorLocalisation;

    @FXML
    private Label lblErrorNbEtoiles;

    @FXML
    private Label lblErrorNom;

    @FXML
    private Label lblErrorPrix;

    @FXML
    public void addHotel(ActionEvent event) throws SQLException {
        if (isInputValid()) {
        String nom = tfNom.getText();
        String description = tfDescription.getText();
        String localisation = tfLocalisation.getText();
        int nbEtoiles = Integer.parseInt(tfNbEtoiles.getText());
        int capacite = Integer.parseInt(tfCapacite.getText());
        double prix = Double.parseDouble(tfPrix.getText());
        String chambre = tfChambre.getText();
        boolean disponibilite = cbDisponibilite.isSelected();

        HotelService hs = new HotelService();
        Hotel hotel = new Hotel(nom, description, prix, disponibilite, capacite, "hotel", nbEtoiles, localisation, chambre);
        try {
            hs.insertOne(hotel);

            Parent dashboardView = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            StackPane contentArea = (StackPane) tfNom.getScene().lookup("#contentArea");
            tfNom.getScene().setRoot(dashboardView);
        } catch (SQLException | IOException e) {
            System.out.println(e.getMessage());
        }
        }
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
        if (tfDescription.getText() == null || tfDescription.getText().isEmpty()){
            tfDescription.getStyleClass().add("error");
            messageErrorDescription="La Description est requise.";
            return false;
        }
        else if (tfDescription.getText().trim().length()<5) {
            messageErrorDescription="Le nom doit être au moins 5 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isLocalisationValid() {
        if (tfLocalisation.getText() == null || tfLocalisation.getText().isEmpty()){
            tfLocalisation.getStyleClass().add("error");
            messageErrorLocalisation="Localisation est requise.";
            return false;
        }
        return true;
    }
    private  boolean isChambreValid() {
        if (tfChambre.getText() == null || tfChambre.getText().isEmpty()){
            tfChambre.getStyleClass().add("error");
            messageErrorChambre="La chambre est requise.";
            return false;
        }
        else if (!(tfChambre.getText().equals("SINGLE")||tfChambre.getText().equals("DOUBLE")||tfChambre.getText().equals("SUITE")||tfChambre.getText().equals("FAMILIALE"))) {
            tfChambre.getStyleClass().add("error");
            messageErrorChambre="La chambre n'est SINGLE/DOUBLE/SUITE/FAMILIALE.";
            return false;
        }
        return true;
    }
    private  boolean isNbEtoilesValid() {

        if (tfNbEtoiles.getText() == null || tfNbEtoiles.getText().isEmpty()){
            tfNbEtoiles.getStyleClass().add("error");
            messageErrorNbEtoiles="Nombre etoiles est requise";
            return false;
        }
        else if (Integer.parseInt(tfNbEtoiles.getText()) < 1 || Integer.parseInt(tfNbEtoiles.getText()) > 5 ) {
            tfNbEtoiles.getStyleClass().add("error");
            messageErrorNbEtoiles="Le Nb etoiles doit être ente 1 et 5 etoiles " ;
            return false;
        }
        return true;
    }
    private boolean isInputValid() {
        resetStyles();
        if (isNomValid() && isPrixValid() && isCapaciteValid() && isChambreValid()  && isDescriptionValid() && isLocalisationValid() && isNbEtoilesValid() ) {

            lblErrorNom.setVisible(false);
            lblErrorNom.setManaged(false);
            lblErrorDescription.setVisible(false);
            lblErrorDescription.setManaged(false);
            lblErrorCapacite.setVisible(false);
            lblErrorCapacite.setManaged(false);
            lblErrorChambre.setVisible(false);
            lblErrorChambre.setManaged(false);
            lblErrorPrix.setVisible(false);
            lblErrorPrix.setManaged(false);
            lblErrorNbEtoiles.setVisible(false);
            lblErrorNbEtoiles.setManaged(false);
            lblErrorLocalisation.setVisible(false);
            lblErrorLocalisation.setManaged(false);
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
            if (!isLocalisationValid()) {
                lblErrorLocalisation.setText(messageErrorLocalisation);
                lblErrorLocalisation.setVisible(true);
                lblErrorLocalisation.setManaged(true);
            }
            if (!isDescriptionValid()){
                lblErrorDescription.setText(messageErrorDescription);
                lblErrorDescription.setVisible(true);
                lblErrorDescription.setManaged(true);
            }
            if (!isChambreValid()){
                lblErrorChambre.setText(messageErrorChambre);
                lblErrorChambre.setVisible(true);
                lblErrorChambre.setManaged(true);
            }
            if (!isNbEtoilesValid()){
                lblErrorNbEtoiles.setText(messageErrorNbEtoiles);
                lblErrorNbEtoiles.setVisible(true);
                lblErrorNbEtoiles.setManaged(true);
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
        lblErrorChambre.setVisible(false);
        lblErrorChambre.setManaged(false);
        lblErrorPrix.setVisible(false);
        lblErrorPrix.setManaged(false);
        lblErrorNbEtoiles.setVisible(false);
        lblErrorNbEtoiles.setManaged(false);
        lblErrorLocalisation.setVisible(false);
        lblErrorLocalisation.setManaged(false);
        tfNom.getStyleClass().remove("error");
        tfPrix.getStyleClass().remove("error");
        tfCapacite.getStyleClass().remove("error");
        tfLocalisation.getStyleClass().remove("error");
        tfDescription.getStyleClass().remove("error");
    }
}