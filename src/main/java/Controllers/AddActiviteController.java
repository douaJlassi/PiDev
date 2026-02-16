package Controllers;

import gestion_activite.Activite;
import Services.ActiviteService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

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

    private String selectedImageName = "default.jpg";
    private ActiviteService service = new ActiviteService();

    @FXML
    public void initialize() {
        // Optionnel : Remplir les ComboBox au démarrage
        if (lieuCombo != null) {
            lieuCombo.getItems().addAll("Douz", "Djerba", "Tunis", "Sousse", "Tozeur");
        }
        if (heureCombo != null) {
            heureCombo.getItems().addAll("08:00", "09:00", "10:00", "14:00", "16:00");
        }
        if (categoryCombo != null) {
            categoryCombo.getItems().addAll("Safari", "Plongée", "Randonnée", "Culture");
        }
    }

    // Méthode pour choisir une image
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
                // Dossier de destination dans vos ressources
                File destDir = new File("src/main/resources/images/");
                if (!destDir.exists()) destDir.mkdirs();

                File destFile = new File(destDir, selectedFile.getName());

                // Copie physique du fichier
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                selectedImageName = selectedFile.getName();
                imagePreview.setImage(new Image(selectedFile.toURI().toString()));

                System.out.println("Image sauvegardée : " + selectedImageName);
            } catch (IOException e) {
                System.err.println("Erreur lors de la copie de l'image : " + e.getMessage());
            }
        }
    }

    // Méthode appelée par le bouton "Ajouter Activité"
    @FXML
    private void handleAddActivite() {
        try {
            // 1. Vérification basique
            if (titreField.getText().isEmpty() || prixField.getText().isEmpty() || datePicker.getValue() == null) {
                System.out.println("Veuillez remplir les champs obligatoires !");
                return;
            }

            // 2. Création de l'objet Activite
            // On convertit le LocalDate du DatePicker en Timestamp pour la DB
            Timestamp timestamp = Timestamp.valueOf(datePicker.getValue().atStartOfDay());

            Activite a = new Activite(
                    titreField.getText(),
                    longDescField.getText(),
                    lieuCombo.getValue() != null ? lieuCombo.getValue() : "Inconnu",
                    timestamp,
                    Integer.parseInt(dureeField.getText()),
                    Double.parseDouble(prixField.getText()),
                    1, // ID du Guide (à dynamiser plus tard avec l'utilisateur connecté)
                    selectedImageName
            );

            // 3. Insertion en base de données
            service.insertOne(a);
            System.out.println("✅ Activité '" + a.getTitre() + "' ajoutée avec succès !");

        } catch (NumberFormatException e) {
            System.err.println("Erreur de format : vérifiez le prix et la durée.");
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }
}