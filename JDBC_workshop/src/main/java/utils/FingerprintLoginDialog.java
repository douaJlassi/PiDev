package utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.PersonService;
import entities.Person;
import java.sql.SQLException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class FingerprintLoginDialog {

    private Stage dialogStage;
    private Integer resultUserId = null;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private ComboBox<String> portCombo;
    private Button scanBtn;
    private TextField emailField;
    private Label emailErrorLabel;
    private PersonService personService;
    private TextArea logArea;

    // This will store the mapping between fingerprint slot IDs and user IDs
    private Map<Integer, Integer> fingerprintToUserMap = new HashMap<>();

    public FingerprintLoginDialog() {
        personService = new PersonService();
        loadFingerprintMappings();
    }

    /**
     * Load the mapping between fingerprint slot IDs and user IDs from the database
     */
    private void loadFingerprintMappings() {
        try {
            List<Person> usersWithFingerprint = personService.getUsersWithFingerprint();

            // Clear existing mappings
            fingerprintToUserMap.clear();

            System.out.println("=== LOADING FINGERPRINT MAPPINGS FROM DATABASE ===");

            // Map fingerprint slot ID to user ID
            for (Person user : usersWithFingerprint) {
                int slotId = user.getFingerprintSlotId();
                if (slotId > 0) {
                    fingerprintToUserMap.put(slotId, user.getId());
                    System.out.println("📋 Loaded: slot #" + slotId + " -> user ID " + user.getId() +
                            " (email: " + user.getEmail() + ")");
                }
            }

            System.out.println("✅ Loaded " + fingerprintToUserMap.size() + " mappings: " + fingerprintToUserMap);
            System.out.println("==================================================");

        } catch (SQLException e) {
            System.err.println("❌ Failed to load fingerprint mappings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Integer showAndWait() {
        createDialog();
        dialogStage.showAndWait();
        return resultUserId;
    }

    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95);" +
                "-fx-background-radius: 30;" +
                "-fx-padding: 30;" +
                "-fx-border-color: #2ecc71;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, #2ecc71, 20, 0, 0, 0);");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(550);
        root.setPrefHeight(750);

        // Title
        Label titleLabel = new Label("🔑 Fingerprint Login");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Fingerprint Icon
        Label fingerprintIcon = new Label("👆");
        fingerprintIcon.setStyle("-fx-font-size: 80px;");

        // Email Field Section
        VBox emailSection = new VBox(5);
        emailSection.setAlignment(Pos.CENTER_LEFT);
        emailSection.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 15; -fx-padding: 15;");

        Label emailLabel = new Label("📧 Email Verification");
        emailLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 16px; -fx-font-weight: bold;");

        emailField = new TextField();
        emailField.setPromptText("Enter your email address");
        emailField.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 10;");
        emailField.setPrefHeight(40);

        emailErrorLabel = new Label("");
        emailErrorLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 12px;");
        emailErrorLabel.setVisible(false);

        emailSection.getChildren().addAll(emailLabel, emailField, emailErrorLabel);

        // Status Label
        statusLabel = new Label("Enter your email and select COM port");
        statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px;");
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        // Progress Indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(60, 60);

        // Port Selection
        portCombo = new ComboBox<>();
        String[] ports = FingerprintFactory.getAvailablePorts();
        portCombo.getItems().addAll(ports);
        if (ports.length > 0) {
            portCombo.getSelectionModel().selectFirst();
        }
        portCombo.setPromptText("Select COM Port");
        portCombo.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 250; -fx-padding: 8; -fx-background-radius: 10;");

        // Log Area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-control-inner-background: #2d2d3a; -fx-text-fill: #00ff00; -fx-font-family: monospace; -fx-font-size: 12px; -fx-background-radius: 10;");
        logArea.setPromptText("Fingerprint scan progress...");

        // Info label
        Label infoLabel = new Label("⚠️ Place your finger on the sensor after entering email");
        infoLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 12px; -fx-font-style: italic;");
        infoLabel.setWrapText(true);

        // Registered users count
        Label registeredLabel = new Label("📋 Registered fingerprints: " + fingerprintToUserMap.size() + " users");
        registeredLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 12px;");

        // Buttons
        scanBtn = new Button("Verify & Scan Fingerprint");
        scanBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 12 30; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 16px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 12 30; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 16px;");

        root.getChildren().addAll(
                titleLabel,
                fingerprintIcon,
                emailSection,
                portCombo,
                registeredLabel,
                logArea,
                infoLabel,
                statusLabel,
                progressIndicator,
                scanBtn,
                cancelBtn
        );

        // Scan button action
        scanBtn.setOnAction(e -> validateAndStartScan());

        cancelBtn.setOnAction(e -> {
            FingerprintFactory.getInstance().disconnect();
            dialogStage.close();
        });

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
    }

    private void validateAndStartScan() {
        // Validate email
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showEmailError("Email is required");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showEmailError("Please enter a valid email address");
            return;
        }

        // Check if email exists in database
        try {
            Person userByEmail = personService.getUserByEmail(email);
            if (userByEmail == null) {
                showEmailError("No account found with this email");
                return;
            }

            // Email is valid, proceed with scan
            emailErrorLabel.setVisible(false);
            startScan(userByEmail);

        } catch (SQLException e) {
            e.printStackTrace();
            showEmailError("Database error: " + e.getMessage());
        }
    }

    private void showEmailError(String message) {
        emailErrorLabel.setText(message);
        emailErrorLabel.setVisible(true);
        emailField.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-border-color: #ff5e62; -fx-border-width: 2; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 10;");
    }

    private void startScan(Person expectedUser) {
        String selectedPort = portCombo.getValue();

        if (selectedPort == null || selectedPort.isEmpty()) {
            statusLabel.setText("Please select a COM port");
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
            return;
        }

        // Clean up port name
        final String finalPort;
        if (selectedPort.contains("(Simulated)")) {
            finalPort = selectedPort.split(" ")[0];
        } else {
            finalPort = selectedPort;
        }

        progressIndicator.setVisible(true);
        scanBtn.setDisable(true);
        emailField.setDisable(true);
        statusLabel.setText("Connecting to sensor...");
        logArea.clear();

        final Person expectedUserFinal = expectedUser;

        new Thread(() -> {
            FingerprintInterface fpUtil = FingerprintFactory.getInstance();

            // Connect to sensor
            logMessage("🔌 Connecting to " + finalPort + "...");
            boolean connected = fpUtil.connect(finalPort);

            if (connected) {
                logMessage("✅ Connected to fingerprint sensor");
                logMessage("📤 Sending search command...");
                logMessage("👆 Place your finger on the sensor now...");

                // Search for fingerprint - this returns the fingerprint slot ID (1-150)
                int fingerprintSlotId = fpUtil.searchFingerprint();

                if (fingerprintSlotId > 0) {
                    logMessage("✅ Fingerprint detected in slot #" + fingerprintSlotId);

                    // Check if this fingerprint slot belongs to the expected user
                    Integer userIdFromSlot = fingerprintToUserMap.get(fingerprintSlotId);

                    if (userIdFromSlot != null && userIdFromSlot.equals(expectedUserFinal.getId())) {
                        logMessage("✅ Fingerprint matches the email: " + expectedUserFinal.getEmail());

                        final int matchedUserId = expectedUserFinal.getId();
                        System.out.println("🔍 SUCCESS - User ID " + matchedUserId + " authenticated");

                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            statusLabel.setText("✅ Login successful! Welcome " + expectedUserFinal.getUsername());
                            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px;");

                            // Set the result before closing
                            resultUserId = matchedUserId;

                            // Close dialog after short delay
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1500);
                                    Platform.runLater(() -> {
                                        fpUtil.disconnect();
                                        dialogStage.close();
                                    });
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }).start();
                        });
                    }

                     else {
                        logMessage("❌ Fingerprint does NOT match the provided email");
                        logMessage("💡 This fingerprint belongs to a different user");

                        if (userIdFromSlot != null) {
                            logMessage("   Expected user: " + expectedUserFinal.getEmail());
                            logMessage("   Actual user ID from fingerprint: " + userIdFromSlot);
                        }

                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            statusLabel.setText("❌ Fingerprint does not match the email");
                            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
                            scanBtn.setDisable(false);
                            emailField.setDisable(false);
                            fpUtil.disconnect();
                        });
                    }
                } else {
                    logMessage("❌ No matching fingerprint found");
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        statusLabel.setText("❌ No matching fingerprint found");
                        statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
                        scanBtn.setDisable(false);
                        emailField.setDisable(false);
                        fpUtil.disconnect();
                    });
                }
            } else {
                logMessage("❌ Failed to connect to sensor");
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("❌ Failed to connect to sensor");
                    statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
                    scanBtn.setDisable(false);
                    emailField.setDisable(false);
                });
            }
        }).start();
    }

    private void showError(FingerprintInterface fpUtil, String message) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            statusLabel.setText("❌ " + message);
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
            scanBtn.setDisable(false);
            emailField.setDisable(false);
            fpUtil.disconnect();
        });
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}