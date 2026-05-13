package controllers;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import entities.Person;
import services.PersonService;
import utils.EmailService;

import java.sql.SQLException;

public class TwoFAVerificationController {

    @FXML
    private Label userEmailLabel;
    @FXML
    private Label emailDisplayLabel;
    @FXML
    private HBox codeBox;
    @FXML
    private TextField code1, code2, code3, code4, code5, code6;
    @FXML
    private Label errorLabel;
    @FXML
    private Label timerLabel;

    private Person currentUser;
    private PersonService personService;
    private Timeline timerTimeline;
    private int timeRemaining = 300; // 5 minutes in seconds
    private boolean canResend = true;
    private Timeline resendCooldownTimeline;
    private Stage currentStage;

    @FXML
    public void initialize() {
        personService = new PersonService();
        startTimer();

        // Setup the 6-digit code fields with auto-tab functionality
        setupCodeFields();
    }

    private void setupCodeFields() {
        // Array of all code fields
        TextField[] codeFields = {code1, code2, code3, code4, code5, code6};

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            TextField field = codeFields[i];

            // Restrict input to single digit
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                // Only allow digits
                if (!newValue.matches("\\d*")) {
                    field.setText(newValue.replaceAll("[^\\d]", ""));
                    return;
                }

                // Limit to 1 character
                if (newValue.length() > 1) {
                    field.setText(newValue.substring(0, 1));
                }

                // Auto move to next field when a digit is entered
                if (newValue.length() == 1 && index < 5) {
                    codeFields[index + 1].requestFocus();
                }

                // Check if all fields are filled for auto-submit
                checkAllFieldsFilled();
            });

            // Handle backspace to go to previous field
            field.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE) {
                    if (field.getText().isEmpty() && index > 0) {
                        codeFields[index - 1].requestFocus();
                        codeFields[index - 1].clear();
                    }
                }
            });

            // Handle left/right arrow keys for navigation
            field.setOnKeyReleased(event -> {
                if (event.getCode() == KeyCode.RIGHT && index < 5) {
                    codeFields[index + 1].requestFocus();
                } else if (event.getCode() == KeyCode.LEFT && index > 0) {
                    codeFields[index - 1].requestFocus();
                }
            });

            // Handle paste operation (if user pastes a 6-digit code)
            field.setOnKeyPressed(event -> {
                if (event.isControlDown() && event.getCode() == KeyCode.V) {
                    // Handle paste later through clipboard
                    javafx.application.Platform.runLater(() -> {
                        String clipboard = javafx.scene.input.Clipboard.getSystemClipboard().getString();
                        if (clipboard != null && clipboard.matches("\\d{6}")) {
                            pasteCode(clipboard);
                        }
                    });
                }
            });
        }
    }

    private void pasteCode(String code) {
        if (code.length() == 6) {
            code1.setText(String.valueOf(code.charAt(0)));
            code2.setText(String.valueOf(code.charAt(1)));
            code3.setText(String.valueOf(code.charAt(2)));
            code4.setText(String.valueOf(code.charAt(3)));
            code5.setText(String.valueOf(code.charAt(4)));
            code6.setText(String.valueOf(code.charAt(5)));
            code6.requestFocus();
            handleVerify();
        }
    }

    private void checkAllFieldsFilled() {
        TextField[] codeFields = {code1, code2, code3, code4, code5, code6};
        boolean allFilled = true;

        for (TextField field : codeFields) {
            if (field.getText().isEmpty()) {
                allFilled = false;
                break;
            }
        }

        if (allFilled) {
            handleVerify();
        }
    }

    private String getEnteredCode() {
        return code1.getText() + code2.getText() + code3.getText() +
                code4.getText() + code5.getText() + code6.getText();
    }

    private void clearCodeFields() {
        code1.clear();
        code2.clear();
        code3.clear();
        code4.clear();
        code5.clear();
        code6.clear();
    }

    private void disableCodeFields(boolean disable) {
        code1.setDisable(disable);
        code2.setDisable(disable);
        code3.setDisable(disable);
        code4.setDisable(disable);
        code5.setDisable(disable);
        code6.setDisable(disable);
    }

    public void setUserData(Person user) {
        this.currentUser = user;
        userEmailLabel.setText(user.getEmail());

        // Mask email for display
        String email = user.getEmail();
        String maskedEmail = maskEmail(email);
        emailDisplayLabel.setText("Verification code sent to:");
        userEmailLabel.setText(maskedEmail);
    }

    public void setStage(Stage stage) {
        this.currentStage = stage;
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
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeRemaining--;
            if (timeRemaining <= 0) {
                timerTimeline.stop();
                timerLabel.setText("Code expired!");
                timerLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                disableCodeFields(true);
                canResend = true;
            } else {
                int minutes = timeRemaining / 60;
                int seconds = timeRemaining % 60;
                timerLabel.setText(String.format("Code expires in: %02d:%02d", minutes, seconds));
                timerLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    @FXML
    private void handleVerify() {
        String code = getEnteredCode();

        if (code.length() != 6) {
            showError("Please enter all 6 digits");
            return;
        }

        if (timeRemaining <= 0) {
            showError("Code has expired. Please request a new code.");
            return;
        }

        try {
            if (personService.verify2FACode(currentUser.getId(), code)) {
                // Code verified successfully
                timerTimeline.stop();
                if (resendCooldownTimeline != null) {
                    resendCooldownTimeline.stop();
                }

                showInfo("✓ Verification successful! Redirecting...");

                // Update user status to online
                personService.updateUserStatus(currentUser.getId(), "online");

                // Navigate to loading then main page
                navigateToLoading();
            } else {
                showError("Invalid code. Please try again.");
                clearCodeFields();
                code1.requestFocus();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleResendCode() {
        if (!canResend) {
            showError("Please wait before requesting another code");
            return;
        }

        try {
            // Generate new code
            String newCode = EmailService.generate2FACode();

            // Save to database
            personService.save2FACode(currentUser.getId(), newCode);

            // Send email
            boolean sent = EmailService.send2FACode(currentUser.getEmail(), newCode);

            if (sent) {
                // Reset timer
                timeRemaining = 300;
                disableCodeFields(false);
                clearCodeFields();
                code1.requestFocus();

                // Restart timer
                if (timerTimeline != null) {
                    timerTimeline.stop();
                }
                startTimer();

                showInfo("✓ New verification code sent to your email");

                // Disable resend for 60 seconds
                canResend = false;
                startResendCooldown();

            } else {
                showError("Failed to send code. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error: " + e.getMessage());
        }
    }

    private void startResendCooldown() {
        resendCooldownTimeline = new Timeline(new KeyFrame(Duration.seconds(60), e -> {
            canResend = true;
        }));
        resendCooldownTimeline.setCycleCount(1);
        resendCooldownTimeline.play();
    }

    @FXML
    private void handleBackToLogin() {
        System.out.println("=== handleBackToLogin called ===");

        try {
            // Stop any running timers
            if (timerTimeline != null) {
                timerTimeline.stop();
                System.out.println("Timer stopped");
            }
            if (resendCooldownTimeline != null) {
                resendCooldownTimeline.stop();
                System.out.println("Cooldown timer stopped");
            }

            // Load the login FXML
            System.out.println("Loading AjouterPersonne.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterPersonne.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("ERROR: Could not find AjouterPersonne.fxml");
                showError("Could not find login page");
                return;
            }

            Parent loginRoot = loader.load();
            System.out.println("AjouterPersonne.fxml loaded successfully");

            // Use the stored stage or try to get from code1
            Stage stage = currentStage;
            if (stage == null && code1 != null && code1.getScene() != null) {
                stage = (Stage) code1.getScene().getWindow();
            }

            if (stage == null) {
                System.err.println("ERROR: Could not get stage reference");
                showError("Navigation error: Could not find window");
                return;
            }

            System.out.println("Stage found: " + stage);
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("Login");
            stage.show();
            System.out.println("Navigation completed");

        } catch (Exception e) {
            System.err.println("Exception in handleBackToLogin:");
            e.printStackTrace();
            showError("Error returning to login: " + e.getMessage());
        }
    }

    private void navigateToLoading() {
        try {
            System.out.println("Loading loading.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/loading.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("ERROR: Could not find loading.fxml");
                showError("Could not find loading screen");
                goToMainPage(); // Skip loading and go directly to main page
                return;
            }

            Parent loadingRoot = loader.load();
            System.out.println("loading.fxml loaded successfully");

            Stage stage = currentStage;
            if (stage == null && code1 != null && code1.getScene() != null) {
                stage = (Stage) code1.getScene().getWindow();
            }

            if (stage == null) {
                System.err.println("ERROR: Could not get stage reference for loading");
                goToMainPage();
                return;
            }

            stage.setScene(new Scene(loadingRoot));
            stage.setTitle("Loading...");
            stage.show();
            System.out.println("Loading screen shown");

            // Wait then go to main page
            PauseTransition pause = new PauseTransition(Duration.seconds(3.8));
            pause.setOnFinished(event -> {
                System.out.println("Pause finished, navigating to main page");
                goToMainPage();
            });
            pause.play();

        } catch (Exception e) {
            System.err.println("Exception in navigateToLoading:");
            e.printStackTrace();
            goToMainPage();
        }
    }

    private void goToMainPage() {
        try {
            System.out.println("Loading mainpage.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainpage.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("ERROR: Could not find mainpage.fxml");
                showError("Could not find main page");
                return;
            }

            Parent mainRoot = loader.load();
            System.out.println("mainpage.fxml loaded successfully");

            MainPageController mainController = loader.getController();
            if (mainController == null) {
                System.err.println("ERROR: MainPageController is null");
                showError("Could not load main page controller");
                return;
            }

            mainController.setUserData(currentUser);
            System.out.println("User data set in MainPageController: " + currentUser.getUsername());

            Stage stage = currentStage;
            if (stage == null && code1 != null && code1.getScene() != null) {
                stage = (Stage) code1.getScene().getWindow();
            }

            if (stage == null) {
                System.err.println("ERROR: Could not get stage reference for main page");
                showError("Navigation error: Could not find window");
                return;
            }

            stage.setScene(new Scene(mainRoot));
            stage.setTitle("Main Page");
            stage.show();
            System.out.println("Main page shown successfully");

        } catch (Exception e) {
            System.err.println("Exception in goToMainPage:");
            e.printStackTrace();
            showError("Error loading main page: " + e.getMessage());
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px;");

            // Auto-hide after 3 seconds
            Timeline hideError = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                errorLabel.setVisible(false);
            }));
            hideError.setCycleCount(1);
            hideError.play();
        } else {
            System.err.println("ERROR: " + message);
        }
    }

    private void showInfo(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");

            // Auto-hide after 3 seconds
            Timeline hideError = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                errorLabel.setVisible(false);
            }));
            hideError.setCycleCount(1);
            hideError.play();
        } else {
            System.out.println("INFO: " + message);
        }
    }
}