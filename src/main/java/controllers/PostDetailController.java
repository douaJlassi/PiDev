package controllers;

import entities.Publication;
import entities.WeatherData;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import services.WeatherService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * PostDetailController — two roles:
 *  1. fx:controller for post_detail.fxml (FXML fields + handlers)
 *  2. Loaded programmatically by PostController via show()
 *
 * The FXML carries all layout; this class only binds data and wires events.
 *
 * PHASE 4C UPDATE: Now fetches and displays weather badges in detail view
 * - Larger weather badge (32x32 icon vs 24x24 in card)
 * - Two-line display (temperature + description)
 * - Same caching and error handling as post cards
 */
public class PostDetailController {

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private Button    closeBtn;

    // Left panel
    @FXML private Circle    authorAvatar;
    @FXML private Label     authorNameLabel;
    @FXML private Label     dateLabel;
    @FXML private HBox      placeWeatherRow;   // wrapping HBox for place + weather badge
    @FXML private Label     placeLabel;
    @FXML private Label     postContentLabel;
    @FXML private VBox      imageContainer;
    @FXML private ImageView postImage;
    @FXML private HBox      statsBar;
    @FXML private Label     likesStatLabel;
    @FXML private Label     commentsStatLabel;
    @FXML private Button    likeBtn;
    @FXML private Button    commentBtn2;

    // PHASE 4C: Weather badge (inside placeWeatherRow)
    @FXML private HBox      weatherBadge;
    @FXML private ImageView weatherIcon;
    @FXML private Label     weatherText;
    @FXML private Label     weatherDescription;

    // Right panel
    @FXML private Label     commentCountLabel;
    @FXML private TextField commentTextField;
    @FXML private Button    commentPostBtn;
    @FXML private VBox      commentsList;

    // ── Injected state ────────────────────────────────────────────────────────
    private Publication         publication;
    private DashboardController dashboard;
    private BorderPane          detailView; // root node of the loaded FXML

    // PHASE 4C: Weather service (shared instance for caching)
    private static final WeatherService weatherService = new WeatherService();

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a");

    // ── Constructor ── used by PostController (programmatic entry) ────────────
    public PostDetailController() {}

    public PostDetailController(Publication pub, DashboardController dash) {
        this.publication = pub;
        this.dashboard   = dash;
    }

    // ── show() — loads FXML, injects data, adds overlay ──────────────────────
    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/post_detail.fxml"));
            // Set THIS as controller so @FXML fields are injected here
            loader.setController(this);
            detailView = loader.load();

            bindData();

            // PHASE 4C: Fetch weather if available
            fetchWeatherIfAvailable();

            dashboard.getContentContainer().getChildren().add(detailView);

            FadeTransition fade = new FadeTransition(Duration.millis(220), detailView);
            fade.setFromValue(0); fade.setToValue(1);
            fade.play();

        } catch (IOException e) {
            e.printStackTrace();
            dashboard.showError("Could not open post detail.");
        }
    }

    // ── Data binding ──────────────────────────────────────────────────────────
    private void bindData() {
        // Author
        String username = publication.getClient().getUsername();
        authorNameLabel.setText(username != null ? username
                : "Traveler #" + publication.getClient().getClientID());
        dateLabel.setText(DATE_FMT.format(publication.getDatePublication()));

        // Place — show the whole row (place label + weather badge placeholder)
        if (publication.getPlace() != null && !publication.getPlace().isEmpty()) {
            placeWeatherRow.setVisible(true);
            placeWeatherRow.setManaged(true);
            placeLabel.setText("📍 " + publication.getPlace());
        }

        // Content
        postContentLabel.setText(publication.getContent());

        // Image
        if (publication.hasImage()) {
            File img = new File(publication.getImagePath());
            if (img.exists()) {
                postImage.setImage(new Image(img.toURI().toString()));
                imageContainer.setVisible(true);
                imageContainer.setManaged(true);
            }
        }

        // Stats — query real counts from services (Publication object lists are not populated from DB)
        int likesCount    = dashboard.getLikeController().getLikeCount(publication.getPublicationID());
        int commentsCount = dashboard.getCommentController().getCommentCount(publication.getPublicationID());

        if (likesCount > 0 || commentsCount > 0) {
            statsBar.setVisible(true);
            statsBar.setManaged(true);
        }
        if (likesCount > 0) {
            likesStatLabel.setText("♥ " + likesCount);
            likesStatLabel.setVisible(true);
            likesStatLabel.setManaged(true);
        }
        if (commentsCount > 0) {
            commentsStatLabel.setText("💬 " + commentsCount);
            commentsStatLabel.setVisible(true);
            commentsStatLabel.setManaged(true);
            commentCountLabel.setText("(" + commentsCount + ")");
        }

        // Like button
        dashboard.getLikeController().initButton(likeBtn, publication);

        // FIX: loadComments only takes 2 arguments
        dashboard.getCommentController().loadComments(publication, commentsList);

        // Enable comment post button when text entered
        commentTextField.textProperty().addListener((obs, old, newVal) -> {
            commentPostBtn.setDisable(newVal == null || newVal.trim().isEmpty());
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    // PHASE 4C: Weather fetching
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Fetch weather if post has location (async)
     * Same logic as PostCardController but larger display
     */
    private void fetchWeatherIfAvailable() {
        if (publication.getPlace() == null || publication.getPlace().trim().isEmpty()) {
            return;
        }

        // Don't fetch weather for very old posts (>7 days)
        long postAgeHours = (System.currentTimeMillis() -
                publication.getDatePublication().getTime()) / (1000 * 60 * 60);
        if (postAgeHours > 168) {
            return;
        }

        String locationName = publication.getPlace();

        weatherService.getWeatherByLocation(locationName)
                .thenAccept(weather -> {
                    Platform.runLater(() -> {
                        if (weather != null && weather.isValid()) {
                            displayWeatherBadge(weather);
                        }
                    });
                })
                .exceptionally(error -> {
                    System.err.println("Weather fetch failed for '" + locationName + "': " + error.getMessage());
                    return null;
                });
    }

    /**
     * Display weather badge — shown inline beside the place label.
     */
    private void displayWeatherBadge(WeatherData weather) {
        weatherText.setText(weather.getFormattedTemperature());

        if (weather.getWeatherDescription() != null) {
            String description = weather.getWeatherDescription();
            description = description.substring(0, 1).toUpperCase() + description.substring(1);
            weatherDescription.setText(description);
        }

        String iconUrl = weather.getIconUrl();
        if (iconUrl != null) {
            try {
                Image icon = new Image(iconUrl, true);
                weatherIcon.setImage(icon);
            } catch (Exception e) {
                System.err.println("Weather icon load failed: " + e.getMessage());
            }
        }

        Tooltip tooltip = new Tooltip(weather.getTooltipText());
        tooltip.getStyleClass().add("weather-tooltip");
        Tooltip.install(weatherBadge, tooltip);

        // Show the badge (it lives inside placeWeatherRow which is already visible)
        weatherBadge.setVisible(true);
        weatherBadge.setManaged(true);
    }

    // ── FXML event handlers ───────────────────────────────────────────────────
    @FXML
    private void onClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(180), detailView);
        fade.setFromValue(1); fade.setToValue(0);
        fade.setOnFinished(e -> dashboard.getContentContainer().getChildren().remove(detailView));
        fade.play();
    }

    @FXML
    private void onLikeClicked() {
        dashboard.getLikeController().handleToggle(publication, likeBtn);
    }

    @FXML
    private void onCommentFocusClicked() {
        commentTextField.requestFocus();
    }

    @FXML
    private void onCommentSubmit() {
        String text = commentTextField.getText();
        if (text == null || text.trim().isEmpty()) return;

        dashboard.getCommentController().addComment(
                publication, text.trim(), commentsList, commentCountLabel);

        commentTextField.clear();

        // Scroll to bottom
        commentsList.layout();
        if (commentsList.getParent() instanceof javafx.scene.control.ScrollPane) {
            javafx.scene.control.ScrollPane sp =
                    (javafx.scene.control.ScrollPane) commentsList.getParent();
            sp.setVvalue(1.0);
        }
    }
}