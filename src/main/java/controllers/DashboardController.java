package controllers;

import entities.Client;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

public class DashboardController {

    @FXML
    private VBox postsContainer;

    @FXML
    private ScrollPane postsScrollPane;

    @FXML
    private Button createPostBtn;

    @FXML
    private Button refreshBtn;

    private PublicationService publicationService;

    // This should be set based on your logged-in user
    private Client currentUser;

    // Directory to store uploaded images
    private static final String UPLOAD_DIR = "uploads/images/";

    @FXML
    public void initialize() {
        publicationService = new PublicationService();

        // Initialize current user (you should get this from your session/login system)
        currentUser = new Client();
        currentUser.setClientID(1); // Replace with actual logged-in user ID

        // Create upload directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load posts
        loadPosts();

        // Add smooth scrolling
        postsScrollPane.setVvalue(0);
    }

    @FXML
    private void showCreatePostDialog() {
        Dialog<Publication> dialog = new Dialog<>();
        dialog.setTitle("Create New Post");
        dialog.setHeaderText("Share your thoughts with the community");

        // Set the button types
        ButtonType createButtonType = new ButtonType("Post", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Apply custom styling
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("What's on your mind?");
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);
        contentArea.getStyleClass().add("dialog-text-area");

        TextField placeField = new TextField();
        placeField.setPromptText("Location (optional)");
        placeField.getStyleClass().add("dialog-text-field");

        // Image selection
        Label imageLabel = new Label("No image selected");
        imageLabel.getStyleClass().add("file-label");

        Button selectImageBtn = new Button("Choose Image");
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
                imageLabel.setText(file.getName());
            }
        });

        HBox imageBox = new HBox(10, selectImageBtn, imageLabel);
        imageBox.setAlignment(Pos.CENTER_LEFT);

        // Labels
        Label contentLabel = new Label("Content:");
        contentLabel.getStyleClass().add("dialog-label");

        Label placeLabel = new Label("Location:");
        placeLabel.getStyleClass().add("dialog-label");

        Label imageSelectLabel = new Label("Image:");
        imageSelectLabel.getStyleClass().add("dialog-label");

        grid.add(contentLabel, 0, 0);
        grid.add(contentArea, 0, 1, 2, 1);
        grid.add(placeLabel, 0, 2);
        grid.add(placeField, 0, 3, 2, 1);
        grid.add(imageSelectLabel, 0, 4);
        grid.add(imageBox, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Enable/Disable create button
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.getStyleClass().add("primary-btn");
        createButton.setDisable(true);

        // Validation
        contentArea.textProperty().addListener((observable, oldValue, newValue) -> {
            createButton.setDisable(newValue.trim().isEmpty());
        });

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String imagePath = null;

                // Handle image upload
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

                Publication publication = new Publication(
                        currentUser,
                        0,
                        contentArea.getText().trim(),
                        new Date(),
                        imagePath,
                        placeField.getText().trim().isEmpty() ? null : placeField.getText().trim()
                );

                return publication;
            }
            return null;
        });

        Optional<Publication> result = dialog.showAndWait();
        result.ifPresent(publication -> {
            try {
                publicationService.insertOne(publication);
                loadPosts();
                showSuccess("Post created successfully!");
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
        postsContainer.getChildren().clear();

        // Show loading indicator
        VBox loadingBox = new VBox(10);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.getStyleClass().add("loading-indicator");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);

        Label loadingText = new Label("Loading posts...");
        loadingText.getStyleClass().add("loading-text");

        loadingBox.getChildren().addAll(progressIndicator, loadingText);
        postsContainer.getChildren().add(loadingBox);

        // Load posts in background
        new Thread(() -> {
            try {
                List<Publication> publications = publicationService.selectALL();

                Platform.runLater(() -> {
                    postsContainer.getChildren().clear();

                    if (publications.isEmpty()) {
                        showEmptyState();
                    } else {
                        for (Publication publication : publications) {
                            VBox postCard = createPostCard(publication);
                            postsContainer.getChildren().add(postCard);
                        }
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    postsContainer.getChildren().clear();
                    showError("Failed to load posts: " + e.getMessage());
                });
            }
        }).start();
    }

    private VBox createPostCard(Publication publication) {
        VBox card = new VBox();
        card.getStyleClass().add("post-card");
        card.setMaxWidth(Double.MAX_VALUE);

        // Header (Author info and date)
        HBox header = new HBox(10);
        header.getStyleClass().add("post-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar placeholder (you can replace with actual avatar)
        Circle avatar = new Circle(20);
        avatar.setFill(javafx.scene.paint.Color.web("#667eea"));

        VBox authorInfo = new VBox(2);
        authorInfo.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label("User #" + publication.getClient().getClientID());
        authorLabel.getStyleClass().add("post-author");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
        Label dateLabel = new Label(dateFormat.format(publication.getDatePublication()));
        dateLabel.getStyleClass().add("post-date");

        authorInfo.getChildren().addAll(authorLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Place tag
        HBox placeBox = new HBox();
        if (publication.getPlace() != null && !publication.getPlace().isEmpty()) {
            Label placeLabel = new Label("📍 " + publication.getPlace());
            placeLabel.getStyleClass().add("post-place");
            placeBox.getChildren().add(placeLabel);
        }

        header.getChildren().addAll(avatar, authorInfo, spacer, placeBox);

        // Content
        VBox contentSection = new VBox(10);
        contentSection.getStyleClass().add("post-content-section");

        Text contentText = new Text(publication.getContent());
        contentText.getStyleClass().add("post-content");
        contentText.wrappingWidthProperty().bind(card.widthProperty().subtract(40));

        contentSection.getChildren().add(contentText);

        // Image (if exists)
        if (publication.getImagePath() != null && !publication.getImagePath().isEmpty()) {
            VBox imageContainer = new VBox();
            imageContainer.getStyleClass().add("post-image-container");

            try {
                File imageFile = new File(publication.getImagePath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.getStyleClass().add("post-image");
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(700);
                    imageView.setSmooth(true);

                    imageContainer.getChildren().add(imageView);
                    contentSection.getChildren().add(imageContainer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Actions footer
        HBox actionsBox = new HBox(20);
        actionsBox.getStyleClass().add("post-actions");
        actionsBox.setAlignment(Pos.CENTER_LEFT);

        // Like button
        Button likeBtn = new Button("❤ " + publication.getLikes().size() + " Likes");
        likeBtn.getStyleClass().add("secondary-btn");
        likeBtn.setOnAction(e -> handleLike(publication));

        // Comment button
        Button commentBtn = new Button("💬 " + publication.getComments().size() + " Comments");
        commentBtn.getStyleClass().add("secondary-btn");
        commentBtn.setOnAction(e -> handleComment(publication));

        actionsBox.getChildren().addAll(likeBtn, commentBtn);

        // Add edit/delete buttons if current user is the author
        if (currentUser.getClientID() == publication.getClient().getClientID()) {
            Region actionSpacer = new Region();
            HBox.setHgrow(actionSpacer, Priority.ALWAYS);

            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().add("secondary-btn");
            editBtn.setOnAction(e -> handleEdit(publication));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("danger-btn");
            deleteBtn.setOnAction(e -> handleDelete(publication));

            actionsBox.getChildren().addAll(actionSpacer, editBtn, deleteBtn);
        }

        card.getChildren().addAll(header, contentSection, actionsBox);

        return card;
    }

    private void handleLike(Publication publication) {
        // Implement like functionality
        showInfo("Like functionality - to be implemented with your Like entity");
    }

    private void handleComment(Publication publication) {
        // Implement comment functionality
        showInfo("Comment functionality - to be implemented with your Comment entity");
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
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);
        contentArea.getStyleClass().add("dialog-text-area");

        TextField placeField = new TextField(publication.getPlace() != null ? publication.getPlace() : "");
        placeField.getStyleClass().add("dialog-text-field");

        Label contentLabel = new Label("Content:");
        contentLabel.getStyleClass().add("dialog-label");

        Label placeLabel = new Label("Location:");
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
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Post");
        alert.setContentText("Are you sure you want to delete this post? This action cannot be undone.");

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
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.getStyleClass().add("empty-state");
        emptyState.setPadding(new Insets(60));

        Label emptyText = new Label("No posts yet");
        emptyText.getStyleClass().add("empty-state-text");

        Label emptySubtext = new Label("Be the first to share something with the community!");
        emptySubtext.getStyleClass().add("empty-state-subtext");

        Button createFirstPost = new Button("Create First Post");
        createFirstPost.getStyleClass().add("primary-btn");
        createFirstPost.setOnAction(e -> showCreatePostDialog());

        emptyState.getChildren().addAll(emptyText, emptySubtext, createFirstPost);
        postsContainer.getChildren().add(emptyState);
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

    // Helper class for creating avatar circles
    private static class Circle extends Region {
        private final double radius;

        public Circle(double radius) {
            this.radius = radius;
            setPrefSize(radius * 2, radius * 2);
            setMaxSize(radius * 2, radius * 2);
            setMinSize(radius * 2, radius * 2);
            setStyle("-fx-background-radius: " + radius + "px;");
        }

        public void setFill(javafx.scene.paint.Paint paint) {
            setStyle(getStyle() + "-fx-background-color: " + toRgbString(paint) + ";");
        }

        private String toRgbString(javafx.scene.paint.Paint paint) {
            if (paint instanceof javafx.scene.paint.Color) {
                javafx.scene.paint.Color color = (javafx.scene.paint.Color) paint;
                return String.format("#%02X%02X%02X",
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255));
            }
            return "#667eea";
        }
    }
}