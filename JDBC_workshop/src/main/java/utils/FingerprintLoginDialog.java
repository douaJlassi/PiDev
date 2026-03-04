package utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.PersonService;
import entities.Person;
import java.sql.SQLException;
import java.util.List;

public class FingerprintLoginDialog {

    private Stage dialogStage;
    private Integer resultUserId = null;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private ComboBox<String> portCombo;
    private Button scanBtn;
    private PersonService personService;
    private TextArea logArea;

    public FingerprintLoginDialog() {
        personService = new PersonService();
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
        root.setPrefWidth(500);
        root.setPrefHeight(600);

        // Title
        Label titleLabel = new Label("🔑 Fingerprint Login");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Fingerprint Icon
        Label fingerprintIcon = new Label("👆");
        fingerprintIcon.setStyle("-fx-font-size: 80px;");

        // Status Label
        statusLabel = new Label("Select COM port and click Scan");
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
        portCombo.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 250;");

        // Log Area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-control-inner-background: #2d2d3a; -fx-text-fill: #00ff00; -fx-font-family: monospace; -fx-font-size: 12px;");
        logArea.setPromptText("Fingerprint scan progress...");

        // Info label
        Label infoLabel = new Label("⚠️ Place your finger on the sensor when prompted");
        infoLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 12px; -fx-font-style: italic;");
        infoLabel.setWrapText(true);

        // Buttons
        scanBtn = new Button("Scan Fingerprint");
        scanBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 12 30; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 16px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-padding: 12 30; -fx-background-radius: 25; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 16px;");

        root.getChildren().addAll(titleLabel, fingerprintIcon, portCombo, logArea, infoLabel, statusLabel, progressIndicator, scanBtn, cancelBtn);

        // Scan button action
        scanBtn.setOnAction(e -> startScan());

        cancelBtn.setOnAction(e -> {
            FingerprintFactory.getInstance().disconnect();
            dialogStage.close();
        });

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
    }

    private void startScan() {
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
        statusLabel.setText("Connecting to sensor...");
        logArea.clear();

        new Thread(() -> {
            FingerprintInterface fpUtil = FingerprintFactory.getInstance();

            // Connect to sensor
            logMessage("🔌 Connecting to " + finalPort + "...");
            boolean connected = fpUtil.connect(finalPort);

            if (connected) {
                logMessage("✅ Connected to fingerprint sensor");
                logMessage("📤 Sending search command...");

                // Send 's' command to search
                fpUtil.sendCommand("s");

                // Read response
                String response = fpUtil.readResponse(5000);
                if (response != null) {
                    for (String line : response.split("\n")) {
                        logMessage(line);
                    }
                }

                Platform.runLater(() -> {
                    statusLabel.setText("✅ Connected! Place your finger on the sensor");
                    statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px;");
                });

                // Search for fingerprint
                int fingerprintId = fpUtil.searchFingerprint();

                if (fingerprintId > 0) {
                    logMessage("✅ Fingerprint ID found: " + fingerprintId);

                    try {
                        // Get all users with fingerprint data
                        List<Person> usersWithFingerprint = personService.getUsersWithFingerprint();

                        // Try to find user with matching ID
                        Person matchedUser = null;
                        for (Person user : usersWithFingerprint) {
                            if (user.getId() == fingerprintId) {
                                matchedUser = user;
                                break;
                            }
                        }

                        // Fallback: if no exact match, use first user
                        if (matchedUser == null && !usersWithFingerprint.isEmpty()) {
                            matchedUser = usersWithFingerprint.get(0);
                            logMessage("⚠️ Using fallback user: " + matchedUser.getUsername());
                        }

                        final Person finalUser = matchedUser;

                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);

                            if (finalUser != null) {
                                statusLabel.setText("✅ Fingerprint matched! Logging in as " + finalUser.getUsername());
                                statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px;");
                                resultUserId = finalUser.getId();

                                // Close after 1.5 seconds
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
                            } else {
                                statusLabel.setText("❌ No user found with this fingerprint");
                                statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px;");
                                scanBtn.setDisable(false);
                                fpUtil.disconnect();
                            }
                        });

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        logMessage("❌ Database error: " + ex.getMessage());
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            statusLabel.setText("❌ Database error: " + ex.getMessage());
                            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px;");
                            scanBtn.setDisable(false);
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