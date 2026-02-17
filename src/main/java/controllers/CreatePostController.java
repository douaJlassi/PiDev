package controllers;

import entities.Publication;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import services.AgencyService;
import entities.Agency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CreatePostController — bound to create_post_dialog.fxml.
 * Works for BOTH Create (existing=null) and Edit (existing!=null) modes.
 *
 * PostController calls:
 *   1. formCtrl.init(dashboard, existing)   — inject data
 *   2. formCtrl.getContentArea()            — wire the Share/Update enable guard
 *   3. formCtrl.buildPublication()          — on confirm in create mode
 *      formCtrl.populateExisting(pub)       — on confirm in edit mode
 */
public class CreatePostController {

    @FXML private Circle    userAvatarCircle;
    @FXML private Label     usernameLabel;
    @FXML private Label     audienceLabel;
    @FXML private TextArea  contentArea;
    @FXML private StackPane imagePreviewBox;
    @FXML private ImageView imagePreview;
    @FXML private TextField placeField;
    @FXML private ComboBox<Map.Entry<Integer,String>> agencyCombo;
    @FXML private HBox      agencyInfoBanner;
    @FXML private Label     agencyBannerText;
    @FXML private Button    photoBtn;

    private static final String UPLOAD_DIR = "uploads/images/";

    /** Loaded from DB in init(). Key = agencyID, Value = agency name. */
    private final Map<Integer,String> AGENCIES = new LinkedHashMap<>();

    private DashboardController dashboard;
    private File selectedImageFile;

    // ─────────────────────────────────────────────────────────────────────────
    // Initialise — called by PostController after FXMLLoader.load()
    // ─────────────────────────────────────────────────────────────────────────
    public void init(DashboardController dash, Publication existing) {
        this.dashboard = dash;

        // User header
        String uname = dash.getCurrentUser().getUsername();
        usernameLabel.setText(uname != null ? uname
                : "Traveler #" + dash.getCurrentUser().getClientID());

        // Agency combo
        agencyCombo.setConverter(new StringConverter<>() {
            public String toString(Map.Entry<Integer,String> e) {
                return e == null ? "" : e.getValue();
            }
            public Map.Entry<Integer,String> fromString(String s) { return null; }
        });
        // Load real agencies from database
        AGENCIES.put(0, "No agency — post publicly");
        try {
            List<Agency> agencies = new AgencyService().selectALL();
            for (Agency a : agencies) {
                AGENCIES.put(a.getAgencyID(), a.getName());
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace(); // Non-fatal: user can still post without agency
        }

        agencyCombo.setItems(FXCollections.observableArrayList(AGENCIES.entrySet()));
        agencyCombo.getSelectionModel().selectFirst(); // "No agency"

        // Pre-fill when editing
        if (existing != null) {
            contentArea.setText(existing.getContent());
            if (existing.getPlace() != null) placeField.setText(existing.getPlace());
            if (existing.hasImage()) {
                File img = new File(existing.getImagePath());
                if (img.exists()) {
                    imagePreview.setImage(new Image(img.toURI().toString()));
                    imagePreviewBox.setVisible(true);
                    imagePreviewBox.setManaged(true);
                }
            }
            if (existing.hasAgency()) {
                agencyCombo.getItems().stream()
                        .filter(e -> e.getKey() == existing.getAgencyId())
                        .findFirst()
                        .ifPresent(e -> agencyCombo.getSelectionModel().select(e));
                updateAgencyBanner(existing.getAgencyId());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FXML handlers
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void onAgencySelected() {
        int id = selectedAgencyId();
        updateAgencyBanner(id);
        if (id > 0) {
            audienceLabel.setText(AGENCIES.get(id) + " (Pending review)");
        } else {
            audienceLabel.setText("Public Feed");
        }
    }

    @FXML
    private void onSelectPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Photo");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif"));
        Stage stage = (Stage) photoBtn.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
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

    // ─────────────────────────────────────────────────────────────────────────
    // Called by PostController
    // ─────────────────────────────────────────────────────────────────────────
    public TextArea getContentArea() { return contentArea; }

    public boolean hasContent() { return !contentArea.getText().trim().isEmpty(); }

    /** Build a brand-new Publication from the form (create mode). */
    public Publication buildPublication() {
        return new Publication(
                dashboard.getCurrentUser(),
                0,
                contentArea.getText().trim(),
                new Date(),
                uploadIfNeeded(),
                blankToNull(placeField.getText()),
                selectedAgencyId()
        );
    }

    /** Update an existing Publication's mutable fields (edit mode). */
    public void populateExisting(Publication p) {
        p.setContent(contentArea.getText().trim());
        p.setPlace(blankToNull(placeField.getText()));
        int newAgency = selectedAgencyId();
        if (newAgency != p.getAgencyId()) p.setAgencyId(newAgency);
        if (selectedImageFile != null) {
            String path = uploadIfNeeded();
            if (path != null) p.setImagePath(path);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void updateAgencyBanner(int agencyId) {
        boolean show = agencyId > 0;
        agencyInfoBanner.setVisible(show);
        agencyInfoBanner.setManaged(show);
        if (show) {
            agencyBannerText.setText(
                    "Your post will be sent to " + AGENCIES.getOrDefault(agencyId, "the agency") +
                            " for review. It won't appear in the public feed until an agency member approves it.");
        }
    }

    private int selectedAgencyId() {
        var sel = agencyCombo.getSelectionModel().getSelectedItem();
        return sel != null ? sel.getKey() : 0;
    }

    private String uploadIfNeeded() {
        if (selectedImageFile == null) return null;
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            String name = System.currentTimeMillis() + "_" + selectedImageFile.getName();
            java.nio.file.Path target = Paths.get(UPLOAD_DIR + name);
            Files.copy(selectedImageFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return UPLOAD_DIR + name;
        } catch (IOException e) {
            e.printStackTrace();
            if (dashboard != null) dashboard.showError("Image upload failed: " + e.getMessage());
            return null;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}