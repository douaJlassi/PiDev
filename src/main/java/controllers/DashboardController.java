package controllers;

import entities.Client;
import entities.Comment;
import entities.Publication;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
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

    @FXML private VBox feedView, detailView, detailPostContainer, detailCommentsList;
    @FXML private TextArea detailCommentInput;
    @FXML private Button submitCommentBtn;
    private Publication currentViewingPost;

    private PublicationService publicationService;
    private CommentService commentService;
    private Client currentUser;
    private static final String UPLOAD_DIR = "uploads/images/";
    private List<Publication> allPosts;
    private boolean isGridView = true;

    // Post detail view
    private BorderPane postDetailView;
    private Publication currentDetailPublication;

    @FXML
    public void initialize() {
        publicationService = new PublicationService();
        commentService = new CommentService();

        currentUser = new Client();
        currentUser.setClientID(2);

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
        Label userName = new Label("Traveler #" + currentUser.getClientID());
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
                "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6;");

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

        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle(420, 0);
        clipRect.setArcWidth(12);
        clipRect.setArcHeight(12);
        clipRect.heightProperty().bind(card.heightProperty());
        card.setClip(clipRect);

        // Make card clickable
        card.setOnMouseClicked(e -> showPostDetail(publication));
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");

        card.getChildren().add(createPostHeader(publication));

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

        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            card.getChildren().add(createImageContainer(publication, true));
        }

        card.getChildren().add(createStatsBar(publication));
        card.getChildren().add(createActionsBar(publication, true));

        return card;
    }

    private VBox createListPostCard(Publication publication) {
        VBox card = new VBox();
        card.getStyleClass().add("post-card-list");

        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle(680, 0);
        clipRect.setArcWidth(12);
        clipRect.setArcHeight(12);
        clipRect.heightProperty().bind(card.heightProperty());
        card.setClip(clipRect);

        // Make card clickable
        card.setOnMouseClicked(e -> showPostDetail(publication));
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");

        card.getChildren().add(createPostHeader(publication));

        VBox contentArea = new VBox(12);
        contentArea.getStyleClass().add("post-content-area");

        Text contentText = new Text(publication.getContent());
        contentText.getStyleClass().add("post-text");
        contentText.wrappingWidthProperty().bind(card.widthProperty().subtract(32));

        contentArea.getChildren().add(contentText);
        card.getChildren().add(contentArea);

        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            card.getChildren().add(createImageContainer(publication, false));
        }

        card.getChildren().add(createStatsBar(publication));
        card.getChildren().add(createActionsBar(publication, false));

        return card;
    }

    private void showPostDetail(Publication publication) {
        this.currentViewingPost = publication;

        // 1. Toggle the visibility of the views
        feedView.setVisible(false);
        feedView.setManaged(false);
        detailView.setVisible(true);
        detailView.setManaged(true);

        // 2. Clear the old content and inject the current post on the left
        detailPostContainer.getChildren().clear();
        VBox bigCard = createListPostCard(publication);

        // Disable the hand cursor and click event for the card inside the detail view
        bigCard.setCursor(javafx.scene.Cursor.DEFAULT);
        bigCard.setOnMouseClicked(null);

        detailPostContainer.getChildren().add(bigCard);

        // 3. Load the comments into the sidebar on the right
        refreshDetailComments();
    }

    // Helper to reload comments specifically for the sidebar
    private void refreshDetailComments() {
        detailCommentsList.getChildren().clear();
        try {
            List<Comment> comments = commentService.getCommentsByPublication(currentViewingPost.getPublicationID());
            if (comments.isEmpty()) {
                Label noComments = new Label("No comments yet. Start the conversation!");
                noComments.getStyleClass().add("post-meta");
                detailCommentsList.getChildren().add(noComments);
            } else {
                for (Comment comment : comments) {
                    // We reuse your existing method to keep Edit/Delete working
                    detailCommentsList.getChildren().add(createCommentItem(comment, currentViewingPost, detailCommentsList));
                }
            }
        } catch (SQLException e) {
            showError("Failed to load comments: " + e.getMessage());
        }
    }

    // Logic for the Submit button in the sidebar
    @FXML
    private void handleDetailCommentSubmit() {
        String content = detailCommentInput.getText().trim();
        if (content.isEmpty()) return;

        try {
            Comment newComment = new Comment(currentUser, currentViewingPost, content, new Date());
            commentService.insertOne(newComment);
            detailCommentInput.clear();
            refreshDetailComments(); // Refresh the sidebar
            // No need to reload the whole feed, but we can if you want comment counts to update
        } catch (SQLException e) {
            showError("Could not post comment: " + e.getMessage());
        }
    }

    private void closePostDetail() {
        if (postDetailView != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(200), postDetailView);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(e -> {
                contentContainer.getChildren().remove(postDetailView);
                postDetailView = null;
                currentDetailPublication = null;
            });
            fade.play();
        }
    }

    private VBox createPostContentPanel(Publication publication) {
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
        Label authorName = new Label("Traveler #" + publication.getClient().getClientID());
        authorName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a");
        Label dateLabel = new Label(dateFormat.format(publication.getDatePublication()));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");

        authorInfo.getChildren().addAll(authorName, dateLabel);
        authorBox.getChildren().addAll(avatar, authorInfo);

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

        content.getChildren().addAll(authorBox, contentText);

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

        // Stats and actions
        content.getChildren().add(createStatsBar(publication));
        content.getChildren().add(createActionsBar(publication, false));

        scrollPane.setContent(content);
        panel.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return panel;
    }

    @FXML
    private void handleBackToFeed() {
        detailView.setVisible(false);
        detailView.setManaged(false);
        feedView.setVisible(true);
        feedView.setManaged(true);
        currentViewingPost = null;

        // Refresh the feed to update comment counts on the cards
        loadPosts();
    }

    private VBox createCommentsPanel(Publication publication) {
        VBox panel = new VBox();
        panel.getStyleClass().add("comments-panel");

        // Comments header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle("-fx-border-color: #e4e6eb; -fx-border-width: 0 0 1 0;");

        Label commentsTitle = new Label("Comments");
        commentsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        try {
            int count = commentService.getCommentCount(publication.getPublicationID());
            Label countLabel = new Label("(" + count + ")");
            countLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #65676b; -fx-padding: 0 0 0 8;");
            header.getChildren().addAll(commentsTitle, countLabel);
        } catch (SQLException e) {
            header.getChildren().add(commentsTitle);
        }

        // Comment input
        HBox commentInput = createCommentInput(publication, panel);

        // Comments list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox commentsList = new VBox(12);
        commentsList.setPadding(new Insets(16, 20, 16, 20));
        commentsList.setId("commentsList");

        scrollPane.setContent(commentsList);

        panel.getChildren().addAll(header, commentInput, scrollPane);

        // Load comments
        loadComments(publication, commentsList);

        return panel;
    }

    private HBox createCommentInput(Publication publication, VBox parentPanel) {
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
                try {
                    Comment comment = new Comment(currentUser, publication, content, new Date());
                    commentService.insertOne(comment);
                    textField.clear();

                    // Reload comments
                    VBox commentsList = (VBox) ((ScrollPane) parentPanel.getChildren().get(2)).getContent();
                    loadComments(publication, commentsList);

                    // Update header count
                    HBox header = (HBox) parentPanel.getChildren().get(0);
                    int count = commentService.getCommentCount(publication.getPublicationID());
                    if (header.getChildren().size() > 1) {
                        ((Label) header.getChildren().get(1)).setText("(" + count + ")");
                    } else {
                        Label countLabel = new Label("(" + count + ")");
                        countLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #65676b; -fx-padding: 0 0 0 8;");
                        header.getChildren().add(countLabel);
                    }

                    // Refresh main view
                    loadPosts();

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Failed to post comment");
                }
            }
        });

        textField.setOnAction(e -> postBtn.fire());

        inputBox.getChildren().addAll(avatar, textField, postBtn);

        return inputBox;
    }

    private void loadComments(Publication publication, VBox commentsList) {
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
        }
    }

    private VBox createCommentItem(Comment comment, Publication publication, VBox commentsList) {
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

        Label nameLabel = new Label("Traveler #" + comment.getClient().getClientID());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label timeLabel = new Label("• " + comment.getTimeAgo());
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");

        nameTimeBox.getChildren().addAll(nameLabel, timeLabel);

        Label contentLabel = new Label(comment.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #050505;");

        contentBox.getChildren().addAll(nameTimeBox, contentLabel);

        topBox.getChildren().addAll(avatar, contentBox);

        // Actions if user owns comment
        if (comment.isOwnedBy(currentUser)) {
            HBox actions = new HBox(12);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setPadding(new Insets(4, 0, 0, 44));

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; " +
                    "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 0;");
            editBtn.setOnAction(e -> handleEditComment(comment, publication, commentsList));

            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #e4e6eb;");

            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; " +
                    "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 0;");
            deleteBtn.setOnAction(e -> handleDeleteComment(comment, publication, commentsList));

            actions.getChildren().addAll(editBtn, dot, deleteBtn);
            item.getChildren().addAll(topBox, actions);
        } else {
            item.getChildren().add(topBox);
        }

        return item;
    }

    private void handleEditComment(Comment comment, Publication publication, VBox commentsList) {
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
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Failed to update comment");
                }
            }
        });
    }

    private void handleDeleteComment(Comment comment, Publication publication, VBox commentsList) {
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
                loadPosts();
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to delete comment");
            }
        }
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

        Button menuBtn = new Button("⋯");
        menuBtn.getStyleClass().add("post-menu-btn");
        if (currentUser.getClientID() == publication.getClient().getClientID()) {
            menuBtn.setOnAction(e -> {
                e.consume(); // Prevent card click
                showPostMenu(publication, menuBtn);
            });
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
        likeBtn.setOnAction(e -> {
            e.consume();
            handleLike(publication);
        });

        Button shareBtn = new Button("↗️ Share");
        shareBtn.getStyleClass().add("action-btn");
        shareBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(shareBtn, Priority.ALWAYS);

        actions.getChildren().addAll(likeBtn, shareBtn);
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

    private void handleEdit(Publication publication) {
        // Close detail view if open
        if (postDetailView != null) {
            closePostDetail();
        }

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
                loadPosts();
                showSuccess("Post updated successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to update post");
            }
        });
    }

    private void handleDelete(Publication publication) {
        // Close detail view if open
        if (postDetailView != null) {
            closePostDetail();
        }

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
                showError("Failed to delete post");
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

        Label emptySubtext = new Label("Be the first to share something!");
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