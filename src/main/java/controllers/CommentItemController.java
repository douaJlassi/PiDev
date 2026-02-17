package controllers;

import entities.Comment;
import entities.Publication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.sql.SQLException;
import java.util.Optional;
/**
 * CommentItemController — bound to comment_item.fxml.
 * Receives a Comment and wires it; edit/delete handled here.
 */
public class CommentItemController {

    @FXML private Circle  avatarCircle;
    @FXML private Label   usernameLabel;
    @FXML private Label   timeLabel;
    @FXML private Label   contentLabel;
    @FXML private HBox    ownerActions;
    @FXML private Button  editBtn;
    @FXML private Button  deleteBtn;

    private Comment             comment;
    private Publication         publication;
    private DashboardController dashboard;
    private Runnable            onChanged; // callback to reload the list

    // ────────────────────────────────────────────────────────────────────────
    // Called by CommentController after FXMLLoader.load()
    // ────────────────────────────────────────────────────────────────────────
    public void init(Comment c, Publication pub,
                     DashboardController dash, Runnable onChanged) {
        this.comment     = c;
        this.publication = pub;
        this.dashboard   = dash;
        this.onChanged   = onChanged;

        bindData();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Data binding
    // ────────────────────────────────────────────────────────────────────────
    private void bindData() {
        String username = comment.getClient().getUsername();
        usernameLabel.setText(username != null && !username.isEmpty()
                ? username : "Traveler #" + comment.getClient().getClientID());

        timeLabel.setText(comment.getTimeAgo());
        contentLabel.setText(comment.getContent());

        // Show edit/delete only for the owner
        boolean isOwner = comment.isOwnedBy(dashboard.getCurrentUser());
        ownerActions.setVisible(isOwner);
        ownerActions.setManaged(isOwner);
    }

    // ────────────────────────────────────────────────────────────────────────
    // FXML event handlers
    // ────────────────────────────────────────────────────────────────────────
    @FXML
    private void onEditClicked() {
        javafx.scene.control.TextInputDialog dlg =
                new javafx.scene.control.TextInputDialog(comment.getContent());
        dlg.setTitle("Edit Comment");
        dlg.setHeaderText(null);
        dlg.setContentText("Edit your comment:");
        ThemeManager.get().apply(dlg.getDialogPane());

        Optional<String> result = dlg.showAndWait();
        result.filter(s -> !s.trim().isEmpty()).ifPresent(newText -> {
            try {
                comment.setContent(newText.trim());
                dashboard.getCommentService().updateOne(comment);
                if (onChanged != null) onChanged.run();
            } catch (SQLException e) {
                dashboard.showError("Failed to update comment.");
            }
        });
    }

    @FXML
    private void onDeleteClicked() {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Comment");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this comment permanently?");
        ThemeManager.get().apply(confirm.getDialogPane());

        confirm.showAndWait()
                .filter(r -> r == javafx.scene.control.ButtonType.OK)
                .ifPresent(r -> {
                    try {
                        dashboard.getCommentService().deleteOne(comment);
                        dashboard.loadPosts();
                        if (onChanged != null) onChanged.run();
                    } catch (SQLException e) {
                        dashboard.showError("Failed to delete comment.");
                    }
                });
    }
}