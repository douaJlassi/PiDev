package controllers;

import entities.Publication;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

/**
 * PostDetailController - Handles the full post detail view
 * - Split-screen layout (post left, comments right)
 * - Full post content display
 * - Integrated comments
 * - Animated transitions
 */
public class PostDetailController {

    private final Publication publication;
    private final DashboardController dashboardController;
    private BorderPane detailView;

    public PostDetailController(Publication publication, DashboardController dashboardController) {
        this.publication = publication;
        this.dashboardController = dashboardController;
    }

    /**
     * Show the post detail view with animation
     */
    public void show() {
        detailView = new BorderPane();
        detailView.getStyleClass().add("post-detail-view");

        // Top bar with close button
        HBox topBar = createTopBar();
        detailView.setTop(topBar);

        // Split view - post content left, comments right
        HBox splitView = new HBox();
        splitView.getStyleClass().add("split-view");

        VBox leftPanel = createPostContentPanel();
        VBox rightPanel = createCommentsPanel();

        splitView.getChildren().addAll(leftPanel, rightPanel);
        detailView.setCenter(splitView);

        // Add to container with fade animation
        dashboardController.getContentContainer().getChildren().add(detailView);

        FadeTransition fade = new FadeTransition(Duration.millis(250), detailView);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /**
     * Close the detail view with animation
     */
    private void close() {
        if (detailView != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(200), detailView);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(e -> {
                dashboardController.getContentContainer().getChildren().remove(detailView);
                detailView = null;
            });
            fade.play();
        }
    }

    /**
     * Create top bar with close button and title
     */
    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("detail-top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 20, 16, 20));

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("close-detail-btn");
        closeBtn.setOnAction(e -> close());

        Label titleLabel = new Label("Post Details");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        topBar.getChildren().addAll(closeBtn, spacer1, titleLabel, spacer2, new Region());

        return topBar;
    }

    /**
     * Create left panel with post content
     */
    private VBox createPostContentPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("post-content-panel");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        // Author header
        HBox authorBox = new HBox(12);
        authorBox.setAlignment(Pos.CENTER_LEFT);

        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(48, 48);
        avatar.setMinSize(48, 48);
        avatar.setMaxSize(48, 48);

        VBox authorInfo = new VBox(4);
        Label authorName = new Label(publication.getClient().getUsername() != null ?
                publication.getClient().getUsername() : "Traveler #" + publication.getClient().getClientID());
        authorName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a");
        Label dateLabel = new Label(dateFormat.format(publication.getDatePublication()));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");

        authorInfo.getChildren().addAll(authorName, dateLabel);
        authorBox.getChildren().addAll(avatar, authorInfo);

        content.getChildren().add(authorBox);

        // Location if exists
        if (publication.getPlace() != null && !publication.getPlace().isEmpty()) {
            Label placeLabel = new Label("📍 " + publication.getPlace());
            placeLabel.getStyleClass().add("post-place-tag");
            placeLabel.setStyle(placeLabel.getStyle() + "-fx-font-size: 14px; -fx-padding: 6 12;");
            content.getChildren().add(placeLabel);
        }

        // Content text
        Text contentText = new Text(publication.getContent());
        contentText.setStyle("-fx-font-size: 16px; -fx-fill: #050505; -fx-line-spacing: 4;");
        contentText.setWrappingWidth(580);

        content.getChildren().add(contentText);

        // Image
        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            try {
                File imageFile = new File(publication.getImagePath());
                if (imageFile.exists()) {
                    ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
                    imageView.setFitWidth(600);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
                    content.getChildren().add(imageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Stats
        HBox stats = createStatsBar();
        content.getChildren().add(stats);

        // Actions
        HBox actions = createActionsBar();
        content.getChildren().add(actions);

        scrollPane.setContent(content);
        panel.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return panel;
    }

    /**
     * Create right panel with comments
     */
    private VBox createCommentsPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("comments-panel");

        // Comments header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle("-fx-border-color: #e4e6eb; -fx-border-width: 0 0 1 0;");

        Label commentsTitle = new Label("Comments");
        commentsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label countLabel = new Label();
        countLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #65676b; -fx-padding: 0 0 0 8;");

        try {
            int count = dashboardController.getCommentService().getCommentCount(publication.getPublicationID());
            countLabel.setText("(" + count + ")");
        } catch (SQLException e) {
            countLabel.setText("(0)");
        }

        header.getChildren().addAll(commentsTitle, countLabel);

        // Comments list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox commentsList = new VBox(12);
        commentsList.setPadding(new Insets(16, 20, 16, 20));

        scrollPane.setContent(commentsList);

        // Comment input at top
        HBox commentInput = dashboardController.getCommentController()
                .createCommentInput(publication, commentsList, countLabel);

        // Load comments
        dashboardController.getCommentController().loadComments(publication, commentsList);

        panel.getChildren().addAll(header, commentInput, scrollPane);

        return panel;
    }

    /**
     * Create stats bar for detail view
     */
    private HBox createStatsBar() {
        HBox stats = new HBox(16);
        stats.getStyleClass().add("post-stats");
        stats.setAlignment(Pos.CENTER_LEFT);

        int likeCount = dashboardController.getLikeController().getLikeCount(publication.getPublicationID());
        int commentCount = dashboardController.getCommentController().getCommentCount(publication.getPublicationID());

        if (likeCount > 0 || commentCount > 0) {
            if (likeCount > 0) {
                Label likesLabel = new Label("❤️ " + likeCount);
                likesLabel.getStyleClass().add("post-stats-text");
                stats.getChildren().add(likesLabel);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            stats.getChildren().add(spacer);

            if (commentCount > 0) {
                Label commentsLabel = new Label(commentCount + " comment" + (commentCount != 1 ? "s" : ""));
                commentsLabel.getStyleClass().add("post-stats-text");
                stats.getChildren().add(commentsLabel);
            }
        }

        return stats;
    }

    /**
     * Create actions bar for detail view
     */
    private HBox createActionsBar() {
        HBox actions = new HBox(8);
        actions.getStyleClass().add("post-actions");
        actions.setAlignment(Pos.CENTER);

        Button likeBtn = dashboardController.getLikeController().createLikeButton(publication);
        HBox.setHgrow(likeBtn, Priority.ALWAYS);

        Button shareBtn = new Button("↗️ Share");
        shareBtn.getStyleClass().add("action-btn");
        shareBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(shareBtn, Priority.ALWAYS);
        shareBtn.setOnAction(e -> dashboardController.showInfo("Share feature coming soon!"));

        actions.getChildren().addAll(likeBtn, shareBtn);
        return actions;
    }
}