package controllers;

import entities.Publication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

/**
 * PostCardController — bound to post_card.fxml.
 * Only data-binding and event wiring; zero manual node construction.
 */
public class PostCardController {

    // ── Header ──────────────────────────────────────────────────────────────
    @FXML private Circle   avatarCircle;
    @FXML private Label    authorNameLabel;
    @FXML private Label    dateLabel;
    @FXML private Label    placeDot;
    @FXML private Label    placeLabel;
    @FXML private Button   menuBtn;

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
    private DashboardController dashboard;

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM dd 'at' hh:mm a");

    // ────────────────────────────────────────────────────────────────────────
    // Called by PostController after FXMLLoader.load()
    // ────────────────────────────────────────────────────────────────────────
    public void init(Publication pub, boolean gridView, DashboardController dash) {
        this.publication = pub;
        this.isGridView  = gridView;
        this.dashboard   = dash;

        bindData();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Data binding
    // ────────────────────────────────────────────────────────────────────────
    private void bindData() {
        // Author
        String username = pub(publication).getClient().getUsername();
        authorNameLabel.setText(username != null ? username
                : "Traveler #" + publication.getClient().getClientID());

        // Date
        dateLabel.setText(DATE_FMT.format(publication.getDatePublication()));

        // Place
        if (publication.getPlace() != null && !publication.getPlace().isEmpty()) {
            placeDot.setVisible(true);  placeDot.setManaged(true);
            placeLabel.setText("📍 " + publication.getPlace());
            placeLabel.setVisible(true); placeLabel.setManaged(true);
        }

        // Content — truncate in grid view
        String text = publication.getContent();
        contentLabel.setText(isGridView && text.length() > 200
                ? text.substring(0, 200) + "…" : text);

        // Image
        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            File imgFile = new File(publication.getImagePath());
            if (imgFile.exists()) {
                postImage.setImage(new Image(imgFile.toURI().toString()));
                postImage.setFitWidth(isGridView ? 420 : 680);
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

        // Like button initial state
        refreshLikeButton();

        // Menu — only show for own posts
        menuBtn.setVisible(
                dashboard.getCurrentUser().getClientID() == publication.getClient().getClientID());
    }

    private void refreshLikeButton() {
        try {
            int     count   = dashboard.getLikeService().getLikeCount(publication.getPublicationID());
            boolean liked   = dashboard.getLikeService()
                    .hasUserLiked(publication.getPublicationID(),
                            dashboard.getCurrentUser().getClientID());
            likeBtn.setText(liked ? "♥  " + count : "♡  " + (count > 0 ? count : "Like"));
            if (liked) { likeBtn.getStyleClass().add("liked"); }
            else        { likeBtn.getStyleClass().remove("liked"); }
        } catch (SQLException e) {
            likeBtn.setText("♡  Like");
        }
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
        // Open detail view so user can see and write comments
        new PostDetailController(publication, dashboard).show();
    }

    @FXML
    private void onMenuClicked() {
        dashboard.getPostController().showPostMenu(publication, menuBtn);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helper
    // ────────────────────────────────────────────────────────────────────────
    private Publication pub(Publication p) { return p; }
}