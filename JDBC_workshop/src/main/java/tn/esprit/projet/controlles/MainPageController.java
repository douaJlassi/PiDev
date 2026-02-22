package tn.esprit.projet.controlles;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.projet.entities.Message;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.entities.Profile;
import tn.esprit.projet.services.MessageService;
import tn.esprit.projet.services.PersonService;
import tn.esprit.projet.services.ProfileService;
import tn.esprit.projet.utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MainPageController {

    @FXML
    private Circle userAvatar;
    @FXML
    private ImageView userAvatarImage;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;
    @FXML
    private Button dashboardBtn;
    @FXML
    private Button activitiesBtn;
    @FXML
    private Button reservationsBtn;
    @FXML
    private Button messagesBtn;
    @FXML
    private Button favoritesBtn;
    @FXML
    private Button settingsBtn;
    @FXML
    private Button logoutBtn;
    @FXML
    private Button chatButton;
    @FXML
    private Label chatUnreadBadge;
    @FXML
    private StackPane contentArea;

    // Ad components
    private Popup adPopup;
    private Timeline adTimeline;
    private boolean isPremiumUser = false;

    private Person currentUser;
    private PersonService personService;
    private ProfileService profileService;
    private Profile userProfile;
    private Timeline messageCheckTimeline;

    @FXML
    public void initialize() {
        personService = new PersonService();
        profileService = new ProfileService();
        System.out.println("MainPageController initialized");


        setupButtonActions();


        setupAvatarClickHandler();
    }

    private void setupAvatarClickHandler() {
        if (userAvatar != null) {
            userAvatar.setOnMouseClicked(this::handleAvatarClick);
            userAvatar.setStyle("-fx-cursor: hand;");
        }
        if (userAvatarImage != null) {
            userAvatarImage.setOnMouseClicked(this::handleAvatarClick);
            userAvatarImage.setStyle("-fx-cursor: hand;");
        }
    }

    @FXML
    private void handleAvatarClick(MouseEvent event) {
        try {
            System.out.println("Avatar clicked, opening profile page");

            // Stop message check timer when leaving main page
            if (messageCheckTimeline != null) {
                messageCheckTimeline.stop();
            }

            // Stop ad timer when leaving main page
            if (adTimeline != null) {
                adTimeline.stop();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Showprofile.fxml"));
            Parent profileRoot = loader.load();

            ShowprofileController profileController = loader.getController();
            profileController.setUserData(currentUser);

            // Get the current stage and replace the scene
            Stage currentStage = (Stage) userAvatar.getScene().getWindow();
            currentStage.setScene(new Scene(profileRoot));
            currentStage.setTitle("My Profile - " + currentUser.getUsername());
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load profile page: " + e.getMessage());
        }
    }

    @FXML
    private void handleChatButton() {
        try {

            if (messageCheckTimeline != null) {
                messageCheckTimeline.stop();
            }


            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_popup.fxml"));
            Parent chatRoot = loader.load();


            Popup popup = new Popup();
            popup.getContent().add(chatRoot);
            popup.setAutoHide(false);


            Stage stage = (Stage) chatButton.getScene().getWindow();
            popup.show(stage);


            popup.setX(stage.getX() + stage.getWidth() - 370);
            popup.setY(stage.getY() + stage.getHeight() - 550);

            // Set user data
            ChatPopupController controller = loader.getController();
            controller.setUserData(currentUser, popup);

            // Restart message check timer when popup closes
            popup.setOnHidden(e -> startMessageCheckTimer());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open chat: " + e.getMessage());
        }
    }

    private void setupButtonActions() {
        // Dashboard button action - OPENS DASHBOARD IN THE SAME WINDOW
        if (dashboardBtn != null) {
            dashboardBtn.setOnAction(event -> {
                System.out.println("Dashboard button clicked");
                if (currentUser != null) {
                    openDashboard();
                } else {
                    System.err.println("currentUser is null in dashboard button action");
                }
            });
        }

        // Activities button action
        if (activitiesBtn != null) {
            activitiesBtn.setOnAction(event -> showMessage("arja3 ghodwa !"));
        }

        // My Bookings button action
        if (reservationsBtn != null) {
            reservationsBtn.setOnAction(event -> showMessage("arja3 ghodwa !"));
        }

        // Messages button action
        if (messagesBtn != null) {
            messagesBtn.setOnAction(event -> showMessage("arja3 ghodwa !"));
        }

        // Favorites button action
        if (favoritesBtn != null) {
            favoritesBtn.setOnAction(event -> showMessage("arja3 ghodwa !"));
        }

        // Settings button action
        if (settingsBtn != null) {
            settingsBtn.setOnAction(event -> showMessage("arja3 ghodwa !"));
        }

        // Chat button action
        if (chatButton != null) {
            chatButton.setOnAction(event -> handleChatButton());
        }

        // Logout button action
        if (logoutBtn != null) {
            logoutBtn.setOnAction(event -> handleLogout());
        }

        // Add hover effects
        addHoverEffect(dashboardBtn);
        addHoverEffect(activitiesBtn);
        addHoverEffect(reservationsBtn);
        addHoverEffect(messagesBtn);
        addHoverEffect(favoritesBtn);
        addHoverEffect(settingsBtn);
        addHoverEffect(logoutBtn);
    }

    private void addHoverEffect(Button button) {
        if (button == null) return;

        button.setOnMouseEntered(e -> {
            if (button == dashboardBtn && dashboardBtn.isVisible()) {
                button.setStyle("-fx-background-color: #d0f0f0; -fx-text-fill: #0FA5A2; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            } else if (button == logoutBtn) {
                button.setStyle("-fx-background-color: #ffe0e0; -fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand;");
            } else if (button != dashboardBtn) {
                button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            }
        });

        button.setOnMouseExited(e -> {
            if (button == dashboardBtn && dashboardBtn.isVisible()) {
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: #0FA5A2; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            } else if (button == logoutBtn) {
                button.setStyle("-fx-background-color: #fff0f0; -fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand;");
            } else if (button != dashboardBtn) {
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            }
        });
    }

    public void setUserData(Person user) {
        System.out.println("setUserData called with user: " + (user != null ? user.getUsername() : "null"));
        this.currentUser = user;

        if (user == null) {
            System.err.println("User is null in setUserData");
            return;
        }

        updateUserInterface();
        loadProfileImage();
        checkUserRole();
        checkUserPremiumStatus();

        // Update unread messages badge
        updateUnreadBadge();

        // Start a timer to periodically check for new messages
        startMessageCheckTimer();

        // Start ad timer for non-premium users
        startAdTimer();
    }

    private void checkUserPremiumStatus() {
        try {
            userProfile = profileService.getProfileByUserId(currentUser.getId());
            if (userProfile != null) {
                String membership = userProfile.getMemberPremium();
                isPremiumUser = membership != null &&
                        (membership.equalsIgnoreCase("Premium") || membership.equalsIgnoreCase("VIP"));
                System.out.println("User premium status: " + isPremiumUser + " (Membership: " + membership + ")");
            } else {
                isPremiumUser = false;
                System.out.println("No profile found, user is not premium");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            isPremiumUser = false;
        }
    }

    private void startAdTimer() {
        // Only show ads for non-premium users
        if (isPremiumUser || isAdmin()) {
            System.out.println("User is premium or admin - no ads will be shown");
            return;
        }

        System.out.println("Starting ad timer for non-premium user");

        adTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            showAdPopup();
        }));
        adTimeline.setCycleCount(Timeline.INDEFINITE);
        adTimeline.play();
    }

    private void showAdPopup() {
        // Don't show if already showing
        if (adPopup != null && adPopup.isShowing()) {
            return;
        }

        try {
            // Create ad popup
            adPopup = new Popup();
            adPopup.setAutoHide(false);

            // Create ad content with light gray background
            VBox adContent = new VBox(20);
            adContent.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 20, 0, 0, 0);");
            adContent.setPrefWidth(400);
            adContent.setPrefHeight(500);
            adContent.setAlignment(javafx.geometry.Pos.TOP_CENTER);

            // Close button (X) at top right - more visible
            HBox topBar = new HBox();
            topBar.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
            topBar.setPadding(new Insets(10, 10, 0, 0));
            topBar.setStyle("-fx-background-color: transparent;");

            Button closeButton = new Button("✕");
            closeButton.setStyle(
                    "-fx-background-color: #e0e0e0;" +
                            "-fx-text-fill: #666;" +
                            "-fx-font-size: 16px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-min-width: 30;" +
                            "-fx-min-height: 30;" +
                            "-fx-background-radius: 15;" +
                            "-fx-cursor: hand;"
            );

            // Hover effect for close button
            closeButton.setOnMouseEntered(e ->
                    closeButton.setStyle(
                            "-fx-background-color: #ff5e62;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-size: 16px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-min-width: 30;" +
                                    "-fx-min-height: 30;" +
                                    "-fx-background-radius: 15;" +
                                    "-fx-cursor: hand;"
                    )
            );
            closeButton.setOnMouseExited(e ->
                    closeButton.setStyle(
                            "-fx-background-color: #e0e0e0;" +
                                    "-fx-text-fill: #666;" +
                                    "-fx-font-size: 16px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-min-width: 30;" +
                                    "-fx-min-height: 30;" +
                                    "-fx-background-radius: 15;" +
                                    "-fx-cursor: hand;"
                    )
            );

            closeButton.setOnAction(e -> {
                if (adPopup != null) {
                    adPopup.hide();
                    adPopup = null; // Allow new popup to be created next time
                }
            });

            topBar.getChildren().add(closeButton);

            // Background Image
            ImageView adImageView = new ImageView();
            try {
                Image adImage = new Image(getClass().getResourceAsStream("/image/ads.jpg"));
                if (adImage != null && !adImage.isError()) {
                    adImageView.setImage(adImage);
                    adImageView.setFitWidth(380);
                    adImageView.setFitHeight(200);
                    adImageView.setPreserveRatio(true);
                    adImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
                } else {
                    adImageView = null;
                }
            } catch (Exception e) {
                System.err.println("Could not load ads.jpg: " + e.getMessage());
                adImageView = null;
            }

            // Ad text
            Label titleLabel = new Label("✨ Make Your Travel Easy! ✨");
            titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

            Label descriptionLabel = new Label(
                    "Join our premium community and get:\n" +
                            "• Exclusive travel deals\n" +
                            "• Priority customer support\n" +
                            "• Special discounts on activities\n" +
                            "• 24/7 concierge service\n" +
                            "• And much more!"
            );
            descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555; -fx-alignment: center;");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setAlignment(javafx.geometry.Pos.CENTER);
            descriptionLabel.setPadding(new Insets(0, 20, 0, 20));

            // Limited time offer label
            Label offerLabel = new Label("⏰ Limited Time Offer! ⏰");
            offerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff5e62;");

            // Get Premium Button
            Button getPremiumBtn = new Button("✨ GET PREMIUM NOW ✨");
            getPremiumBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #FEC74C, #0FA5A2);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 16px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 15 30;" +
                            "-fx-background-radius: 30;" +
                            "-fx-cursor: hand;"
            );
            getPremiumBtn.setOnAction(e -> {
                // Handle premium upgrade
                if (adPopup != null) {
                    adPopup.hide();
                    adPopup = null;
                }
                showMessage("✨ Welcome to Premium! ✨\n\nYou now have access to exclusive travel deals and priority support!");
                // Here you can navigate to a payment page or update membership status
            });

            // Hover effect for button
            getPremiumBtn.setOnMouseEntered(e ->
                    getPremiumBtn.setStyle(
                            "-fx-background-color: linear-gradient(to right, #0FA5A2, #FEC74C);" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-size: 16px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 15 30;" +
                                    "-fx-background-radius: 30;" +
                                    "-fx-cursor: hand;" +
                                    "-fx-scale-x: 1.05;" +
                                    "-fx-scale-y: 1.05;"
                    )
            );
            getPremiumBtn.setOnMouseExited(e ->
                    getPremiumBtn.setStyle(
                            "-fx-background-color: linear-gradient(to right, #FEC74C, #0FA5A2);" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-size: 16px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 15 30;" +
                                    "-fx-background-radius: 30;" +
                                    "-fx-cursor: hand;"
                    )
            );

            // Add all elements to ad content
            adContent.getChildren().add(topBar);

            if (adImageView != null) {
                VBox.setMargin(adImageView, new Insets(0, 10, 0, 10));
                adContent.getChildren().add(adImageView);
            } else {
                // Create a stylish fallback
                Rectangle fallbackRect = new Rectangle(380, 200);
                fallbackRect.setFill(Color.web("#0FA5A2"));
                fallbackRect.setArcWidth(20);
                fallbackRect.setArcHeight(20);

                Label fallbackLabel = new Label("Travel Easy");
                fallbackLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

                StackPane fallbackPane = new StackPane(fallbackRect, fallbackLabel);
                VBox.setMargin(fallbackPane, new Insets(0, 10, 0, 10));
                adContent.getChildren().add(fallbackPane);
            }

            adContent.getChildren().addAll(titleLabel, descriptionLabel, offerLabel, getPremiumBtn);

            // Add some padding at the bottom
            VBox.setMargin(getPremiumBtn, new Insets(10, 0, 20, 0));

            // Add to popup
            adPopup.getContent().add(adContent);

            // Position in center of screen
            Stage stage = (Stage) chatButton.getScene().getWindow();
            adPopup.show(stage);

            // Center position
            adPopup.setX(stage.getX() + (stage.getWidth() - 400) / 2);
            adPopup.setY(stage.getY() + (stage.getHeight() - 500) / 2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserInterface() {
        if (currentUser != null) {
            // Set username from database
            userNameLabel.setText(currentUser.getUsername());

            // Set role label based on user role
            String role = currentUser.getRole();
            if (role != null) {
                String lowerRole = role.toLowerCase().trim();
                if (lowerRole.contains("admin")) {
                    userRoleLabel.setText("Administrator");
                    userRoleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #0FA5A2; -fx-font-weight: bold;");
                } else {
                    userRoleLabel.setText("Member");
                    userRoleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #FEC74C; -fx-font-weight: bold;");
                }
            }
        }
    }

    private void loadProfileImage() {
        if (currentUser == null) return;

        try {
            // Get profile from database
            userProfile = profileService.getProfileByUserId(currentUser.getId());

            if (userProfile != null && userProfile.getImage() != null && userProfile.getImage().length > 0) {
                // Convert byte array to Image
                ByteArrayInputStream bis = new ByteArrayInputStream(userProfile.getImage());
                Image profileImage = new Image(bis);

                // Set the image to the ImageView
                userAvatarImage.setImage(profileImage);

                // Show image view, hide gradient circle
                userAvatarImage.setVisible(true);
                userAvatar.setVisible(false);

                System.out.println("Profile image loaded successfully");
            } else {
                // No profile image, use default gradient
                userAvatarImage.setVisible(false);
                userAvatar.setVisible(true);
                System.out.println("No profile image found, using gradient");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Fallback to gradient
            userAvatarImage.setVisible(false);
            userAvatar.setVisible(true);
        }
    }

    private void checkUserRole() {
        if (currentUser != null) {
            // Get role and handle null case
            String userRole = currentUser.getRole();

            boolean isAdmin = false;

            if (userRole != null) {
                String trimmedRole = userRole.trim();
                String lowerRole = trimmedRole.toLowerCase();

                System.out.println("Original role: '" + userRole + "'");
                System.out.println("Trimmed role: '" + trimmedRole + "'");
                System.out.println("Lowercase role: '" + lowerRole + "'");

                // Check multiple conditions
                isAdmin = lowerRole.equals("admin") ||
                        lowerRole.equals("administrator") ||
                        lowerRole.contains("admin");

                System.out.println("Is admin: " + isAdmin);
            }

            // Show/hide dashboard button based on role
            dashboardBtn.setVisible(isAdmin);
            dashboardBtn.setManaged(isAdmin);

            System.out.println("Dashboard button visible: " + dashboardBtn.isVisible());
        }
    }

    private boolean isAdmin() {
        if (currentUser == null || currentUser.getRole() == null) return false;
        return currentUser.getRole().toLowerCase().contains("admin");
    }

    /**
     * Opens the dashboard in the same window
     */
    private void openDashboard() {
        try {
            System.out.println("Opening dashboard in same window");

            if (currentUser == null) {
                System.err.println("Cannot open dashboard: currentUser is null");
                return;
            }

            // Stop timers when leaving main page
            if (messageCheckTimeline != null) {
                messageCheckTimeline.stop();
            }
            if (adTimeline != null) {
                adTimeline.stop();
            }
            if (adPopup != null && adPopup.isShowing()) {
                adPopup.hide();
                adPopup = null;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("Could not find dashboard.fxml at /dashboard.fxml");
                showAlert("Error", "Could not find dashboard.fxml");
                return;
            }

            Parent dashboardRoot = loader.load();

            DashboardController dashboardController = loader.getController();
            if (dashboardController != null) {
                System.out.println("Passing user to DashboardController: " + currentUser.getUsername());
                dashboardController.setUserData(currentUser);
            }

            // Get the current stage and replace the scene
            Stage currentStage = (Stage) dashboardBtn.getScene().getWindow();
            currentStage.setScene(new Scene(dashboardRoot));
            currentStage.setTitle("Dashboard - " + currentUser.getUsername());
            currentStage.show();

        } catch (IOException e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load dashboard: " + e.getMessage());
        }
    }

    private void updateUnreadBadge() {
        try {
            MessageService messageService = new MessageService();
            List<Message> unreadMessages = messageService.getUnreadMessagesForUser(currentUser.getId());
            int unreadCount = unreadMessages.size();

            if (unreadCount > 0) {
                chatUnreadBadge.setText(String.valueOf(unreadCount));
                chatUnreadBadge.setVisible(true);
            } else {
                chatUnreadBadge.setVisible(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startMessageCheckTimer() {
        messageCheckTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            if (currentUser != null) {
                updateUnreadBadge();
            }
        }));
        messageCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        messageCheckTimeline.play();
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        System.out.println("Logout button clicked");

        // Stop all timers
        if (messageCheckTimeline != null) {
            messageCheckTimeline.stop();
        }
        if (adTimeline != null) {
            adTimeline.stop();
        }
        if (adPopup != null && adPopup.isShowing()) {
            adPopup.hide();
            adPopup = null;
        }

        try {
            // Update user status to offline in database
            if (currentUser != null) {
                System.out.println("Setting user " + currentUser.getUsername() + " to offline");
                personService.updateUserStatus(currentUser.getId(), "offline");
            }

            // Clear session
            SessionManager.clearSession();
            System.out.println("Session cleared");

            // Get current stage
            Stage stage = (Stage) logoutBtn.getScene().getWindow();

            // Load loading screen
            FXMLLoader loadingLoader = new FXMLLoader(getClass().getResource("/loading.fxml"));
            Parent loadingRoot = loadingLoader.load();

            stage.setScene(new Scene(loadingRoot));
            stage.setTitle("Loading...");
            stage.show();
            System.out.println("Loading screen shown");

            // Wait 4 seconds then open login page
            PauseTransition pause = new PauseTransition(Duration.seconds(4));

            pause.setOnFinished(event -> {
                try {
                    System.out.println("4 seconds passed, loading login page");

                    FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/AjouterPersonne.fxml"));
                    Parent loginRoot = loginLoader.load();

                    // Set the stage reference for the login controller
                    AjouterPersonne loginController = loginLoader.getController();
                    loginController.setPrimaryStage(stage);

                    stage.setScene(new Scene(loginRoot));
                    stage.setTitle("Login");
                    stage.show();
                    System.out.println("Login page shown");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            pause.play();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}