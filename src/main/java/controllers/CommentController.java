package controllers;

import entities.Client;
import entities.Comment;
import entities.Publication;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import services.CommentService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * CommentController — manages comment CRUD and list rendering.
 * Each comment row is loaded from comment_item.fxml.
 * This class only orchestrates: query DB → load FXML per row → inject data.
 */
public class CommentController {

    private final CommentService      commentService;
    private final Client              currentUser;
    private final WajdiDashboardController dashboard;

    public CommentController(CommentService svc, Client user, WajdiDashboardController dash) {
        this.commentService = svc;
        this.currentUser    = user;
        this.dashboard      = dash;
    }

    // ── Load all comments for a publication into a VBox list ──────────────────
    public void loadComments(Publication publication, VBox commentsList) {
        commentsList.getChildren().clear();

        try {
            List<Comment> comments = commentService
                    .getCommentsByPublication(publication.getPublicationID());

            if (comments.isEmpty()) {
                Label empty = new Label("No comments yet. Be the first!");
                empty.setStyle("-fx-text-fill: #8a8d91; -fx-font-size: 13px; -fx-padding: 16 0;");
                commentsList.getChildren().add(empty);
                return;
            }

            for (Comment c : comments) {
                VBox row = loadCommentRow(c, publication, commentsList);
                if (row != null) commentsList.getChildren().add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Could not load comments.");
            err.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
            commentsList.getChildren().add(err);
        }
    }

    // ── Load a single comment_item.fxml row ───────────────────────────────────
    private VBox loadCommentRow(Comment comment, Publication publication, VBox list) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/comment_item.fxml"));
            VBox row = loader.load();

            CommentItemController ctrl = loader.getController();
            // onChanged: reload entire list + update main feed count
            ctrl.init(comment, publication, dashboard, () -> {
                loadComments(publication, list);
                dashboard.loadPosts();
            });

            return row;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Add a new comment (called from PostDetailController) ──────────────────
    public void addComment(Publication publication, String content,
                           VBox commentsList, Label countLabel) {
        if (content == null || content.trim().isEmpty()) return;

        try {
            Comment comment = new Comment(currentUser, publication,
                    content.trim(), new Date());
            commentService.insertOne(comment);

            loadComments(publication, commentsList);

            if (countLabel != null) {
                int count = commentService.getCommentCount(publication.getPublicationID());
                countLabel.setText("(" + count + ")");
            }

            dashboard.loadPosts();

        } catch (SQLException e) {
            e.printStackTrace();
            dashboard.showError("Failed to post comment.");
        }
    }

    // ── Convenience getter ────────────────────────────────────────────────────
    public int getCommentCount(int publicationID) {
        try { return commentService.getCommentCount(publicationID); }
        catch (SQLException e) { return 0; }
    }
}