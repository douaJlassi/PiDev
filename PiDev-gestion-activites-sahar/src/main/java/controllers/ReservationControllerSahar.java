package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import services.ActiviteService;
import services.AchatService;
import gestion_activite.Achat;
import gestion_activite.Activite;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.geometry.Insets;
import services.EmailService;
import utils.EventBus;
import utils.MyDBConnexion;

import java.io.IOException;
import java.sql.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import java.util.Optional;


import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import java.util.Optional;

public class ReservationControllerSahar {

    @FXML private ImageView activityImageView;
    @FXML private Label activityTitleLabel;
    @FXML private Label activityDescriptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label availablePlacesLabel;
    @FXML private TextField participantsField;
    @FXML private Label totalPriceLabel;
    @FXML private Button cancelButton;
    @FXML private Button confirmButton;

    @FXML private VBox guestsContainer;

    private Stage stage;
    private Activite activite;
    private int clientId;
    private ActiviteService activiteService = new ActiviteService();
    private AchatService achatService = new AchatService();
    private String clientEmail;
    private String clientNom;
    @FXML
    private VBox securePaymentBox;
    private boolean paymentConfirmed = false;
    private List<TextField> guestFields = new ArrayList<>();




    @FXML
    public void initialize() {
        securePaymentBox.setCursor(Cursor.HAND);
        securePaymentBox.setOnMouseClicked(this::handleSecurePaymentClick);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
        loadClientInfo();
    }

    public void setActivite(Activite activite) {
        this.activite = activite;
        afficherDetails();
        chargerImage();

        participantsField.textProperty().addListener((obs, oldVal, newVal) -> {
            calculerPrixTotal();
            updateGuestFields();
        });
    }


    private void handleSecurePaymentClick(MouseEvent event) {
        showPaymentDialog();
    }







    private void showPaymentDialog() {
        try {


            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/paiment.fxml"));
            Parent root = loader.load();

            PaiementController ctrl = loader.getController();
            Stage payStage = new Stage();
            payStage.initModality(Modality.APPLICATION_MODAL);
            payStage.initOwner(stage);
            payStage.setTitle("Paiement Sécurisé");
            payStage.setResizable(false);

            ctrl.setStage(payStage);
            int nbPlaces;
            try {
                nbPlaces = Integer.parseInt(participantsField.getText().trim());
                if (nbPlaces <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Veuillez indiquer un nombre de participants valide.");
                    return;
                }
                if (nbPlaces > activite.getPlacesDisponibles()) {
                    showAlert(Alert.AlertType.ERROR,
                            "Nombre de places insuffisant. Maximum : " + activite.getPlacesDisponibles());
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Veuillez indiquer le nombre de participants.");
                return;
            }

            ctrl.setMontant(activite.getPrix() * nbPlaces);
            ctrl.setOnResult(success -> {
                if (success) {
                    paymentConfirmed = true;
                    securePaymentBox.setStyle("-fx-background-color: #27ae60;");
                }
            });

            payStage.setScene(new Scene(root));
            payStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    private void afficherDetails() {
        if (activite != null) {
            activityTitleLabel.setText(activite.getTitre());
            activityDescriptionLabel.setText(activite.getDescription());
            priceLabel.setText(String.format("%.0f DT", activite.getPrix()));
            availablePlacesLabel.setText(activite.getPlacesDisponibles() + " places disponibles");
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


    private void updateGuestFields() {

        guestsContainer.getChildren().clear();
        guestFields.clear();

        int nbGuests;
        try {
            nbGuests = Integer.parseInt(participantsField.getText().trim());
            if (nbGuests <= 0) return;
            if (nbGuests > activite.getPlacesDisponibles()) {

                nbGuests = activite.getPlacesDisponibles();


            }
        } catch (NumberFormatException e) {
            return;
        }
        /*
        for (int i = 1; i <= nbGuests; i++) {
            TextField guestField = new TextField();
            guestField.setPromptText("Nom du participant " + i);
            guestField.getStyleClass().add("guest-field");
            guestsContainer.getChildren().add(guestField);
            guestFields.add(guestField);
        }*/
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    private void loadClientInfo() {
        String query = "SELECT email, nom, prenom FROM clients1 WHERE idUser = ?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                clientEmail = rs.getString("email");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                clientNom = (nom != null ? nom : "") + " " + (prenom != null ? prenom : "");
                if (clientNom.trim().isEmpty()) clientNom = "Client #" + clientId;
            } else {
                clientEmail = null;
                clientNom = "Client #" + clientId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            clientEmail = null;
            clientNom = "Client #" + clientId;
        }
    }

    @FXML

    private void handleConfirm() {


        if (participantsField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Veuillez indiquer le nombre de places.");
            return;
        }
        if (!paymentConfirmed) {
            showAlert(Alert.AlertType.WARNING, "Veuillez d'abord effectuer le paiement sécurisé.");
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

            double montantTotal = nbPlaces * activite.getPrix();

            // Save reservation
            Achat achat = new Achat(
                    new Timestamp(System.currentTimeMillis()),
                    montantTotal,
                    "Confirmé",
                    clientId,
                    nbPlaces,
                    activite.getIdActivite()
            );
            achatService.insert(achat);
            activiteService.updatePlaces(activite.getIdActivite(),
                    activite.getPlacesDisponibles() - nbPlaces);
            EventBus.getInstance().publish();

            final String emailToSend = clientEmail;
            final String nomToSend = (clientNom != null && !clientNom.isBlank())
                    ? clientNom : "Client #" + clientId;
            final String activiteTitre = activite.getTitre();
            final int finalNbPlaces = nbPlaces;
            final double finalMontant = montantTotal;

            final String qrContent = String.format(
                    "RESERVATION\nActivite: %s\nClient: %s\nPlaces: %d\nMontant: %.0f DT\nDate: %s",
                    activiteTitre,
                    nomToSend,
                    finalNbPlaces,
                    finalMontant,
                    new java.util.Date()
            );

            if (emailToSend != null && !emailToSend.isEmpty()) {
                new Thread(() -> {
                    try {
                        byte[] qrCodeBytes = utils.QRCodeGenerator.generateQRCode(qrContent, 300, 300);
                        EmailService.envoyerConfirmation(
                                emailToSend,
                                nomToSend,
                                activiteTitre,
                                finalNbPlaces,
                                finalMontant,
                                qrCodeBytes
                        );
                    } catch (Exception e) {
                        System.err.println("❌ Erreur génération QR / envoi email : " + e.getMessage());
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("⚠️ Aucun email client trouvé pour l'ID " + clientId);
            }

            showAlert(Alert.AlertType.INFORMATION,
                    "Réservation confirmée pour " + nbPlaces + " personne(s) !\n"
                            + "Montant total : " + String.format("%.0f", montantTotal) + " DT\n"
                            + (emailToSend != null ? "Un email avec QR code a été envoyé." : "Aucun email n'a pu être envoyé."));
            stage.close();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Le nombre de participants doit être un entier positif.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.show();
    }
}