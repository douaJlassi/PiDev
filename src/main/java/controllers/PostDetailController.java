package controllers;

import entities.Publication;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

/**
 * PostDetailController — two roles:
 *  1. fx:controller for post_detail.fxml (FXML fields + handlers)
 *  2. Loaded programmatically by PostController via show()
 *
 * The FXML carries all layout; this class only binds data and wires events.
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
    @FXML private Button    shareBtn;

    // Right panel
    @FXML private Label     commentCountLabel;
    @FXML private TextField commentTextField;
    @FXML private Button    commentPostBtn;
    @FXML private VBox      commentsList;

    // ── Injected state ────────────────────────────────────────────────────────
    private Publication         publication;
    private DashboardController dashboard;
    private BorderPane          detailView; // root node of the loaded FXML

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
            placeLabel.setText("📍 " + publication.getPlace());
            placeLabel.setVisible(true); placeLabel.setManaged(true);
        }

        // Content
        postContentLabel.setText(publication.getContent());

        // Image
        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            File imgFile = new File(publication.getImagePath());
            if (imgFile.exists()) {
                postImage.setImage(new Image(imgFile.toURI().toString()));
                imageContainer.setVisible(true);
                imageContainer.setManaged(true);
            }
        }

        // Stats
        try {
            int likes    = dashboard.getLikeService().getLikeCount(publication.getPublicationID());
            int comments = dashboard.getCommentService().getCommentCount(publication.getPublicationID());
            if (likes > 0) {
                likesStatLabel.setText("♥ " + likes);
                likesStatLabel.setVisible(true); likesStatLabel.setManaged(true);
            }
            if (comments > 0) {
                commentsStatLabel.setText(comments + " comment" + (comments != 1 ? "s" : ""));
                commentsStatLabel.setVisible(true); commentsStatLabel.setManaged(true);
            }
        } catch (SQLException ignored) {}

        // Like button
        dashboard.getLikeController().initButton(likeBtn, publication);

        // Comment count
        int count = dashboard.getCommentController().getCommentCount(publication.getPublicationID());
        commentCountLabel.setText("(" + count + ")");

        // Comment input — enable Post button only when text present
        commentPostBtn.setDisable(true);
        commentTextField.textProperty().addListener(
                (obs, o, n) -> commentPostBtn.setDisable(n.trim().isEmpty()));

        // Load comments
        dashboard.getCommentController().loadComments(publication, commentsList);
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────
    @FXML
    private void onClose() {
        if (detailView == null) return;
        FadeTransition fade = new FadeTransition(Duration.millis(180), detailView);
        fade.setFromValue(1); fade.setToValue(0);
        fade.setOnFinished(e -> {
            dashboard.getContentContainer().getChildren().remove(detailView);
            detailView = null;
        });
        fade.play();
    }

    @FXML
    private void onLikeClicked() {
        dashboard.getLikeController().handleToggle(publication, likeBtn);
        // Refresh stat label
        int likes = dashboard.getLikeController().getLikeCount(publication.getPublicationID());
        if (likes > 0) {
            likesStatLabel.setText("♥ " + likes);
            likesStatLabel.setVisible(true); likesStatLabel.setManaged(true);
        } else {
            likesStatLabel.setVisible(false); likesStatLabel.setManaged(false);
        }
    }

    @FXML
    private void onCommentFocusClicked() {
        // Focus the comment input field
        commentTextField.requestFocus();
    }

    @FXML
    private void onCommentSubmit() {
        String text = commentTextField.getText().trim();
        if (text.isEmpty()) return;

        dashboard.getCommentController()
                .addComment(publication, text, commentsList, commentCountLabel);

        commentTextField.clear();

        // Scroll to bottom
        javafx.application.Platform.runLater(() -> {
            commentsList.getParent().getParent().requestLayout();
        });
    }
}