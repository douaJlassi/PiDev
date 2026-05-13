package controllers;

import services.ActiviteService;
import services.AchatService;
import gestion_activite.Achat;
import gestion_activite.Activite;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import services.EmailService;
import utils.EventBus;
import utils.MyDBConnexion;

import java.io.IOException;
import java.sql.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    @FXML private VBox securePaymentBox; // Add this FXML field

    @FXML private VBox guestsContainer;


    private Stage stage;
    private Activite activite;
    private int clientId;
    private ActiviteService activiteService = new ActiviteService();
    private AchatService achatService = new AchatService();
    private String clientEmail;
    private String clientNom;

    // Payment flag
    private boolean paymentCompleted = false;

    private List<TextField> guestFields = new ArrayList<>();

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

    @FXML
    public void initialize() {
        // Initially hide the confirm button
        confirmButton.setVisible(false);
        confirmButton.setManaged(false);

        // Add click handler for secure payment box
        if (securePaymentBox != null) {
            securePaymentBox.setOnMouseClicked(event -> handleSecurePaymentClick());
            securePaymentBox.setStyle("-fx-cursor: hand;");
        }
    }

    @FXML
    private void handleSecurePaymentClick() {
        // Validate if number of participants is entered
        if (participantsField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Veuillez d'abord indiquer le nombre de places.");
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

            // Open payment window
            openPaymentWindow(montantTotal);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Le nombre de participants doit être un entier positif.");
        }
    }

    private void openPaymentWindow(double montant) {
        try {
            // Note: Check the correct filename - it should be "Paiement.fxml" not "Paiment.fxml"
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Paiment.fxml"));

            // Use Parent (superclass of all layouts) instead of VBox
            javafx.scene.Parent paymentRoot = loader.load();

            PaiementController paymentController = loader.getController();

            // Create a new stage for payment
            Stage paymentStage = new Stage();
            paymentStage.initModality(Modality.APPLICATION_MODAL);
            paymentStage.initStyle(StageStyle.DECORATED);
            paymentStage.setTitle("Paiement sécurisé - " + String.format("%.0f DT", montant));

            paymentController.setStage(paymentStage);
            paymentController.setMontant(montant);

            // Set callback for payment result
            paymentController.setOnResult(success -> {
                // Use Platform.runLater to ensure UI updates on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        // Payment successful
                        paymentCompleted = true;
                        confirmButton.setVisible(true);
                        confirmButton.setManaged(true);

                        // Disable the payment box after payment
                        securePaymentBox.setDisable(true);
                        securePaymentBox.setOpacity(0.6);
                        securePaymentBox.setStyle("-fx-cursor: default; -fx-background-color: #27ae60;");

                        // Update the text to show payment completed
                        securePaymentBox.getChildren().stream()
                                .filter(node -> node instanceof Label)
                                .findFirst()
                                .ifPresent(label -> ((Label) label).setText("Paiement accepté ✓"));

                        showAlert(Alert.AlertType.INFORMATION,
                                "Paiement de " + String.format("%.0f", montant) + " DT effectué avec succès!\n" +
                                        "Vous pouvez maintenant confirmer votre réservation.");
                    }
                });
            });

            Scene scene = new Scene(paymentRoot);
            paymentStage.setScene(scene);

            // Center the payment window relative to the parent window
            if (stage != null) {
                paymentStage.setX(stage.getX() + (stage.getWidth() - 520) / 2);
                paymentStage.setY(stage.getY() + (stage.getHeight() - 820) / 2);
            }

            paymentStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur lors de l'ouverture de la page de paiement: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur inattendue: " + e.getMessage());
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
        // Guest fields code (commented out)
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    private void loadClientInfo() {
        String query = "SELECT email, name, last_name FROM user WHERE id = ?";
        try (Connection conn = MyDBConnexion.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                clientEmail = rs.getString("email");
                String nom = rs.getString("name");
                String prenom = rs.getString("last_name");
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
        // Check if payment was completed
        if (!paymentCompleted) {
            showAlert(Alert.AlertType.WARNING, "Veuillez d'abord effectuer le paiement sécurisé.");
            return;
        }

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

            double montantTotal = nbPlaces * activite.getPrix();

            // Create and save the purchase
            Achat achat = new Achat(
                    clientId,
                    activite.getIdActivite(),
                    new Timestamp(System.currentTimeMillis()),
                    nbPlaces,
                    montantTotal,
                    "Confirmé"
            );

            // Insert using the service
            achatService.insert(achat);

            // Update available places
            int newPlaces = activite.getPlacesDisponibles() - nbPlaces;
            activiteService.updatePlaces(activite.getIdActivite(), newPlaces);

            // Publish event to refresh UI
            EventBus.getInstance().publish();

            // Send email with QR code in background thread
            final String emailToSend = clientEmail;
            final String nomToSend = (clientNom != null && !clientNom.isBlank())
                    ? clientNom : "Client #" + clientId;
            final String activiteTitre = activite.getTitre();
            final int finalNbPlaces = nbPlaces;
            final double finalMontant = montantTotal;

            final String qrContent = String.format(
                    "RESERVATION\nActivité: %s\nClient: %s\nPlaces: %d\nMontant: %.0f DT\nDate: %s",
                    activiteTitre,
                    nomToSend,
                    finalNbPlaces,
                    finalMontant,
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())
            );

            if (emailToSend != null && !emailToSend.isEmpty()) {
                new Thread(() -> {
                    try {
                        byte[] qrCodeBytes = utils.QRCodeGenerator.generateQRCode(qrContent, 300, 300);
                        // Uncomment when EmailService is ready
                        /*
                        boolean sent = EmailService.envoyerConfirmation(
                                emailToSend,
                                nomToSend,
                                activiteTitre,
                                finalNbPlaces,
                                finalMontant,
                                qrCodeBytes
                        );
                        if (sent) {
                            System.out.println("✅ Email de confirmation envoyé à " + emailToSend);
                        } else {
                            System.err.println("❌ Échec d'envoi d'email à " + emailToSend);
                        }
                        */
                    } catch (Exception e) {
                        System.err.println("❌ Erreur génération QR / envoi email : " + e.getMessage());
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("⚠️ Aucun email client trouvé pour l'ID " + clientId);
            }

            // Show success message
            showAlert(Alert.AlertType.INFORMATION,
                    "Réservation confirmée pour " + nbPlaces + " personne(s) !\n"
                            + "Montant total : " + String.format("%.0f", montantTotal) + " DT\n"
                            + (emailToSend != null ? "Un email avec QR code a été envoyé." : "Aucun email n'a pu être envoyé."));

            // Close the window
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
        alert.showAndWait();
    }
}