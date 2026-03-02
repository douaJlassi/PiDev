package utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRScannerDialog {

    private Webcam webcam;
    private ImageView cameraView;
    private Stage dialogStage;
    private Label statusLabel;
    private AnimationTimer animationTimer;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean scanning = true;
    private boolean qrDetected = false;
    private String scannedResult = null;

    // Scanner line animation
    private Line scannerLine;
    private TranslateTransition scannerAnimation;
    private javafx.animation.Timeline pulseTimeline;
    private boolean isLineAnimating = false;

    // Pattern to match user credentials from QR code
    private static final Pattern CREDENTIALS_PATTERN =
            Pattern.compile("USER INFORMATION\\s*━━━━━━━━━━━━━━━\\s*Username:\\s*(\\S+)\\s*Email:\\s*(\\S+)\\s*Password:\\s*(\\S+)",
                    Pattern.DOTALL | Pattern.MULTILINE);

    public QRScannerDialog(int cameraIndex) {
        initCamera(cameraIndex);
    }

    private void initCamera(int cameraIndex) {
        try {
            java.util.List<Webcam> webcams = Webcam.getWebcams();
            if (webcams != null && !webcams.isEmpty() && cameraIndex < webcams.size()) {
                webcam = webcams.get(cameraIndex);

                // Set resolution for QR scanning (higher is better for QR codes)
                Dimension[] resolutions = new Dimension[] {
                        new Dimension(1280, 720),
                        WebcamResolution.VGA.getSize(),
                        WebcamResolution.QVGA.getSize()
                };
                webcam.setCustomViewSizes(resolutions);

                // Try to use highest resolution
                Dimension bestSize = null;
                for (Dimension size : webcam.getViewSizes()) {
                    if (size.width >= 1280 && bestSize == null) {
                        bestSize = size;
                    } else if (size.width >= 640 && bestSize == null) {
                        bestSize = size;
                    }
                }

                webcam.setViewSize(bestSize != null ? bestSize : WebcamResolution.VGA.getSize());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] showAndWait() {
        createDialog();
        dialogStage.showAndWait();

        if (scannedResult != null) {
            return parseCredentials(scannedResult);
        }
        return null;
    }

    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setStyle(
                "-fx-background-color: rgba(20, 20, 30, 0.95);" +
                        "-fx-background-radius: 30;" +
                        "-fx-border-color: rgba(255,128,0,0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 30;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,128,0,0.5), 20, 0, 0, 0);" +
                        "-fx-padding: 25;"
        );
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(700);
        root.setPrefHeight(600);

        // Title
        Label titleLabel = new Label("📷 Scan QR Code");
        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, #FF8008, 10, 0, 0, 0);"
        );

        // Subtitle
        Label subtitleLabel = new Label("Position the QR code in the frame");
        subtitleLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px;");

        // Camera container with overlay
        StackPane cameraContainer = new StackPane();
        cameraContainer.setPrefWidth(600);
        cameraContainer.setPrefHeight(400);
        cameraContainer.setStyle(
                "-fx-background-color: #16213e;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,128,0,0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 15;"
        );

        // Camera view
        cameraView = new ImageView();
        cameraView.setFitWidth(600);
        cameraView.setFitHeight(400);
        cameraView.setPreserveRatio(true);

        // Create scanner overlay with corners and scanning line
        Pane overlayPane = new Pane();
        overlayPane.setPrefSize(600, 400);

        // Draw corner rectangles for scan area
        int cornerSize = 30;
        int lineWidth = 3;

        // Top-left corner
        Rectangle topLeft1 = new Rectangle(0, 0, cornerSize, lineWidth);
        topLeft1.setFill(Color.rgb(255, 128, 0, 0.9));
        Rectangle topLeft2 = new Rectangle(0, 0, lineWidth, cornerSize);
        topLeft2.setFill(Color.rgb(255, 128, 0, 0.9));

        // Top-right corner
        Rectangle topRight1 = new Rectangle(600 - cornerSize, 0, cornerSize, lineWidth);
        topRight1.setFill(Color.rgb(255, 128, 0, 0.9));
        Rectangle topRight2 = new Rectangle(600 - lineWidth, 0, lineWidth, cornerSize);
        topRight2.setFill(Color.rgb(255, 128, 0, 0.9));

        // Bottom-left corner
        Rectangle bottomLeft1 = new Rectangle(0, 400 - lineWidth, cornerSize, lineWidth);
        bottomLeft1.setFill(Color.rgb(255, 128, 0, 0.9));
        Rectangle bottomLeft2 = new Rectangle(0, 400 - cornerSize, lineWidth, cornerSize);
        bottomLeft2.setFill(Color.rgb(255, 128, 0, 0.9));

        // Bottom-right corner
        Rectangle bottomRight1 = new Rectangle(600 - cornerSize, 400 - lineWidth, cornerSize, lineWidth);
        bottomRight1.setFill(Color.rgb(255, 128, 0, 0.9));
        Rectangle bottomRight2 = new Rectangle(600 - lineWidth, 400 - cornerSize, lineWidth, cornerSize);
        bottomRight2.setFill(Color.rgb(255, 128, 0, 0.9));

        // Add corners to overlay
        overlayPane.getChildren().addAll(
                topLeft1, topLeft2,
                topRight1, topRight2,
                bottomLeft1, bottomLeft2,
                bottomRight1, bottomRight2
        );

        // Create scanner line with glow effect
        scannerLine = new Line(100, 200, 500, 200); // Start in middle
        scannerLine.setStroke(Color.rgb(255, 128, 0, 0.8));
        scannerLine.setStrokeWidth(2);
        scannerLine.setEffect(new Glow(0.5));

        // Add scanner line to overlay (initially hidden)
        scannerLine.setOpacity(0);
        overlayPane.getChildren().add(scannerLine);

        // Add semi-transparent overlay
        Rectangle overlay = new Rectangle(600, 400);
        overlay.setFill(Color.rgb(0, 0, 0, 0.2));
        overlayPane.getChildren().add(overlay);

        // Bring scanner line to front
        scannerLine.toFront();

        cameraContainer.getChildren().addAll(cameraView, overlayPane);

        // Status label
        statusLabel = new Label("Initializing camera...");
        statusLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px;");

        // Cancel button
        Button cancelBtn = new Button("❌ Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff5e62, #d43f3f);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 40;" +
                        "-fx-background-radius: 25;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,94,98,0.5), 10, 0, 0, 0);"
        );
        cancelBtn.setOnAction(e -> {
            stopCamera();
            scannedResult = null;
            dialogStage.close();
        });

        root.getChildren().addAll(titleLabel, subtitleLabel, cameraContainer, statusLabel, cancelBtn);

        startCamera();
        // Don't start animation here - will start when QR is detected

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.setOnCloseRequest(e -> stopCamera());
    }

    private void startScannerAnimation() {
        if (isLineAnimating) return;

        isLineAnimating = true;

        // Show the scanner line
        scannerLine.setOpacity(1);

        // Create scanning line animation
        scannerAnimation = new TranslateTransition(Duration.seconds(1.5), scannerLine);
        scannerAnimation.setFromY(50);
        scannerAnimation.setToY(350);
        scannerAnimation.setCycleCount(TranslateTransition.INDEFINITE);
        scannerAnimation.setAutoReverse(true);
        scannerAnimation.play();

        // Add pulse effect to the line
        pulseTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(scannerLine.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(Duration.seconds(0.3),
                        new javafx.animation.KeyValue(scannerLine.opacityProperty(), 0.7)),
                new javafx.animation.KeyFrame(Duration.seconds(0.6),
                        new javafx.animation.KeyValue(scannerLine.opacityProperty(), 1.0))
        );
        pulseTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        pulseTimeline.play();

        // Update status
        statusLabel.setText("🎯 QR Code detected! Scanning...");
        statusLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");
    }

    private void stopScannerAnimation() {
        isLineAnimating = false;
        if (scannerAnimation != null) {
            scannerAnimation.stop();
        }
        if (pulseTimeline != null) {
            pulseTimeline.stop();
        }
        scannerLine.setOpacity(0);
    }

    private void startCamera() {
        if (webcam != null) {
            try {
                webcam.open();
                statusLabel.setText("✅ Camera ready. Look for QR code...");
                statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");

                animationTimer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        if (scanning && !qrDetected && webcam.isOpen()) {
                            BufferedImage image = webcam.getImage();
                            if (image != null) {
                                // Update camera view
                                Image fxImage = SwingFXUtils.toFXImage(image, null);
                                cameraView.setImage(fxImage);

                                // Scan for QR code in background
                                scanQRCode(image);
                            }
                        }
                    }
                };
                animationTimer.start();
            } catch (Exception e) {
                statusLabel.setText("❌ Failed to open camera");
                statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px;");
            }
        }
    }

    private void scanQRCode(BufferedImage image) {
        executor.submit(() -> {
            try {
                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                Reader reader = new MultiFormatReader();
                Result result = reader.decode(bitmap);

                if (result != null && scanning && !qrDetected) {
                    qrDetected = true;
                    scannedResult = result.getText();

                    javafx.application.Platform.runLater(() -> {
                        // Start the scanner line animation
                        startScannerAnimation();

                        // Add 2-second delay before processing
                        new Thread(() -> {
                            try {
                                // 2 second delay
                                Thread.sleep(2000);

                                javafx.application.Platform.runLater(() -> {
                                    statusLabel.setText("✅ Processing QR Code...");
                                    statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");

                                    // Stop scanner animation
                                    stopScannerAnimation();

                                    // Visual feedback - change line color to green briefly
                                    scannerLine.setStroke(Color.rgb(46, 204, 113, 0.9));
                                    scannerLine.setStrokeWidth(4);
                                    scannerLine.setOpacity(1);

                                    // Close after short delay
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(500);
                                            javafx.application.Platform.runLater(() -> {
                                                stopCamera();
                                                dialogStage.close();
                                            });
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }).start();
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });
                }
            } catch (NotFoundException e) {
                // No QR code found - ignore
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String[] parseCredentials(String qrText) {
        if (qrText == null) return null;

        String username = null;
        String email = null;
        String password = null;

        // Try pattern matching first
        Matcher matcher = CREDENTIALS_PATTERN.matcher(qrText);
        if (matcher.find()) {
            username = matcher.group(1).trim();
            email = matcher.group(2).trim();
            password = matcher.group(3).trim();

            System.out.println("✅ QR Code parsed via pattern - Username: " + username + ", Email: " + email);
            return new String[]{username, email, password};
        }

        // Try line-by-line parsing
        String[] lines = qrText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Username:") || line.startsWith("username:")) {
                username = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("Email:") || line.startsWith("email:")) {
                email = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("Password:") || line.startsWith("password:")) {
                password = line.substring(line.indexOf(":") + 1).trim();
            }
        }

        if (username != null && email != null && password != null) {
            System.out.println("✅ QR Code parsed via line-by-line - Username: " + username + ", Email: " + email);
            return new String[]{username, email, password};
        }

        // Try comma-separated format
        String[] parts = qrText.split(",");
        if (parts.length >= 3) {
            username = parts[0].trim();
            email = parts[1].trim();
            password = parts[2].trim();
            System.out.println("✅ QR Code parsed via comma-separated - Username: " + username + ", Email: " + email);
            return new String[]{username, email, password};
        }

        System.out.println("❌ Failed to parse QR code: " + qrText);
        return null;
    }

    private void stopCamera() {
        scanning = false;
        if (animationTimer != null) {
            animationTimer.stop();
        }
        stopScannerAnimation();
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        executor.shutdownNow();
    }
}