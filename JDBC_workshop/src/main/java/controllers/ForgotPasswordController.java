package controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import entities.Person;
import services.PersonService;
import utils.EmailService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

public class ForgotPasswordController {

    @FXML
    private VBox forgotPasswordContainer;

    // Step 1 components
    @FXML
    private VBox step1Container;
    @FXML
    private TextField emailOrUsernameField;
    @FXML
    private Label verificationErrorLabel;
    @FXML
    private Button sendCodeBtn;

    // Step 2 components
    @FXML
    private VBox step2Container;
    @FXML
    private Label codeSentEmailLabel;
    @FXML
    private TextField code1, code2, code3, code4, code5, code6;
    @FXML
    private Label timerLabel;
    @FXML
    private Label resendCodeLink;
    @FXML
    private Button verifyCodeBtn;

    // Step 3 components
    @FXML
    private VBox step3Container;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmNewPasswordField;
    @FXML
    private Label passwordErrorLabel;
    @FXML
    private Button resetPasswordBtn;

    @FXML
    private Label backToLoginLink;

    private PersonService personService;
    private String generatedCode;
    private int timeRemaining = 300; // 5 minutes in seconds
    private Timeline timerTimeline;
    private Person foundUser;
    private String userEmail;

    @FXML
    public void initialize() {
        personService = new PersonService();
        setupCodeFields();
    }

    private void setupCodeFields() {
        TextField[] codeFields = {code1, code2, code3, code4, code5, code6};

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            TextField field = codeFields[i];

            // Restrict to single digit
            field.textProperty().addListener((obs, old, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    field.setText(newValue.replaceAll("[^\\d]", ""));
                }
                if (newValue.length() > 1) {
                    field.setText(newValue.substring(0, 1));
                }
                // Auto-advance
                if (newValue.length() == 1 && index < 5) {
                    codeFields[index + 1].requestFocus();
                }
            });

            // Handle backspace
            field.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE && field.getText().isEmpty() && index > 0) {
                    codeFields[index - 1].requestFocus();
                }
            });
        }
    }

    @FXML
    private void handleSendCode() {
        String emailOrUsername = emailOrUsernameField.getText().trim();

        if (emailOrUsername.isEmpty()) {
            showError(verificationErrorLabel, "Please enter your email or username");
            return;
        }

        try {
            // Find user by email or username
            foundUser = personService.findByEmailOrUsername(emailOrUsername);

            if (foundUser == null) {
                showError(verificationErrorLabel, "No account found with this email or username");
                return;
            }

            userEmail = foundUser.getEmail();

            // Generate 6-digit code
            Random random = new Random();
            generatedCode = String.format("%06d", random.nextInt(1000000));

            // Send code via email
            boolean sent = EmailService.sendPasswordResetCode(userEmail, generatedCode);

            if (sent) {
                // Move to step 2
                step1Container.setVisible(false);
                step1Container.setManaged(false);
                step2Container.setVisible(true);
                step2Container.setManaged(true);

                codeSentEmailLabel.setText("Code sent to: " + maskEmail(userEmail));
                startTimer();
                showSuccess("Verification code sent to your email");
            } else {
                showError(verificationErrorLabel, "Failed to send email. Please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError(verificationErrorLabel, "Database error: " + e.getMessage());
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 3) {
            String localPart = email.substring(0, atIndex);
            String domain = email.substring(atIndex);
            String maskedLocal = localPart.substring(0, 2) + "****" + localPart.substring(localPart.length() - 2);
            return maskedLocal + domain;
        }
        return email;
    }

    private void startTimer() {
        timeRemaining = 300;
        updateTimerLabel();

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeRemaining--;
            updateTimerLabel();
            if (timeRemaining <= 0) {
                timerTimeline.stop();
                timerLabel.setText("Code expired!");
                timerLabel.setStyle("-fx-text-fill: #ff5e62;");
                disableCodeFields(true);
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void updateTimerLabel() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("Code expires in: %02d:%02d", minutes, seconds));
        timerLabel.setStyle("-fx-text-fill: #FEC74C;");
    }

    private void disableCodeFields(boolean disable) {
        TextField[] codeFields = {code1, code2, code3, code4, code5, code6};
        for (TextField field : codeFields) {
            field.setDisable(disable);
        }
        verifyCodeBtn.setDisable(disable);
    }

    @FXML
    private void handleResendCode() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        // Generate new code
        Random random = new Random();
        generatedCode = String.format("%06d", random.nextInt(1000000));

        // Send code via email
        boolean sent = EmailService.sendPasswordResetCode(userEmail, generatedCode);

        if (sent) {
            timeRemaining = 300;
            disableCodeFields(false);
            clearCodeFields();
            startTimer();
            showSuccess("New verification code sent");
        } else {
            showError(verificationErrorLabel, "Failed to resend code");
        }
    }

    private void clearCodeFields() {
        code1.clear();
        code2.clear();
        code3.clear();
        code4.clear();
        code5.clear();
        code6.clear();
        code1.requestFocus();
    }

    private String getEnteredCode() {
        return code1.getText() + code2.getText() + code3.getText() +
                code4.getText() + code5.getText() + code6.getText();
    }

    @FXML
    private void handleVerifyCode() {
        String enteredCode = getEnteredCode();

        if (enteredCode.length() != 6) {
            showError(verificationErrorLabel, "Please enter all 6 digits");
            return;
        }

        if (timeRemaining <= 0) {
            showError(verificationErrorLabel, "Code has expired. Please request a new one.");
            return;
        }

        if (enteredCode.equals(generatedCode)) {
            // Code verified, move to step 3
            if (timerTimeline != null) {
                timerTimeline.stop();
            }
            step2Container.setVisible(false);
            step2Container.setManaged(false);
            step3Container.setVisible(true);
            step3Container.setManaged(true);
        } else {
            showError(verificationErrorLabel, "Invalid code. Please try again.");
            clearCodeFields();
        }
    }

    @FXML
    private void handleResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmNewPasswordField.getText();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError(passwordErrorLabel, "Please fill all fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError(passwordErrorLabel, "Passwords do not match");
            return;
        }

        if (newPassword.length() < 8) {
            showError(passwordErrorLabel, "Password must be at least 8 characters");
            return;
        }

        try {
            // Update password in database
            foundUser.setPassword(newPassword);
            personService.updateOne(foundUser);

            // Show success message
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Success");
            success.setHeaderText(null);
            success.setContentText("Password reset successfully! You can now login with your new password.");
            success.showAndWait();

            // Go back to login page
            goBackToLogin();

        } catch (SQLException e) {
            e.printStackTrace();
            showError(passwordErrorLabel, "Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        goBackToLogin();
    }

    private void goBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterPersonne.fxml"));
            Parent loginRoot = loader.load();

            Stage stage = (Stage) forgotPasswordContainer.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("Login");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError(verificationErrorLabel, "Failed to load login page");
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);

        // Auto-hide after 3 seconds
        Timeline hideTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            label.setVisible(false);
            label.setManaged(false);
        }));
        hideTimer.setCycleCount(1);
        hideTimer.play();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onLinkHover(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Label label) {
            label.setScaleX(1.1);
            label.setScaleY(1.1);
        }
    }

    @FXML
    private void onLinkExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Label label) {
            label.setScaleX(1.0);
            label.setScaleY(1.0);
        }
    }
}