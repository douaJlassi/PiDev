package controllers;

import entities.Publication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

/**
 * CreatePostController — bound to create_post_dialog.fxml.
 * Used for both Create and Edit (pre-populated via init).
 */
public class CreatePostController {

    @FXML private Circle    userAvatar;
    @FXML private Label     usernameLabel;
    @FXML private TextArea  contentArea;
    @FXML private VBox      imagePreviewBox;
    @FXML private ImageView imagePreview;
    @FXML private Button    removeImageBtn;
    @FXML private TextField placeField;
    @FXML private Button    photoBtn;

    private static final String UPLOAD_DIR = "uploads/images/";

    private DashboardController dashboard;
    private File selectedImageFile;

    /** Result — set when user confirms, read by PostController */
    private Publication result;

    // ────────────────────────────────────────────────────────────────────────
    // Init — called by PostController after loading the FXML
    // ────────────────────────────────────────────────────────────────────────
    public void init(DashboardController dash, Publication existing) {
        this.dashboard = dash;

        // Populate user header
        String username = dash.getCurrentUser().getUsername();
        usernameLabel.setText(username != null ? username
                : "Traveler #" + dash.getCurrentUser().getClientID());

        // If editing, pre-fill fields
        if (existing != null) {
            contentArea.setText(existing.getContent());
            if (existing.getPlace() != null) placeField.setText(existing.getPlace());
            if (existing.getImagePath() != null && !existing.getImagePath().isEmpty()) {
                File img = new File(existing.getImagePath());
                if (img.exists()) {
                    imagePreview.setImage(new Image(img.toURI().toString()));
                    imagePreviewBox.setVisible(true);
                    imagePreviewBox.setManaged(true);
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // FXML handlers
    // ────────────────────────────────────────────────────────────────────────
    @FXML
    private void onSelectPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files",
                        "*.png", "*.jpg", "*.jpeg", "*.gif"));

        Stage stage = (Stage) photoBtn.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            selectedImageFile = file;
            imagePreview.setImage(new Image(file.toURI().toString()));
            imagePreviewBox.setVisible(true);
            imagePreviewBox.setManaged(true);
        }
    }

    @FXML
    private void onRemoveImage() {
        selectedImageFile = null;
        imagePreview.setImage(null);
        imagePreviewBox.setVisible(false);
        imagePreviewBox.setManaged(false);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Called by PostController to retrieve the result
    // ────────────────────────────────────────────────────────────────────────
    public boolean hasContent() {
        return !contentArea.getText().trim().isEmpty();
    }

    public Publication buildPublication() {
        String imagePath = null;

        if (selectedImageFile != null) {
            try {
                Files.createDirectories(Paths.get(UPLOAD_DIR));
                String name = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                java.nio.file.Path target = Paths.get(UPLOAD_DIR + name);
                Files.copy(selectedImageFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                imagePath = UPLOAD_DIR + name;
            } catch (IOException e) {
                e.printStackTrace();
                dashboard.showError("Image upload failed: " + e.getMessage());
            }
        }

        String place = placeField.getText().trim();
        return new Publication(
                dashboard.getCurrentUser(),
                0,
                contentArea.getText().trim(),
                new Date(),
                imagePath,
                place.isEmpty() ? null : place
        );
    }

    public void populateExisting(Publication p) {
        p.setContent(contentArea.getText().trim());
        String place = placeField.getText().trim();
        p.setPlace(place.isEmpty() ? null : place);
    }

    public TextArea getContentArea() { return contentArea; }
}