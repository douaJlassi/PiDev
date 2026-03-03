package controllers;

import entities.Publication;
import entities.WeatherData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import services.WeatherService;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * PostCardController — bound to post_card.fxml.
 * Only data-binding and event wiring; zero manual node construction.
 *
 * PHASE 4B UPDATE: Now fetches and displays weather badges
 * - Fetches weather asynchronously if post has location
 * - Uses WeatherCache for performance (30-min TTL)
 * - Shows weather icon + temperature
 * - Tooltip with detailed weather info
 */
public class PostCardController {

    // ── Header ──────────────────────────────────────────────────────────────
    @FXML private Circle   avatarCircle;
    @FXML private Label    authorNameLabel;
    @FXML private Label    dateLabel;
    @FXML private Label    placeLabel;
    @FXML private Button   menuBtn;
    @FXML private HBox     metaBox;      // second-row container for place+weather

    // ── PHASE 4B: Weather badge ─────────────────────────────────────────────
    @FXML private HBox     weatherBadge;
    @FXML private ImageView weatherIcon;
    @FXML private Label    weatherText;

    // ── Stats bar ────────────────────────────────────────────────────────────
    @FXML private HBox     statsBar;     // hidden when no likes or comments

    // ── Content ──────────────────────────────────────────────────────────────
    @FXML private Label    contentLabel;

    // ── Image ────────────────────────────────────────────────────────────────
    @FXML private VBox     imageContainer;
    @FXML private ImageView postImage;

    // ── Stats ────────────────────────────────────────────────────────────────
    @FXML private Label    likesStatLabel;
    @FXML private Label    commentsStatLabel;

    // ── Actions ──────────────────────────────────────────────────────────────
    @FXML private Button   likeBtn;
    @FXML private Button   commentBtn;

    // ── Injected by PostController after load ────────────────────────────────
    private Publication         publication;
    private boolean             isGridView;
    private WajdiDashboardController dashboard;

    // PHASE 4B: Weather service (shared instance for caching)
    private static final WeatherService weatherService = new WeatherService();

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM dd 'at' hh:mm a");

    // ────────────────────────────────────────────────────────────────────────
    // Called by PostController after FXMLLoader.load()
    // ────────────────────────────────────────────────────────────────────────
    public void init(Publication pub, boolean gridView, WajdiDashboardController dash) {
        this.publication = pub;
        this.isGridView  = gridView;
        this.dashboard   = dash;

        bindData();

        // PHASE 4B: Fetch weather if post has location
        fetchWeatherIfAvailable();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Data binding
    // ────────────────────────────────────────────────────────────────────────
    private void bindData() {
        // Author
        String username = publication.getClient().getUsername();
        authorNameLabel.setText(username != null ? username
                : "Traveler #" + publication.getClient().getClientID());

        // Date
        dateLabel.setText(DATE_FMT.format(publication.getDatePublication()));

        // Place — show second row (metaBox) containing place tag + weather
        if (publication.getPlace() != null && !publication.getPlace().isEmpty()) {
            placeLabel.setVisible(true); placeLabel.setManaged(true);
            placeLabel.setText("📍 " + publication.getPlace());
            metaBox.setVisible(true);    metaBox.setManaged(true);
        }

        // Content
        contentLabel.setText(publication.getContent());

        // Image
// Image
        if (publication.hasImage()) {
            File img = new File(publication.getImagePath());
            if (img.exists()) {
                postImage.setImage(new Image(img.toURI().toString()));
                imageContainer.setVisible(true);
                imageContainer.setManaged(true);
                postImage.setFitWidth(isGridView ? 420 : 680);

                // ── LIGHTBOX: click image to zoom ──────────────────────────────
                imageContainer.setStyle("-fx-cursor: hand;");
                imageContainer.setOnMouseClicked(e -> {
                    e.consume(); // Don't bubble up to the card's detail-view handler
                    ImageLightboxOverlay.show(
                            dashboard.getContentContainer(),
                            postImage.getImage()
                    );
                });
            }
        }

        // Stats — only show the bar when there is something to display
        int likesCount    = publication.getLikes()    != null ? publication.getLikes().size()    : 0;
        int commentsCount = publication.getComments() != null ? publication.getComments().size() : 0;

        if (likesCount > 0) {
            likesStatLabel.setText("♥ " + likesCount);
            likesStatLabel.setVisible(true);
            likesStatLabel.setManaged(true);
        }
        if (commentsCount > 0) {
            commentsStatLabel.setText("💬 " + commentsCount);
            commentsStatLabel.setVisible(true);
            commentsStatLabel.setManaged(true);
        }
        // Show the bar itself only when at least one stat is populated
        if (likesCount > 0 || commentsCount > 0) {
            statsBar.setVisible(true);
            statsBar.setManaged(true);
        }

        // Like button (delegated)
        dashboard.getLikeController().initButton(likeBtn, publication);
    }

    // ────────────────────────────────────────────────────────────────────────
    // PHASE 4B: Weather fetching
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Fetch weather if post has location (async)
     * - Only fetches if publication.getPlace() is not null
     * - Uses WeatherCache (instant if cached)
     * - Updates UI on Platform thread
     * - Fails silently if error (no weather badge shown)
     */
    private void fetchWeatherIfAvailable() {
        // Check if post has location
        if (publication.getPlace() == null || publication.getPlace().trim().isEmpty()) {
            return; // No location = no weather
        }

        // Don't fetch weather for very old posts (>7 days)
        // Weather from a week ago is not relevant/accurate
        long postAgeHours = (System.currentTimeMillis() -
                publication.getDatePublication().getTime()) / (1000 * 60 * 60);
        if (postAgeHours > 720) { // 720 hours = 30 days (matches PostDetailController)
            return; // Old post = skip weather
        }

        // Fetch weather asynchronously
        String locationName = publication.getPlace();

        weatherService.getWeatherByLocation(locationName)
                .thenAccept(weather -> {
                    // Update UI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (weather != null && weather.isValid()) {
                            displayWeatherBadge(weather);
                        }
                        // If weather is null (error/timeout), silently do nothing
                        // Post displays normally without weather badge
                    });
                })
                .exceptionally(error -> {
                    // Log error but don't show to user (weather is optional)
                    System.err.println("Weather fetch failed for '" + locationName + "': " + error.getMessage());
                    return null;
                });
    }



    /**
     * Display weather badge with icon and temperature
     * - Shows weather icon from OpenWeatherMap
     * - Shows temperature in Celsius
     * - Adds tooltip with detailed info (hover for full weather)
     */
    private void displayWeatherBadge(WeatherData weather) {
        // Set temperature text
        weatherText.setText(weather.getFormattedTemperature());

        // Load weather icon (async to avoid blocking UI)
        String iconUrl = weather.getIconUrl();
        if (iconUrl != null) {
            try {
                // Background loading (true = load in background thread)
                Image icon = new Image(iconUrl, true);
                weatherIcon.setImage(icon);
            } catch (Exception e) {
                // If icon fails to load, weather badge still shows with text
                System.err.println("Weather icon load failed: " + e.getMessage());
            }
        }

        // Create tooltip with detailed weather info
        Tooltip tooltip = new Tooltip(weather.getTooltipText());
        tooltip.getStyleClass().add("weather-tooltip");
        Tooltip.install(weatherBadge, tooltip);

        // Show the weather badge
        weatherBadge.setVisible(true);
        weatherBadge.setManaged(true);
    }

    // ────────────────────────────────────────────────────────────────────────
    // FXML event handlers
    // ────────────────────────────────────────────────────────────────────────

    @FXML
    private void onLikeClicked() {
        dashboard.getLikeController().handleToggle(publication, likeBtn);
    }

    @FXML
    private void onCommentClicked() {
        // Open post detail view (overlay with comments panel)
        new PostDetailController(publication, dashboard).show();
    }

    @FXML
    private void onMenuClicked() {
        dashboard.getPostController().showPostMenu(publication, menuBtn);
    }
}