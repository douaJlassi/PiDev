package Controllers;

import Controllers.DashboardGuideController;
import gestion_activite.Activite;
import Services.ActiviteService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import utils.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.time.format.DateTimeFormatter;

public class AddActiviteController {

    // On utilise exactement les mêmes IDs (fx:id) que dans le fichier FXML
    @FXML private TextField titreField;
    @FXML private TextField prixField;
    @FXML private TextField dureeField;
    @FXML private TextField placesField;
    @FXML private ComboBox<String> lieuCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> heureCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextArea shortDescField;
    @FXML private TextArea longDescField;
    @FXML private CheckBox publicToggle;
    @FXML private ImageView imagePreview;

    private DashboardGuideController dashboardController;

    private BorderPane mainBorderPane;
    private Node previousView;

    public void setMainBorderPane(BorderPane mainBorderPane) {
        this.mainBorderPane = mainBorderPane;
    }

    public void setPreviousView(Node previousView) {
        this.previousView = previousView;
    }

    private Activite currentActivite = null;
    private String selectedImageName = "default.jpg";
    private ActiviteService service = new ActiviteService();

    @FXML
    public void initialize() {
        if (lieuCombo != null) {
            lieuCombo.getItems().addAll("Douz", "Djerba", "Tunis", "Sousse", "Tozeur");
        }
        if (heureCombo != null) {
            heureCombo.getItems().addAll("08:00", "09:00", "10:00", "14:00", "16:00");
        }
        if (categoryCombo != null) {
            categoryCombo.getItems().addAll("Safari", "Culture", "Sports", "Aventure", "Monuments");
        }
    }

    @FXML
    private void handleReturn() {
        if (mainBorderPane != null && previousView != null) {
            // Restaure la vue précédente (la liste des activités)
            mainBorderPane.setCenter(previousView);
        } else {
            System.err.println("Impossible de revenir en arrière : références manquantes.");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de navigation", ButtonType.OK);
            alert.show();
        }
    }




    public void setDashboardController(DashboardGuideController dashboardController) {
        this.dashboardController = dashboardController;
    }

    // Helper to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.show();
    }

    public void setActivite(Activite activite) {
        this.currentActivite = activite; // store for later use

        // Text fields
        titreField.setText(activite.getTitre());
        prixField.setText(String.valueOf(activite.getPrix()));
        dureeField.setText(String.valueOf(activite.getDureParJour()));
        placesField.setText(String.valueOf(activite.getPlacesDisponibles()));

        // Descriptions
        String description = activite.getDescription();
        longDescField.setText(description != null ? description : "");
        shortDescField.setText(""); // adjust if needed

        // Location
        if (activite.getLieu() != null) {
            lieuCombo.setValue(activite.getLieu());
        }

        // ✅ Category – set the combo box value from the activity's category
        if (activite.getCategorie() != null) {
            categoryCombo.setValue(activite.getCategorie());
        }

        // Date and time
        if (activite.getDateActivite() != null) {
            datePicker.setValue(activite.getDateActivite().toLocalDateTime().toLocalDate());

            String heure = activite.getDateActivite().toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            if (heureCombo.getItems().contains(heure)) {
                heureCombo.setValue(heure);
            }
        }

        selectedImageName = activite.getImage();
        loadImage(selectedImageName);

        publicToggle.setSelected("Actif".equals(activite.getStatut()));
    }

    private void loadImage(String imageName) {
        if (imageName != null && !imageName.isEmpty()) {
            InputStream is = getClass().getResourceAsStream("/images/" + imageName);
            if (is != null) {
                imagePreview.setImage(new Image(is));
            } else {
                loadDefaultImage();
            }
        } else {
            loadDefaultImage();
        }
    }

    private void loadDefaultImage() {
        InputStream defaultIs = getClass().getResourceAsStream("/images/default.jpg");
        if (defaultIs != null) {
            imagePreview.setImage(new Image(defaultIs));
        } else {
            imagePreview.setImage(null);
            System.err.println("Default image not found at /images/default.jpg");
        }
    }

    @FXML
    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo d'activité");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(imagePreview.getScene().getWindow());

        if (selectedFile != null) {
            try {
                File destDir = new File("src/main/resources/images/");
                if (!destDir.exists()) destDir.mkdirs();

                File destFile = new File(destDir, selectedFile.getName());
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                selectedImageName = selectedFile.getName();
                imagePreview.setImage(new Image(selectedFile.toURI().toString()));

                System.out.println("Image sauvegardée : " + selectedImageName);
            } catch (IOException e) {
                System.err.println("Erreur lors de la copie de l'image : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddActivite() {
        try {
            if (titreField.getText().isEmpty() || prixField.getText().isEmpty() || datePicker.getValue() == null) {
                showAlert("Veuillez remplir les champs obligatoires !");
                return;
            }

            String titre = titreField.getText();
            String description = longDescField.getText();
            String lieu = (lieuCombo.getValue() != null) ? lieuCombo.getValue() : "Inconnu";

            Timestamp timestamp = Timestamp.valueOf(datePicker.getValue().atStartOfDay());

            int duree = Integer.parseInt(dureeField.getText());
            double prix = Double.parseDouble(prixField.getText());

            int places = 0;
            if (placesField.getText() != null && !placesField.getText().isEmpty()) {
                places = Integer.parseInt(placesField.getText());
            }

            int idGuide = 1; // TODO: replace with actual logged-in guide ID
            String statut = publicToggle.isSelected() ? "Actif" : "Privé";
            String image = (selectedImageName != null) ? selectedImageName : "default.jpg";
            String categorie = categoryCombo.getValue();

            Activite a = new Activite(
                    titre,
                    description,
                    lieu,
                    timestamp,
                    duree,
                    prix,
                    idGuide,
                    image,
                    statut,
                    places,
                    categorie
            );

            if (currentActivite != null) {
                a.setIdActivite(currentActivite.getIdActivite());
                service.updateOne(a);
                System.out.println("✅ Activité '" + a.getTitre() + "' mise à jour avec succès !");
            } else {
                service.insertOne(a);
                System.out.println("✅ Activité '" + a.getTitre() + "' ajoutée avec succès !");
            }

            // Après la sauvegarde réussie
            if (dashboardController != null) {
                dashboardController.refreshActivites();
            }

            EventBus.getInstance().publish();

            // 🔁 Refresh the guide dashboard
            if (dashboardController != null) {
                dashboardController.refreshActivites();
            }

            // 🔙 Return to previous view
            if (mainBorderPane != null && previousView != null) {
                mainBorderPane.setCenter(previousView);
            } else {
                System.err.println("Impossible de revenir en arrière : références manquantes.");
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de navigation", ButtonType.OK);
                alert.show();
            }

        } catch (NumberFormatException e) {
            showAlert("Erreur de format : vérifiez que le prix, la durée et le nombre de places sont des nombres.");
        } catch (SQLException e) {
            showAlert("Erreur SQL : " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.show();
    }
}