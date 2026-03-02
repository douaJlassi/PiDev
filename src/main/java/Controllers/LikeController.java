package Controllers;

import entities.Client;
import entities.Publication;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.util.Duration;
import services.LikeService;

import java.sql.SQLException;

/**
 * LikeController — handles like/unlike toggle and button state.
 * No UI construction here; operates on Button nodes created by FXML.
 */
public class LikeController {

    private final LikeService         likeService;
    private final Client              currentUser;
    private final WajdiDashboardController dashboard;

    public LikeController(LikeService svc, Client user, WajdiDashboardController dash) {
        this.likeService = svc;
        this.currentUser = user;
        this.dashboard   = dash;
    }

    // ── Called by PostCardController and PostDetailController ─────────────────
    /** Initialise button appearance for a publication. */
    public void initButton(Button btn, Publication pub) {
        refreshButton(btn, pub);
    }

    /** Toggle like/unlike on click — called from FXML handler via init'd button. */
    public void handleToggle(Publication pub, Button btn) {
        try {
            likeService.toggleLike(pub.getPublicationID(), currentUser.getClientID());
            pulseAnimation(btn);
            refreshButton(btn, pub);
            dashboard.loadPosts();
        } catch (SQLException e) {
            dashboard.showError("Failed to update like.");
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────────
    public int getLikeCount(int publicationID) {
        try { return likeService.getLikeCount(publicationID); }
        catch (SQLException e) { return 0; }
    }

    public boolean hasUserLiked(int publicationID) {
        try { return likeService.hasUserLiked(publicationID, currentUser.getClientID()); }
        catch (SQLException e) { return false; }
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private void refreshButton(Button btn, Publication pub) {
        try {
            int     count = likeService.getLikeCount(pub.getPublicationID());
            boolean liked = likeService.hasUserLiked(pub.getPublicationID(),
                    currentUser.getClientID());

            btn.setText(liked ? "♥  " + count : "♡  " + (count > 0 ? count : "Like"));
            btn.getStyleClass().remove("liked");
            if (liked) btn.getStyleClass().add("liked");

        } catch (SQLException e) {
            btn.setText("♡  Like");
        }
    }

    private void pulseAnimation(Button btn) {
        ScaleTransition up = new ScaleTransition(Duration.millis(90), btn);
        up.setToX(1.3); up.setToY(1.3);
        ScaleTransition down = new ScaleTransition(Duration.millis(90), btn);
        down.setToX(1.0); down.setToY(1.0);
        up.setOnFinished(e -> down.play());
        up.play();
    }
}