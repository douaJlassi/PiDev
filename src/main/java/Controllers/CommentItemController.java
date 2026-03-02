package Controllers;

import entities.Comment;
import entities.Publication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.sql.SQLException;

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
    private WajdiDashboardController dashboard;
    private Runnable            onChanged; // callback to reload the list

    // ────────────────────────────────────────────────────────────────────────
    // Called by CommentController after FXMLLoader.load()
    // ────────────────────────────────────────────────────────────────────────
    public void init(Comment c, Publication pub,
                     WajdiDashboardController dash, Runnable onChanged) {
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
        // Replace content label with inline TextField
        javafx.scene.control.TextField editField = new javafx.scene.control.TextField(comment.getContent());
        editField.setStyle("-fx-background-color: #f0f2f5; -fx-border-color: #17B3A6; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10; -fx-font-size: 13px;");
        editField.setOnAction(e -> saveEdit(editField.getText()));

        // Replace the contentLabel node with the TextField temporarily
        javafx.scene.layout.VBox parent = (javafx.scene.layout.VBox) contentLabel.getParent();
        int idx = parent.getChildren().indexOf(contentLabel);
        parent.getChildren().set(idx, editField);
        editField.requestFocus();
        editField.selectAll();

        // On focus loss or Enter, save
        editField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && parent.getChildren().contains(editField)) {
                saveEdit(editField.getText());
                parent.getChildren().set(parent.getChildren().indexOf(editField), contentLabel);
            }
        });
    }

    private void saveEdit(String newText) {
        if (newText == null || newText.trim().isEmpty()) return;
        try {
            comment.setContent(newText.trim());
            dashboard.getCommentService().updateOne(comment);
            contentLabel.setText(newText.trim());
            if (onChanged != null) onChanged.run();
        } catch (SQLException e) {
            dashboard.showError("Failed to update comment.");
        }
    }

    @FXML
    private void onDeleteClicked() {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Comment");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this comment permanently?");
        ThemeManager.get().applyToPane(confirm.getDialogPane());

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