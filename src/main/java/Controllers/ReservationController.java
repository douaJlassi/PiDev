package Controllers;

import gestion_activite.Activite;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;

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

    public void setStage(Stage stage) {
        this.stage = stage;
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
    private void handleConfirm()
    {
        if (participantsField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez indiquer le nombre de places.", ButtonType.OK);
            alert.show();
            return;
        }

        try {
            int nbPlaces = Integer.parseInt(participantsField.getText().trim());
            if (nbPlaces <= 0) {
                throw new NumberFormatException();
            }

            if (nbPlaces > activite.getPlacesDisponibles()) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Nombre de places insuffisant. Maximum : " + activite.getPlacesDisponibles(),
                        ButtonType.OK);
                alert.show();
                return;
            }


            Alert success = new Alert(Alert.AlertType.INFORMATION,
                    "Réservation confirmée pour " + nbPlaces + " personne(s) !",
                    ButtonType.OK);
            success.showAndWait();

            stage.close();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Le nombre de participants doit être un entier positif.",
                    ButtonType.OK);
            alert.show();
        }
    }
}