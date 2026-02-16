package controllers;

import entities.Client;
import entities.Comment;
import entities.Publication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import services.CommentService;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML
    private FlowPane postsGrid;

    @FXML
    private VBox postsList;

    @FXML
    private StackPane contentContainer;

    @FXML
    private ScrollPane postsScrollPane;

    @FXML
    private Button createPostBtn;

    @FXML
    private Button refreshBtn;

    @FXML
    private Button gridViewBtn;

    @FXML
    private Button listViewBtn;

    @FXML
    private TextField searchField;

    @FXML
    private ImageView logoImage;

    private PublicationService publicationService;
    private CommentService commentService;
    private Client currentUser;
    private static final String UPLOAD_DIR = "uploads/images/";
    private List<Publication> allPosts;
    private boolean isGridView = true;

    @FXML
    public void initialize() {
        publicationService = new PublicationService();
        commentService = new CommentService();

        currentUser = new Client();
        currentUser.setClientID(1); // Replace with actual logged-in user ID

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }

        setupSearchFilter();
        loadPosts();
        postsScrollPane.setVvalue(0);
    }

    private void setupSearchFilter() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterPosts(newValue);
            });
        }
    }

    private void filterPosts(String searchTerm) {
        if (allPosts == null) return;

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            displayPosts(allPosts);
        } else {
            String lowerSearch = searchTerm.toLowerCase();
            List<Publication> filtered = allPosts.stream()
                    .filter(p -> p.getContent().toLowerCase().contains(lowerSearch) ||
                            (p.getPlace() != null && p.getPlace().toLowerCase().contains(lowerSearch)))
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                showNoResultsState(searchTerm);
            } else {
                displayPosts(filtered);
            }
        }
    }

    @FXML
    private void switchToGridView() {
        if (isGridView) return;

        isGridView = true;
        gridViewBtn.getStyleClass().add("active-view");
        listViewBtn.getStyleClass().remove("active-view");

        postsGrid.setVisible(true);
        postsGrid.setManaged(true);
        postsList.setVisible(false);
        postsList.setManaged(false);

        if (allPosts != null) {
            displayPosts(allPosts);
        }
    }

    @FXML
    private void switchToListView() {
        if (!isGridView) return;

        isGridView = false;
        listViewBtn.getStyleClass().add("active-view");
        gridViewBtn.getStyleClass().remove("active-view");

        postsList.setVisible(true);
        postsList.setManaged(true);
        postsGrid.setVisible(false);
        postsGrid.setManaged(false);

        if (allPosts != null) {
            displayPosts(allPosts);
        }
    }

    @FXML
    private void showCreatePostDialog() {
        Dialog<Publication> dialog = new Dialog<>();
        dialog.setTitle("Create New Post");
        dialog.setHeaderText("Share your adventure with the community 🌍");

        ButtonType createButtonType = new ButtonType("Post", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("What's on your mind?");
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);
        contentArea.getStyleClass().add("dialog-text-area");

        TextField placeField = new TextField();
        placeField.setPromptText("Add location (e.g., Tunis, Carthage...)");
        placeField.getStyleClass().add("dialog-text-field");

        Label imageLabel = new Label("No image selected");
        imageLabel.getStyleClass().add("file-label");

        Button selectImageBtn = new Button("📷 Add Photo");
        selectImageBtn.getStyleClass().add("file-btn");

        final File[] selectedImageFile = {null};

        selectImageBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                selectedImageFile[0] = file;
                imageLabel.setText("✓ " + file.getName());
                imageLabel.setStyle("-fx-text-fill: #17B3A6; -fx-font-weight: bold;");
            }
        });

        HBox imageBox = new HBox(12, selectImageBtn, imageLabel);
        imageBox.setAlignment(Pos.CENTER_LEFT);

        Label contentLabel = new Label("Content");
        contentLabel.getStyleClass().add("dialog-label");

        Label placeLabel = new Label("Location");
        placeLabel.getStyleClass().add("dialog-label");

        Label imageSelectLabel = new Label("Photo");
        imageSelectLabel.getStyleClass().add("dialog-label");

        grid.add(contentLabel, 0, 0);
        grid.add(contentArea, 0, 1, 2, 1);
        grid.add(placeLabel, 0, 2);
        grid.add(placeField, 0, 3, 2, 1);
        grid.add(imageSelectLabel, 0, 4);
        grid.add(imageBox, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.getStyleClass().add("primary-btn");
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
                        showError("Failed to upload image: " + e.getMessage());
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
                loadPosts();
                showSuccess("Post shared successfully! 🎉");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to create post: " + e.getMessage());
            }
        });
    }

    @FXML
    private void refreshPosts() {
        loadPosts();
    }

    private void loadPosts() {
        clearCurrentView();

        VBox loadingBox = new VBox(12);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.getStyleClass().add("loading-container");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);

        Label loadingText = new Label("Loading posts...");
        loadingText.getStyleClass().add("loading-text");

        loadingBox.getChildren().addAll(progressIndicator, loadingText);

        if (isGridView) {
            postsGrid.getChildren().add(loadingBox);
        } else {
            postsList.getChildren().add(loadingBox);
        }

        new Thread(() -> {
            try {
                List<Publication> publications = publicationService.selectALL();
                allPosts = publications;

                Platform.runLater(() -> {
                    clearCurrentView();

                    if (publications.isEmpty()) {
                        showEmptyState();
                    } else {
                        displayPosts(publications);
                    }

                    if (searchField != null) {
                        searchField.clear();
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearCurrentView();
                    showError("Failed to load posts: " + e.getMessage());
                });
            }
        }).start();
    }

    private void clearCurrentView() {
        postsGrid.getChildren().clear();
        postsList.getChildren().clear();
    }

    private void displayPosts(List<Publication> publications) {
        clearCurrentView();

        for (Publication publication : publications) {
            VBox postCard = isGridView ? createGridPostCard(publication) : createListPostCard(publication);

            if (isGridView) {
                postsGrid.getChildren().add(postCard);
            } else {
                postsList.getChildren().add(postCard);
            }
        }
    }

    private VBox createGridPostCard(Publication publication) {
        VBox card = new VBox();
        card.getStyleClass().add("post-card-grid");

        // Clip to bounds to prevent overflow
        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle(420, 0);
        clipRect.setArcWidth(12);
        clipRect.setArcHeight(12);
        clipRect.heightProperty().bind(card.heightProperty());
        card.setClip(clipRect);

        // Header
        card.getChildren().add(createPostHeader(publication));

        // Content
        VBox contentArea = new VBox(12);
        contentArea.getStyleClass().add("post-content-area");

        String content = publication.getContent();
        if (content.length() > 200) {
            content = content.substring(0, 200) + "...";
        }

        Text contentText = new Text(content);
        contentText.getStyleClass().add("post-text-preview");
        contentText.wrappingWidthProperty().bind(card.widthProperty().subtract(32));

        contentArea.getChildren().add(contentText);
        card.getChildren().add(contentArea);

        // Image
        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            card.getChildren().add(createImageContainer(publication, true));
        }

        // Stats
        card.getChildren().add(createStatsBar(publication));

        // Actions
        card.getChildren().add(createActionsBar(publication, true));

        return card;
    }

    private VBox createListPostCard(Publication publication) {
        VBox card = new VBox();
        card.getStyleClass().add("post-card-list");

        // Clip to bounds to prevent overflow
        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle(680, 0);
        clipRect.setArcWidth(12);
        clipRect.setArcHeight(12);
        clipRect.heightProperty().bind(card.heightProperty());
        card.setClip(clipRect);

        // Header
        card.getChildren().add(createPostHeader(publication));

        // Content
        VBox contentArea = new VBox(12);
        contentArea.getStyleClass().add("post-content-area");

        Text contentText = new Text(publication.getContent());
        contentText.getStyleClass().add("post-text");
        contentText.wrappingWidthProperty().bind(card.widthProperty().subtract(32));

        contentArea.getChildren().add(contentText);
        card.getChildren().add(contentArea);

        // Image
        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            card.getChildren().add(createImageContainer(publication, false));
        }

        // Stats
        card.getChildren().add(createStatsBar(publication));

        // Actions
        card.getChildren().add(createActionsBar(publication, false));

        return card;
    }

    private HBox createPostHeader(Publication publication) {
        HBox header = new HBox(12);
        header.getStyleClass().add("post-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(40, 40);
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);

        // Author info
        VBox authorInfo = new VBox(2);

        Label authorName = new Label("Traveler #" + publication.getClient().getClientID());
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

        // Menu button
        Button menuBtn = new Button("⋯");
        menuBtn.getStyleClass().add("post-menu-btn");
        if (currentUser.getClientID() == publication.getClient().getClientID()) {
            menuBtn.setOnAction(e -> showPostMenu(publication, menuBtn));
        } else {
            menuBtn.setVisible(false);
        }

        header.getChildren().addAll(avatar, authorInfo, spacer, menuBtn);
        return header;
    }

    private VBox createImageContainer(Publication publication, boolean isGrid) {
        VBox imageContainer = new VBox();
        imageContainer.getStyleClass().add("post-image-container");
        imageContainer.setMaxWidth(isGrid ? 420 : 680);

        try {
            File imageFile = new File(publication.getImagePath());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.getStyleClass().add(isGrid ? "post-image-grid" : "post-image-list");
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setFitWidth(isGrid ? 420 : 680);

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

        // Load actual comment count from database
        int commentCount = 0;
        try {
            commentCount = commentService.getCommentCount(publication.getPublicationID());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (publication.getLikes().size() > 0 || commentCount > 0) {
            if (publication.getLikes().size() > 0) {
                Label likesLabel = new Label("❤️ " + publication.getLikes().size());
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

    private HBox createActionsBar(Publication publication, boolean isCompact) {
        HBox actions = new HBox(8);
        actions.getStyleClass().add("post-actions");
        actions.setAlignment(Pos.CENTER);

        Button likeBtn = new Button("👍 Like");
        likeBtn.getStyleClass().add("action-btn");
        likeBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(likeBtn, Priority.ALWAYS);
        likeBtn.setOnAction(e -> handleLike(publication));

        Button commentBtn = new Button("💬 Comment");
        commentBtn.getStyleClass().add("action-btn");
        commentBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(commentBtn, Priority.ALWAYS);
        commentBtn.setOnAction(e -> handleComment(publication));

        Button shareBtn = new Button("↗️ Share");
        shareBtn.getStyleClass().add("action-btn");
        shareBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(shareBtn, Priority.ALWAYS);

        actions.getChildren().addAll(likeBtn, commentBtn, shareBtn);
        return actions;
    }

    private void showPostMenu(Publication publication, Button menuBtn) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏️ Edit Post");
        editItem.setOnAction(e -> handleEdit(publication));

        MenuItem deleteItem = new MenuItem("🗑️ Delete Post");
        deleteItem.setOnAction(e -> handleDelete(publication));

        menu.getItems().addAll(editItem, deleteItem);
        menu.show(menuBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void handleLike(Publication publication) {
        showInfo("Like functionality - to be implemented");
    }

    private void handleComment(Publication publication) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Comments");
        dialog.setHeaderText(publication.getComments().size() + " Comments");

        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(500);

        VBox mainContainer = new VBox(16);
        mainContainer.setPadding(new Insets(0));

        // Comment input area
        HBox commentInputArea = new HBox(12);
        commentInputArea.setPadding(new Insets(0, 0, 16, 0));
        commentInputArea.setAlignment(Pos.CENTER_LEFT);
        commentInputArea.setStyle("-fx-border-color: #e4e6eb; -fx-border-width: 0 0 1 0;");

        // User avatar
        Region userAvatar = new Region();
        userAvatar.getStyleClass().add("avatar");
        userAvatar.setPrefSize(32, 32);
        userAvatar.setMinSize(32, 32);
        userAvatar.setMaxSize(32, 32);

        // Comment input
        TextField commentInput = new TextField();
        commentInput.setPromptText("Write a comment...");
        commentInput.getStyleClass().add("dialog-text-field");
        HBox.setHgrow(commentInput, Priority.ALWAYS);

        // Post button
        Button postButton = new Button("Post");
        postButton.getStyleClass().add("primary-btn");
        postButton.setDisable(true);

        commentInput.textProperty().addListener((obs, old, newVal) -> {
            postButton.setDisable(newVal.trim().isEmpty());
        });

        postButton.setOnAction(e -> {
            String commentText = commentInput.getText().trim();
            if (!commentText.isEmpty()) {
                try {
                    Comment newComment = new Comment(
                            currentUser,
                            publication,
                            commentText,
                            new Date()
                    );
                    commentService.insertOne(newComment);
                    publication.addComment(newComment);

                    commentInput.clear();
                    dialog.setHeaderText(publication.getComments().size() + " Comments");

                    // Reload comments
                    loadCommentsInDialog(mainContainer, publication);

                    // Refresh the post card to update comment count
                    loadPosts();

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Failed to post comment: " + ex.getMessage());
                }
            }
        });

        commentInput.setOnAction(e -> postButton.fire());

        commentInputArea.getChildren().addAll(userAvatar, commentInput, postButton);

        // Comments list container
        ScrollPane commentsScrollPane = new ScrollPane();
        commentsScrollPane.setFitToWidth(true);
        commentsScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(commentsScrollPane, Priority.ALWAYS);

        VBox commentsList = new VBox(12);
        commentsList.setPadding(new Insets(16, 0, 0, 0));
        commentsScrollPane.setContent(commentsList);

        mainContainer.getChildren().addAll(commentInputArea, commentsScrollPane);

        // Load existing comments
        loadCommentsInDialog(mainContainer, publication);

        dialog.getDialogPane().setContent(mainContainer);
        dialog.showAndWait();
    }

    private void loadCommentsInDialog(VBox mainContainer, Publication publication) {
        // Get the comments scroll pane
        ScrollPane scrollPane = (ScrollPane) mainContainer.getChildren().get(1);
        VBox commentsList = (VBox) scrollPane.getContent();
        commentsList.getChildren().clear();

        try {
            List<Comment> comments = commentService.getCommentsByPublication(publication.getPublicationID());

            if (comments.isEmpty()) {
                Label noComments = new Label("No comments yet. Be the first to comment!");
                noComments.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14px; -fx-padding: 20;");
                commentsList.getChildren().add(noComments);
            } else {
                for (Comment comment : comments) {
                    commentsList.getChildren().add(createCommentItem(comment, publication));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Failed to load comments");
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            commentsList.getChildren().add(errorLabel);
        }
    }

    private HBox createCommentItem(Comment comment, Publication publication) {
        HBox commentItem = new HBox(12);
        commentItem.setPadding(new Insets(12));
        commentItem.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 8;");

        // Avatar
        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(32, 32);
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);

        // Comment content
        VBox contentBox = new VBox(4);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Author and content
        VBox textBox = new VBox(4);
        textBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8 12;");

        Label authorLabel = new Label("Traveler #" + comment.getClient().getClientID());
        authorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #050505;");

        Label contentLabel = new Label(comment.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #050505;");

        textBox.getChildren().addAll(authorLabel, contentLabel);

        // Meta info (time, actions)
        HBox metaBox = new HBox(12);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.setPadding(new Insets(4, 0, 0, 12));

        Label timeLabel = new Label(comment.getTimeAgo());
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b; -fx-font-weight: 600;");

        metaBox.getChildren().add(timeLabel);

        // Add edit/delete buttons if user owns the comment
        if (comment.isOwnedBy(currentUser)) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 0;");
            editBtn.setOnAction(e -> handleEditComment(comment, publication));

            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 0;");
            deleteBtn.setOnAction(e -> handleDeleteComment(comment, publication));

            Label dot1 = new Label("•");
            dot1.setStyle("-fx-text-fill: #65676b;");
            Label dot2 = new Label("•");
            dot2.setStyle("-fx-text-fill: #65676b;");

            metaBox.getChildren().addAll(dot1, editBtn, dot2, deleteBtn);
        }

        contentBox.getChildren().addAll(textBox, metaBox);

        commentItem.getChildren().addAll(avatar, contentBox);

        return commentItem;
    }

    private void handleEditComment(Comment comment, Publication publication) {
        TextInputDialog editDialog = new TextInputDialog(comment.getContent());
        editDialog.setTitle("Edit Comment");
        editDialog.setHeaderText("Edit your comment");
        editDialog.setContentText("Comment:");

        editDialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );

        Optional<String> result = editDialog.showAndWait();
        result.ifPresent(newContent -> {
            if (!newContent.trim().isEmpty()) {
                try {
                    comment.setContent(newContent.trim());
                    commentService.updateOne(comment);
                    showSuccess("Comment updated successfully!");
                    // Reload would happen when dialog refreshes
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Failed to update comment: " + e.getMessage());
                }
            }
        });
    }

    private void handleDeleteComment(Comment comment, Publication publication) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Comment");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("This comment will be permanently deleted.");

        confirmDialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentService.deleteOne(comment);
                publication.removeComment(comment);
                showSuccess("Comment deleted successfully!");
                loadPosts(); // Refresh to update comment count
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to delete comment: " + e.getMessage());
            }
        }
    }

    private void handleEdit(Publication publication) {
        Dialog<Publication> dialog = new Dialog<>();
        dialog.setTitle("Edit Post");
        dialog.setHeaderText("Update your post");

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextArea contentArea = new TextArea(publication.getContent());
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);
        contentArea.getStyleClass().add("dialog-text-area");

        TextField placeField = new TextField(publication.getPlace() != null ? publication.getPlace() : "");
        placeField.getStyleClass().add("dialog-text-field");

        Label contentLabel = new Label("Content");
        contentLabel.getStyleClass().add("dialog-label");

        Label placeLabel = new Label("Location");
        placeLabel.getStyleClass().add("dialog-label");

        grid.add(contentLabel, 0, 0);
        grid.add(contentArea, 0, 1, 2, 1);
        grid.add(placeLabel, 0, 2);
        grid.add(placeField, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Button updateButton = (Button) dialog.getDialogPane().lookupButton(updateButtonType);
        updateButton.getStyleClass().add("primary-btn");

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
                loadPosts();
                showSuccess("Post updated successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to update post: " + e.getMessage());
            }
        });
    }

    private void handleDelete(Publication publication) {
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
                loadPosts();
                showSuccess("Post deleted successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to delete post: " + e.getMessage());
            }
        }
    }

    private void showEmptyState() {
        VBox emptyState = new VBox(20);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.getStyleClass().add("empty-state");

        Label icon = new Label("📝");
        icon.getStyleClass().add("empty-state-icon");

        Label emptyText = new Label("No posts yet");
        emptyText.getStyleClass().add("empty-state-text");

        Label emptySubtext = new Label("Be the first to share something with the community!");
        emptySubtext.getStyleClass().add("empty-state-subtext");

        Button createFirstPost = new Button("Create First Post");
        createFirstPost.getStyleClass().add("primary-btn");
        createFirstPost.setOnAction(e -> showCreatePostDialog());

        emptyState.getChildren().addAll(icon, emptyText, emptySubtext, createFirstPost);

        if (isGridView) {
            postsGrid.getChildren().add(emptyState);
        } else {
            postsList.getChildren().add(emptyState);
        }
    }

    private void showNoResultsState(String searchTerm) {
        VBox noResults = new VBox(20);
        noResults.setAlignment(Pos.CENTER);
        noResults.getStyleClass().add("empty-state");

        Label icon = new Label("🔍");
        icon.getStyleClass().add("empty-state-icon");

        Label noResultsText = new Label("No posts found");
        noResultsText.getStyleClass().add("empty-state-text");

        Label noResultsSubtext = new Label("Try a different search term");
        noResultsSubtext.getStyleClass().add("empty-state-subtext");

        Button clearSearch = new Button("Clear Search");
        clearSearch.getStyleClass().add("secondary-btn");
        clearSearch.setOnAction(e -> {
            if (searchField != null) {
                searchField.clear();
            }
        });

        noResults.getChildren().addAll(icon, noResultsText, noResultsSubtext, clearSearch);

        if (isGridView) {
            postsGrid.getChildren().add(noResults);
        } else {
            postsList.getChildren().add(noResults);
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        alert.showAndWait();
    }
}