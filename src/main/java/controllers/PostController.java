package controllers;

import entities.Client;
import entities.Publication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import services.PublicationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * PostController - Handles all publication-related operations
 * - Create, edit, delete posts
 * - Generate post cards (grid & list views)
 * - Handle post detail view
 */
public class PostController {

    private final PublicationService publicationService;
    private final Client currentUser;
    private final DashboardController dashboardController;
    private static final String UPLOAD_DIR = "uploads/images/";

    public PostController(PublicationService publicationService, Client currentUser, DashboardController dashboardController) {
        this.publicationService = publicationService;
        this.currentUser = currentUser;
        this.dashboardController = dashboardController;

        // Ensure upload directory exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show create post dialog with modern UI
     */
    public void showCreateDialog() {
        Dialog<Publication> dialog = new Dialog<>();
        dialog.setTitle("Create Post");
        dialog.setHeaderText(null);

        ButtonType createButtonType = new ButtonType("Share", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("create-post-dialog");
        dialog.getDialogPane().setPrefWidth(550);

        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(24));

        // Header with avatar
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(48, 48);
        avatar.setMinSize(48, 48);
        avatar.setMaxSize(48, 48);

        VBox userInfo = new VBox(2);
        Label userName = new Label(currentUser.getUsername() != null ? currentUser.getUsername() : "Traveler #" + currentUser.getClientID());
        userName.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        Label visibility = new Label("🌍 Public");
        visibility.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");
        userInfo.getChildren().addAll(userName, visibility);

        header.getChildren().addAll(avatar, userInfo);

        // Content area
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("What's on your mind?");
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-font-size: 15px; -fx-text-fill: #050505;");
        contentArea.requestFocus();

        // Image preview area
        VBox imagePreviewBox = new VBox(8);
        imagePreviewBox.setVisible(false);
        imagePreviewBox.setManaged(false);
        imagePreviewBox.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 8; -fx-padding: 12;");

        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(500);
        imagePreview.setPreserveRatio(true);
        imagePreview.setSmooth(true);

        Button removeImageBtn = new Button("✕ Remove");
        removeImageBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");

        imagePreviewBox.getChildren().addAll(imagePreview, removeImageBtn);

        final File[] selectedImageFile = {null};

        removeImageBtn.setOnAction(e -> {
            selectedImageFile[0] = null;
            imagePreviewBox.setVisible(false);
            imagePreviewBox.setManaged(false);
        });

        // Location input
        TextField placeField = new TextField();
        placeField.setPromptText("📍 Add location");
        placeField.setStyle("-fx-background-color: #f0f2f5; -fx-border-color: transparent; " +
                "-fx-background-radius: 8; -fx-padding: 10 12; -fx-font-size: 14px;");

        // Add to post section
        HBox addToPost = new HBox(8);
        addToPost.setAlignment(Pos.CENTER_LEFT);
        addToPost.setStyle("-fx-border-color: #e4e6eb; -fx-border-width: 1; -fx-border-radius: 8; " +
                "-fx-padding: 12; -fx-background-color: white; -fx-background-radius: 8;");

        Label addLabel = new Label("Add to your post");
        addLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button photoBtn = new Button("📷");
        photoBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 24px; " +
                "-fx-cursor: hand; -fx-padding: 4 8;");
        photoBtn.setOnMouseEntered(e -> photoBtn.setStyle(photoBtn.getStyle() + "-fx-background-color: #f0f2f5; -fx-background-radius: 50%;"));
        photoBtn.setOnMouseExited(e -> photoBtn.setStyle(photoBtn.getStyle().replace("-fx-background-color: #f0f2f5;", "-fx-background-color: transparent;")));

        photoBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                selectedImageFile[0] = file;
                try {
                    Image img = new Image(file.toURI().toString());
                    imagePreview.setImage(img);
                    imagePreviewBox.setVisible(true);
                    imagePreviewBox.setManaged(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        addToPost.getChildren().addAll(addLabel, spacer, photoBtn);

        mainBox.getChildren().addAll(header, contentArea, imagePreviewBox, placeField, addToPost);

        dialog.getDialogPane().setContent(mainBox);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.getStyleClass().add("primary-btn");
        createButton.setStyle("-fx-pref-width: 100%; -fx-font-size: 15px; -fx-font-weight: bold;");
        createButton.setDisable(true);

        contentArea.textProperty().addListener((observable, oldValue, newValue) -> {
            createButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String imagePath = null;

                if (selectedImageFile[0] != null) {
                    try {
                        String fileName = System.currentTimeMillis() + "_" + selectedImageFile[0].getName();
                        Path targetPath = Paths.get(UPLOAD_DIR + fileName);
                        Files.copy(selectedImageFile[0].toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                        imagePath = UPLOAD_DIR + fileName;
                    } catch (IOException e) {
                        e.printStackTrace();
                        dashboardController.showError("Failed to upload image: " + e.getMessage());
                    }
                }

                return new Publication(
                        currentUser,
                        0,
                        contentArea.getText().trim(),
                        new Date(),
                        imagePath,
                        placeField.getText().trim().isEmpty() ? null : placeField.getText().trim()
                );
            }
            return null;
        });

        Optional<Publication> result = dialog.showAndWait();
        result.ifPresent(publication -> {
            try {
                publicationService.insertOne(publication);
                dashboardController.loadPosts();
                dashboardController.showSuccess("Post shared successfully! 🎉");
            } catch (SQLException e) {
                e.printStackTrace();
                dashboardController.showError("Failed to create post: " + e.getMessage());
            }
        });
    }

    /**
     * Show edit post dialog
     */
    public void showEditDialog(Publication publication) {
        Dialog<Publication> dialog = new Dialog<>();
        dialog.setTitle("Edit Post");
        dialog.setHeaderText(null);

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("create-post-dialog");
        dialog.getDialogPane().setPrefWidth(550);

        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(24));

        TextArea contentArea = new TextArea(publication.getContent());
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-font-size: 15px; -fx-text-fill: #050505;");

        TextField placeField = new TextField(publication.getPlace() != null ? publication.getPlace() : "");
        placeField.setPromptText("📍 Add location");
        placeField.setStyle("-fx-background-color: #f0f2f5; -fx-border-color: transparent; " +
                "-fx-background-radius: 8; -fx-padding: 10 12; -fx-font-size: 14px;");

        mainBox.getChildren().addAll(contentArea, placeField);
        dialog.getDialogPane().setContent(mainBox);

        Button updateButton = (Button) dialog.getDialogPane().lookupButton(updateButtonType);
        updateButton.getStyleClass().add("primary-btn");
        updateButton.setStyle("-fx-pref-width: 100%; -fx-font-size: 15px; -fx-font-weight: bold;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                publication.setContent(contentArea.getText().trim());
                publication.setPlace(placeField.getText().trim().isEmpty() ? null : placeField.getText().trim());
                return publication;
            }
            return null;
        });

        Optional<Publication> result = dialog.showAndWait();
        result.ifPresent(updatedPublication -> {
            try {
                publicationService.updateOne(updatedPublication);
                dashboardController.loadPosts();
                dashboardController.showSuccess("Post updated successfully! ✨");
            } catch (SQLException e) {
                e.printStackTrace();
                dashboardController.showError("Failed to update post: " + e.getMessage());
            }
        });
    }

    /**
     * Delete post with confirmation
     */
    public void deletePost(Publication publication) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This post will be permanently deleted.");

        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                publicationService.deleteOne(publication);
                dashboardController.loadPosts();
                dashboardController.showSuccess("Post deleted successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
                dashboardController.showError("Failed to delete post: " + e.getMessage());
            }
        }
    }

    /**
     * Create post card for grid or list view
     */
    public VBox createPostCard(Publication publication, boolean isGridView) {
        VBox card = new VBox();
        card.getStyleClass().add(isGridView ? "post-card-grid" : "post-card-list");

        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle(isGridView ? 420 : 680, 0);
        clipRect.setArcWidth(12);
        clipRect.setArcHeight(12);
        clipRect.heightProperty().bind(card.heightProperty());
        card.setClip(clipRect);

        // Make card clickable to open detail view
        card.setOnMouseClicked(e -> {
            PostDetailController detailController = new PostDetailController(
                    publication,
                    dashboardController
            );
            detailController.show();
        });
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");

        // Header
        card.getChildren().add(createPostHeader(publication));

        // Content
        VBox contentArea = new VBox(12);
        contentArea.getStyleClass().add("post-content-area");

        String content = publication.getContent();
        if (isGridView && content.length() > 200) {
            content = content.substring(0, 200) + "...";
        }

        Text contentText = new Text(content);
        contentText.getStyleClass().add(isGridView ? "post-text-preview" : "post-text");
        contentText.wrappingWidthProperty().bind(card.widthProperty().subtract(32));

        contentArea.getChildren().add(contentText);
        card.getChildren().add(contentArea);

        // Image
        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            card.getChildren().add(createImageContainer(publication, isGridView));
        }

        // Stats
        card.getChildren().add(createStatsBar(publication));

        // Actions
        card.getChildren().add(createActionsBar(publication));

        return card;
    }

    private HBox createPostHeader(Publication publication) {
        HBox header = new HBox(12);
        header.getStyleClass().add("post-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(40, 40);
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);

        VBox authorInfo = new VBox(2);

        Label authorName = new Label(publication.getClient().getUsername() != null ?
                publication.getClient().getUsername() : "Traveler #" + publication.getClient().getClientID());
        authorName.getStyleClass().add("post-author-name");

        HBox metaBox = new HBox(8);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd 'at' hh:mm a");
        Label dateLabel = new Label(dateFormat.format(publication.getDatePublication()));
        dateLabel.getStyleClass().add("post-meta");

        metaBox.getChildren().add(dateLabel);

        if (publication.getPlace() != null && !publication.getPlace().isEmpty()) {
            Label dot = new Label("•");
            dot.getStyleClass().add("post-meta");
            Label placeLabel = new Label("📍 " + publication.getPlace());
            placeLabel.getStyleClass().add("post-place-tag");
            metaBox.getChildren().addAll(dot, placeLabel);
        }

        authorInfo.getChildren().addAll(authorName, metaBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button menuBtn = new Button("⋯");
        menuBtn.getStyleClass().add("post-menu-btn");
        if (currentUser.getClientID() == publication.getClient().getClientID()) {
            menuBtn.setOnAction(e -> {
                e.consume();
                showPostMenu(publication, menuBtn);
            });
        } else {
            menuBtn.setVisible(false);
        }

        header.getChildren().addAll(avatar, authorInfo, spacer, menuBtn);
        return header;
    }

    private VBox createImageContainer(Publication publication, boolean isGridView) {
        VBox imageContainer = new VBox();
        imageContainer.getStyleClass().add("post-image-container");
        imageContainer.setMaxWidth(isGridView ? 420 : 680);

        try {
            File imageFile = new File(publication.getImagePath());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.getStyleClass().add(isGridView ? "post-image-grid" : "post-image-list");
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setFitWidth(isGridView ? 420 : 680);

                imageContainer.getChildren().add(imageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageContainer;
    }

    private HBox createStatsBar(Publication publication) {
        HBox stats = new HBox(16);
        stats.getStyleClass().add("post-stats");
        stats.setAlignment(Pos.CENTER_LEFT);

        int likeCount = 0;
        int commentCount = 0;

        try {
            likeCount = dashboardController.getLikeService().getLikeCount(publication.getPublicationID());
            commentCount = dashboardController.getCommentService().getCommentCount(publication.getPublicationID());
        } catch (SQLException e) {
            e.printStackTrace();
        }

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

    private HBox createActionsBar(Publication publication) {
        HBox actions = new HBox(8);
        actions.getStyleClass().add("post-actions");
        actions.setAlignment(Pos.CENTER);

        // Like button - delegated to LikeController
        Button likeBtn = dashboardController.getLikeController().createLikeButton(publication);
        HBox.setHgrow(likeBtn, Priority.ALWAYS);

        Button shareBtn = new Button("Share");
        shareBtn.getStyleClass().add("action-btn");
        shareBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(shareBtn, Priority.ALWAYS);
        shareBtn.setOnAction(e -> {
            e.consume();
            dashboardController.showInfo("Share feature coming soon!");
        });

        actions.getChildren().addAll(likeBtn, shareBtn);
        return actions;
    }

    private void showPostMenu(Publication publication, Button menuBtn) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏️ Edit Post");
        editItem.setOnAction(e -> showEditDialog(publication));

        MenuItem deleteItem = new MenuItem("🗑️ Delete Post");
        deleteItem.setOnAction(e -> deletePost(publication));

        menu.getItems().addAll(editItem, deleteItem);
        menu.show(menuBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }
}