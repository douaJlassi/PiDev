package controllers;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import entities.Message;
import entities.Person;
import entities.Profile;
import services.MessageService;
import services.PersonService;
import services.ProfileService;
import services.ServiceMessage;
import utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
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

    @FXML
    private Button btnChat;

    // New FXML elements for coin collection
    @FXML
    private Button collectCoinBtn;
    @FXML
    private Label coinCountLabel;
    @FXML
    private Label nextCoinTimerLabel;
    @FXML
    private ProgressIndicator coinProgressIndicator;
    @FXML
    private HBox coinCollectionBox;
    @FXML
    private Button shopBtn;
    @FXML
    private Button aiClassifierBtn;


    // Ad components
    private Popup adPopup;
    private Timeline adTimeline;
    private boolean isPremiumUser = false;
    private String userMembership = "Standard";

    private Person currentUser;
    private PersonService personService;
    private ProfileService profileService;
    private Profile userProfile;
    private Timeline messageCheckTimeline;
    private ChatPopupController chatPopupController;
    private Popup chatPopup;

    // Coin timer components
    private Timeline coinTimer;
    private LocalDateTime lastCoinTime;
    private Popup profilePopup;
    private Timeline hoverTimer;
    private double coinRate = 0.1; // Default for standard
    @FXML
    private Label lblBadge;
    private ServiceMessage serMsg = new ServiceMessage();
    @FXML
    private BorderPane mainBorderPane;
    private int currentUserId =11;
    @FXML
    public void initialize() {

        Platform.runLater(() -> {
            refreshBadge();
        });
        app.Session.loginAs(40,"USER");

        personService = new PersonService();
        profileService = new ProfileService();
        System.out.println("MainPageController initialized");

        setupButtonActions();
        setupAvatarHoverHandler();
        setupAvatarClickHandler();

        // Initialize coin collection UI as hidden until user data is loaded
        if (coinCollectionBox != null) {
            coinCollectionBox.setVisible(false);
        }
    }
    private void loadPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            root.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );

            contentArea.getChildren().setAll(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showExploreOffers() {
        loadPage("/fxml/VoyageurOffersGrid.fxml");
    }

    @FXML
    private void showCartView() {
        loadPage("/fxml/CartView.fxml");
    }

    @FXML
    private void showMyReservations() {
        loadPage("/fxml/MyReservations.fxml");
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

    private void setupAvatarHoverHandler() {
        if (userAvatar != null) {
            userAvatar.setOnMouseEntered(event -> startHoverTimer());
            userAvatar.setOnMouseExited(event -> stopHoverTimer());
        }
        if (userAvatarImage != null) {
            userAvatarImage.setOnMouseEntered(event -> startHoverTimer());
            userAvatarImage.setOnMouseExited(event -> stopHoverTimer());
        }
    }

    private void startHoverTimer() {
        if (hoverTimer != null) {
            hoverTimer.stop();
        }

        hoverTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            showProfilePopup();
        }));
        hoverTimer.setCycleCount(1);
        hoverTimer.play();
    }

    private void stopHoverTimer() {
        if (hoverTimer != null) {
            hoverTimer.stop();
        }
        if (profilePopup != null && profilePopup.isShowing()) {
            profilePopup.hide();
        }
    }

    private void showProfilePopup() {
        if (userProfile == null) return;

        profilePopup = new Popup();
        profilePopup.setAutoHide(true);

        String membership = userMembership;
        int coins = userProfile.getCoins();

        String icon = "👤";
        String color = "#666";
        String bgColor = "#f5f5f5";

        switch(membership.toLowerCase()) {
            case "vip+":
                icon = "💎";
                color = "#1D4D7C";
                bgColor = "#e6f0fa";
                break;
            case "vip":
                icon = "👑";
                color = "#FEC74C";
                bgColor = "#fff9e6";
                break;
            case "premium":
                icon = "⭐";
                color = "#0FA5A2";
                bgColor = "#e6f7f5";
                break;
            default:
                icon = "👤";
                color = "#666";
                bgColor = "#f5f5f5";
        }

        String coinRateText = getCoinRateText();

        VBox popupContent = new VBox(10);
        popupContent.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-padding: 15;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 0);" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 15;"
        );
        popupContent.setMinWidth(200);

        // Header with icon and membership
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label membershipLabel = new Label(membership.toUpperCase());
        membershipLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        headerBox.getChildren().addAll(iconLabel, membershipLabel);

        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: " + color + ";");

        // Coins info
        HBox coinsBox = new HBox(10);
        coinsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label coinIcon = new Label("🪙");
        coinIcon.setStyle("-fx-font-size: 18px;");

        Label coinsValue = new Label(coins + " coins");
        coinsValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        coinsBox.getChildren().addAll(coinIcon, coinsValue);

        // Rate info
        HBox rateBox = new HBox(10);
        rateBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label rateIcon = new Label("⚡");
        rateIcon.setStyle("-fx-font-size: 18px;");

        Label rateValue = new Label(coinRateText);
        rateValue.setStyle("-fx-font-size: 14px;");

        rateBox.getChildren().addAll(rateIcon, rateValue);

        // Add all to popup
        popupContent.getChildren().addAll(headerBox, separator, coinsBox, rateBox);

        profilePopup.getContent().add(popupContent);

        // Show popup near avatar
        Stage stage = (Stage) userAvatar.getScene().getWindow();
        double x = stage.getX() + userAvatar.localToScene(0, 0).getX() + userAvatar.getScene().getX() + 50;
        double y = stage.getY() + userAvatar.localToScene(0, 0).getY() + userAvatar.getScene().getY() + 50;

        profilePopup.show(stage, x, y);
    }

    @FXML
    private void handleAvatarClick(MouseEvent event) {
        try {
            System.out.println("Avatar clicked, opening profile page");

            // Stop timers when leaving main page
            stopAllTimers();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Showprofile.fxml"));
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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/chat_popup.fxml"));
            Parent chatRoot = loader.load();

            chatPopup = new Popup();
            chatPopup.getContent().add(chatRoot);
            chatPopup.setAutoHide(false);

            Stage stage = (Stage) chatButton.getScene().getWindow();
            chatPopup.show(stage);

            chatPopup.setX(stage.getX() + stage.getWidth() - 370);
            chatPopup.setY(stage.getY() + stage.getHeight() - 550);

            // Get controller and store reference
            chatPopupController = loader.getController();
            chatPopupController.setUserData(currentUser, chatPopup);

            // Restart message check timer when popup closes
            chatPopup.setOnHidden(e -> {
                chatPopupController = null;
                startMessageCheckTimer();
            });

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open chat: " + e.getMessage());
        }
    }

    public void closeChatPopup() {
        if (chatPopup != null && chatPopup.isShowing()) {
            chatPopup.hide();
            chatPopup = null;
            chatPopupController = null;
            System.out.println("Chat popup closed");
        }
    }

    private void setupButtonActions() {
        // Dashboard button action
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
        if (shopBtn != null) {
            shopBtn.setOnAction(event -> openShop());
        }
        if (aiClassifierBtn != null) {
            aiClassifierBtn.setOnAction(event -> openAIClassifier());
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

        // Coin collection button action
        if (collectCoinBtn != null) {
            collectCoinBtn.setOnAction(event -> collectCoin());
        }
    }

    private void openAIClassifier() {
        try {
            stopAllTimers();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AIClassifier.fxml"));
            Parent aiRoot = loader.load();

            AIClassifierController aiController = loader.getController();
            aiController.setUserData(currentUser);

            Stage currentStage = (Stage) aiClassifierBtn.getScene().getWindow();
            currentStage.setScene(new Scene(aiRoot));
            currentStage.setTitle("AI Travel Guide - " + currentUser.getUsername());
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open AI classifier: " + e.getMessage());
        }
    }
    private void openShop() {
        try {
            stopAllTimers();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Shop.fxml"));
            Parent shopRoot = loader.load();

            ShopController shopController = loader.getController();
            shopController.setUserData(currentUser);

            Stage currentStage = (Stage) shopBtn.getScene().getWindow();
            currentStage.setScene(new Scene(shopRoot));
            currentStage.setTitle("Shop - " + currentUser.getUsername());
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open shop: " + e.getMessage());
        }
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

        // Start ad timer for standard users only
        startAdTimer();

        // Setup coin collection for all users (with different rates)
        setupCoinCollection();
    }

    private void checkUserPremiumStatus() {
        try {
            userProfile = profileService.getProfileByUserId(currentUser.getId());
            if (userProfile != null) {
                String membership = userProfile.getMemberPremium();
                userMembership = membership != null ? membership : "Standard";
                isPremiumUser = membership != null &&
                        (membership.equalsIgnoreCase("Premium") ||
                                membership.equalsIgnoreCase("VIP") ||
                                membership.equalsIgnoreCase("VIP+"));
                System.out.println("User membership: " + userMembership + " (Is premium: " + isPremiumUser + ")");
            } else {
                isPremiumUser = false;
                userMembership = "Standard";
                System.out.println("No profile found, user is standard");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            isPremiumUser = false;
            userMembership = "Standard";
        }
    }

    private String getCoinRateText() {
        switch(userMembership.toLowerCase()) {
            case "vip+": return "5 coins/30s";
            case "vip": return "5 coins/30s";
            case "premium": return "1 coin/30s";
            default: return "0.1 coin/30s";
        }
    }

    private void setupCoinCollection() {
        if (userProfile == null) return;

        // Show coin collection UI
        if (coinCollectionBox != null) {
            coinCollectionBox.setVisible(true);
        }

        // Set coin rate based on membership
        switch(userMembership.toLowerCase()) {
            case "vip+":
            case "vip":
                coinRate = 5.0;
                break;
            case "premium":
                coinRate = 1.0;
                break;
            default:
                coinRate = 0.1;
        }

        // Update coin display
        updateCoinDisplay();

        // Start coin timer
        lastCoinTime = LocalDateTime.now();
        coinTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCoinTimer()));
        coinTimer.setCycleCount(Timeline.INDEFINITE);
        coinTimer.play();
    }

    private void updateCoinDisplay() {
        if (coinCountLabel != null && userProfile != null) {
            coinCountLabel.setText(userProfile.getCoins() + " 🪙");
        }
    }

    private void updateCoinTimer() {
        if (lastCoinTime == null || userProfile == null) return;

        LocalDateTime now = LocalDateTime.now();
        long secondsElapsed = java.time.Duration.between(lastCoinTime, now).getSeconds();
        long secondsRemaining = 30 - secondsElapsed;

        if (secondsRemaining <= 0) {
            // Ready to collect
            if (collectCoinBtn != null) {
                collectCoinBtn.setDisable(false);
                collectCoinBtn.setText("⚡");
                collectCoinBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15; -fx-cursor: hand;");
            }
            if (coinProgressIndicator != null) {
                coinProgressIndicator.setProgress(1.0);
            }
            if (nextCoinTimerLabel != null) {
                nextCoinTimerLabel.setText("Ready!");
                nextCoinTimerLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            }
        } else {
            // Waiting for next collection
            if (collectCoinBtn != null) {
                collectCoinBtn.setDisable(true);
                collectCoinBtn.setText(String.valueOf(secondsRemaining));
                collectCoinBtn.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #666; -fx-font-size: 12px; -fx-font-weight: bold; -fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15;");
            }
            if (coinProgressIndicator != null) {
                double progress = 1.0 - ((double) secondsRemaining / 30);
                coinProgressIndicator.setProgress(progress);
            }
            if (nextCoinTimerLabel != null) {
                nextCoinTimerLabel.setText(secondsRemaining + "s");
                nextCoinTimerLabel.setStyle("-fx-text-fill: #666;");
            }
        }
    }

    private void collectCoin() {
        try {
            int currentCoins = userProfile.getCoins();

            // Add coins based on membership rate
            double coinsToAdd = coinRate;
            int roundedCoins = (int) Math.round(coinsToAdd);

            userProfile.setCoins(currentCoins + roundedCoins);
            profileService.updateOne(userProfile);

            // Update display
            updateCoinDisplay();

            // Reset timer
            lastCoinTime = LocalDateTime.now();

            // Show animation
            if (collectCoinBtn != null) {
                collectCoinBtn.setText("✓");
                collectCoinBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15;");

                // Reset after 1 second
                Timeline resetBtn = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    collectCoinBtn.setText("⚡");
                }));
                resetBtn.setCycleCount(1);
                resetBtn.play();
            }

            System.out.println("Collected " + roundedCoins + " coins. Total: " + userProfile.getCoins());

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to collect coins: " + e.getMessage());
        }
    }

    private void startAdTimer() {
        // Only show ads for Standard users
        if (!"Standard".equalsIgnoreCase(userMembership) || isAdmin()) {
            System.out.println("User is " + userMembership + " or admin - no ads will be shown");
            return;
        }

        System.out.println("Starting ad timer for Standard user");

        adTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            showAdPopup();
        }));
        adTimeline.setCycleCount(Timeline.INDEFINITE);
        adTimeline.play();
    }

    private void showAdPopup() {
        if (adPopup != null && adPopup.isShowing()) {
            return;
        }

        try {
            adPopup = new Popup();
            adPopup.setAutoHide(false);

            VBox adContent = new VBox(20);
            adContent.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 20, 0, 0, 0);");
            adContent.setPrefWidth(400);
            adContent.setPrefHeight(500);
            adContent.setAlignment(javafx.geometry.Pos.TOP_CENTER);

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
                    adPopup = null;
                }
            });

            topBar.getChildren().add(closeButton);

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

            Label offerLabel = new Label("⏰ Limited Time Offer! ⏰");
            offerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff5e62;");

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
                if (adPopup != null) {
                    adPopup.hide();
                    adPopup = null;
                }
                openPremiumSubscription();
            });

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

            adContent.getChildren().add(topBar);

            if (adImageView != null) {
                VBox.setMargin(adImageView, new Insets(0, 10, 0, 10));
                adContent.getChildren().add(adImageView);
            } else {
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
            VBox.setMargin(getPremiumBtn, new Insets(10, 0, 20, 0));

            adPopup.getContent().add(adContent);

            Stage stage = (Stage) chatButton.getScene().getWindow();
            adPopup.show(stage);
            adPopup.setX(stage.getX() + (stage.getWidth() - 400) / 2);
            adPopup.setY(stage.getY() + (stage.getHeight() - 500) / 2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserInterface() {
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getUsername());

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
            userProfile = profileService.getProfileByUserId(currentUser.getId());

            if (userProfile != null && userProfile.getImage() != null && userProfile.getImage().length > 0) {
                ByteArrayInputStream bis = new ByteArrayInputStream(userProfile.getImage());
                Image profileImage = new Image(bis);

                userAvatarImage.setImage(profileImage);
                userAvatarImage.setVisible(true);
                userAvatar.setVisible(false);

                System.out.println("Profile image loaded successfully");
            } else {
                userAvatarImage.setVisible(false);
                userAvatar.setVisible(true);
                System.out.println("No profile image found, using gradient");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            userAvatarImage.setVisible(false);
            userAvatar.setVisible(true);
        }
    }

    private void checkUserRole() {
        if (currentUser != null) {
            String userRole = currentUser.getRole();
            boolean isAdmin = false;

            if (userRole != null) {
                String trimmedRole = userRole.trim();
                String lowerRole = trimmedRole.toLowerCase();

                isAdmin = lowerRole.equals("admin") ||
                        lowerRole.equals("administrator") ||
                        lowerRole.contains("admin");
            }

            dashboardBtn.setVisible(isAdmin);
            dashboardBtn.setManaged(isAdmin);
        }
    }

    private boolean isAdmin() {
        if (currentUser == null || currentUser.getRole() == null) return false;
        return currentUser.getRole().toLowerCase().contains("admin");
    }

    private void openDashboard() {
        try {
            System.out.println("Opening dashboard in same window");

            if (currentUser == null) {
                System.err.println("Cannot open dashboard: currentUser is null");
                return;
            }

            stopAllTimers();

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

    private void openPremiumSubscription() {
        // Only allow Standard users to access subscription page
        if (!"Standard".equalsIgnoreCase(userMembership)) {
            showAlert("Already Premium", "You are already a " + userMembership + " member!\n\nYour premium benefits are already active.");
            return;
        }

        try {
            System.out.println("Opening premium subscription page");

            stopAllTimers();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PremiumSubscription.fxml"));
            Parent subscriptionRoot = loader.load();

            PremiumSubscriptionController subscriptionController = loader.getController();
            subscriptionController.setUserData(currentUser);

            Stage currentStage = (Stage) chatButton.getScene().getWindow();
            currentStage.setScene(new Scene(subscriptionRoot));
            currentStage.setTitle("Premium Subscription - " + currentUser.getUsername());
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open premium subscription: " + e.getMessage());
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

    private void stopAllTimers() {
        if (messageCheckTimeline != null) {
            messageCheckTimeline.stop();
        }
        if (adTimeline != null) {
            adTimeline.stop();
        }
        if (coinTimer != null) {
            coinTimer.stop();
        }
        if (hoverTimer != null) {
            hoverTimer.stop();
        }
        if (adPopup != null && adPopup.isShowing()) {
            adPopup.hide();
            adPopup = null;
        }
        if (profilePopup != null && profilePopup.isShowing()) {
            profilePopup.hide();
        }
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

        closeChatPopup();
        stopAllTimers();

        try {
            if (currentUser != null) {
                System.out.println("Setting user " + currentUser.getUsername() + " to offline");
                personService.updateUserStatus(currentUser.getId(), "offline");
            }

            SessionManager.clearSession();
            System.out.println("Session cleared");

            Stage stage = (Stage) logoutBtn.getScene().getWindow();

            FXMLLoader loadingLoader = new FXMLLoader(getClass().getResource("/views/loading.fxml"));
            Parent loadingRoot = loadingLoader.load();

            stage.setScene(new Scene(loadingRoot));
            stage.setTitle("Loading...");
            stage.show();
            System.out.println("Loading screen shown");

            PauseTransition pause = new PauseTransition(Duration.seconds(4));

            pause.setOnFinished(event -> {
                try {
                    System.out.println("4 seconds passed, loading login page");

                    FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/views/AjouterPersonne.fxml"));
                    Parent loginRoot = loginLoader.load();

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

    public void refreshBadge() {
        int count = 0;
        try {
            count = serMsg.countUnreadMessages(currentUserId);
            System.out.println("DEBUG BADGE - Nombre trouvé : " + count);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (count > 0) {
            lblBadge.setText(String.valueOf(count > 99 ? "99+" : count)); // On limite à 99+
            lblBadge.setVisible(true);
            lblBadge.setManaged(true);
        } else {
            lblBadge.setVisible(false);
            lblBadge.setManaged(false);
        }

    }
    @FXML
    private void showChatView() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/ChatView.fxml"));
            mainBorderPane.setCenter(root);
            refreshBadge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showServicesView() {
        try {

            FXMLLoader loader  = new FXMLLoader(getClass().getResource("/views/Services.fxml"));
            Parent root = loader.load();
            ServicesController servicesController = loader.getController();
            servicesController.setConnectedUser(currentUser);
            mainBorderPane.setCenter(root);
            refreshBadge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showReservationsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Reservations.fxml"));
            Parent root = loader.load();

            ReservationsController reservationsC = loader.getController();
            reservationsC.setConnectedUser(currentUser);

            mainBorderPane.setCenter(root);
            refreshBadge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}