package controllers;

import entities.Client;
import entities.Publication;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import services.PublicationService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * PostController — manages post CRUD and card creation.
 * UI lives in post_card.fxml and create_post_dialog.fxml.
 * This class only orchestrates: load FXML → inject data → handle result.
 */
public class PostController {

    private final PublicationService  publicationService;
    private final Client              currentUser;
    private final DashboardController dashboard;

    public PostController(PublicationService svc, Client user, DashboardController dash) {
        this.publicationService = svc;
        this.currentUser        = user;
        this.dashboard          = dash;
    }

    // ── Card factory ─────────────────────────────────────────────────────────
    public VBox createPostCard(Publication publication, boolean isGridView) {
        try {
            FXMLLoader loader = fxml("/views/post_card.fxml");
            VBox card = loader.load();

            card.getStyleClass().clear();
            card.getStyleClass().add(isGridView ? "post-card-grid" : "post-card-list");

            Rectangle clip = new Rectangle(isGridView ? 420 : 680, 0);
            clip.setArcWidth(12); clip.setArcHeight(12);
            clip.heightProperty().bind(card.heightProperty());
            card.setClip(clip);

            card.setStyle("-fx-cursor: hand;");
            card.setOnMouseClicked(e ->
                    new PostDetailController(publication, dashboard).show());

            PostCardController ctrl = loader.getController();
            ctrl.init(publication, isGridView, dashboard);

            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return new VBox(new Label("Card load error"));
        }
    }

    // ── Create dialog ─────────────────────────────────────────────────────────
    public void showCreateDialog() {
        try {
            FXMLLoader loader = fxml("/views/create_post_dialog.fxml");
            javafx.scene.Parent content = loader.load();
            CreatePostController form = loader.getController();
            form.init(dashboard, null);

            Dialog<Publication> dialog = buildDialog("Create Post", "Share", content);
            Button shareBtn = okButton(dialog);
            shareBtn.setDisable(true);
            form.getContentArea().textProperty().addListener(
                    (obs, o, n) -> shareBtn.setDisable(n.trim().isEmpty()));

            dialog.setResultConverter(bt ->
                    bt.getButtonData() == ButtonBar.ButtonData.OK_DONE
                            ? form.buildPublication() : null);

            dialog.showAndWait().ifPresent(pub -> {
                try { publicationService.insertOne(pub); dashboard.loadPosts(); }
                catch (SQLException ex) { dashboard.showError("Failed to create post: " + ex.getMessage()); }
            });
        } catch (IOException e) { dashboard.showError("Could not open dialog: " + e.getMessage()); }
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    public void showEditDialog(Publication publication) {
        try {
            FXMLLoader loader = fxml("/views/create_post_dialog.fxml");
            javafx.scene.Parent content = loader.load();
            CreatePostController form = loader.getController();
            form.init(dashboard, publication);

            Dialog<Publication> dialog = buildDialog("Edit Post", "Update", content);
            dialog.setResultConverter(bt -> {
                if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    form.populateExisting(publication);
                    return publication;
                }
                return null;
            });

            dialog.showAndWait().ifPresent(pub -> {
                try { publicationService.updateOne(pub); dashboard.loadPosts(); }
                catch (SQLException ex) { dashboard.showError("Failed to update post: " + ex.getMessage()); }
            });
        } catch (IOException e) { dashboard.showError("Could not open dialog: " + e.getMessage()); }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    public void deletePost(Publication publication) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Post");
        confirm.setHeaderText(null);
        confirm.setContentText("This post will be permanently deleted.");
        stylize(confirm);
        confirm.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .ifPresent(r -> {
                    try { publicationService.deleteOne(publication); dashboard.loadPosts(); }
                    catch (SQLException ex) { dashboard.showError("Failed to delete: " + ex.getMessage()); }
                });
    }

    // ── Context menu (called from PostCardController) ─────────────────────────
    public void showPostMenu(Publication publication, Button anchor) {
        ContextMenu menu = new ContextMenu();
        MenuItem edit   = new MenuItem("Edit Post");
        MenuItem delete = new MenuItem("Delete Post");
        edit.setOnAction(e -> showEditDialog(publication));
        delete.setOnAction(e -> deletePost(publication));
        menu.getItems().addAll(edit, delete);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private FXMLLoader fxml(String path) {
        return new FXMLLoader(getClass().getResource(path));
    }

    private Dialog<Publication> buildDialog(String title, String okLabel,
                                            javafx.scene.Parent content) {
        Dialog<Publication> d = new Dialog<>();
        d.setTitle(title); d.setHeaderText(null);
        ButtonType ok = new ButtonType(okLabel, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        d.getDialogPane().setContent(content);
        d.getDialogPane().setPrefWidth(540);
        ThemeManager.get().apply(d.getDialogPane());
        d.getDialogPane().getStyleClass().add("create-post-dialog");
        okButton(d).getStyleClass().add("primary-btn");
        return d;
    }

    private Button okButton(Dialog<?> d) {
        return (Button) d.getDialogPane().lookupButton(
                d.getDialogPane().getButtonTypes().get(0));
    }

    private void stylize(Alert a) {
        ThemeManager.get().apply(a.getDialogPane());
    }
}