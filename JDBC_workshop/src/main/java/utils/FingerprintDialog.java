package utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.PersonService;
import java.sql.SQLException;
import java.util.Arrays;

public class FingerprintDialog {

    private Stage dialogStage;
    private boolean success = false;
    private int slotId = -1;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private Button enrollBtn;
    private ComboBox<String> portCombo;
    private TextArea logArea;
    private PersonService personService;
    private int userId;
    private FingerprintInterface fpUtil;

    public static class EnrollmentResult {
        private boolean success;
        private int slotId;

        public EnrollmentResult(boolean success, int slotId) {
            this.success = success;
            this.slotId = slotId;
        }

        public boolean isSuccess() { return success; }
        public int getSlotId() { return slotId; }
    }

    public EnrollmentResult showAndWait(int userId) {
        this.userId = userId;
        this.personService = new PersonService();
        createDialog();
        dialogStage.showAndWait();
        return new EnrollmentResult(success, slotId);
    }

    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95);" +
                "-fx-background-radius: 30;" +
                "-fx-padding: 30;" +
                "-fx-border-color: #0FA5A2;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, #0FA5A2, 20, 0, 0, 0);");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(650);
        root.setPrefHeight(750);

        // Title
        Label titleLabel = new Label("🔐 Fingerprint Enrollment");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Fingerprint Icon
        Label fingerprintIcon = new Label("👆");
        fingerprintIcon.setStyle("-fx-font-size: 80px;");

        // Status Label
        statusLabel = new Label("Select COM port and click Start");
        statusLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 16px;");
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        // Progress Indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(60, 60);

        // Port Selection
        portCombo = new ComboBox<>();
        String[] ports = FingerprintFactory.getAvailablePorts();
        System.out.println("Available ports: " + Arrays.toString(ports));
        portCombo.getItems().addAll(ports);

        if (ports.length > 0 && !ports[0].equals("No ports detected")) {
            portCombo.getSelectionModel().selectFirst();
        }

        portCombo.setPromptText("Select COM Port");
        portCombo.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 250;");

        // Instructions
        Label instructionLabel = new Label(
                "📋 IMPORTANT INSTRUCTIONS:\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "1. Click 'Start Enrollment' to connect to Arduino\n" +
                        "2. Place your finger when prompted\n" +
                        "3. Remove when asked\n" +
                        "4. Place the SAME finger again\n" +
                        "5. Wait for success message\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        instructionLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");
        instructionLabel.setWrapText(true);
        instructionLabel.setAlignment(Pos.CENTER_LEFT);

        // Log Area to show Arduino output
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(250);
        logArea.setStyle("-fx-control-inner-background: #2d2d3a; -fx-text-fill: #00ff00; -fx-font-family: monospace; -fx-font-size: 12px;");
        logArea.setPromptText("Arduino communication will appear here...");

        // Buttons
        enrollBtn = new Button("Start Enrollment");
        enrollBtn.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 12 30; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 16px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 12 30; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 16px;");

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(enrollBtn, cancelBtn);

        root.getChildren().addAll(
                titleLabel,
                fingerprintIcon,
                portCombo,
                instructionLabel,
                logArea,
                statusLabel,
                progressIndicator,
                buttonBox
        );

        // Enroll button action
        enrollBtn.setOnAction(e -> startEnrollment());

        cancelBtn.setOnAction(e -> {
            if (fpUtil != null) {
                fpUtil.disconnect();
            }
            dialogStage.close();
        });

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
    }

    private void startEnrollment() {
        String selectedPort = portCombo.getValue();
        if (selectedPort == null || selectedPort.isEmpty()) {
            statusLabel.setText("Please select a COM port");
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
            return;
        }

        progressIndicator.setVisible(true);
        enrollBtn.setDisable(true);
        statusLabel.setText("Connecting to sensor...");
        logArea.clear();

        new Thread(() -> {
            fpUtil = FingerprintFactory.getInstance();

            // Connect to sensor
            logMessage("🔌 Connecting to " + selectedPort + "...");
            boolean connected = fpUtil.connect(selectedPort);

            if (connected) {
                logMessage("✅ Connected to fingerprint sensor");
                logMessage("");
                logMessage("🤖 AUTOMATIC ENROLLMENT PROCESS:");
                logMessage("1. Java will automatically find an available slot");
                logMessage("2. Place your finger when prompted");
                logMessage("3. Remove when asked");
                logMessage("4. Place the SAME finger again");
                logMessage("");
                logMessage("⏳ Starting enrollment...");

                Platform.runLater(() -> {
                    statusLabel.setText("✅ Connected! Starting automatic enrollment...");
                    statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                });

                // Start enrollment - this will return the actual slot ID on success
                int actualSlotId = fpUtil.enrollFingerprint(userId);

                if (actualSlotId > 0) {
                    logMessage("");
                    logMessage("🎉 FINGERPRINT ENROLLMENT SUCCESSFUL! Slot #" + actualSlotId);

                    try {
                        // Save to database with the actual slot ID from Arduino
                        byte[] fingerprintData = new byte[]{1};
                        personService.saveFingerprintData(userId, fingerprintData, actualSlotId);

                        logMessage("✅ Fingerprint data saved to database for user ID: " + userId + " in slot #" + actualSlotId);

                        slotId = actualSlotId;

                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            statusLabel.setText("✅ Fingerprint enrolled and saved successfully!");
                            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px;");
                            success = true;

                            // Close after 3 seconds
                            new Thread(() -> {
                                try {
                                    Thread.sleep(3000);
                                    Platform.runLater(() -> {
                                        fpUtil.disconnect();
                                        dialogStage.close();
                                    });
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }).start();
                        });

                    } catch (SQLException e) {
                        logMessage("❌ Database error: " + e.getMessage());
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            statusLabel.setText("❌ Database error");
                            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
                            enrollBtn.setDisable(false);
                        });
                    }
                } else {
                    logMessage("");
                    logMessage("❌ ENROLLMENT FAILED");
                    logMessage("Check the console for error messages");

                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        statusLabel.setText("❌ Enrollment failed. Check console.");
                        statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
                        enrollBtn.setDisable(false);
                        fpUtil.disconnect();
                    });
                }
            } else {
                logMessage("❌ Failed to connect to sensor");
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("❌ Failed to connect to sensor");
                    statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
                    enrollBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}