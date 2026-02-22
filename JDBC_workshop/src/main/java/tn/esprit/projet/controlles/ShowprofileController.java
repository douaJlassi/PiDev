package tn.esprit.projet.controlles;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.projet.utils.*;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.entities.Profile;
import tn.esprit.projet.services.PersonService;
import tn.esprit.projet.services.ProfileService;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Optional;

public class ShowprofileController {

    @FXML
    private Circle profileAvatar;
    @FXML
    private ImageView profileImageView;
    @FXML
    private Button changePhotoBtn;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;
    @FXML
    private Label userEmailLabel;
    @FXML
    private Label coinsLabel;
    @FXML
    private Label membershipLabel;
    @FXML
    private Label languageLabel;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<String> languageCombo;
    @FXML
    private Label languageDisplayLabel;
    @FXML
    private ComboBox<String> membershipCombo;
    @FXML
    private Label membershipDisplayLabel;
    @FXML
    private Button editProfileBtn;
    @FXML
    private Button saveProfileBtn;
    @FXML
    private Button cancelEditBtn;
    @FXML
    private Button backButton;
    @FXML
    private Label usernameLockLabel;
    @FXML
    private Label membershipLockLabel;

    // New FXML fields for navigation and settings
    @FXML
    private VBox profileInfoSection;
    @FXML
    private VBox settingsSection;
    @FXML
    private VBox securitySection;
    @FXML
    private Button profileInfoTab;
    @FXML
    private Button settingsTab;
    @FXML
    private Button securityTab;
    @FXML
    private ComboBox<String> settingsLanguageCombo;
    @FXML
    private ComboBox<String> regionCombo;
    @FXML
    private Label regionDisplayLabel;
    @FXML
    private ToggleButton twoFAButton;
    @FXML
    private VBox twoFASetup;
    @FXML
    private ToggleButton faceIDButton;
    @FXML
    private VBox faceIDSetup;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    private String selectedRegion = "";
    private boolean twoFAEnabled = false;
    private boolean faceIDEnabled = false;

    private Person currentUser;
    private Profile currentProfile;
    private PersonService personService;
    private ProfileService profileService;
    private boolean isEditMode = false;
    private boolean isAdmin = false;
    private byte[] newProfileImage = null;

    @FXML
    public void initialize() {
        personService = new PersonService();
        profileService = new ProfileService();

        // Initialize combo boxes
        languageCombo.getItems().addAll("English", "French");
        membershipCombo.getItems().addAll("Standard", "Premium", "VIP");
        settingsLanguageCombo.getItems().addAll("English", "French", "Arabic", "Spanish", "German");
        regionCombo.getItems().addAll("North America", "South America", "Europe", "Asia", "Africa", "Australia", "Middle East", "Tunisia");

        System.out.println("=== SHOWPROFILE CONTROLLER INITIALIZED ===");

        // Initially show profile info section
        showProfileInfo();
    }

    public void setUserData(Person user) {
        this.currentUser = user;
        this.isAdmin = user.getRole() != null && user.getRole().toLowerCase().contains("admin");
        loadUserProfile();
    }

    private void loadUserProfile() {
        try {
            currentProfile = profileService.getProfileByUserId(currentUser.getId());

            userNameLabel.setText(currentUser.getName() + " " + currentUser.getLastName());
            userRoleLabel.setText(currentUser.getRole());
            userEmailLabel.setText(currentUser.getEmail());

            usernameField.setText(currentUser.getUsername());
            firstNameField.setText(currentUser.getName());
            lastNameField.setText(currentUser.getLastName());
            emailField.setText(currentUser.getEmail());

            if (currentProfile != null) {
                System.out.println("Loading existing profile - ID: " + currentProfile.getId());

                if (currentProfile.getImage() != null && currentProfile.getImage().length > 0) {
                    try {
                        Image image = new Image(new ByteArrayInputStream(currentProfile.getImage()));
                        profileImageView.setImage(image);
                        profileImageView.setPreserveRatio(false);
                        profileImageView.setFitWidth(130);
                        profileImageView.setFitHeight(130);
                        System.out.println("Profile image loaded, size: " + currentProfile.getImage().length + " bytes");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("No profile image found");
                }

                membershipLabel.setText(currentProfile.getMemberPremium());
                membershipDisplayLabel.setText(currentProfile.getMemberPremium());
                membershipCombo.setValue(currentProfile.getMemberPremium());

                languageLabel.setText(currentProfile.getLanguage());
                languageDisplayLabel.setText(currentProfile.getLanguage());
                languageCombo.setValue(currentProfile.getLanguage());
                settingsLanguageCombo.setValue(currentProfile.getLanguage());

                coinsLabel.setText(String.valueOf(currentProfile.getCoins()));
                System.out.println("Profile loaded: membership=" + currentProfile.getMemberPremium() +
                        ", language=" + currentProfile.getLanguage() +
                        ", coins=" + currentProfile.getCoins());
            } else {
                System.out.println("No profile found for user " + currentUser.getId() + ", will create new one on save");
                currentProfile = new Profile();
                currentProfile.setIdUser(currentUser.getId());
                membershipLabel.setText("Standard");
                languageLabel.setText("English");
                coinsLabel.setText("0");
            }

            // Load 2FA and Face ID status from database
            loadTwoFAStatus();
            loadFaceIDStatus();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load profile: " + e.getMessage());
        }
    }

    private void loadTwoFAStatus() {
        try {
            // Get fresh user data from database
            Person freshUser = personService.getUserById(currentUser.getId());
            if (freshUser != null) {
                twoFAEnabled = freshUser.isTwoFactorEnabled();

                if (twoFAEnabled) {
                    twoFAButton.setText("ON");
                    twoFAButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
                } else {
                    twoFAButton.setText("OFF");
                    twoFAButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadFaceIDStatus() {
        try {
            byte[] faceData = personService.getFaceData(currentUser.getId());
            faceIDEnabled = (faceData != null && faceData.length > 0);

            if (faceIDEnabled) {
                faceIDButton.setText("ON");
                faceIDButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
                faceIDSetup.setVisible(true);
                faceIDSetup.setManaged(true);

                // Add camera info to faceIDSetup if you want
                int cameraCount = CameraUtil.getCameraCount();
                Label cameraInfo = new Label("Available cameras: " + cameraCount);
                cameraInfo.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 12px;");
                if (!faceIDSetup.getChildren().contains(cameraInfo)) {
                    faceIDSetup.getChildren().add(0, cameraInfo);
                }
            } else {
                faceIDButton.setText("OFF");
                faceIDButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
                faceIDSetup.setVisible(false);
                faceIDSetup.setManaged(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showProfileInfo() {
        // Update tab styles
        profileInfoTab.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        settingsTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        securityTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");

        // Show/hide sections
        profileInfoSection.setVisible(true);
        profileInfoSection.setManaged(true);
        settingsSection.setVisible(false);
        settingsSection.setManaged(false);
        securitySection.setVisible(false);
        securitySection.setManaged(false);
    }

    @FXML
    public void showSettings() {
        // Update tab styles
        profileInfoTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        settingsTab.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        securityTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");

        // Load saved settings
        loadSettings();

        // Show/hide sections
        profileInfoSection.setVisible(false);
        profileInfoSection.setManaged(false);
        settingsSection.setVisible(true);
        settingsSection.setManaged(true);
        securitySection.setVisible(false);
        securitySection.setManaged(false);
    }

    @FXML
    public void showSecurity() {
        // Update tab styles
        profileInfoTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        settingsTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        securityTab.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");

        // Refresh security statuses
        loadTwoFAStatus();
        loadFaceIDStatus();

        // Show/hide sections
        profileInfoSection.setVisible(false);
        profileInfoSection.setManaged(false);
        settingsSection.setVisible(false);
        settingsSection.setManaged(false);
        securitySection.setVisible(true);
        securitySection.setManaged(true);
    }

    @FXML
    public void handleSaveLanguage() {
        String selectedLanguage = settingsLanguageCombo.getValue();
        if (selectedLanguage != null && !selectedLanguage.isEmpty()) {
            // Update profile language
            if (currentProfile != null) {
                currentProfile.setLanguage(selectedLanguage);
                languageLabel.setText(selectedLanguage);
                languageDisplayLabel.setText(selectedLanguage);
                languageCombo.setValue(selectedLanguage);

                try {
                    profileService.updateOne(currentProfile);
                    showAlert("Success", "Language updated to " + selectedLanguage + "!", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to update language: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        }
    }

    @FXML
    public void handleSaveRegion() {
        selectedRegion = regionCombo.getValue();
        if (selectedRegion != null && !selectedRegion.isEmpty()) {
            regionDisplayLabel.setText(selectedRegion);
            showAlert("Success", "Region updated to " + selectedRegion + "!", Alert.AlertType.INFORMATION);
            // Save to database or preferences
        }
    }

    @FXML
    public void handleTwoFA() {
        twoFAEnabled = !twoFAEnabled;
        if (twoFAEnabled) {
            twoFAButton.setText("ON");
            twoFAButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");

            // Show confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Enable 2FA");
            confirm.setHeaderText("Enable Two-Factor Authentication");
            confirm.setContentText("This will send a verification code to your email. Continue?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                enable2FA();
            } else {
                // Revert if user cancels
                twoFAEnabled = false;
                twoFAButton.setText("OFF");
                twoFAButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
            }
        } else {
            twoFAButton.setText("OFF");
            twoFAButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");

            // Disable 2FA
            disable2FA();
        }
    }

    private void enable2FA() {
        try {
            // Update database
            personService.setTwoFactorEnabled(currentUser.getId(), true);

            // Send activation email
            EmailService.send2FAActivationEmail(currentUser.getEmail(), currentUser.getUsername());

            showAlert("Success", "2FA has been enabled successfully! A confirmation email has been sent.", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to enable 2FA: " + e.getMessage(), Alert.AlertType.ERROR);

            // Revert UI
            twoFAEnabled = false;
            twoFAButton.setText("OFF");
            twoFAButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void disable2FA() {
        try {
            // Update database
            personService.setTwoFactorEnabled(currentUser.getId(), false);

            showAlert("Success", "2FA has been disabled.", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to disable 2FA: " + e.getMessage(), Alert.AlertType.ERROR);

            // Revert UI
            twoFAEnabled = true;
            twoFAButton.setText("ON");
            twoFAButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    @FXML
    public void handleChangePassword() {
        String currentPwd = currentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showAlert("Error", "All password fields must be filled!", Alert.AlertType.ERROR);
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showAlert("Error", "New password and confirm password do not match!", Alert.AlertType.ERROR);
            return;
        }

        if (newPwd.length() < 8) {
            showAlert("Error", "Password must be at least 8 characters long!", Alert.AlertType.ERROR);
            return;
        }

        // Verify current password
        try {
            Person verified = personService.login(currentUser.getEmail(), currentPwd);
            if (verified != null) {
                // Update password
                currentUser.setPassword(newPwd);
                personService.updateOne(currentUser);
                showAlert("Success", "Password changed successfully!", Alert.AlertType.INFORMATION);

                // Clear fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            } else {
                showAlert("Error", "Current password is incorrect!", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to change password: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadSettings() {
        // Load current language
        if (currentProfile != null) {
            settingsLanguageCombo.setValue(currentProfile.getLanguage());
        }

        // Load saved region (from preferences or database)
        if (!selectedRegion.isEmpty()) {
            regionCombo.setValue(selectedRegion);
            regionDisplayLabel.setText(selectedRegion);
        }
    }

    @FXML
    public void handleChangePhoto() {
        System.out.println("Change photo button clicked");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(changePhotoBtn.getScene().getWindow());

        if (selectedFile != null) {
            try {
                BufferedImage originalImage = ImageIO.read(selectedFile);

                if (originalImage == null) {
                    showAlert("Error", "Invalid image file format.");
                    return;
                }

                int size = 300;
                BufferedImage squareImage = cropToSquare(originalImage);
                BufferedImage resizedImage = resizeToSquare(squareImage, size);
                newProfileImage = compressImage(resizedImage, "jpg", 0.8f);

                Image image = new Image(new ByteArrayInputStream(newProfileImage));
                profileImageView.setImage(image);
                profileImageView.setPreserveRatio(false);
                profileImageView.setFitWidth(130);
                profileImageView.setFitHeight(130);

                showAlert("Success", "Image loaded and compressed successfully!", Alert.AlertType.INFORMATION);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to load image: " + e.getMessage());
            }
        }
    }

    private BufferedImage cropToSquare(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width == height) {
            return image;
        }

        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        return image.getSubimage(x, y, size, size);
    }

    private BufferedImage resizeToSquare(BufferedImage image, int targetSize) {
        java.awt.Image scaledImage = image.getScaledInstance(targetSize, targetSize, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
        resizedImage.getGraphics().drawImage(scaledImage, 0, 0, null);
        return resizedImage;
    }

    private byte[] compressImage(BufferedImage image, String format, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IOException("No JPEG writers found");
            }

            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            ImageIO.write(image, format, baos);
        }

        return baos.toByteArray();
    }

    private byte[] compressImageBytes(byte[] imageData, String format, float quality) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(bais);

        if (originalImage == null) {
            return imageData;
        }

        return compressImage(originalImage, format, quality);
    }

    @FXML
    public void handleEditProfile() {
        System.out.println("Edit profile button clicked");
        isEditMode = true;

        // Enable editing for all users
        firstNameField.setEditable(true);
        lastNameField.setEditable(true);
        emailField.setEditable(true);
        languageCombo.setVisible(true);
        languageDisplayLabel.setVisible(false);

        // Username and Membership are locked for non-admin users
        if (isAdmin) {
            // Admin can edit everything
            usernameField.setEditable(true);
            membershipCombo.setVisible(true);
            membershipDisplayLabel.setVisible(false);
            if (usernameLockLabel != null) usernameLockLabel.setVisible(false);
            if (membershipLockLabel != null) membershipLockLabel.setVisible(false);
        } else {
            // Non-admin users cannot edit username and membership
            usernameField.setEditable(false);
            membershipCombo.setVisible(false);
            membershipDisplayLabel.setVisible(true);
            if (usernameLockLabel != null) usernameLockLabel.setVisible(true);
            if (membershipLockLabel != null) membershipLockLabel.setVisible(true);
        }

        // Apply styles
        String editableStyle = "-fx-background-color: white; -fx-border-color: #0FA5A2; -fx-border-radius: 10; -fx-padding: 12; -fx-font-size: 14px; -fx-font-weight: 500;";
        String nonEditableStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-padding: 12; -fx-font-size: 14px; -fx-font-weight: 500;";

        usernameField.setStyle(isAdmin ? editableStyle : nonEditableStyle);
        firstNameField.setStyle(editableStyle);
        lastNameField.setStyle(editableStyle);
        emailField.setStyle(editableStyle);

        // Toggle buttons
        editProfileBtn.setVisible(false);
        saveProfileBtn.setVisible(true);
        cancelEditBtn.setVisible(true);
        changePhotoBtn.setVisible(true);
    }

    @FXML
    public void handleSaveProfile() {
        System.out.println("Save profile button clicked!");

        try {
            // Validate inputs
            if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty() || emailField.getText().isEmpty()) {
                showAlert("Error", "All fields must be filled!", Alert.AlertType.ERROR);
                return;
            }

            // Validate email format
            if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                showAlert("Error", "Please enter a valid email address!", Alert.AlertType.ERROR);
                return;
            }

            System.out.println("==================================");
            System.out.println("=== SAVING PROFILE ===");
            System.out.println("User ID: " + currentUser.getId());
            System.out.println("Username: " + currentUser.getUsername());
            System.out.println("isAdmin: " + isAdmin);
            System.out.println("----------------------------------");

            // Update Person object
            if (isAdmin) {
                currentUser.setUsername(usernameField.getText());
                System.out.println("Username updated to: " + usernameField.getText());
            }
            currentUser.setName(firstNameField.getText());
            currentUser.setLastName(lastNameField.getText());
            currentUser.setEmail(emailField.getText());

            System.out.println("First Name: " + firstNameField.getText());
            System.out.println("Last Name: " + lastNameField.getText());
            System.out.println("Email: " + emailField.getText());

            // Update person in database
            personService.updateOne(currentUser);
            System.out.println("✓ Person updated in database");

            // Get the latest profile from database
            Profile dbProfile = profileService.getProfileByUserId(currentUser.getId());
            if (dbProfile != null) {
                System.out.println("Found profile in DB with ID: " + dbProfile.getId());
                currentProfile = dbProfile;
            } else {
                System.out.println("No profile found in DB, will create new one");
                currentProfile = new Profile();
                currentProfile.setIdUser(currentUser.getId());
            }

            // Update Profile object
            System.out.println("----------------------------------");
            System.out.println("Updating profile (ID: " + currentProfile.getId() + "):");

            // Set membership (if admin)
            if (isAdmin) {
                String newMembership = membershipCombo.getValue();
                currentProfile.setMemberPremium(newMembership);
                System.out.println("  Membership set to: '" + newMembership + "'");
            }

            // Set language
            String newLanguage = languageCombo.getValue();
            currentProfile.setLanguage(newLanguage);
            System.out.println("  Language set to: '" + newLanguage + "'");

            // Handle image if changed
            if (newProfileImage != null) {
                System.out.println("  New image size: " + newProfileImage.length + " bytes");
                // Check image size and compress if needed
                if (newProfileImage.length > 50000) { // 50KB threshold
                    System.out.println("  Compressing image...");
                    byte[] compressedImage = compressImageBytes(newProfileImage, "jpg", 0.6f);
                    System.out.println("  Compressed size: " + compressedImage.length + " bytes");
                    currentProfile.setImage(compressedImage);
                } else {
                    currentProfile.setImage(newProfileImage);
                }
                System.out.println("  ✓ Profile image updated");
            } else {
                System.out.println("  No new image selected - keeping existing image");
                if (dbProfile != null && dbProfile.getImage() != null) {
                    currentProfile.setImage(dbProfile.getImage());
                    System.out.println("  Keeping existing image (" + dbProfile.getImage().length + " bytes)");
                }
            }

            // Update or insert profile
            if (currentProfile.getId() == 0) {
                System.out.println("Inserting new profile...");
                profileService.insertOne(currentProfile);
                System.out.println("✓ New profile inserted with ID: " + currentProfile.getId());
            } else {
                System.out.println("Updating existing profile with ID: " + currentProfile.getId());
                profileService.updateOne(currentProfile);
                System.out.println("✓ Profile updated");
            }

            // Verify the update by fetching again
            Profile verifyProfile = profileService.getProfileByUserId(currentUser.getId());
            if (verifyProfile != null) {
                System.out.println("----------------------------------");
                System.out.println("VERIFICATION - Profile in DB now has:");
                System.out.println("  ID: " + verifyProfile.getId());
                System.out.println("  Membership: " + verifyProfile.getMemberPremium());
                System.out.println("  Language: " + verifyProfile.getLanguage());
                System.out.println("  Coins: " + verifyProfile.getCoins());
                System.out.println("  Image exists: " + (verifyProfile.getImage() != null));
            }

            // Update display labels
            if (isAdmin) {
                membershipLabel.setText(membershipCombo.getValue());
                membershipDisplayLabel.setText(membershipCombo.getValue());
            }
            languageLabel.setText(languageCombo.getValue());
            languageDisplayLabel.setText(languageCombo.getValue());

            // Update user name display
            userNameLabel.setText(currentUser.getName() + " " + currentUser.getLastName());

            System.out.println("----------------------------------");
            System.out.println("✓ PROFILE SAVED SUCCESSFULLY");
            System.out.println("==================================");

            cancelEditMode();
            showAlert("Success", "Profile updated successfully!", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            String errorMessage = e.getMessage();
            System.err.println("SQL Error: " + errorMessage);
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());

            if (errorMessage.contains("Data too long for column 'image'")) {
                showAlert("Error", "The image file is too large. Please choose a smaller image.", Alert.AlertType.ERROR);
            } else if (errorMessage.contains("Duplicate entry")) {
                if (errorMessage.contains("email")) {
                    showAlert("Error", "This email is already in use.", Alert.AlertType.ERROR);
                } else if (errorMessage.contains("username")) {
                    showAlert("Error", "This username is already taken.", Alert.AlertType.ERROR);
                } else {
                    showAlert("Error", "Duplicate entry error: " + errorMessage, Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Error", "Failed to save profile: " + errorMessage, Alert.AlertType.ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to process image: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleCancelEdit() {
        System.out.println("Cancel edit button clicked");

        // Revert changes
        usernameField.setText(currentUser.getUsername());
        firstNameField.setText(currentUser.getName());
        lastNameField.setText(currentUser.getLastName());
        emailField.setText(currentUser.getEmail());

        if (currentProfile != null) {
            membershipCombo.setValue(currentProfile.getMemberPremium());
            languageCombo.setValue(currentProfile.getLanguage());

            // Restore original image
            if (currentProfile.getImage() != null && currentProfile.getImage().length > 0) {
                try {
                    Image image = new Image(new ByteArrayInputStream(currentProfile.getImage()));
                    profileImageView.setImage(image);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                profileImageView.setImage(null);
            }
        }

        cancelEditMode();
    }

    private void cancelEditMode() {
        isEditMode = false;

        // Disable editing
        usernameField.setEditable(false);
        firstNameField.setEditable(false);
        lastNameField.setEditable(false);
        emailField.setEditable(false);

        // Reset visibility
        languageDisplayLabel.setVisible(true);
        languageCombo.setVisible(false);
        membershipDisplayLabel.setVisible(true);
        membershipCombo.setVisible(false);

        if (usernameLockLabel != null) usernameLockLabel.setVisible(false);
        if (membershipLockLabel != null) membershipLockLabel.setVisible(false);

        // Reset styles
        String defaultStyle = "-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-font-size: 14px; -fx-font-weight: 500;";
        usernameField.setStyle(defaultStyle);
        firstNameField.setStyle(defaultStyle);
        lastNameField.setStyle(defaultStyle);
        emailField.setStyle(defaultStyle);

        // Toggle buttons
        editProfileBtn.setVisible(true);
        saveProfileBtn.setVisible(false);
        cancelEditBtn.setVisible(false);
        changePhotoBtn.setVisible(true);

        newProfileImage = null;
    }

    @FXML
    public void handleBack() {
        System.out.println("Back button clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainpage.fxml"));
            Parent mainRoot = loader.load();

            MainPageController mainController = loader.getController();
            mainController.setUserData(currentUser);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(mainRoot));
            stage.setTitle("Main Page");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to main page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        showAlert(title, content, Alert.AlertType.ERROR);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void handleFaceID() {
        faceIDEnabled = !faceIDEnabled;
        if (faceIDEnabled) {
            faceIDButton.setText("ON");
            faceIDButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");

            // Show confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Enable Face ID");
            confirm.setHeaderText("Set up Face ID");
            confirm.setContentText("This will open your camera to capture your face. Continue?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                setupFaceID();
            } else {
                // Revert if user cancels
                faceIDEnabled = false;
                faceIDButton.setText("OFF");
                faceIDButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
            }
        } else {
            faceIDButton.setText("OFF");
            faceIDButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");

            // Disable Face ID
            disableFaceID();
        }
    }

    private void setupFaceID() {
        try {
            int cameraIndex = 0;

            if (CameraUtil.hasMultipleCameras()) {
                CameraSelectionDialog selectionDialog = new CameraSelectionDialog();
                int selectedCamera = selectionDialog.showAndWait();
                if (selectedCamera == -1) return;
                cameraIndex = selectedCamera;
            }

            EnhancedFaceCaptureDialog dialog = new EnhancedFaceCaptureDialog(cameraIndex);
            byte[] faceData = dialog.showAndWait("setup");

            if (faceData != null) {
                // Save to database
                personService.saveFaceData(currentUser.getId(), faceData);

                // Train the recognizer
                FaceRecognitionUtil faceUtil = new FaceRecognitionUtil();
                faceUtil.trainFace(currentUser.getId(), faceData);

                // Update UI
                faceIDEnabled = true;
                faceIDButton.setText("ON");
                faceIDButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
                faceIDSetup.setVisible(true);
                faceIDSetup.setManaged(true);

                showAlert("Success", "Face ID has been set up successfully!", Alert.AlertType.INFORMATION);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save face data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private void disableFaceID() {
        try {
            // Remove face data from database
            personService.saveFaceData(currentUser.getId(), null);

            showAlert("Success", "Face ID has been disabled.", Alert.AlertType.INFORMATION);

            // Update UI
            faceIDEnabled = false;
            faceIDButton.setText("OFF");
            faceIDButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
            faceIDSetup.setVisible(false);
            faceIDSetup.setManaged(false);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to disable Face ID: " + e.getMessage(), Alert.AlertType.ERROR);

            // Revert UI
            faceIDEnabled = true;
            faceIDButton.setText("ON");
            faceIDButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
            faceIDSetup.setVisible(true);
            faceIDSetup.setManaged(true);
        }
    }
}