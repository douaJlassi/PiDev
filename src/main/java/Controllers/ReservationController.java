package Controllers;

import Services.ActiviteService;
import Services.AchatService;
import gestion_activite.Achat;
import gestion_activite.Activite;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.sql.Timestamp;

public class ReservationController {

    @FXML private ImageView activityImageView;
    @FXML private Label activityTitleLabel;
    @FXML private Label activityDescriptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label availablePlacesLabel;
    @FXML private TextField participantsField;
    @FXML private Label totalPriceLabel;
    @FXML private Button cancelButton;
    @FXML private Button confirmButton;

    private Stage stage;
    private Activite activite;
    private int clientId;
    private ActiviteService activiteService = new ActiviteService();
    private AchatService achatService = new AchatService();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setActivite(Activite activite) {
        this.activite = activite;
        afficherDetails();
        chargerImage();
    }

    private void afficherDetails() {
        if (activite != null) {
            activityTitleLabel.setText(activite.getTitre());
            activityDescriptionLabel.setText(activite.getDescription());
            priceLabel.setText(String.format("%.0f DT", activite.getPrix()));
            availablePlacesLabel.setText(activite.getPlacesDisponibles() + " places disponibles");

            participantsField.textProperty().addListener((obs, oldVal, newVal) -> calculerPrixTotal());
        }
    }

    private void chargerImage() {
        try {
            String imageName = activite.getImage();
            if (imageName == null || imageName.trim().isEmpty()) {
                imageName = "default.jpg";
            }
            if (!imageName.contains(".")) {
                imageName += ".jpg";
            }
            String path = "/images/" + imageName;
            java.net.URL resource = getClass().getResource(path);
            if (resource != null) {
                activityImageView.setImage(new Image(resource.toExternalForm()));
            } else {
                java.net.URL defaultUrl = getClass().getResource("/images/default.jpg");
                if (defaultUrl != null) {
                    activityImageView.setImage(new Image(defaultUrl.toExternalForm()));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image : " + e.getMessage());
        }
    }

    private void calculerPrixTotal() {
        try {
            int nbParticipants = Integer.parseInt(participantsField.getText().trim());
            if (nbParticipants > 0) {
                double total = nbParticipants * activite.getPrix();
                totalPriceLabel.setText(String.format("%.0f DT", total));
            } else {
                totalPriceLabel.setText("0 DT");
            }
        } catch (NumberFormatException e) {
            totalPriceLabel.setText("0 DT");
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    @FXML
    private void handleConfirm() {
        // Check if field is empty
        if (participantsField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Veuillez indiquer le nombre de places.");
            return;
        }

        try {
            int nbPlaces = Integer.parseInt(participantsField.getText().trim());
            if (nbPlaces <= 0) throw new NumberFormatException();

            if (nbPlaces > activite.getPlacesDisponibles()) {
                showAlert(Alert.AlertType.ERROR,
                        "Nombre de places insuffisant. Maximum : " + activite.getPlacesDisponibles());
                return;
            }

            // Calculate total
            double montantTotal = nbPlaces * activite.getPrix();

            // Create Achat object
            Achat achat = new Achat(
                    new Timestamp(System.currentTimeMillis()),
                    montantTotal,
                    "Confirmé",
                    clientId,
                    nbPlaces,
                    activite.getIdActivite()
            );

            // Save to database
            achatService.insert(achat);

            // Update available places in the activity
            int remainingPlaces = activite.getPlacesDisponibles() - nbPlaces;
            activiteService.updatePlaces(activite.getIdActivite(), remainingPlaces);

            // Show success message
            showAlert(Alert.AlertType.INFORMATION,
                    "Réservation confirmée pour " + nbPlaces + " personne(s) !\nMontant total : " + montantTotal + " DT");

            stage.close();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Le nombre de participants doit être un entier positif.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement de la réservation.");
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.show();
    }
}