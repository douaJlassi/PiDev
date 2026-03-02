package tn.esprit.projet.controlles;

import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.entities.Profile;
import tn.esprit.projet.services.PersonService;
import tn.esprit.projet.services.ProfileService;
import tn.esprit.projet.utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Optional;
import java.util.prefs.Preferences;

public class AjouterPersonne {

    @FXML private HBox mainContainer;
    @FXML private VBox leftPane;
    @FXML private StackPane formPane;
    @FXML private VBox signupPane;
    @FXML private VBox loginPane;
    @FXML private Button signupButton;
    @FXML private Button loginButton;

    // Form fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField dateField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    // Role selection - Updated to RadioButtons
    @FXML private RadioButton userRoleRadio;
    @FXML private RadioButton guiderRoleRadio;
    @FXML private RadioButton agenceRoleRadio;
    @FXML private ToggleGroup roleGroup;

    // Login fields
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button faceIDLoginButton;
    @FXML private Button qrScanButton;
    @FXML private Label forgotPasswordLink;

    // Validation lines and error messages
    private Line firstNameLine;
    private Line lastNameLine;
    private Line usernameLine;
    private Line emailLine;
    private Line dateLine;
    private Line passwordLine;
    private Line confirmPasswordLine;

    private Label firstNameError;
    private Label lastNameError;
    private Label usernameError;
    private Label emailError;
    private Label dateError;
    private Label passwordError;
    private Label confirmPasswordError;

    // Login error label
    private Label loginError;

    // Success message label
    private Label successMessage;

    // Validation properties
    private final BooleanProperty firstNameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty lastNameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty usernameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty emailValid = new SimpleBooleanProperty(false);
    private final BooleanProperty dateValid = new SimpleBooleanProperty(false);
    private final BooleanProperty passwordValid = new SimpleBooleanProperty(false);
    private final BooleanProperty confirmPasswordValid = new SimpleBooleanProperty(false);

    // Store user after email validation
    private Person currentFaceIDUser;

    private static final Duration FAST = Duration.millis(250);
    private static final Duration NORMAL = Duration.millis(450);

    private final Interpolator SMOOTH = Interpolator.SPLINE(0.25, 0.8, 0.25, 1);

    // Service
    private PersonService personService;

    // Preferences for Remember Me
    private Preferences preferences;

    // Store stage reference - STATIC to persist across instances
    private static Stage primaryStage;

    @FXML
    public void initialize() {
        // Initialize service
        personService = new PersonService();

        // Initialize preferences
        preferences = Preferences.userRoot().node(this.getClass().getName());

        // Ensure login hidden
        loginPane.setVisible(false);
        loginPane.setOpacity(0);

        // Initial smooth entrance
        leftPane.setOpacity(0);
        leftPane.setTranslateX(-60);
        formPane.setOpacity(0);
        formPane.setTranslateX(60);

        ParallelTransition intro = new ParallelTransition(
                fadeSlide(leftPane, 0, 1, -60, 0, NORMAL),
                fadeSlide(formPane, 0, 1, 60, 0, NORMAL)
        );
        intro.play();

        // Create validation lines and error messages
        createValidationLines();
        createErrorLabels();
        createSuccessMessage();
        createLoginError();

        // Setup validation listeners
        setupValidation();

        // Disable signup button initially
        signupButton.setDisable(true);

        // Bind signup button to validation
        signupButton.disableProperty().bind(
                firstNameValid.not()
                        .or(lastNameValid.not())
                        .or(usernameValid.not())
                        .or(emailValid.not())
                        .or(dateValid.not())
                        .or(passwordValid.not())
                        .or(confirmPasswordValid.not())
        );

        // Add action to signup button
        signupButton.setOnAction(event -> handleSignUp());

        // Add action to login button
        loginButton.setOnAction(event -> handleLogin());

        // Initialize Face ID login
        initializeFaceIDLogin();

        // Subtle floating animation
        createFloatingEffect(leftPane);

        // Check for existing session after UI is initialized
        javafx.application.Platform.runLater(() -> {
            checkExistingSession();
        });

        // Debug: Verify role selection
        System.out.println("=== ROLE SELECTION DEBUG ===");
        System.out.println("User Radio: " + (userRoleRadio != null));
        System.out.println("Guider Radio: " + (guiderRoleRadio != null));
        System.out.println("Agency Radio: " + (agenceRoleRadio != null));
        System.out.println("Toggle Group: " + (roleGroup != null));

        // Set default selection
        if (userRoleRadio != null) {
            userRoleRadio.setSelected(true);
        }
    }

    private void initializeFaceIDLogin() {
        // Check if any cameras are available
        int cameraCount = CameraUtil.getCameraCount();
        System.out.println("📷 Available cameras: " + cameraCount);

        if (cameraCount == 0) {
            faceIDLoginButton.setDisable(true);
            faceIDLoginButton.setOpacity(0.5);
            faceIDLoginButton.setTooltip(new Tooltip("No camera detected"));
        } else {
            // Show available cameras in console
            String[] cameraNames = CameraUtil.getCameraNames();
            for (String name : cameraNames) {
                System.out.println("   - " + name);
            }
        }
    }

    /**
     * Set the primary stage reference (call this from your main application)
     */
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Get stage safely - always returns the stored primaryStage
     */
    private Stage getStage() {
        if (primaryStage != null) {
            return primaryStage;
        } else if (loginButton != null && loginButton.getScene() != null && loginButton.getScene().getWindow() != null) {
            primaryStage = (Stage) loginButton.getScene().getWindow();
            return primaryStage;
        }
        return null;
    }

    /**
     * Check if there's an existing session from previous login
     */
    private void checkExistingSession() {
        Person loggedInUser = SessionManager.getCurrentUser();
        if (loggedInUser != null) {
            // User is already logged in, go directly to main page
            System.out.println("Existing session found for user: " + loggedInUser.getUsername());
            // Use Platform.runLater to ensure UI is ready
            javafx.application.Platform.runLater(() -> {
                goToMainPage(loggedInUser);
            });
        } else {
            // Then check for remembered user if no session
            checkRememberedUser();
        }
    }

    /**
     * Check if user had "Remember Me" checked
     */
    private void checkRememberedUser() {
        String savedEmail = preferences.get("remembered_email", null);
        boolean rememberMeChecked = preferences.getBoolean("remember_me", false);

        if (savedEmail != null && rememberMeChecked) {
            loginEmailField.setText(savedEmail);
            if (rememberMeCheckBox != null) {
                rememberMeCheckBox.setSelected(true);
            }
        }
    }

    private void createValidationLines() {
        firstNameLine = createValidationLine(firstNameField);
        lastNameLine = createValidationLine(lastNameField);
        usernameLine = createValidationLine(usernameField);
        emailLine = createValidationLine(emailField);
        dateLine = createValidationLine(dateField);
        passwordLine = createValidationLine(passwordField);
        confirmPasswordLine = createValidationLine(confirmPasswordField);
    }

    private Line createValidationLine(TextField field) {
        Line line = new Line();
        line.setStartX(0);
        line.setStartY(0);
        line.setEndX(field.getWidth() - 30);
        line.setEndY(0);
        line.setStrokeWidth(2.5);
        line.setStroke(Color.TRANSPARENT);
        line.setTranslateY(2);
        line.setOpacity(0);

        // Add line to the parent VBox (after the field)
        if (field.getParent() instanceof VBox) {
            VBox parent = (VBox) field.getParent();
            parent.getChildren().add(line);
        }

        // Update line width when field width changes
        field.widthProperty().addListener((obs, old, val) -> {
            line.setEndX(val.doubleValue() - 30);
        });

        return line;
    }

    private void createErrorLabels() {
        firstNameError = createErrorLabel("First name must contain only letters, min 3 chars");
        lastNameError = createErrorLabel("Last name must contain only letters, min 3 chars");
        usernameError = createErrorLabel("Username: 3-15 chars, letters, numbers, underscore only");
        emailError = createErrorLabel("Enter a valid email (e.g., name@domain.com)");
        dateError = createErrorLabel("Use format: DD/MM/YYYY (Day:1-31, Month:1-12, Year:1946-2026)");
        passwordError = createErrorLabel("Password must be at least 8 characters");
        confirmPasswordError = createErrorLabel("Passwords do not match");

        addErrorToParent(firstNameField, firstNameError);
        addErrorToParent(lastNameField, lastNameError);
        addErrorToParent(usernameField, usernameError);
        addErrorToParent(emailField, emailError);
        addErrorToParent(dateField, dateError);
        addErrorToParent(passwordField, passwordError);
        addErrorToParent(confirmPasswordField, confirmPasswordError);
    }

    private Label createErrorLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.INDIANRED);
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        label.setOpacity(0);
        label.setWrapText(true);
        label.setMaxWidth(350);
        return label;
    }

    private void addErrorToParent(TextField field, Label errorLabel) {
        if (field.getParent() instanceof VBox) {
            VBox parent = (VBox) field.getParent();
            parent.getChildren().add(errorLabel);
        }
    }

    private void createSuccessMessage() {
        successMessage = new Label("✓ Account created successfully!");
        successMessage.setTextFill(Color.LIMEGREEN);
        successMessage.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        successMessage.setAlignment(Pos.CENTER);
        successMessage.setOpacity(0);
        successMessage.setWrapText(true);
        successMessage.setMaxWidth(350);

        // Add to signup pane (after the role selection and before the button)
        int roleIndex = signupPane.getChildren().indexOf(signupPane.getChildren().stream()
                .filter(node -> node instanceof VBox && ((VBox) node).getChildren().stream()
                        .anyMatch(child -> child instanceof RadioButton))
                .findFirst().orElse(null));

        if (roleIndex >= 0) {
            signupPane.getChildren().add(roleIndex + 1, successMessage);
        }
    }

    private void createLoginError() {
        loginError = new Label("Invalid email/username or password");
        loginError.setTextFill(Color.INDIANRED);
        loginError.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        loginError.setAlignment(Pos.CENTER);
        loginError.setOpacity(0);
        loginError.setWrapText(true);
        loginError.setMaxWidth(350);

        // Add to login pane (after password field and before login button)
        int index = loginPane.getChildren().indexOf(loginPasswordField.getParent());
        if (index >= 0) {
            loginPane.getChildren().add(index + 1, loginError);
        }
    }

    private void setupValidation() {
        // First Name validation
        firstNameField.textProperty().addListener((obs, old, val) -> {
            boolean valid = val != null && val.matches("[A-Za-z]+") && val.length() >= 3 && !val.trim().isEmpty();
            firstNameValid.set(valid);
            updateFieldValidation(firstNameLine, firstNameError, valid, val,
                    "First name must contain only letters, min 3 chars");
        });

        // Last Name validation
        lastNameField.textProperty().addListener((obs, old, val) -> {
            boolean valid = val != null && val.matches("[A-Za-z]+") && val.length() >= 3 && !val.trim().isEmpty();
            lastNameValid.set(valid);
            updateFieldValidation(lastNameLine, lastNameError, valid, val,
                    "Last name must contain only letters, min 3 chars");
        });

        // Username validation
        usernameField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 15) {
                usernameField.setText(old);
                return;
            }
            boolean valid = val != null && val.matches("[A-Za-z0-9_]+") && val.length() >= 3;
            usernameValid.set(valid);
            updateFieldValidation(usernameLine, usernameError, valid, val,
                    "Username: 3-15 chars, letters, numbers, underscore only");
        });

        // Email validation
        emailField.textProperty().addListener((obs, old, val) -> {
            boolean valid = val != null &&
                    val.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
            emailValid.set(valid);
            updateFieldValidation(emailLine, emailError, valid, val,
                    "Enter a valid email (e.g., name@domain.com)");
        });

        // Date validation
        dateField.textProperty().addListener((obs, old, val) -> {
            boolean valid = validateDate(val);
            dateValid.set(valid);
            String errorMessage = getDateErrorMessage(val);
            updateFieldValidation(dateLine, dateError, valid, val, errorMessage);
        });

        // Password validation
        passwordField.textProperty().addListener((obs, old, val) -> {
            boolean valid = val != null && val.length() >= 8;
            passwordValid.set(valid);
            updatePasswordValidation(passwordLine, passwordError, valid, val);
            validateConfirmPassword();
        });

        // Confirm Password validation
        confirmPasswordField.textProperty().addListener((obs, old, val) -> {
            validateConfirmPassword();
        });
    }

    private boolean validateDate(String date) {
        if (date == null || date.isEmpty()) return false;
        if (!date.matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19[4-9][0-9]|20[01][0-9]|202[0-6])$")) {
            return false;
        }
        try {
            String[] parts = date.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            if (year < 1946 || year > 2026) return false;
            if (month < 1 || month > 12) return false;
            return isValidDayForMonth(day, month, year);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getDateErrorMessage(String date) {
        if (date == null || date.isEmpty()) return "Date is required";
        if (!date.matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19[4-9][0-9]|20[01][0-9]|202[0-6])$")) {
            return "Format must be DD/MM/YYYY (e.g., 31/12/2024)";
        }
        try {
            String[] parts = date.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            if (year < 1946 || year > 2026) return "Year must be between 1946 and 2026";
            if (month < 1 || month > 12) return "Month must be between 01 and 12";
            if (!isValidDayForMonth(day, month, year)) {
                if (month == 2) {
                    return isLeapYear(year) ? "February in leap year has 1-29 days" : "February has 1-28 days";
                } else if (month == 4 || month == 6 || month == 9 || month == 11) {
                    return "This month has only 30 days";
                } else {
                    return "This month has 31 days";
                }
            }
        } catch (NumberFormatException e) {
            return "Invalid date format";
        }
        return "Invalid date";
    }

    private boolean isValidDayForMonth(int day, int month, int year) {
        int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (month == 2 && isLeapYear(year)) return day >= 1 && day <= 29;
        return day >= 1 && day <= daysInMonth[month];
    }

    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    private void validateConfirmPassword() {
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        boolean valid = password != null && confirm != null &&
                password.length() >= 8 && password.equals(confirm);
        confirmPasswordValid.set(valid);
        if (confirm == null || confirm.isEmpty()) {
            fadeLine(confirmPasswordLine, 0);
            fadeError(confirmPasswordError, 0);
        } else if (valid) {
            showLine(confirmPasswordLine, Color.LIMEGREEN);
            fadeError(confirmPasswordError, 0);
        } else {
            showLine(confirmPasswordLine, Color.INDIANRED);
            showError(confirmPasswordError, "Passwords do not match");
        }
    }

    private void updateFieldValidation(Line line, Label errorLabel, boolean valid,
                                       String value, String errorMessage) {
        if (value == null || value.isEmpty()) {
            fadeLine(line, 0);
            fadeError(errorLabel, 0);
        } else if (valid) {
            showLine(line, Color.LIMEGREEN);
            fadeError(errorLabel, 0);
        } else {
            showLine(line, Color.INDIANRED);
            showError(errorLabel, errorMessage);
        }
    }

    private void updatePasswordValidation(Line line, Label errorLabel, boolean valid, String password) {
        if (password == null || password.isEmpty()) {
            fadeLine(line, 0);
            fadeError(errorLabel, 0);
        } else {
            double strength = calculatePasswordStrength(password);
            String strengthText = getPasswordStrengthText(password);
            Color lineColor;
            if (valid) {
                lineColor = Color.LIMEGREEN;
                fadeError(errorLabel, 0);
            } else if (strength > 0.5) {
                lineColor = Color.ORANGE;
                showError(errorLabel, "Password needs " + (8 - password.length()) + " more characters");
            } else {
                lineColor = Color.INDIANRED;
                showError(errorLabel, "Password is too weak. " + strengthText);
            }
            showLine(line, lineColor);
        }
    }

    private void showLine(Line line, Color color) {
        line.setStroke(color);
        FadeTransition ft = new FadeTransition(Duration.millis(200), line);
        ft.setToValue(1);
        ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), line);
        st.setFromX(0.5);
        st.setToX(1);
        st.play();
    }

    private void fadeLine(Line line, double opacity) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), line);
        ft.setToValue(opacity);
        ft.play();
    }

    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setToValue(1);
        ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), errorLabel);
        tt.setFromY(-5);
        tt.setToY(0);
        tt.play();
    }

    private void fadeError(Label errorLabel, double opacity) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setToValue(opacity);
        ft.play();
    }

    private double calculatePasswordStrength(String password) {
        double strength = 0.0;
        if (password.length() >= 8) strength += 0.3;
        if (password.length() >= 12) strength += 0.1;
        if (password.matches(".*[A-Z].*")) strength += 0.2;
        if (password.matches(".*[a-z].*")) strength += 0.1;
        if (password.matches(".*\\d.*")) strength += 0.2;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) strength += 0.2;
        return Math.min(strength, 1.0);
    }

    private String getPasswordStrengthText(String password) {
        double strength = calculatePasswordStrength(password);
        if (strength < 0.4) return "Very weak";
        if (strength < 0.7) return "Medium";
        if (strength < 0.9) return "Strong";
        return "Very strong";
    }

    /**
     * Handle Sign Up button click - Save user to database
     */
    private void handleSignUp() {
        // Hide any previous success/error messages
        fadeError(firstNameError, 0);
        fadeError(lastNameError, 0);
        fadeError(usernameError, 0);
        fadeError(emailError, 0);
        fadeError(dateError, 0);
        fadeError(passwordError, 0);
        fadeError(confirmPasswordError, 0);
        fadeSuccessMessage(0);

        try {
            // Parse date from DD/MM/YYYY to SQL Date
            String[] dateParts = dateField.getText().split("/");
            String sqlDateStr = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];
            Date sqlDate = Date.valueOf(sqlDateStr);

            // Determine role based on radio button selection
            String role;
            if (guiderRoleRadio.isSelected()) {
                role = "GUIDER";
            } else if (agenceRoleRadio.isSelected()) {
                role = "AGENCY";
            } else {
                role = "USER";
            }

            // Create Person object
            Person newPerson = new Person(
                    0,
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    emailField.getText().trim().toLowerCase(),
                    passwordField.getText(),
                    sqlDate,
                    role,
                    usernameField.getText().trim()
            );

            // Save to database
            personService.insertOneUpdated(newPerson);

            // Show success message with appropriate role
            String roleText = role.equals("GUIDER") ? "Travel Guider" :
                    role.equals("AGENCY") ? "Agency" : "User";
            showSuccessMessage("✓ Account created successfully! Welcome " + firstNameField.getText() +
                    "! You are now registered as a " + roleText);

            // Clear form after successful registration
            clearForm();

        } catch (SQLException e) {
            // Handle duplicate entry errors from database constraints
            if (e.getMessage().contains("Duplicate entry")) {
                if (e.getMessage().contains("email")) {
                    showError(emailError, "This email is already registered");
                    showLine(emailLine, Color.INDIANRED);
                } else if (e.getMessage().contains("username")) {
                    showError(usernameError, "This username is already taken");
                    showLine(usernameLine, Color.INDIANRED);
                } else {
                    showTemporaryError("Database error: " + e.getMessage());
                }
            } else {
                showTemporaryError("Error creating account: " + e.getMessage());
            }
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            showError(dateError, "Invalid date format");
            showLine(dateLine, Color.INDIANRED);
        }
    }

    /**
     * Handle Login button click - Verify credentials and switch to dashboard
     */
    @FXML
    private void handleLogin() {
        String emailOrUsername = loginEmailField.getText();
        String password = loginPasswordField.getText();

        if (emailOrUsername.isEmpty() || password.isEmpty()) {
            showLoginError("Please fill all fields");
            return;
        }

        try {
            Person user = personService.login(emailOrUsername, password);

            if (user != null) {
                System.out.println("Login successful for user: " + user.getUsername() + " with role: " + user.getRole());

                // Check if 2FA is enabled
                boolean twoFAEnabled = personService.isTwoFactorEnabled(user.getId());

                if (twoFAEnabled) {
                    // Generate and save 2FA code
                    String code = EmailService.generate2FACode();
                    personService.save2FACode(user.getId(), code);

                    // Send code via email
                    boolean sent = EmailService.send2FACode(user.getEmail(), code);

                    if (sent) {
                        // Navigate to 2FA verification page
                        goTo2FAVerification(user);
                    } else {
                        showLoginError("Failed to send verification code. Please try again.");
                    }
                } else {
                    // No 2FA, proceed with normal login
                    completeLogin(user);
                }

            } else {
                showLoginError("Invalid email/username or password");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Database error");
        }
    }

    private void completeLogin(Person user) throws SQLException {
        // Update user status to online in database
        personService.updateUserStatus(user.getId(), "online");

        // Check and set default profile image if needed
        checkAndSetDefaultProfileImage(user);

        // Handle Remember Me
        if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
            preferences.put("remembered_email", loginEmailField.getText());
            preferences.putBoolean("remember_me", true);
        } else {
            preferences.remove("remembered_email");
            preferences.putBoolean("remember_me", false);
        }

        // Create session
        SessionManager.createSession(user);

        // Navigate to main page with loading animation
        navigateToDashboardWithLoading(user);
    }

    private void goTo2FAVerification(Person user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/twofa_verification.fxml"));
            Parent verificationRoot = loader.load();

            TwoFAVerificationController controller = loader.getController();
            controller.setUserData(user);

            // Pass the current stage
            Stage stage = getStage();
            if (stage == null) {
                showLoginError("Cannot get stage reference");
                return;
            }

            controller.setStage(stage);

            stage.setScene(new Scene(verificationRoot));
            stage.setTitle("2FA Verification");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showLoginError("Failed to load verification page: " + e.getMessage());
        }
    }

    private void checkAndSetDefaultProfileImage(Person user) {
        try {
            ProfileService profileService = new ProfileService();
            Profile profile = profileService.getProfileByUserId(user.getId());

            // If profile doesn't exist, create one with default image
            if (profile == null) {
                System.out.println("No profile found for user: " + user.getUsername() + ". Creating default profile.");

                profile = new Profile();
                profile.setIdUser(user.getId());
                profile.setMemberPremium("Standard");
                profile.setLanguage("English");
                profile.setCoins(0);

                // Load default image from absolute path
                String imagePath = "C:\\Users\\pyrox\\Downloads\\default_image.png";
                File imageFile = new File(imagePath);

                if (imageFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(imageFile)) {
                        byte[] defaultImage = fis.readAllBytes();
                        profile.setImage(defaultImage);
                        System.out.println("Default image loaded and set for user: " + user.getUsername() + " from: " + imagePath);
                    } catch (IOException e) {
                        System.err.println("Failed to load default image: " + e.getMessage());
                    }
                } else {
                    System.err.println("Default image not found at: " + imagePath);
                }

                profileService.insertOne(profile);
                System.out.println("Default profile created for user: " + user.getUsername());
            }
            // If profile exists but has no image, update with default image
            else if (profile.getImage() == null || profile.getImage().length == 0) {
                System.out.println("Profile exists but no image found for user: " + user.getUsername() + ". Setting default image.");

                String imagePath = "C:\\Users\\pyrox\\Downloads\\default_image.png";
                File imageFile = new File(imagePath);

                if (imageFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(imageFile)) {
                        byte[] defaultImage = fis.readAllBytes();
                        profile.setImage(defaultImage);
                        profileService.updateOne(profile);
                        System.out.println("Default image set for user: " + user.getUsername() + " from: " + imagePath);
                    } catch (IOException e) {
                        System.err.println("Failed to load default image: " + e.getMessage());
                    }
                } else {
                    System.err.println("Default image not found at: " + imagePath);
                }
            } else {
                System.out.println("User already has a profile image: " + user.getUsername());
            }

        } catch (SQLException e) {
            System.err.println("Error checking profile for user: " + user.getUsername());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to dashboard with loading animation
     */
    private void navigateToDashboardWithLoading(Person user) {
        try {
            Stage stage = getStage();
            if (stage == null) {
                System.err.println("Cannot get stage reference in navigateToDashboardWithLoading");
                return;
            }

            // ===== LOAD LOADING SCREEN =====
            FXMLLoader loadingLoader = new FXMLLoader(getClass().getResource("/loading.fxml"));
            Parent loadingRoot = loadingLoader.load();

            stage.setScene(new Scene(loadingRoot));
            stage.show();

            // ===== WAIT THEN OPEN MAIN PAGE =====
            PauseTransition pause = new PauseTransition(Duration.seconds(3.8));

            pause.setOnFinished(event -> {
                goToMainPage(user); // Make sure this passes the user
            });

            pause.play();

        } catch (Exception e) {
            e.printStackTrace();
            // If loading screen fails, go directly to main page
            goToMainPage(user);
        }
    }

    private void goToMainPage(Person user) {
        try {
            System.out.println("goToMainPage called with user: " + (user != null ? user.getUsername() : "null"));

            // Debug: Check if the file exists
            java.net.URL resource = getClass().getResource("/mainpage.fxml");
            System.out.println("Loading mainpage.fxml from: " + resource);

            if (resource == null) {
                System.err.println("Could not find mainpage.fxml!");
                showLoginError("Could not find mainpage.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Object controller = loader.getController();
            System.out.println("Loaded controller class: " + controller.getClass().getName());

            if (!(controller instanceof MainPageController)) {
                System.err.println("ERROR: Controller is not MainPageController, it's: " + controller.getClass().getName());
                showLoginError("Wrong controller type: " + controller.getClass().getSimpleName());
                return;
            }

            MainPageController mainController = (MainPageController) controller;
            mainController.setUserData(user);

            Stage stage = getStage();
            if (stage == null) {
                System.err.println("Cannot get stage reference in goToMainPage");
                if (loginButton != null && loginButton.getScene() != null) {
                    stage = (Stage) loginButton.getScene().getWindow();
                    primaryStage = stage;
                } else {
                    return;
                }
            }

            stage.setScene(new Scene(root));
            stage.setTitle("Main Page");
            stage.show();

            System.out.println("Main page loaded and user data set");

        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Error loading main page: " + e.getMessage());
        }
    }

    private void showSuccessMessage(String message) {
        successMessage.setText(message);

        FadeTransition ft = new FadeTransition(Duration.millis(400), successMessage);
        ft.setToValue(1);
        ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(400), successMessage);
        st.setFromX(0.5);
        st.setFromY(0.5);
        st.setToX(1);
        st.setToY(1);
        st.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> fadeSuccessMessage(0));
        pause.play();
    }

    private void fadeSuccessMessage(double opacity) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), successMessage);
        ft.setToValue(opacity);
        ft.play();
    }

    private void showLoginError(String message) {
        loginError.setText(message);
        FadeTransition ft = new FadeTransition(Duration.millis(200), loginError);
        ft.setToValue(1);
        ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(100), loginButton);
        tt.setFromX(0);
        tt.setToX(10);
        tt.setCycleCount(2);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void showTemporaryError(String message) {
        Label tempError = new Label(message);
        tempError.setTextFill(Color.INDIANRED);
        tempError.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        tempError.setAlignment(Pos.CENTER);

        signupPane.getChildren().add(tempError);

        FadeTransition ft = new FadeTransition(Duration.millis(300), tempError);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            FadeTransition ft2 = new FadeTransition(Duration.millis(300), tempError);
            ft2.setToValue(0);
            ft2.setOnFinished(ev -> signupPane.getChildren().remove(tempError));
            ft2.play();
        });
        pause.play();
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        usernameField.clear();
        emailField.clear();
        dateField.clear();
        passwordField.clear();
        confirmPasswordField.clear();

        // Reset role selection to default (USER)
        userRoleRadio.setSelected(true);

        fadeLine(firstNameLine, 0);
        fadeLine(lastNameLine, 0);
        fadeLine(usernameLine, 0);
        fadeLine(emailLine, 0);
        fadeLine(dateLine, 0);
        fadeLine(passwordLine, 0);
        fadeLine(confirmPasswordLine, 0);
    }

    @FXML
    private void switchToLogin() {
        System.out.println("=== SWITCHING TO LOGIN PANE ===");
        loginPane.setVisible(true);

        ParallelTransition animation = new ParallelTransition(
                fadeSlide(signupPane, 1, 0, 0, -40, FAST),
                fadeSlide(loginPane, 0, 1, 40, 0, NORMAL)
        );

        animation.setOnFinished(e -> {
            signupPane.setVisible(false);
            signupPane.setTranslateX(0);
        });

        animation.play();
    }

    @FXML
    private void switchToSignup() {
        signupPane.setVisible(true);

        ParallelTransition animation = new ParallelTransition(
                fadeSlide(loginPane, 1, 0, 0, 40, FAST),
                fadeSlide(signupPane, 0, 1, -40, 0, NORMAL)
        );

        animation.setOnFinished(e -> {
            loginPane.setVisible(false);
            loginPane.setTranslateX(0);
        });

        animation.play();
    }

    private ParallelTransition fadeSlide(Node node,
                                         double fromOpacity, double toOpacity,
                                         double fromX, double toX,
                                         Duration duration) {

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(fromOpacity);
        fade.setToValue(toOpacity);
        fade.setInterpolator(SMOOTH);

        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromX(fromX);
        slide.setToX(toX);
        slide.setInterpolator(SMOOTH);

        return new ParallelTransition(fade, slide);
    }

    private void createFloatingEffect(Node node) {
        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(3), node);
        floatAnim.setFromY(0);
        floatAnim.setToY(-10);
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.setInterpolator(Interpolator.EASE_BOTH);
        floatAnim.play();
    }

    @FXML
    private void onSignupButtonHover(MouseEvent e) { scale(e, 1.05); }

    @FXML
    private void onSignupButtonExit(MouseEvent e) { scale(e, 1); }

    @FXML
    private void onLoginButtonHover(MouseEvent e) { scale(e, 1.05); }

    @FXML
    private void onLoginButtonExit(MouseEvent e) { scale(e, 1); }

    @FXML
    private void onLinkHover(MouseEvent e) { scale(e, 1.07); }

    @FXML
    private void onLinkExit(MouseEvent e) { scale(e, 1); }

    private void scale(MouseEvent event, double value) {
        if (event.getSource() instanceof Node node) {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), node);
            st.setToX(value);
            st.setToY(value);
            st.setInterpolator(SMOOTH);
            st.play();
        }
    }

    /**
     * Handle Face ID Login with popup email dialog
     */
    @FXML
    public void handleFaceIDLogin() {
        System.out.println("==========================================");
        System.out.println("🔵 FACE ID LOGIN BUTTON CLICKED!");
        System.out.println("==========================================");

        // Create a custom dialog for email input
        Dialog<String> emailDialog = new Dialog<>();
        emailDialog.setTitle("Face ID Login");
        emailDialog.setHeaderText("Enter your email for Face ID verification");

        // Set the button types
        ButtonType loginButtonType = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        emailDialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the email input field
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        // Create error label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(new Label("Please enter your email address:"), emailField, errorLabel);

        emailDialog.getDialogPane().setContent(content);

        // Request focus on the email field by default
        javafx.application.Platform.runLater(emailField::requestFocus);

        // Convert the result to a string when the login button is clicked
        emailDialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return emailField.getText();
            }
            return null;
        });

        // Show the dialog and wait for result
        Optional<String> result = emailDialog.showAndWait();

        result.ifPresent(email -> {
            // Validate email
            if (email == null || email.trim().isEmpty()) {
                showAlert("Email Required", "Please enter your email address.", Alert.AlertType.WARNING);
                return;
            }

            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                showAlert("Invalid Email", "Please enter a valid email address.", Alert.AlertType.WARNING);
                return;
            }

            // Check if email exists in database and has face data
            try {
                Person user = personService.getUserByEmail(email);

                if (user == null) {
                    showAlert("Email Not Found", "No account found with this email address.", Alert.AlertType.ERROR);
                    return;
                }

                byte[] faceData = user.getFaceData();
                if (faceData == null || faceData.length == 0) {
                    showAlert("Face ID Not Set Up", "This account doesn't have Face ID set up. Please use password login or set up Face ID in your profile.", Alert.AlertType.WARNING);
                    return;
                }

                // Proceed with Face ID login
                currentFaceIDUser = user;

                // Check if multiple cameras are available
                if (CameraUtil.hasMultipleCameras()) {
                    System.out.println("📷 Multiple cameras detected: " + CameraUtil.getCameraCount());

                    // Show camera selection dialog
                    CameraSelectionDialog selectionDialog = new CameraSelectionDialog();
                    int selectedCamera = selectionDialog.showAndWait();

                    if (selectedCamera == -1) {
                        System.out.println("⚠️ Camera selection cancelled");
                        return;
                    }

                    System.out.println("📷 Selected camera index: " + selectedCamera);
                    processFaceIDWithLBPH(selectedCamera, currentFaceIDUser);
                } else {
                    System.out.println("📷 Single camera detected, using default");
                    processFaceIDWithLBPH(0, currentFaceIDUser);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Database Error", "Error checking email: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Process Face ID login using LBPH recognizer with Person object
     */
    private void processFaceIDWithLBPH(int cameraIndex, Person user) {
        // Disable button to prevent multiple clicks
        faceIDLoginButton.setDisable(true);
        faceIDLoginButton.setText("Processing...");

        // Check camera availability with selected index
        System.out.println("🔍 Initializing camera " + cameraIndex + "...");
        CameraUtil cameraCheck = new CameraUtil(cameraIndex);
        boolean cameraAvailable = cameraCheck.isCameraAvailable();
        System.out.println("📷 Camera available: " + cameraAvailable + " - " + cameraCheck.getCameraName());

        if (!cameraAvailable) {
            System.out.println("❌ ERROR: Selected camera not available");
            showAlert("Camera Error", "Selected camera is not available. Please try another camera.", Alert.AlertType.ERROR);
            faceIDLoginButton.setDisable(false);
            faceIDLoginButton.setText("Login with Face ID");
            return;
        }

        try {
            System.out.println("📸 Creating EnhancedFaceCaptureDialog with camera: " + cameraCheck.getCameraName());
            // Use EnhancedFaceCaptureDialog for better visual feedback
            EnhancedFaceCaptureDialog dialog = new EnhancedFaceCaptureDialog(cameraIndex);

            System.out.println("⏳ Showing face capture dialog (waiting for user)...");
            byte[] capturedFace = dialog.showAndWait("login");

            System.out.println("📥 Captured face: " + (capturedFace != null ? "Yes (" + capturedFace.length + " bytes)" : "No"));

            if (capturedFace != null) {
                System.out.println("🔎 Verifying face for user: " + user.getUsername());
                System.out.println("⚡ Using LBPH Face Recognizer with threshold: > 51%");

                // Show loading indicator
                ProgressIndicator pi = new ProgressIndicator();
                pi.setMaxSize(50, 50);
                loginPane.getChildren().add(pi);

                try {
                    // Verify face data matches
                    byte[] storedFace = user.getFaceData();
                    if (storedFace == null || storedFace.length == 0) {
                        loginPane.getChildren().remove(pi);
                        System.out.println("❌ No face data found for user: " + user.getUsername());
                        showAlert("Error", "No face data registered for this user. Please set up Face ID first.", Alert.AlertType.ERROR);
                        return;
                    }

                    // Compare faces using LBPH with user ID for training
                    FaceRecognitionUtil faceUtil = new FaceRecognitionUtil();
                    double similarity = faceUtil.compareFacesWithUser(capturedFace, storedFace, user.getId());
                    System.out.println("LBPH Similarity: " + similarity);

                    loginPane.getChildren().remove(pi);

                    if (similarity > 0.41) { // 51% threshold
                        System.out.println("✅ Face ID verified for user: " + user.getUsername());
                        showAlert("Success", "Face ID verified! Logging in...", Alert.AlertType.INFORMATION);

                        // Check if 2FA is enabled
                        boolean twoFAEnabled = personService.isTwoFactorEnabled(user.getId());
                        System.out.println("🔐 2FA enabled: " + twoFAEnabled);

                        if (twoFAEnabled) {
                            System.out.println("➡️ Redirecting to 2FA verification");
                            goToTwoFAVerification(user);
                        } else {
                            System.out.println("➡️ Redirecting to loading page");
                            completeFaceIDLogin(user);
                        }
                    } else {
                        System.out.println("❌ Face does not match for user: " + user.getUsername() + " (similarity: " + similarity + ")");
                        showAlert("Error", "Face does not match the registered face.\n(LBPH Similarity: " + String.format("%.1f%%", similarity * 100) + " needed: >51%)", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    System.err.println("🔥 SQL Error in Face ID login: " + e.getMessage());
                    e.printStackTrace();
                    loginPane.getChildren().remove(pi);
                    showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            } else {
                System.out.println("⚠️ Face capture cancelled or failed (no image captured)");
            }
        } catch (Exception e) {
            System.err.println("💥 Unexpected error in Face ID login: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unexpected error: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            // Re-enable button
            System.out.println("🔄 Re-enabling Face ID button");
            faceIDLoginButton.setDisable(false);
            faceIDLoginButton.setText("Login with Face ID");
        }
        System.out.println("==========================================");
    }

    /**
     * Complete Face ID login
     */
    private void completeFaceIDLogin(Person user) throws SQLException {
        // Update user status to online in database
        personService.updateUserStatus(user.getId(), "online");

        // Check and set default profile image if needed
        checkAndSetDefaultProfileImage(user);

        // Create session
        SessionManager.createSession(user);

        // Navigate to main page with loading animation
        navigateToDashboardWithLoading(user);
    }

    private void goToTwoFAVerification(Person user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/twofa_verification.fxml"));
            Parent root = loader.load();

            TwoFAVerificationController controller = loader.getController();
            controller.setUserData(user);
            controller.setStage((Stage) loginPane.getScene().getWindow());

            Stage stage = (Stage) loginPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("2FA Verification");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load 2FA page: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void handleQRScan() {
        System.out.println("==========================================");
        System.out.println("🟠 QR SCAN BUTTON CLICKED!");
        System.out.println("==========================================");

        // Check if multiple cameras are available
        int cameraIndex = 0;
        if (CameraUtil.hasMultipleCameras()) {
            System.out.println("📷 Multiple cameras detected: " + CameraUtil.getCameraCount());

            // Show camera selection dialog
            CameraSelectionDialog selectionDialog = new CameraSelectionDialog();
            int selectedCamera = selectionDialog.showAndWait();

            if (selectedCamera == -1) {
                System.out.println("⚠️ Camera selection cancelled");
                return;
            }

            cameraIndex = selectedCamera;
            System.out.println("📷 Selected camera index: " + cameraIndex);
        }

        // Show QR scanner dialog
        QRScannerDialog scannerDialog = new QRScannerDialog(cameraIndex);
        String[] credentials = scannerDialog.showAndWait();

        if (credentials != null && credentials.length == 3) {
            String username = credentials[0];
            String email = credentials[1];
            String password = credentials[2];

            System.out.println("📋 QR Code contains - Username: " + username + ", Email: " + email);

            // Show loading indicator
            ProgressIndicator pi = new ProgressIndicator();
            pi.setMaxSize(50, 50);
            loginPane.getChildren().add(pi);

            try {
                // Try to login with the credentials
                Person user = personService.login(email, password);

                if (user == null) {
                    // Try with username as emailOrUsername parameter
                    user = personService.login(username, password);
                }

                loginPane.getChildren().remove(pi);

                if (user != null) {
                    // Verify that the username matches (extra security)
                    if (user.getUsername().equals(username) || user.getEmail().equals(email)) {
                        System.out.println("✅ QR Code login successful for user: " + user.getUsername());
                        showAlert("Success", "QR Code verified! Logging in...", Alert.AlertType.INFORMATION);

                        // Complete login (similar to normal login)
                        completeQRLogin(user);
                    } else {
                        System.out.println("❌ Username mismatch: QR says " + username + " but DB has " + user.getUsername());
                        showAlert("Error", "QR Code data does not match user account.", Alert.AlertType.ERROR);
                    }
                } else {
                    System.out.println("❌ Invalid credentials from QR code");
                    showAlert("Error", "Invalid username/email or password in QR code.", Alert.AlertType.ERROR);
                }

            } catch (SQLException e) {
                loginPane.getChildren().remove(pi);
                e.printStackTrace();
                showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            System.out.println("❌ No valid QR code detected or cancelled");
            showAlert("Info", "No QR code detected or scan cancelled.", Alert.AlertType.INFORMATION);
        }
    }

    /**
     * Complete QR code login
     */
    private void completeQRLogin(Person user) throws SQLException {
        // Update user status to online in database
        personService.updateUserStatus(user.getId(), "online");

        // Check and set default profile image if needed
        checkAndSetDefaultProfileImage(user);

        // Create session
        SessionManager.createSession(user);

        // Navigate to main page with loading animation
        navigateToDashboardWithLoading(user);
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgot_password.fxml"));
            Parent forgotRoot = loader.load();

            Stage stage = getStage();
            if (stage == null) {
                showAlert("Error", "Cannot get stage reference", Alert.AlertType.ERROR);
                return;
            }

            stage.setScene(new Scene(forgotRoot));
            stage.setTitle("Forgot Password");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load forgot password page: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}