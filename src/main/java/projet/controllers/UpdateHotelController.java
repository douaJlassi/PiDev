package projet.controllers;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import projet.entites.Hotel;
import projet.entites.vol;
import projet.services.HotelService;
import projet.services.VolService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class UpdateHotelController {
    String messageErrorNom = "";
    String messageErrorDescription = "";
    String messageErrorLocalisation = "";
    String messageErrorNbEtoiles = "";
    String messageErrorPrix = "";
    String messageErrorCapacite = "";
    String messageErrorChambre = "";
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
    private Hotel currentService;

    private boolean isNomValid() {
        if (tfNom.getText() == null || tfNom.getText().trim().isEmpty()) {
            tfNom.getStyleClass().add("error");
            messageErrorNom = "Le nom ne peut pas être vide";
            return false;
        } else if (tfNom.getText().trim().length() < 3) {
            messageErrorNom = "Le nom doit être au moins 3 caracteres";
            return false;
        }
        return true;
    }
    private boolean isPrixValid() {
        if (tfPrix.getText() == null || tfPrix.getText().isEmpty()) {
            messageErrorPrix = "Le prix est requis.";
            tfPrix.getStyleClass().add("error");
            return false;
        } else {
            try {
                double prix = Double.parseDouble(tfPrix.getText());
                if (prix <= 0) {
                    messageErrorPrix = "Le prix doit être un nombre positif.";
                    tfPrix.getStyleClass().add("error");
                    return false;
                }
            } catch (NumberFormatException e) {
                messageErrorPrix = "Le prix doit être un nombre valide (ex: 150.50)";
                tfPrix.getStyleClass().add("error");
                return false;
            }
        }
        return true;
    }
    private boolean isCapaciteValid() {
        if (tfCapacite.getText() == null || tfCapacite.getText().isEmpty()) {
            messageErrorCapacite = "La capacité est requise.";
            tfCapacite.getStyleClass().add("error");
            return false;
        } else {
            try {
                int capacite = Integer.parseInt(tfCapacite.getText());
                if (capacite <= 0) {
                    messageErrorCapacite = "La capacité doit être supérieure à 0.";
                    tfCapacite.getStyleClass().add("error");
                    return false;
                }
            } catch (NumberFormatException e) {
                messageErrorCapacite = "La capacité doit être un nombre entier.\n";
                tfCapacite.getStyleClass().add("error");
            }
        }
        return true;
    }
    private boolean isDescriptionValid() {
        if (tfDescription.getText() == null || tfDescription.getText().isEmpty()) {
            tfDescription.getStyleClass().add("error");
            messageErrorDescription = "La Description est requise.";
            return false;
        } else if (tfDescription.getText().trim().length() < 5) {
            messageErrorDescription = "La Description doit être au moins 5 caracteres";
            return false;
        }
        return true;
    }
    private boolean isLocalisationValid() {
        if (tfLocalisation.getText() == null || tfLocalisation.getText().isEmpty()) {
            tfLocalisation.getStyleClass().add("error");
            messageErrorLocalisation = "Localisation est requise.";
            return false;
        }
        return true;
    }
    private boolean isChambreValid() {
        if (tfChambre.getText() == null || tfChambre.getText().isEmpty()) {
            tfChambre.getStyleClass().add("error");
            messageErrorChambre = "La chambre est requise.";
            return false;
        } else if (!(tfChambre.getText().equals("SINGLE") || tfChambre.getText().equals("DOUBLE") || tfChambre.getText().equals("SUITE") || tfChambre.getText().equals("FAMILIALE"))) {
            tfChambre.getStyleClass().add("error");
            messageErrorChambre = "La chambre n'est SINGLE/DOUBLE/SUITE/FAMILIALE.\n";
            return false;
        }
        return true;
    }
    private boolean isNbEtoilesValid() {

        if (tfNbEtoiles.getText() == null || tfNbEtoiles.getText().isEmpty()) {
            tfNbEtoiles.getStyleClass().add("error");
            messageErrorNbEtoiles = "Nombre etoiles est requise";
            return false;
        } else if (Integer.parseInt(tfNbEtoiles.getText()) < 1 || Integer.parseInt(tfNbEtoiles.getText()) > 5) {
            messageErrorNbEtoiles = "Le Nb etoiles doit être ente 1 et 5 etoiles ";
            return false;
        }
        return true;
    }
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
        resetStyles();
        if (isNomValid() && isPrixValid() && isCapaciteValid() && isChambreValid() && isNbEtoilesValid()&& isDescriptionValid() && isLocalisationValid() && isNbEtoilesValid() ) {
            String newName = tfNom.getText();
            double newPrice = Double.parseDouble(tfPrix.getText());
            String newdescription = tfDescription.getText();
            int newnbetoiles = Integer.parseInt(tfNbEtoiles.getText());
            int newcapacite = Integer.parseInt(tfCapacite.getText());
            boolean newdisponibilite = cbDisponibilite.isSelected();
            String newlocalisation = tfLocalisation.getText();
            String newtypechambre = tfChambre.getText();
            HotelService vs = new HotelService();
            Hotel v = new Hotel(newName, newdescription, newPrice, newdisponibilite, newcapacite, "hotel", newnbetoiles, newlocalisation, newtypechambre);
            try {
                vs.updateOne(currentService.getNom(), v);
                Parent dashboardView = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
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
        else {
            // Afficher les erreurs
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


