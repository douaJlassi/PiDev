package controllers;

import gestion_activite.Activite;
import services.ActiviteService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import utils.EventBus;
import utils.SessionManager;
import entities.Person;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddActiviteController {

    // FXML fields
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

    // Controllers and navigation
    private DashboardGuideController dashboardController;
    private BorderPane mainBorderPane;
    private Node previousView;

    // Current user from session
    private Person currentUser;
    private int currentGuideId;

    // Activity data
    private Activite currentActivite = null;
    private String selectedImageName = "default.jpg";
    private ActiviteService service = new ActiviteService();

    // Date formatter for dd/MM/yyyy format
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Setters for navigation
    public void setMainBorderPane(BorderPane mainBorderPane) {
        this.mainBorderPane = mainBorderPane;
    }

    public void setPreviousView(Node previousView) {
        this.previousView = previousView;
    }

    public void setDashboardController(DashboardGuideController dashboardController) {
        this.dashboardController = dashboardController;
    }

    /**
     * Set the current user from session
     */
    public void setCurrentUser(Person user) {
        this.currentUser = user;
        if (user != null) {
            this.currentGuideId = user.getId();
            System.out.println("AddActiviteController: User set from session - " + user.getUsername() + " (ID: " + currentGuideId + ")");
        }
    }

    @FXML
    public void initialize() {
        // Get current user from session
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            currentGuideId = currentUser.getId();
            System.out.println("AddActiviteController initialized with user: " + currentUser.getUsername() + " (ID: " + currentGuideId + ")");
        } else {
            System.err.println("⚠️ No user logged in! Please login first.");
            currentGuideId = -1; // Invalid ID to prevent insertion
        }

        // Initialize combo boxes
        if (lieuCombo != null) {
            lieuCombo.getItems().addAll("Douz", "Djerba", "Tunis", "Sousse", "Tozeur", "Hammamet", "Monastir");
        }

        if (heureCombo != null) {
            heureCombo.getItems().addAll("08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00");
        }

        if (categoryCombo != null) {
            categoryCombo.getItems().addAll("Safari", "Culture", "Sports", "Aventure", "Monuments", "Plage", "Désert");
        }

        // Set up DatePicker with custom converter for dd/MM/yyyy format
        if (datePicker != null) {
            setupDatePicker();
        }
    }

    /**
     * Configure DatePicker to use dd/MM/yyyy format
     */
    private void setupDatePicker() {
        // Set custom converter
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? dateFormatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try {
                        return LocalDate.parse(string, dateFormatter);
                    } catch (DateTimeParseException e) {
                        // Show error indicator but don't crash
                        datePicker.getEditor().setStyle("-fx-border-color: #ff5e62;");
                        showAlert("Format de date invalide", "Utilisez le format JJ/MM/AAAA");
                        return null;
                    }
                }
                return null;
            }
        });

        // Set prompt text
        datePicker.setPromptText("JJ/MM/AAAA");

        // Add listener to clear error style when user types
        datePicker.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            datePicker.getEditor().setStyle("");
        });
    }

    @FXML
    private void handleReturn() {
        if (mainBorderPane != null && previousView != null) {
            // Restore previous view
            mainBorderPane.setCenter(previousView);
        } else {
            System.err.println("Impossible de revenir en arrière : références manquantes.");
            showAlert("Erreur de navigation", "Impossible de revenir à la page précédente.");
        }
    }

    /**
     * Load activity data for editing
     */
    public void setActivite(Activite activite) {
        this.currentActivite = activite;

        // Text fields
        titreField.setText(activite.getTitre());
        prixField.setText(String.valueOf(activite.getPrix()));
        dureeField.setText(String.valueOf(activite.getDureParJour()));
        placesField.setText(String.valueOf(activite.getPlacesDisponibles()));

        // Descriptions
        String description = activite.getDescription();
        longDescField.setText(description != null ? description : "");
        shortDescField.setText(""); // Adjust if needed

        // Location
        if (activite.getLieu() != null) {
            lieuCombo.setValue(activite.getLieu());
        }

        // Category
        if (activite.getCategorie() != null) {
            categoryCombo.setValue(activite.getCategorie());
        }

        // Date and time
        if (activite.getDateActivite() != null) {
            LocalDate date = activite.getDateActivite().toLocalDateTime().toLocalDate();
            datePicker.setValue(date);

            String heure = activite.getDateActivite().toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            if (heureCombo.getItems().contains(heure)) {
                heureCombo.setValue(heure);
            }
        }

        // Image
        selectedImageName = activite.getImage();
        loadImage(selectedImageName);

        // Status
        publicToggle.setSelected("Actif".equals(activite.getStatut()));

        // Set guide ID from activity (for editing)
        this.currentGuideId = activite.getIdGuide();
    }

    /**
     * Load image from resources
     */
    private void loadImage(String imageName) {
        if (imageName != null && !imageName.isEmpty()) {
            // Try to load from resources
            InputStream is = getClass().getResourceAsStream("/images/" + imageName);
            if (is != null) {
                imagePreview.setImage(new Image(is));
                return;
            }

            // Try to load from file system
            File imageFile = new File("src/main/resources/images/" + imageName);
            if (imageFile.exists()) {
                imagePreview.setImage(new Image(imageFile.toURI().toString()));
                return;
            }
        }

        // Load default image if nothing found
        loadDefaultImage();
    }

    /**
     * Load default image
     */
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
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(imagePreview.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Create destination directory if it doesn't exist
                File destDir = new File("src/main/resources/images/");
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }

                // Generate unique filename to avoid conflicts
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                File destFile = new File(destDir, fileName);

                // Copy file
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Update image
                selectedImageName = fileName;
                imagePreview.setImage(new Image(selectedFile.toURI().toString()));

                System.out.println("Image sauvegardée : " + selectedImageName);

            } catch (IOException e) {
                System.err.println("Erreur lors de la copie de l'image : " + e.getMessage());
                showAlert("Erreur", "Impossible de sauvegarder l'image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddActivite() {
        try {
            // Check if user is logged in
            if (currentUser == null) {
                // Try to get from session again
                currentUser = SessionManager.getCurrentUser();
                if (currentUser == null) {
                    showAlert("Erreur de session", "Aucun utilisateur connecté. Veuillez vous reconnecter.");
                    return;
                }
                currentGuideId = currentUser.getId();
            }

            // Validate required fields
            if (titreField.getText() == null || titreField.getText().trim().isEmpty()) {
                showAlert("Validation", "Le titre est obligatoire.");
                titreField.requestFocus();
                return;
            }

            if (prixField.getText() == null || prixField.getText().trim().isEmpty()) {
                showAlert("Validation", "Le prix est obligatoire.");
                prixField.requestFocus();
                return;
            }

            if (datePicker.getValue() == null) {
                showAlert("Validation", "La date est obligatoire.");
                datePicker.requestFocus();
                return;
            }

            // Get form values
            String titre = titreField.getText().trim();
            String description = longDescField.getText() != null ? longDescField.getText().trim() : "";
            String lieu = (lieuCombo.getValue() != null) ? lieuCombo.getValue() : "Inconnu";

            LocalDate selectedDate = datePicker.getValue();

            // Date validation
            if (currentActivite == null) {
                // New activity: date cannot be in the past
                if (selectedDate.isBefore(LocalDate.now())) {
                    showAlert("Validation", "La date de l'activité ne peut pas être dans le passé.");
                    datePicker.requestFocus();
                    return;
                }
            } else {
                // Editing: check if date has changed
                LocalDate oldDate = currentActivite.getDateActivite().toLocalDateTime().toLocalDate();
                if (!selectedDate.equals(oldDate) && selectedDate.isBefore(LocalDate.now())) {
                    showAlert("Validation", "Vous ne pouvez pas modifier la date vers une date passée.");
                    datePicker.requestFocus();
                    return;
                }
            }

            // Parse numeric fields
            int duree;
            double prix;
            int places = 0;

            try {
                duree = Integer.parseInt(dureeField.getText().trim());
                if (duree <= 0) {
                    showAlert("Validation", "La durée doit être un nombre positif.");
                    dureeField.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation", "La durée doit être un nombre valide.");
                dureeField.requestFocus();
                return;
            }

            try {
                prix = Double.parseDouble(prixField.getText().trim());
                if (prix <= 0) {
                    showAlert("Validation", "Le prix doit être un nombre positif.");
                    prixField.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation", "Le prix doit être un nombre valide.");
                prixField.requestFocus();
                return;
            }

            if (placesField.getText() != null && !placesField.getText().trim().isEmpty()) {
                try {
                    places = Integer.parseInt(placesField.getText().trim());
                    if (places < 0) {
                        showAlert("Validation", "Le nombre de places ne peut pas être négatif.");
                        placesField.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Validation", "Le nombre de places doit être un nombre valide.");
                    placesField.requestFocus();
                    return;
                }
            }

            // Create timestamp from date and time
            Timestamp timestamp;
            if (heureCombo.getValue() != null && !heureCombo.getValue().isEmpty()) {
                String[] timeParts = heureCombo.getValue().split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                timestamp = Timestamp.valueOf(selectedDate.atTime(hour, minute));
            } else {
                timestamp = Timestamp.valueOf(selectedDate.atStartOfDay());
            }

            // Get guide ID from logged-in user
            int idGuide = this.currentGuideId;

            // Validate guide ID is valid
            if (idGuide <= 0) {
                showAlert("Erreur", "ID de guide invalide. Veuillez vous reconnecter.");
                return;
            }

            System.out.println("Creating activity with guide ID from session: " + idGuide + " (User: " + currentUser.getUsername() + ")");

            String statut = publicToggle.isSelected() ? "Actif" : "Privé";
            String image = (selectedImageName != null) ? selectedImageName : "default.jpg";
            String categorie = categoryCombo.getValue();

            // Create activity object
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

            // Save to database
            if (currentActivite != null) {
                a.setIdActivite(currentActivite.getIdActivite());
                service.updateOne(a);
                showInfo("Succès", "Activité '" + a.getTitre() + "' mise à jour avec succès !");
                System.out.println("✅ Activité '" + a.getTitre() + "' mise à jour avec succès !");
            } else {
                service.insertOne(a);
                showInfo("Succès", "Activité '" + a.getTitre() + "' ajoutée avec succès !");
                System.out.println("✅ Activité '" + a.getTitre() + "' ajoutée avec succès !");
            }

            // Refresh dashboard if available
            if (dashboardController != null) {
                dashboardController.refreshActivites();
            }

            // Notify other components
            EventBus.getInstance().publish();

            // Return to previous view
            if (mainBorderPane != null && previousView != null) {
                mainBorderPane.setCenter(previousView);
            } else {
                System.err.println("Impossible de revenir en arrière : références manquantes.");
                showAlert("Navigation", "Retour à la page précédente impossible.");
            }

        } catch (NumberFormatException e) {
            showAlert("Erreur de format", "Vérifiez que le prix, la durée et le nombre de places sont des nombres.");
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getMessage().contains("foreign key constraint")) {
                showAlert("Erreur de base de données", "L'utilisateur avec l'ID " + currentGuideId + " n'est pas un guide valide. Veuillez vous connecter avec un compte guide.");
            } else {
                showAlert("Erreur SQL", "Erreur lors de la sauvegarde: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur inattendue", "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Show error alert
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show information alert
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Clear all form fields
     */
    @FXML
    private void handleClear() {
        titreField.clear();
        prixField.clear();
        dureeField.clear();
        placesField.clear();
        lieuCombo.setValue(null);
        categoryCombo.setValue(null);
        heureCombo.setValue(null);
        datePicker.setValue(null);
        shortDescField.clear();
        longDescField.clear();
        publicToggle.setSelected(true);
        selectedImageName = "default.jpg";
        loadDefaultImage();
        currentActivite = null;
    }
}