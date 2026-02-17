package controllers;

import entities.Client;
import entities.Publication;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import services.LikeService;

import java.sql.SQLException;

/**
 * LikeController - Handles all like-related operations
 * - Create animated like buttons
 * - Toggle like/unlike
 * - Update UI in real-time
 * - Manage like state
 */
public class LikeController {

    private final LikeService likeService;
    private final Client currentUser;
    private final DashboardController dashboardController;

    public LikeController(LikeService likeService, Client currentUser, DashboardController dashboardController) {
        this.likeService = likeService;
        this.currentUser = currentUser;
        this.dashboardController = dashboardController;
    }

    /**
     * Create an animated like button for a publication
     * Updates UI and database on click
     */
    public Button createLikeButton(Publication publication) {
        Button likeBtn = new Button();
        likeBtn.getStyleClass().add("action-btn");
        likeBtn.setMaxWidth(Double.MAX_VALUE);

        // Check initial like state and get count
        updateLikeButton(likeBtn, publication);

        // Handle like toggle
        likeBtn.setOnAction(e -> {
            e.consume(); // Prevent card click event
            toggleLike(likeBtn, publication);
        });

        return likeBtn;
    }

    /**
     * Toggle like state with animation
     */
    private void toggleLike(Button likeBtn, Publication publication) {
        try {
            boolean nowLiked = likeService.toggleLike(publication.getPublicationID(), currentUser.getClientID());

            // Animate the button
            animateLikeButton(likeBtn);

            // Update button appearance
            updateLikeButton(likeBtn, publication);

            // Refresh the posts to update counts
            dashboardController.loadPosts();

        } catch (SQLException e) {
            e.printStackTrace();
            dashboardController.showError("Failed to update like: " + e.getMessage());
        }
    }

    /**
     * Update like button text and style based on current state
     */
    private void updateLikeButton(Button likeBtn, Publication publication) {
        try {
            boolean userHasLiked = likeService.hasUserLiked(publication.getPublicationID(), currentUser.getClientID());
            int likeCount = likeService.getLikeCount(publication.getPublicationID());

            likeBtn.getStyleClass().remove("liked");

            if (userHasLiked) {
                likeBtn.setText("♥  " + likeCount);
                likeBtn.getStyleClass().add("liked");
            } else {
                likeBtn.setText("♡  " + (likeCount > 0 ? likeCount : "Like"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            likeBtn.setText("♡  Like");
        }
    }

    /**
     * Animate like button with scale effect
     */
    private void animateLikeButton(Button button) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(1.3);
        scaleUp.setToY(1.3);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setFromX(1.3);
        scaleDown.setFromY(1.3);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        scaleUp.setOnFinished(e -> scaleDown.play());
        scaleUp.play();
    }

    /**
     * Get like count for a publication
     */
    public int getLikeCount(int publicationID) {
        try {
            return likeService.getLikeCount(publicationID);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Check if current user has liked a publication
     */
    public boolean hasUserLiked(int publicationID) {
        try {
            return likeService.hasUserLiked(publicationID, currentUser.getClientID());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}