package controllers;

import entities.Client;
import entities.Publication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import services.CommentService;
import services.LikeService;
import services.PublicationService;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main Dashboard Controller - Coordinates all UI components
 * Delegates specific functionality to PostController, CommentController, and LikeController
 */
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
    private ImageView topLogoImage;

    @FXML
    private HBox createEditPanel;

    @FXML
    private Button postsNavBtn;

    // Sidebar labels (optional — populated in initialize)
    @FXML
    private Label sidebarUsername;

    @FXML
    private Label sidebarRole;

    @FXML
    private Button themeToggleBtn;

    @FXML
    private Button mapFab;

    @FXML
    private StackPane mapOverlay;

    // Services
    private PublicationService publicationService;
    private CommentService commentService;
    private LikeService likeService;

    // Sub-controllers
    private PostController postController;
    private CommentController commentController;
    private LikeController likeController;

    // State
    private Client currentUser;
    private List<Publication> allPosts;
    private boolean isGridView = true;

    @FXML
    public void initialize() {
        // Initialize services
        publicationService = new PublicationService();
        commentService = new CommentService();
        likeService = new LikeService();

        // Initialize current user (replace with actual session management)
        currentUser = new Client();
        currentUser.setClientID(1);
        currentUser.setUsername("Traveler");

        // Populate sidebar
        if (sidebarUsername != null) {
            sidebarUsername.setText(currentUser.getUsername() != null ? currentUser.getUsername() : "User");
        }
        if (sidebarRole != null) {
            sidebarRole.setText("USER");
        }

        // Initialize sub-controllers
        postController = new PostController(publicationService, currentUser, this);
        commentController = new CommentController(commentService, currentUser, this);
        likeController = new LikeController(likeService, currentUser, this);

        setupSearchFilter();
        loadPosts();
        postsScrollPane.setVvalue(0);

        // Register scene with ThemeManager. Handle both cases: scene already
        // attached (some loaders) and scene attached later (normal lifecycle).
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Scene s = contentContainer.getScene();
            if (s != null) {
                ThemeManager.get().register(s);
            } else {
                contentContainer.sceneProperty().addListener((obs, oldS, newS) -> {
                    if (newS != null) ThemeManager.get().register(newS);
                    if (oldS != null) ThemeManager.get().unregister(oldS);
                });
            }
        });
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
    private void showCreatePostPanel() {
        showPostFormPanel(null);  // null = create mode
    }

    /**
     * Show the create/edit post panel as a slide-in from the right.
     * @param existing — null for create, populated for edit
     */
    public void showPostFormPanel(Publication existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/create_post_dialog.fxml"));
            Parent form = loader.load();
            CreatePostController ctrl = loader.getController();
            ctrl.init(this, existing);

            // ── FIX 1: Wrap the form in a ScrollPane ─────────────────────────────
            // This ensures if the image is big, the middle scrolls, but buttons stay fixed.
            ScrollPane scrollWrapper = new ScrollPane(form);
            scrollWrapper.setFitToWidth(true);
            // Remove border and make background white
            scrollWrapper.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: transparent;");

            // IMPORTANT: Allow the scroll pane to grow and fill empty space
            VBox.setVgrow(scrollWrapper, javafx.scene.layout.Priority.ALWAYS);
            // ─────────────────────────────────────────────────────────────────────

            // Build panel container: Fixed Header + Scrollable Form + Fixed Footer
            VBox wrapper = new VBox(0);
            wrapper.setStyle("-fx-background-color: white;");
            wrapper.setPrefWidth(480); wrapper.setMinWidth(480); wrapper.setMaxWidth(480);

            // ── HEADER: Close button bar ──
            HBox closeBar = new HBox();
            closeBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            closeBar.setPadding(new javafx.geometry.Insets(14, 16, 14, 16));
            closeBar.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; -fx-border-width: 0 0 1 0;");

            // ── FIX 2: Professional "✕" Button ──
            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 50%; -fx-cursor: hand;");
            closeBtn.setOnAction(e -> hidePostFormPanel());

            javafx.scene.layout.Region spacer1 = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);

            Label title = new Label(existing == null ? "Create Post" : "Edit Post");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #050505;");

            javafx.scene.layout.Region spacer2 = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);

            javafx.scene.layout.Region placeholder = new javafx.scene.layout.Region();
            placeholder.setPrefWidth(36); // Balances the width of the close button

            closeBar.getChildren().addAll(closeBtn, spacer1, title, spacer2, placeholder);

            // ── FOOTER: Action bar ──
            HBox actionBar = new HBox(10);
            actionBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            actionBar.setPadding(new javafx.geometry.Insets(14, 20, 14, 20));
            actionBar.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #e4e6eb; -fx-border-width: 1 0 0 0;");

            Button cancelBtn = new Button("Cancel");
            cancelBtn.getStyleClass().add("secondary-btn");
            cancelBtn.setOnAction(e -> hidePostFormPanel());

            Button submitBtn = new Button(existing == null ? "Share" : "Update");
            submitBtn.getStyleClass().add("primary-btn");
            submitBtn.setDisable(true);

            ctrl.getContentArea().textProperty().addListener((obs, o, n) ->
                    submitBtn.setDisable(n == null || n.trim().isEmpty()));

            submitBtn.setOnAction(e -> {
                try {
                    if (existing == null) {
                        publicationService.insertOne(ctrl.buildPublication());
                        showSuccess("Post shared!");
                    } else {
                        ctrl.populateExisting(existing);
                        publicationService.updateOne(existing);
                        showSuccess("Post updated!");
                    }
                    hidePostFormPanel();
                    loadPosts();
                } catch (SQLException ex) {
                    showError("Failed: " + ex.getMessage());
                }
            });
            actionBar.getChildren().addAll(cancelBtn, submitBtn);

            // ── FIX 3: Add the SCROLLABLE wrapper instead of raw form ──
            wrapper.getChildren().addAll(closeBar, scrollWrapper, actionBar);
            createEditPanel.getChildren().setAll(wrapper);

            // Slide in from right
            createEditPanel.setVisible(true);
            createEditPanel.setManaged(true);
            createEditPanel.setTranslateX(480);
            TranslateTransition slide = new TranslateTransition(Duration.millis(250), createEditPanel);
            slide.setToX(0);
            slide.play();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open form: " + e.getMessage());
        }
    }

    private void hidePostFormPanel() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), createEditPanel);
        slide.setToX(480);
        slide.setOnFinished(e -> {
            createEditPanel.setVisible(false);
            createEditPanel.setManaged(false);
        });
        slide.play();
    }

    @FXML
    private void showMapView() {
        try {
            // MapController loads its own FXML via setController — same pattern
            // as PostDetailController. We instantiate it, let it load the FXML,
            // then grab the root node it produced.
            controllers.MapController mapCtrl = new controllers.MapController();
            mapCtrl.setOnClose(() -> closeMapView());
            javafx.scene.Parent mapRoot = mapCtrl.loadForOverlay();

            // Fill the overlay pane with the map view
            mapOverlay.getChildren().setAll(mapRoot);
            mapOverlay.setOpacity(0);
            mapOverlay.setVisible(true);
            mapOverlay.setManaged(true);

            // Hide the FAB while map is showing
            if (mapFab != null) { mapFab.setVisible(false); mapFab.setManaged(false); }

            // Fade in
            FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(280), mapOverlay);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open map: " + e.getMessage());
        }
    }

    private void closeMapView() {
        FadeTransition fadeOut = new FadeTransition(javafx.util.Duration.millis(220), mapOverlay);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            mapOverlay.setVisible(false);
            mapOverlay.setManaged(false);
            mapOverlay.getChildren().clear();
            if (mapFab != null) { mapFab.setVisible(true); mapFab.setManaged(true); }
        });
        fadeOut.play();
    }

    @FXML
    private void refreshPosts() {
        loadPosts();
    }

    @FXML
    private void toggleTheme() {
        ThemeManager.get().toggle();
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(ThemeManager.get().isDark() ? "Light Mode" : "Dark Mode");
        }
    }

    public void loadPosts() {
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
            VBox postCard = postController.createPostCard(publication, isGridView);

            if (isGridView) {
                postsGrid.getChildren().add(postCard);
            } else {
                postsList.getChildren().add(postCard);
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
        createFirstPost.setOnAction(e -> showCreatePostPanel());

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

    // Utility methods for sub-controllers
    public void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.get().applyToPane(alert.getDialogPane());
        alert.showAndWait();
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        ThemeManager.get().applyToPane(alert.getDialogPane());
        alert.showAndWait();
    }

    public void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.get().applyToPane(alert.getDialogPane());
        alert.showAndWait();
    }

    // Getters for sub-controllers
    public Client getCurrentUser() {
        return currentUser;
    }

    public StackPane getContentContainer() {
        return contentContainer;
    }

    public PostController getPostController() {
        return postController;
    }

    public CommentController getCommentController() {
        return commentController;
    }

    public LikeController getLikeController() {
        return likeController;
    }

    public CommentService getCommentService() {
        return commentService;
    }

    public LikeService getLikeService() {
        return likeService;
    }
}