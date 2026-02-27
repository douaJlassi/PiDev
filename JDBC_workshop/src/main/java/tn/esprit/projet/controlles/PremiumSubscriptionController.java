package tn.esprit.projet.controlles;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.entities.Profile;
import tn.esprit.projet.services.PersonService;
import tn.esprit.projet.services.ProfileService;
import tn.esprit.projet.utils.EmailService;
import tn.esprit.projet.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class PremiumSubscriptionController {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label currentPlanLabel;
    @FXML
    private Label coinsCountLabel;
    @FXML
    private Label paymentStatusLabel;

    @FXML
    private Button premiumPlanBtn;
    @FXML
    private Button vipPlanBtn;
    @FXML
    private Button vipPlusPlanBtn;

    private Person currentUser;
    private Profile userProfile;
    private PersonService personService;
    private ProfileService profileService;

    private static final int PREMIUM_PRICE = 29;
    private static final int VIP_PRICE = 59;
    private static final int VIPPLUS_PRICE = 99;

    private static final int PREMIUM_BONUS_COINS = 10;
    private static final int VIP_BONUS_COINS = 20;
    private static final int VIPPLUS_BONUS_COINS = 25;

    @FXML
    public void initialize() {
        personService = new PersonService();
        profileService = new ProfileService();
    }

    public void setUserData(Person user) {
        this.currentUser = user;
        loadUserProfile();
        updateUserInfo();
    }

    private void loadUserProfile() {
        try {
            if (currentUser != null) {
                userProfile = profileService.getProfileByUserId(currentUser.getId());
                if (userProfile == null) {
                    // Create profile if doesn't exist
                    userProfile = new Profile();
                    userProfile.setIdUser(currentUser.getId());
                    userProfile.setCoins(0);
                    userProfile.setMemberPremium("Standard");
                    userProfile.setLanguage("English");
                    userProfile.setImage(null);
                    profileService.insertOne(userProfile);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load profile: " + e.getMessage());
        }
    }

    private void updateUserInfo() {
        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            emailLabel.setText(currentUser.getEmail());

            if (userProfile != null) {
                String membership = userProfile.getMemberPremium();
                if (membership != null) {
                    if (membership.equalsIgnoreCase("Premium")) {
                        currentPlanLabel.setText("Premium Member");
                        currentPlanLabel.setStyle("-fx-text-fill: #0FA5A2; -fx-font-weight: bold;");
                        disablePlanButtons();
                    } else if (membership.equalsIgnoreCase("VIP")) {
                        currentPlanLabel.setText("VIP Member");
                        currentPlanLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-weight: bold;");
                        disablePlanButtons();
                    } else if (membership.equalsIgnoreCase("VIP+")) {
                        currentPlanLabel.setText("VIP+ Member");
                        currentPlanLabel.setStyle("-fx-text-fill: #1D4D7C; -fx-font-weight: bold;");
                        disablePlanButtons();
                    } else {
                        currentPlanLabel.setText("Standard Member");
                        currentPlanLabel.setStyle("-fx-text-fill: #666;");
                    }
                }

                // Display coins
                coinsCountLabel.setText(String.valueOf(userProfile.getCoins()));
            }
        }
    }

    private void disablePlanButtons() {
        premiumPlanBtn.setDisable(true);
        vipPlanBtn.setDisable(true);
        vipPlusPlanBtn.setDisable(true);

        premiumPlanBtn.setText("✓ CURRENT");
        vipPlanBtn.setText("✓ CURRENT");
        vipPlusPlanBtn.setText("✓ CURRENT");
    }

    @FXML
    private void handlePremiumPlan() {
        showBeautifulPaymentDialog("Premium", PREMIUM_PRICE, PREMIUM_BONUS_COINS, 1, 0);
    }

    @FXML
    private void handleVIPPlan() {
        showBeautifulPaymentDialog("VIP", VIP_PRICE, VIP_BONUS_COINS, 5, 2);
    }

    @FXML
    private void handleVIPPlusPlan() {
        showBeautifulPaymentDialog("VIP+", VIPPLUS_PRICE, VIPPLUS_BONUS_COINS, 5, 3);
    }

    /**
     * Beautiful credit card payment dialog with real credit card design
     */
    private void showBeautifulPaymentDialog(String planName, int price, int bonusCoins, int coinsPer30Sec, int discountPercent) {
        // Create custom dialog
        Dialog<ButtonType> paymentDialog = new Dialog<>();
        paymentDialog.setTitle("💳 Secure Payment");
        paymentDialog.setHeaderText(null);

        // Style the dialog pane
        DialogPane dialogPane = paymentDialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #0FA5A2, #1D4D7C); -fx-background-radius: 30; -fx-padding: 20;");
        dialogPane.setPrefWidth(550);
        dialogPane.setPrefHeight(720);

        // Create header
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 20, 0));

        Label iconLabel = new Label("💳");
        iconLabel.setStyle("-fx-font-size: 48px; -fx-background-color: #FEC74C; -fx-background-radius: 50; -fx-padding: 15; -fx-text-fill: #1D4D7C;");
        iconLabel.setEffect(new DropShadow(15, Color.web("#FEC74C80")));

        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Secure Payment");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Upgrade to " + planName + " • " + price + " DT/month");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.9);");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        headerBox.getChildren().addAll(iconLabel, titleBox);

        // Create main content with credit card design
        VBox content = new VBox(25);
        content.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 20; -fx-padding: 25;");
        content.setEffect(new DropShadow(20, Color.web("#00000040")));

        // ===== CREDIT CARD VISUAL DESIGN =====
        StackPane creditCard = new StackPane();
        creditCard.setPrefWidth(450);
        creditCard.setPrefHeight(250);
        creditCard.setPadding(new Insets(20));

        // Card background with gradient
        Rectangle cardBg = new Rectangle(450, 250);
        cardBg.setArcWidth(20);
        cardBg.setArcHeight(20);
        cardBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0FA5A2")),
                new Stop(1, Color.web("#1D4D7C"))));
        cardBg.setEffect(new DropShadow(20, Color.web("#00000060")));

        // Card chip
        Rectangle chip = new Rectangle(50, 40);
        chip.setArcWidth(8);
        chip.setArcHeight(8);
        chip.setFill(Color.web("#FEC74C"));
        chip.setEffect(new DropShadow(5, Color.web("#00000040")));

        // Card brand logo
        Label brandLogo = new Label("💳");
        brandLogo.setStyle("-fx-font-size: 40px; -fx-text-fill: white;");

        // Card number display (masked)
        Label cardNumberDisplay = new Label("•••• •••• •••• ••••");
        cardNumberDisplay.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Monospaced';");
        cardNumberDisplay.setEffect(new DropShadow(5, Color.web("#00000040")));

        // Cardholder name display
        Label cardholderDisplay = new Label("CARDHOLDER NAME");
        cardholderDisplay.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.8);");

        // Expiry date display
        Label expiryDisplay = new Label("MM/YY");
        expiryDisplay.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.8);");

        // Position elements on card
        GridPane cardLayout = new GridPane();
        cardLayout.setPadding(new Insets(20));
        cardLayout.setHgap(15);
        cardLayout.setVgap(15);

        cardLayout.add(chip, 0, 0);
        cardLayout.add(brandLogo, 1, 0);
        cardLayout.add(cardNumberDisplay, 0, 1, 2, 1);

        HBox cardFooter = new HBox(50);
        cardFooter.setAlignment(Pos.CENTER_LEFT);

        VBox cardholderBox = new VBox(2);
        Label cardholderLabel = new Label("Card Holder");
        cardholderLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.6);");
        cardholderBox.getChildren().addAll(cardholderLabel, cardholderDisplay);

        VBox expiryBox = new VBox(2);
        Label expiresLabel = new Label("Expires");
        expiresLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.6);");
        expiryBox.getChildren().addAll(expiresLabel, expiryDisplay);

        cardFooter.getChildren().addAll(cardholderBox, expiryBox);
        cardLayout.add(cardFooter, 0, 2, 2, 1);

        creditCard.getChildren().addAll(cardBg, cardLayout);
        StackPane.setAlignment(cardBg, Pos.CENTER);
        StackPane.setAlignment(cardLayout, Pos.CENTER);

        // ===== PAYMENT FORM =====
        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(10, 0, 0, 0));

        // Card Number Field with icon
        HBox cardNumberBox = new HBox(10);
        cardNumberBox.setAlignment(Pos.CENTER_LEFT);
        Label cardIcon = new Label("💳");
        cardIcon.setStyle("-fx-font-size: 20px;");

        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("1234 5678 9012 3456");
        cardNumberField.setPrefHeight(45);
        cardNumberField.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px; -fx-font-family: 'Monospaced';");

        // Format card number as user types - FIXED VERSION
        cardNumberField.textProperty().addListener((obs, old, val) -> {
            // Remove all spaces
            String digits = val.replaceAll("\\s", "");

            // Limit to 16 digits
            if (digits.length() > 16) {
                digits = digits.substring(0, 16);
            }

            // Format with spaces every 4 digits
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    formatted.append(" ");
                }
                formatted.append(digits.charAt(i));
            }

            String newText = formatted.toString();

            // Only update if the text actually changed (prevents infinite loop)
            if (!newText.equals(val)) {
                cardNumberField.setText(newText);
                // Move cursor to end
                cardNumberField.positionCaret(newText.length());
            }

            // Update card display
            if (digits.isEmpty()) {
                cardNumberDisplay.setText("•••• •••• •••• ••••");
            } else {
                // Show masked version with dots for missing digits
                StringBuilder display = new StringBuilder();
                for (int i = 0; i < 16; i++) {
                    if (i < digits.length()) {
                        display.append(digits.charAt(i));
                    } else {
                        display.append("•");
                    }
                    if ((i + 1) % 4 == 0 && i < 15) {
                        display.append(" ");
                    }
                }
                cardNumberDisplay.setText(display.toString());
            }
        });

        cardNumberBox.getChildren().addAll(cardIcon, cardNumberField);
        HBox.setHgrow(cardNumberField, Priority.ALWAYS);

        // Expiry and CVV row
        HBox expiryCvvRow = new HBox(15);
        expiryCvvRow.setAlignment(Pos.CENTER_LEFT);

        // Expiry Field
        HBox expiryBox2 = new HBox(10);
        expiryBox2.setAlignment(Pos.CENTER_LEFT);
        Label expiryIcon = new Label("📅");
        expiryIcon.setStyle("-fx-font-size: 20px;");

        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        expiryField.setPrefHeight(45);
        expiryField.setPrefWidth(100);
        expiryField.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px;");

        // Format expiry as user types - FIXED VERSION
        expiryField.textProperty().addListener((obs, old, val) -> {
            // Remove any non-digit characters
            String digits = val.replaceAll("[^0-9]", "");

            // Limit to 4 digits (MMYY)
            if (digits.length() > 4) {
                digits = digits.substring(0, 4);
            }

            // Format as MM/YY
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i == 2 && digits.length() > 2) {
                    formatted.append("/");
                }
                formatted.append(digits.charAt(i));
            }

            String newText = formatted.toString();

            // Only update if the text actually changed
            if (!newText.equals(val)) {
                expiryField.setText(newText);
                expiryField.positionCaret(newText.length());
            }

            // Update card display
            if (digits.length() >= 4) {
                expiryDisplay.setText(digits.substring(0, 2) + "/" + digits.substring(2, 4));
            } else if (digits.length() >= 2) {
                expiryDisplay.setText(digits.substring(0, 2) + "/YY");
            } else if (!digits.isEmpty()) {
                expiryDisplay.setText(digits + "/YY");
            } else {
                expiryDisplay.setText("MM/YY");
            }
        });

        expiryBox2.getChildren().addAll(expiryIcon, expiryField);

        // CVV Field
        HBox cvvBox = new HBox(10);
        cvvBox.setAlignment(Pos.CENTER_LEFT);
        Label cvvIcon = new Label("🔒");
        cvvIcon.setStyle("-fx-font-size: 20px;");

        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("CVV");
        cvvField.setPrefHeight(45);
        cvvField.setPrefWidth(80);
        cvvField.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px; -fx-font-family: 'Monospaced';");

        // CVV input limit (3-4 digits)
        cvvField.textProperty().addListener((obs, old, val) -> {
            if (val.length() > 4) {
                cvvField.setText(val.substring(0, 4));
            }
        });

        cvvBox.getChildren().addAll(cvvIcon, cvvField);

        expiryCvvRow.getChildren().addAll(expiryBox2, cvvBox);

        // Cardholder Name Field
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameIcon = new Label("👤");
        nameIcon.setStyle("-fx-font-size: 20px;");

        TextField nameField = new TextField();
        nameField.setPromptText("John Doe");
        nameField.setPrefHeight(45);
        nameField.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px;");

        // Update card display with name
        nameField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) {
                cardholderDisplay.setText("CARDHOLDER NAME");
            } else {
                cardholderDisplay.setText(val.toUpperCase());
            }
        });

        nameBox.getChildren().addAll(nameIcon, nameField);
        HBox.setHgrow(nameField, Priority.ALWAYS);

        formBox.getChildren().addAll(cardNumberBox, expiryCvvRow, nameBox);

        // Plan benefits summary
        VBox benefitsBox = new VBox(8);
        benefitsBox.setPadding(new Insets(15, 0, 0, 0));
        benefitsBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-padding: 15;");

        Label benefitsTitle = new Label("✨ " + planName + " Benefits");
        benefitsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        VBox benefitsList = new VBox(5);

        Label benefit1 = new Label("✓ " + bonusCoins + " bonus coins on signup");
        benefit1.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        Label benefit2 = new Label("✓ " + (planName.equals("Premium") ? "1" : "5") + " coin every 30 seconds");
        benefit2.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        Label benefit3 = new Label("✓ Change username anytime");
        benefit3.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        benefitsList.getChildren().addAll(benefit1, benefit2, benefit3);

        if (discountPercent > 0) {
            Label discountLabel = new Label("✓ " + discountPercent + "% discount on all bookings");
            discountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-font-weight: bold; -fx-text-fill: #0FA5A2;");
            benefitsList.getChildren().add(discountLabel);
        }

        if (planName.equals("VIP+")) {
            Label groupLabel = new Label("✓ Create groups without admin confirmation");
            groupLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
            benefitsList.getChildren().add(groupLabel);
        }

        benefitsBox.getChildren().addAll(benefitsTitle, benefitsList);

        // Price summary
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        priceBox.setPadding(new Insets(10, 0, 0, 0));

        Label totalLabel = new Label("Total:");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #666;");

        Label amountLabel = new Label(price + " DT");
        amountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0FA5A2;");

        priceBox.getChildren().addAll(totalLabel, amountLabel);

        content.getChildren().addAll(creditCard, formBox, benefitsBox, priceBox);

        // Button Bar
        ButtonType payButtonType = new ButtonType("💳 Pay " + price + " DT", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(payButtonType, cancelButtonType);

        // Style buttons
        Button payButton = (Button) dialogPane.lookupButton(payButtonType);
        payButton.setStyle("-fx-background-color: linear-gradient(to right, #0FA5A2, #1D4D7C); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 25; -fx-cursor: hand;");
        payButton.setEffect(new DropShadow(10, Color.web("#0FA5A280")));

        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 25; -fx-background-radius: 25; -fx-cursor: hand;");
        cancelButton.setEffect(new DropShadow(10, Color.web("#ff5e6280")));

        // Disable pay button initially
        payButton.setDisable(true);

        // Enable pay button when all fields are filled
        Runnable updatePayButton = () -> {
            boolean valid = !cardNumberField.getText().isEmpty() &&
                    cardNumberField.getText().replaceAll("\\s", "").length() == 16 &&
                    !expiryField.getText().isEmpty() &&
                    expiryField.getText().replaceAll("[^0-9]", "").length() == 4 &&
                    !cvvField.getText().isEmpty() &&
                    cvvField.getText().length() >= 3 &&
                    !nameField.getText().isEmpty();
            payButton.setDisable(!valid);
        };

        cardNumberField.textProperty().addListener((obs, old, val) -> updatePayButton.run());
        expiryField.textProperty().addListener((obs, old, val) -> updatePayButton.run());
        cvvField.textProperty().addListener((obs, old, val) -> updatePayButton.run());
        nameField.textProperty().addListener((obs, old, val) -> updatePayButton.run());

        // Combine everything
        VBox mainContent = new VBox(20);
        mainContent.getChildren().addAll(headerBox, content);

        dialogPane.setContent(mainContent);

        // Show dialog and process result
        Optional<ButtonType> result = paymentDialog.showAndWait();
        if (result.isPresent() && result.get() == payButtonType) {
            processPayment(planName, bonusCoins);
        }
    }

    /**
     * Process payment with PauseTransition instead of Timeline to avoid animation conflicts
     */
    /**
     * Process payment with PauseTransition and proper threading
     */
    private void processPayment(String planName, int bonusCoins) {
        try {
            paymentStatusLabel.setText("Processing payment...");
            paymentStatusLabel.setStyle("-fx-text-fill: #0FA5A2;");
            paymentStatusLabel.setVisible(true);

            // Disable plan buttons during processing
            premiumPlanBtn.setDisable(true);
            vipPlanBtn.setDisable(true);
            vipPlusPlanBtn.setDisable(true);

            // Create a simple loading animation
            Timeline loadingAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> paymentStatusLabel.setText("⏳ Processing...")),
                    new KeyFrame(Duration.seconds(0.5), e -> paymentStatusLabel.setText("⏳ Processing...")),
                    new KeyFrame(Duration.seconds(1.0), e -> paymentStatusLabel.setText("⏳ Almost there...")),
                    new KeyFrame(Duration.seconds(1.5), e -> paymentStatusLabel.setText("⏳ Finalizing..."))
            );
            loadingAnimation.setCycleCount(1);
            loadingAnimation.play();

            // Use a simple PauseTransition for the delay
            PauseTransition pause = new PauseTransition(Duration.seconds(2));

            pause.setOnFinished(event -> {
                // Run database operations in background thread
                new Thread(() -> {
                    try {
                        // Update user profile to selected plan
                        userProfile.setMemberPremium(planName);

                        // Add bonus coins
                        int currentCoins = userProfile.getCoins();
                        userProfile.setCoins(currentCoins + bonusCoins);

                        profileService.updateOne(userProfile);

                        // Update UI on JavaFX thread
                        javafx.application.Platform.runLater(() -> {
                            // Update UI
                            currentPlanLabel.setText(planName + " Member");
                            if (planName.equals("Premium")) {
                                currentPlanLabel.setStyle("-fx-text-fill: #0FA5A2; -fx-font-weight: bold;");
                            } else if (planName.equals("VIP")) {
                                currentPlanLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-weight: bold;");
                            } else if (planName.equals("VIP+")) {
                                currentPlanLabel.setStyle("-fx-text-fill: #1D4D7C; -fx-font-weight: bold;");
                            }

                            coinsCountLabel.setText(String.valueOf(userProfile.getCoins()));
                            disablePlanButtons();

                            // Show success message with animation
                            paymentStatusLabel.setText("✅ Payment successful! Welcome to " + planName + "!");
                            paymentStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

                            // Simple success animation
                            Timeline successAnimation = new Timeline(
                                    new KeyFrame(Duration.ZERO, e -> paymentStatusLabel.setScaleX(1.0)),
                                    new KeyFrame(Duration.seconds(0.1), e -> paymentStatusLabel.setScaleX(1.1)),
                                    new KeyFrame(Duration.seconds(0.2), e -> paymentStatusLabel.setScaleX(1.0))
                            );
                            successAnimation.setCycleCount(3);
                            successAnimation.play();

                            // Update session
                            SessionManager.createSession(currentUser);

                            // Show success alert after a tiny delay (using Platform.runLater)
                            javafx.application.Platform.runLater(() -> {
                                // Use a simple Alert but ensure it's not during animation
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Subscription Successful");
                                alert.setHeaderText(null);
                                alert.setContentText("✨ Congratulations! You are now a " + planName + " Member!\n\n" +
                                        "✓ " + bonusCoins + " bonus coins added to your account\n" +
                                        "✓ Start collecting coins now!");
                                alert.showAndWait();
                            });
                        });

                        // Send confirmation email (can run in background)
                        sendPremiumConfirmationEmail(planName, bonusCoins);

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        javafx.application.Platform.runLater(() -> {
                            paymentStatusLabel.setText("❌ Payment failed: " + ex.getMessage());
                            paymentStatusLabel.setStyle("-fx-text-fill: #ff5e62;");

                            // Re-enable buttons on failure
                            premiumPlanBtn.setDisable(false);
                            vipPlanBtn.setDisable(false);
                            vipPlusPlanBtn.setDisable(false);

                            // Check if user already has a plan
                            if (userProfile != null && !"Standard".equalsIgnoreCase(userProfile.getMemberPremium())) {
                                disablePlanButtons();
                            }
                        });
                    }
                }).start();
            });

            pause.play();

        } catch (Exception e) {
            e.printStackTrace();

            // Re-enable buttons
            premiumPlanBtn.setDisable(false);
            vipPlanBtn.setDisable(false);
            vipPlusPlanBtn.setDisable(false);

            showAlert("Error", "Payment failed: " + e.getMessage());
        }
    }

    private void sendPremiumConfirmationEmail(String planName, int bonusCoins) {
        String toEmail = currentUser.getEmail();
        String username = currentUser.getUsername();

        String subject = "🎉 Welcome to " + planName + " - Rehletna.tn";

        String emailContent = "<html>" +
                "<head><style>" +
                "body { font-family: Arial, sans-serif; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "h1 { color: #0FA5A2; }" +
                ".benefits { background-color: #f8f9fa; padding: 15px; border-radius: 10px; }" +
                ".coins { font-size: 24px; color: #FEC74C; font-weight: bold; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "<h1>✨ Welcome to " + planName + ", " + username + "! ✨</h1>" +
                "<p>Thank you for subscribing to Rehletna.tn " + planName + "!</p>" +
                "<div class='benefits'>" +
                "<h3>Your " + planName + " Benefits:</h3>" +
                "<ul>" +
                "<li>✅ Change username anytime</li>" +
                "<li>✅ " + (planName.equals("Premium") ? "1" : "5") + " coin every 30 seconds</li>" +
                "<li>✅ <span class='coins'>" + bonusCoins + " bonus coins</span> added to your account</li>" +
                (planName.equals("VIP") ? "<li>✅ 2% discount on all bookings</li>" : "") +
                (planName.equals("VIP+") ? "<li>✅ 3% discount on all bookings</li>" : "") +
                (planName.equals("VIP+") ? "<li>✅ Create groups without admin confirmation</li>" : "") +
                "<li>✅ Priority customer support</li>" +
                "</ul>" +
                "</div>" +
                "<p>Your current coin balance: <strong>" + userProfile.getCoins() + " coins</strong></p>" +
                "<p>Start collecting coins now!</p>" +
                "<hr>" +
                "<p style='color: #999; font-size: 12px;'>© 2025 Rehletna.tn - All rights reserved</p>" +
                "</div>" +
                "</body>" +
                "</html>";

        EmailService.sendEmail(toEmail, subject, emailContent);
    }

    @FXML
    private void handleBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainPage.fxml"));
            Parent mainRoot = loader.load();

            MainPageController mainController = loader.getController();
            mainController.setUserData(currentUser);

            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            stage.setScene(new Scene(mainRoot));
            stage.setTitle("Main Page - " + currentUser.getUsername());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to main page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}