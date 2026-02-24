package tn.esprit.projet.controlles;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
    private static final int VIPPLUS_PRICE = 239;

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
        showPaymentDialog("Premium", PREMIUM_PRICE, PREMIUM_BONUS_COINS, 1, 0);
    }

    @FXML
    private void handleVIPPlan() {
        showPaymentDialog("VIP", VIP_PRICE, VIP_BONUS_COINS, 5, 2);
    }

    @FXML
    private void handleVIPPlusPlan() {
        showPaymentDialog("VIP+", VIPPLUS_PRICE, VIPPLUS_BONUS_COINS, 5, 3);
    }

    private void showPaymentDialog(String planName, int price, int bonusCoins, int coinsPer30Sec, int discountPercent) {
        // Create custom payment dialog
        Dialog<ButtonType> paymentDialog = new Dialog<>();
        paymentDialog.setTitle("Premium Subscription");
        paymentDialog.setHeaderText("✨ Upgrade to " + planName + " - " + price + " DT/month");

        // Set the button types
        ButtonType payButtonType = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
        paymentDialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

        // Create payment form
        VBox paymentForm = new VBox(15);
        paymentForm.setStyle("-fx-padding: 20;");

        Label cardLabel = new Label("💳 Card Number:");
        TextField cardField = new TextField();
        cardField.setPromptText("1234 5678 9012 3456");

        Label expiryLabel = new Label("📅 Expiry Date:");
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");

        Label cvvLabel = new Label("🔒 CVV:");
        TextField cvvField = new TextField();
        cvvField.setPromptText("123");

        Label nameLabel = new Label("👤 Cardholder Name:");
        TextField nameField = new TextField();
        nameField.setPromptText("John Doe");

        // Plan benefits
        Label benefitsLabel = new Label("✨ " + planName + " Benefits:");
        benefitsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0FA5A2;");

        VBox benefits = new VBox(5);

        if (planName.equals("Premium")) {
            benefits.getChildren().addAll(
                    new Label("✓ Change username anytime"),
                    new Label("✓ 1 coin every 30 seconds"),
                    new Label("✓ " + bonusCoins + " bonus coins on signup"),
                    new Label("✓ Priority customer support")
            );
        } else if (planName.equals("VIP")) {
            benefits.getChildren().addAll(
                    new Label("✓ Change username anytime"),
                    new Label("✓ 5 coins every 30 seconds"),
                    new Label("✓ " + bonusCoins + " bonus coins on signup"),
                    new Label("✓ " + discountPercent + "% discount on all bookings"),
                    new Label("✓ Priority customer support")
            );
        } else if (planName.equals("VIP+")) {
            benefits.getChildren().addAll(
                    new Label("✓ Change username anytime"),
                    new Label("✓ 5 coins every 30 seconds"),
                    new Label("✓ " + bonusCoins + " bonus coins on signup"),
                    new Label("✓ " + discountPercent + "% discount on all bookings"),
                    new Label("✓ Create groups without admin confirmation"),
                    new Label("✓ Priority customer support")
            );
        }

        benefits.setStyle("-fx-padding: 0 0 0 10;");

        paymentForm.getChildren().addAll(
                benefitsLabel, benefits,
                new Separator(),
                cardLabel, cardField,
                expiryLabel, expiryField,
                cvvLabel, cvvField,
                nameLabel, nameField
        );

        paymentDialog.getDialogPane().setContent(paymentForm);

        // Show dialog and process result
        Optional<ButtonType> result = paymentDialog.showAndWait();
        if (result.isPresent() && result.get() == payButtonType) {
            // Validate form
            if (cardField.getText().isEmpty() || expiryField.getText().isEmpty() ||
                    cvvField.getText().isEmpty() || nameField.getText().isEmpty()) {
                showAlert("Error", "Please fill in all payment details");
                return;
            }

            // Process payment (simulate)
            processPayment(planName, bonusCoins);
        }
    }

    private void processPayment(String planName, int bonusCoins) {
        try {
            paymentStatusLabel.setText("Processing payment...");
            paymentStatusLabel.setStyle("-fx-text-fill: #0FA5A2;");
            paymentStatusLabel.setVisible(true);

            // Simulate payment processing
            Timeline paymentProcessing = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                try {
                    // Update user profile to selected plan
                    userProfile.setMemberPremium(planName);

                    // Add bonus coins
                    int currentCoins = userProfile.getCoins();
                    userProfile.setCoins(currentCoins + bonusCoins);

                    profileService.updateOne(userProfile);

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

                    paymentStatusLabel.setText("✓ Payment successful! Welcome to " + planName + "!");
                    paymentStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

                    // Update session
                    SessionManager.createSession(currentUser);

                    // Show success message
                    showAlert("Subscription Successful!",
                            "✨ Congratulations! You are now a " + planName + " Member!\n\n" +
                                    "Benefits unlocked:\n" +
                                    "✓ " + bonusCoins + " bonus coins added to your account\n" +
                                    "✓ Start collecting coins now!\n\n" +
                                    "Thank you for subscribing!");

                    // Send confirmation email
                    sendPremiumConfirmationEmail(planName, bonusCoins);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    paymentStatusLabel.setText("❌ Payment failed: " + ex.getMessage());
                    paymentStatusLabel.setStyle("-fx-text-fill: #ff5e62;");
                }
            }));

            paymentProcessing.setCycleCount(1);
            paymentProcessing.play();

        } catch (Exception e) {
            e.printStackTrace();
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