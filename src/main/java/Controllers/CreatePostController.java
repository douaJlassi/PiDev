package Controllers;

import entities.Publication;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import services.AgencyService;
import entities.Agency;

import javafx.application.Platform;
import services.UnsplashService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
 * PHASE 3 UPDATE: Now uses PlacesAutocompleteField for location input
 * - Replaces plain TextField with autocomplete dropdown
 * - Stores both place name AND place_id
 * - Falls back to plain text if user doesn't select from dropdown
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
    @FXML private VBox      placeFieldContainer;  // Container for custom field (since FXML can't instantiate custom controls)
    @FXML private ComboBox<Map.Entry<Integer,String>> agencyCombo;
    @FXML private HBox      agencyInfoBanner;
    @FXML private Label     agencyBannerText;
    @FXML private Button    photoBtn;

    private static final String UPLOAD_DIR = "uploads/images/";

    /** Loaded from DB in init(). Key = agencyID, Value = agency name. */
    private final Map<Integer,String> AGENCIES = new LinkedHashMap<>();

    private WajdiDashboardController dashboard;
    private File selectedImageFile;

    // Unsplash integration
    private final UnsplashService unsplashService = new UnsplashService();
    private Button                suggestPhotoBtn; // injected programmatically

    // PHASE 3: Custom autocomplete field (created programmatically)
    private PlacesAutocompleteField placeField;

    // ─────────────────────────────────────────────────────────────────────────
    // Initialise — called by PostController after FXMLLoader.load()
    // ─────────────────────────────────────────────────────────────────────────
    public void init(WajdiDashboardController dash, Publication existing) {
        this.dashboard = dash;

        // PHASE 3: Create and configure autocomplete field
        setupLocationField();

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

            // PHASE 3: Set location with place_id if available
            if (existing.getPlace() != null) {
                if (existing.getPlaceId() != null) {
                    // Has place_id: set it programmatically (from Nominatim selection)
                    placeField.setSelectedPlace(existing.getPlaceId(), existing.getPlace());
                } else {
                    // No place_id: just set the text (user typed it manually)
                    placeField.setText(existing.getPlace());
                }
            }

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
    // PHASE 3: Setup custom location autocomplete field
    // ─────────────────────────────────────────────────────────────────────────
    private void setupLocationField() {
        // Create the custom autocomplete field
        placeField = new PlacesAutocompleteField();
        placeField.setPromptText("Enter location (e.g., Tunis, Sahara...)");
        placeField.setPrefWidth(350); // Fill container width

        // ── Unsplash "Suggest Photo" button ─────────────────────────────────
        suggestPhotoBtn = new Button("✦ Suggest Photo");
        suggestPhotoBtn.getStyleClass().add("unsplash-suggest-btn");
        suggestPhotoBtn.setVisible(false);
        suggestPhotoBtn.setManaged(false);
        suggestPhotoBtn.setOnAction(e -> onSuggestPhoto());

        // Show the button once the user has typed at least 2 chars in the location field
        placeField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasLocation = newVal != null && newVal.trim().length() >= 2;
            suggestPhotoBtn.setVisible(hasLocation);
            suggestPhotoBtn.setManaged(hasLocation);
        });

        // Add to container (replaces what would have been <TextField fx:id="placeField"/> in FXML)
        placeFieldContainer.getChildren().clear();
        placeFieldContainer.getChildren().addAll(placeField, suggestPhotoBtn);
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

    /**
     * Called when user clicks "✦ Suggest Photo".
     * Fetches a photo from Unsplash based on the typed location,
     * downloads it to uploads/images/, then shows it in the preview
     * exactly as if the user had selected it manually.
     */
    private void onSuggestPhoto() {
        String location = placeField.getLocationText();
        if (location == null || location.trim().isEmpty()) return;

        // Disable button and show loading state while fetching
        suggestPhotoBtn.setDisable(true);
        suggestPhotoBtn.setText("⟳ Fetching...");

        unsplashService.fetchPhoto(location)
                .thenAccept(result -> Platform.runLater(() -> {
                    suggestPhotoBtn.setDisable(false);
                    suggestPhotoBtn.setText("✦ Suggest Photo");

                    if (result == null) {
                        if (dashboard != null) {
                            dashboard.showInfo("No photo found for \"" + location + "\". " +
                                    "Try a more specific location or upload your own.");
                        }
                        return;
                    }

                    // Download the image to local uploads directory
                    File downloaded = downloadUnsplashPhoto(result.regularUrl(), location);
                    if (downloaded != null) {
                        selectedImageFile = downloaded;
                        imagePreview.setImage(new javafx.scene.image.Image(
                                downloaded.toURI().toString(), true));
                        imagePreviewBox.setVisible(true);
                        imagePreviewBox.setManaged(true);

                        // Show attribution as required by Unsplash TOS
                        if (dashboard != null) {
                            dashboard.showInfo(
                                    "Photo by " + result.photographer() + " on Unsplash ✦\n" +
                                            "The photo has been added to your post.");
                        }
                    } else {
                        if (dashboard != null) {
                            dashboard.showError("Could not download the photo. Please try again or upload your own.");
                        }
                    }
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> {
                        suggestPhotoBtn.setDisable(false);
                        suggestPhotoBtn.setText("✦ Suggest Photo");
                        if (dashboard != null) dashboard.showError("Photo fetch failed: " + err.getMessage());
                    });
                    return null;
                });
    }

    /**
     * Downloads a photo from Unsplash to the local uploads/images/ directory.
     * Returns the downloaded File, or null on failure.
     */
    private File downloadUnsplashPhoto(String imageUrl, String location) {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));

            // Sanitise location for use in filename
            String safeName = location.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
            String filename  = System.currentTimeMillis() + "_unsplash_" + safeName + ".jpg";
            java.nio.file.Path target = Paths.get(UPLOAD_DIR + filename);

            // Stream from Unsplash URL directly to disk
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);
            // Required by Unsplash API — identifies the download as triggered by the API
            conn.setRequestProperty("User-Agent", "Rehletna/1.0");
            conn.connect();

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return target.toFile();

        } catch (Exception e) {
            System.err.println("[CreatePostController] Unsplash download failed: " + e.getMessage());
            return null;
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
    // Called by WajdiDashboardController (slide-in panel)
    // ─────────────────────────────────────────────────────────────────────────
    public TextArea getContentArea() { return contentArea; }

    public boolean hasContent() { return !contentArea.getText().trim().isEmpty(); }

    /** Build a brand-new Publication from the form (create mode). */
    public Publication buildPublication() {
        Publication pub = new Publication(
                dashboard.getCurrentUser(),
                0,
                contentArea.getText().trim(),
                new Date(),
                uploadIfNeeded(),
                blankToNull(getLocationText()),  // PHASE 3: Use helper method
                selectedAgencyId()
        );

        // PHASE 3: Set place_id if user selected from autocomplete
        if (placeField.hasSelectedPlace()) {
            pub.setPlaceId(placeField.getSelectedPlaceId());
        }

        return pub;
    }

    /** Update an existing Publication's mutable fields (edit mode). */
    public void populateExisting(Publication p) {
        p.setContent(contentArea.getText().trim());

        // PHASE 3: Update place and place_id
        p.setPlace(blankToNull(getLocationText()));
        if (placeField.hasSelectedPlace()) {
            p.setPlaceId(placeField.getSelectedPlaceId());
        } else {
            // User typed manually (no selection from dropdown)
            p.setPlaceId(null);
        }

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

    /**
     * PHASE 3: Get location text (selected name or typed text)
     * - If user selected from dropdown: returns selected name (e.g., "Tunis, Tunisia")
     * - If user typed manually: returns typed text (e.g., "My Secret Beach")
     */
    private String getLocationText() {
        return placeField.getLocationText();
    }

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