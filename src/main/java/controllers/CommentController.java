package controllers;

import entities.Client;
import entities.Comment;
import entities.Publication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.CommentService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * CommentController - Handles all comment-related operations
 * - Load and display comments
 * - Add, edit, delete comments
 * - Create comment UI components
 * - Manage comment interactions
 */
public class CommentController {

    private final CommentService commentService;
    private final Client currentUser;
    private final DashboardController dashboardController;

    public CommentController(CommentService commentService, Client currentUser, DashboardController dashboardController) {
        this.commentService = commentService;
        this.currentUser = currentUser;
        this.dashboardController = dashboardController;
    }

    /**
     * Create comment input area with avatar and text field
     */
    public HBox createCommentInput(Publication publication, VBox commentsListContainer, Label countLabel) {
        HBox inputBox = new HBox(12);
        inputBox.setPadding(new Insets(16, 20, 16, 20));
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.setStyle("-fx-border-color: #e4e6eb; -fx-border-width: 0 0 1 0; -fx-background-color: #f0f2f5;");

        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(36, 36);
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);

        TextField textField = new TextField();
        textField.setPromptText("Write a comment...");
        textField.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 20; -fx-background-radius: 20; " +
                "-fx-padding: 8 16; -fx-font-size: 14px;");
        HBox.setHgrow(textField, Priority.ALWAYS);

        Button postBtn = new Button("Post");
        postBtn.getStyleClass().add("primary-btn");
        postBtn.setDisable(true);

        textField.textProperty().addListener((obs, old, newVal) -> {
            postBtn.setDisable(newVal.trim().isEmpty());
        });

        postBtn.setOnAction(e -> {
            String content = textField.getText().trim();
            if (!content.isEmpty()) {
                addComment(publication, content, commentsListContainer, countLabel);
                textField.clear();
            }
        });

        textField.setOnAction(e -> postBtn.fire());

        inputBox.getChildren().addAll(avatar, textField, postBtn);

        return inputBox;
    }

    /**
     * Add a new comment
     */
    private void addComment(Publication publication, String content, VBox commentsListContainer, Label countLabel) {
        try {
            Comment comment = new Comment(currentUser, publication, content, new Date());
            commentService.insertOne(comment);

            // Reload comments
            loadComments(publication, commentsListContainer);

            // Update count label
            if (countLabel != null) {
                int count = commentService.getCommentCount(publication.getPublicationID());
                countLabel.setText("(" + count + ")");
            }

            // Refresh main view to update counts
            dashboardController.loadPosts();

        } catch (SQLException ex) {
            ex.printStackTrace();
            dashboardController.showError("Failed to post comment");
        }
    }

    /**
     * Load all comments for a publication
     */
    public void loadComments(Publication publication, VBox commentsList) {
        commentsList.getChildren().clear();

        try {
            List<Comment> comments = commentService.getCommentsByPublication(publication.getPublicationID());

            if (comments.isEmpty()) {
                Label empty = new Label("No comments yet. Be the first to comment!");
                empty.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14px; -fx-padding: 20;");
                commentsList.getChildren().add(empty);
            } else {
                for (Comment comment : comments) {
                    commentsList.getChildren().add(createCommentItem(comment, publication, commentsList));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Failed to load comments");
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            commentsList.getChildren().add(errorLabel);
        }
    }

    /**
     * Create a single comment UI item
     */
    public VBox createCommentItem(Comment comment, Publication publication, VBox commentsList) {
        VBox item = new VBox(8);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);");

        HBox topBox = new HBox(12);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(32, 32);
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);

        VBox contentBox = new VBox(4);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox nameTimeBox = new HBox(8);
        nameTimeBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(comment.getClient().getUsername() != null ?
                comment.getClient().getUsername() : "Traveler #" + comment.getClient().getClientID());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label timeLabel = new Label("• " + comment.getTimeAgo());
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");

        nameTimeBox.getChildren().addAll(nameLabel, timeLabel);

        Label contentLabel = new Label(comment.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #050505;");

        contentBox.getChildren().addAll(nameTimeBox, contentLabel);

        topBox.getChildren().addAll(avatar, contentBox);

        item.getChildren().add(topBox);

        // Actions if user owns comment
        if (comment.isOwnedBy(currentUser)) {
            HBox actions = new HBox(12);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setPadding(new Insets(4, 0, 0, 44));

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; " +
                    "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 0;");
            editBtn.setOnAction(e -> editComment(comment, publication, commentsList));

            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #e4e6eb;");

            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; " +
                    "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 0;");
            deleteBtn.setOnAction(e -> deleteComment(comment, publication, commentsList));

            actions.getChildren().addAll(editBtn, dot, deleteBtn);
            item.getChildren().add(actions);
        }

        return item;
    }

    /**
     * Edit a comment
     */
    private void editComment(Comment comment, Publication publication, VBox commentsList) {
        TextInputDialog dialog = new TextInputDialog(comment.getContent());
        dialog.setTitle("Edit Comment");
        dialog.setHeaderText("Edit your comment");
        dialog.setContentText(null);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newContent -> {
            if (!newContent.trim().isEmpty()) {
                try {
                    comment.setContent(newContent.trim());
                    commentService.updateOne(comment);
                    loadComments(publication, commentsList);
                    dashboardController.showSuccess("Comment updated!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    dashboardController.showError("Failed to update comment");
                }
            }
        });
    }

    /**
     * Delete a comment with confirmation
     */
    private void deleteComment(Comment comment, Publication publication, VBox commentsList) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Comment");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This comment will be permanently deleted.");

        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentService.deleteOne(comment);
                loadComments(publication, commentsList);
                dashboardController.loadPosts(); // Refresh main view
                dashboardController.showSuccess("Comment deleted!");
            } catch (SQLException e) {
                e.printStackTrace();
                dashboardController.showError("Failed to delete comment");
            }
        }
    }

    /**
     * Get comment count for a publication
     */
    public int getCommentCount(int publicationID) {
        try {
            return commentService.getCommentCount(publicationID);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}