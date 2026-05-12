package controllers;

import entities.Publication;
import entities.WeatherData;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
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
    @FXML private Label     placeLabel;
    @FXML private Label     postContentLabel;
    @FXML private VBox      imageContainer;
    @FXML private ImageView postImage;
    @FXML private Label     likesStatLabel;
    @FXML private Label     commentsStatLabel;
    @FXML private Button    likeBtn;
    @FXML private Button    commentBtn2;

    // Place+weather wrapper row & stats bar (matched to redesigned FXML)
    @FXML private HBox      placeWeatherRow;
    // PHASE 4C: Weather badge
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
    private WajdiDashboardController dashboard;
    private BorderPane          detailView; // root node of the loaded FXML

    // PHASE 4C: Weather service (shared instance for caching)
    private static final WeatherService weatherService = new WeatherService();

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a");

    // ── Constructor ── used by PostController (programmatic entry) ────────────
    public PostDetailController() {}

    public PostDetailController(Publication pub, WajdiDashboardController dash) {
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

        // Place
        if (publication.getPlace() != null && !publication.getPlace().isEmpty()) {
            placeWeatherRow.setVisible(true);
            placeWeatherRow.setManaged(true);
            placeLabel.setText("📍 " + publication.getPlace());
        }

        // Content
        postContentLabel.setText(publication.getContent());

// Image — wait for layout then bind width
// Image
        if (publication.hasImage()) {
            File img = new File(publication.getImagePath());
            if (img.exists()) {
                postImage.setImage(new Image(img.toURI().toString()));
                imageContainer.setVisible(true);
                imageContainer.setManaged(true);

                javafx.application.Platform.runLater(() -> {
                    javafx.application.Platform.runLater(() -> {
                        double w = imageContainer.getWidth();
                        if (w > 0) {
                            setupImage(w);
                        } else {
                            imageContainer.widthProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
                                @Override
                                public void changed(javafx.beans.value.ObservableValue<? extends Number> obs,
                                                    Number oldW, Number newW) {
                                    if (newW.doubleValue() > 0) {
                                        setupImage(newW.doubleValue());
                                        imageContainer.widthProperty().removeListener(this);
                                    }
                                }
                            });
                        }
                    });
                });
            }
        }

        // Stats — always query DB for accurate counts
        int likesCount    = dashboard.getLikeController().getLikeCount(publication.getPublicationID());
        int commentsCount = dashboard.getCommentController().getCommentCount(publication.getPublicationID());

        likesStatLabel.setText("♥ " + likesCount);
        likesStatLabel.setVisible(true);
        likesStatLabel.setManaged(true);
        commentsStatLabel.setText("💬 " + commentsCount);
        commentsStatLabel.setVisible(true);
        commentsStatLabel.setManaged(true);

        // Like button
        dashboard.getLikeController().initButton(likeBtn, publication);

        // Comments panel header count pill
        commentCountLabel.setText(String.valueOf(commentsCount));

        // Load comments list
        dashboard.getCommentController().loadComments(publication, commentsList);

        // Enable comment post button only when text is entered
        commentTextField.textProperty().addListener((obs, old, newVal) ->
                commentPostBtn.setDisable(newVal == null || newVal.trim().isEmpty()));
    }

    private void applyRoundedClip(ImageView iv) {
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        clip.widthProperty().bind(iv.fitWidthProperty());
        // Height must track the actual rendered bounds, not fitHeight
        iv.boundsInLocalProperty().addListener((obs, o, n) -> {
            clip.setHeight(n.getHeight());
        });
        // Set initial height in case bounds already have a value
        clip.setHeight(iv.getBoundsInLocal().getHeight());
        iv.setClip(clip);
    }

    private void applyHoverZoom(ImageView iv) {
        // Wrap in a clip-respecting container — scale from center
        iv.setStyle("-fx-cursor: hand;");

        ScaleTransition zoomIn = new ScaleTransition(Duration.millis(200), iv);
        zoomIn.setToX(1.04);
        zoomIn.setToY(1.04);
        zoomIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        ScaleTransition zoomOut = new ScaleTransition(Duration.millis(180), iv);
        zoomOut.setToX(1.0);
        zoomOut.setToY(1.0);
        zoomOut.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        iv.setOnMouseEntered(e -> { zoomOut.stop(); zoomIn.play(); });
        iv.setOnMouseExited(e ->  { zoomIn.stop();  zoomOut.play(); });

        // Click still opens lightbox
        iv.setOnMouseClicked(e -> {
            e.consume();
            ImageLightboxOverlay.show(dashboard.getContentContainer(), iv.getImage());
        });
    }
    private void setupImage(double containerWidth) {
        // Image fills ~85% of container width — dominant, not boxed
        double imgW = containerWidth * 0.85;
        double imgH = 420; // tall hero — dominates the top half

        postImage.setFitWidth(imgW);
        postImage.setFitHeight(imgH);

        // Rounded corners via clip
        applyRoundedClip(postImage);

        // Programmatic shadow — bypasses ScrollPane clipping
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.35));
        shadow.setRadius(20);
        shadow.setSpread(0.05);
        shadow.setOffsetX(0);
        shadow.setOffsetY(6);
        postImage.setEffect(shadow);

        // Hover zoom
        applyHoverZoom(postImage);
    }

    // ────────────────────────────────────────────────────────────────────────
    // PHASE 4C: Weather fetching
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Fetch weather if post has location (async)
     * Same logic as PostCardController but larger display
     */
    private void fetchWeatherIfAvailable() {
        // Check if post has location
        if (publication.getPlace() == null || publication.getPlace().trim().isEmpty()) {
            return;
        }

        // Don't fetch weather for very old posts (>7 days)
        long postAgeHours = (System.currentTimeMillis() -
                publication.getDatePublication().getTime()) / (1000 * 60 * 60);
        if (postAgeHours > 720) {
            return;
        }

        // Fetch weather asynchronously
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
     * Display weather badge with icon, temperature, and description
     * Enhanced version for detail view (larger, more info)
     */
    private void displayWeatherBadge(WeatherData weather) {
        // Set temperature text
        weatherText.setText(weather.getFormattedTemperature());

        // Set weather description (e.g., "Clear sky")
        if (weather.getWeatherDescription() != null) {
            String description = weather.getWeatherDescription();
            // Capitalize first letter
            description = description.substring(0, 1).toUpperCase() + description.substring(1);
            weatherDescription.setText(description);
        }

        // Load weather icon (async)
        String iconUrl = weather.getIconUrl();
        if (iconUrl != null) {
            try {
                Image icon = new Image(iconUrl, true); // background loading
                weatherIcon.setImage(icon);
            } catch (Exception e) {
                System.err.println("Weather icon load failed: " + e.getMessage());
            }
        }

        // Create tooltip with detailed info
        Tooltip tooltip = new Tooltip(weather.getTooltipText());
        tooltip.getStyleClass().add("weather-tooltip");
        Tooltip.install(weatherBadge, tooltip);

        // Show the weather badge
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

    // In PostDetailController — fix onCommentSubmit to pass the header label
    @FXML
    private void onCommentSubmit() {
        String text = commentTextField.getText();
        if (text == null || text.trim().isEmpty()) return;

        dashboard.getCommentController().addComment(
                publication, text.trim(), commentsList, commentCountLabel); // ← was missing commentCountLabel

        commentTextField.clear();

        commentsList.layout();
        if (commentsList.getParent() instanceof ScrollPane sp) {
            sp.setVvalue(1.0);
        }
    }
}