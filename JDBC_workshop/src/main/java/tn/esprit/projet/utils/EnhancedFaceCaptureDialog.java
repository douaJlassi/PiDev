package tn.esprit.projet.utils;

import javafx.animation.AnimationTimer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

// JavaCV imports
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import org.bytedeco.opencv.opencv_objdetect.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class EnhancedFaceCaptureDialog {

    private CameraUtil cameraUtil;
    private ImageView cameraView;
    private Canvas overlayCanvas;
    private Stage dialogStage;
    private byte[] capturedFace;
    private Label statusLabel;
    private AnimationTimer animationTimer;
    private int cameraIndex;

    // OpenCV detectors
    private CascadeClassifier faceDetector;
    private CascadeClassifier eyeDetector;
    private boolean detectorsLoaded = false;

    // Face detection data
    private Rect detectedFace = null;
    private Rect[] detectedEyes = new Rect[0];
    private double detectionConfidence = 0;

    // For image processing
    private Mat currentFrame = new Mat();

    // Camera frame dimensions
    private int frameWidth = 640;
    private int frameHeight = 480;

    public EnhancedFaceCaptureDialog() {
        this(0);
    }

    public EnhancedFaceCaptureDialog(int cameraIndex) {
        this.cameraIndex = cameraIndex;
        this.cameraUtil = new CameraUtil(cameraIndex);

        // Get the best available resolution
        int[] bestResolution = cameraUtil.getBestResolution();
        if (bestResolution != null && bestResolution.length >= 2) {
            this.frameWidth = bestResolution[0];
            this.frameHeight = bestResolution[1];
            System.out.println("📷 Using camera resolution: " + frameWidth + "x" + frameHeight);
        }

        loadDetectors();
    }

    private void loadDetectors() {
        try {
            // Create resources directory if it doesn't exist
            File resourcesDir = new File("src/main/resources");
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs();
            }

            // Load face detection classifier
            String faceCascadePath = "src/main/resources/haarcascade_frontalface_default.xml";
            String eyeCascadePath = "src/main/resources/haarcascade_eye.xml";

            File faceFile = new File(faceCascadePath);
            File eyeFile = new File(eyeCascadePath);

            if (!faceFile.exists()) {
                downloadCascade(faceCascadePath,
                        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml");
            }
            if (!eyeFile.exists()) {
                downloadCascade(eyeCascadePath,
                        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml");
            }

            faceDetector = new CascadeClassifier(faceCascadePath);
            eyeDetector = new CascadeClassifier(eyeCascadePath);

            if (faceDetector.empty() || eyeDetector.empty()) {
                System.err.println("❌ Failed to load detectors");
                detectorsLoaded = false;
            } else {
                detectorsLoaded = true;
                System.out.println("✅ Face and eye detectors loaded successfully");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load detectors: " + e.getMessage());
            e.printStackTrace();
            detectorsLoaded = false;
        }
    }

    private void downloadCascade(String savePath, String url) throws IOException {
        System.out.println("📥 Downloading cascade: " + savePath);
        try (java.io.InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(savePath));
            System.out.println("✅ Cascade downloaded successfully");
        }
    }

    public byte[] showAndWait(String mode) {
        createDialog(mode);
        dialogStage.showAndWait();
        return capturedFace;
    }

    private void createDialog(String mode) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        // Main container with glass morphism effect
        VBox root = new VBox(20);
        root.setStyle(
                "-fx-background-color: rgba(20, 20, 30, 0.95);" +
                        "-fx-background-radius: 30;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,200,255,0.3), 20, 0, 0, 0);" +
                        "-fx-border-color: rgba(15,165,162,0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 30;"
        );
        root.setAlignment(Pos.CENTER);

        // Adjust size based on frame dimensions
        int containerWidth = Math.min(frameWidth + 100, 1000);
        int containerHeight = Math.min(frameHeight + 250, 800);
        root.setPrefWidth(containerWidth);
        root.setPrefHeight(containerHeight);
        root.setPadding(new Insets(25));

        // Title with gradient
        Label titleLabel = new Label(mode.equals("setup") ? "✨ Face ID Setup" : "🔐 Face ID Login");
        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, #0FA5A2, 10, 0, 0, 0);"
        );

        // Camera info with cool badge
        HBox cameraInfoBox = new HBox(10);
        cameraInfoBox.setAlignment(Pos.CENTER);
        cameraInfoBox.setStyle(
                "-fx-background-color: rgba(15,165,162,0.2);" +
                        "-fx-background-radius: 50;" +
                        "-fx-padding: 8 20;"
        );

        Circle cameraDot = new Circle(5, Color.rgb(15, 165, 162));
        cameraDot.setStyle("-fx-effect: dropshadow(gaussian, #0FA5A2, 10, 0, 0, 0);");

        Label cameraInfoLabel = new Label(cameraUtil.getCameraName() + " | " + frameWidth + "x" + frameHeight);
        cameraInfoLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");

        cameraInfoBox.getChildren().addAll(cameraDot, cameraInfoLabel);

        // Camera view with overlay - use full frame size
        StackPane cameraContainer = new StackPane();
        cameraContainer.setStyle(
                "-fx-background-color: #16213e;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(15,165,162,0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 0);"
        );
        cameraContainer.setPadding(new Insets(10));

        cameraView = new ImageView();
        cameraView.setFitWidth(frameWidth);
        cameraView.setFitHeight(frameHeight);
        cameraView.setPreserveRatio(true);

        overlayCanvas = new Canvas(frameWidth, frameHeight);
        overlayCanvas.setMouseTransparent(true);

        cameraContainer.getChildren().addAll(cameraView, overlayCanvas);

        // Status card
        VBox statusCard = new VBox(10);
        statusCard.setStyle(
                "-fx-background-color: rgba(0,0,0,0.5);" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 15;" +
                        "-fx-border-color: rgba(15,165,162,0.3);" +
                        "-fx-border-radius: 15;"
        );
        statusCard.setAlignment(Pos.CENTER);
        statusCard.setPrefWidth(600);

        statusLabel = new Label("🎥 Initializing camera...");
        statusLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Detection info
        HBox detectionInfo = new HBox(20);
        detectionInfo.setAlignment(Pos.CENTER);

        Label faceStatus = new Label("👤 Face: --");
        faceStatus.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px;");

        Label eyesStatus = new Label("👀 Eyes: --");
        eyesStatus.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px;");

        Label confidenceStatus = new Label("📊 Confidence: --");
        confidenceStatus.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px;");

        detectionInfo.getChildren().addAll(faceStatus, eyesStatus, confidenceStatus);
        statusCard.getChildren().addAll(statusLabel, detectionInfo);

        // Progress indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(60, 60);
        progressIndicator.setStyle(
                "-fx-progress-color: #0FA5A2;" +
                        "-fx-effect: dropshadow(gaussian, #0FA5A2, 10, 0, 0, 0);"
        );

        // Buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button captureBtn = new Button(mode.equals("setup") ? "📸 Capture Face" : "✅ Verify Face");
        captureBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #0FA5A2, #1D4D7C);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 15 40;" +
                        "-fx-background-radius: 50;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,165,162,0.5), 15, 0, 0, 0);"
        );

        // Hover effect
        captureBtn.setOnMouseEntered(e ->
                captureBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #1D4D7C, #0FA5A2);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 18px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 15 40;" +
                                "-fx-background-radius: 50;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, #FEC74C, 20, 0, 0, 0);"
                )
        );
        captureBtn.setOnMouseExited(e ->
                captureBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #0FA5A2, #1D4D7C);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 18px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 15 40;" +
                                "-fx-background-radius: 50;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(15,165,162,0.5), 15, 0, 0, 0);"
                )
        );

        Button cancelBtn = new Button("❌ Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff5e62, #d43f3f);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 15 40;" +
                        "-fx-background-radius: 50;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,94,98,0.5), 15, 0, 0, 0);"
        );

        buttonBox.getChildren().addAll(captureBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, cameraInfoBox, cameraContainer, statusCard, progressIndicator, buttonBox);

        // Start camera and detection
        startCamera(faceStatus, eyesStatus, confidenceStatus);

        // Capture button action
        captureBtn.setOnAction(e -> {
            progressIndicator.setVisible(true);
            captureBtn.setDisable(true);
            cancelBtn.setDisable(true);
            if (mode.equals("setup")) {
                captureFaceForSetup(progressIndicator, captureBtn, cancelBtn);
            } else {
                captureFaceForLogin(progressIndicator, captureBtn, cancelBtn);
            }
        });

        // Cancel button action
        cancelBtn.setOnAction(e -> {
            stopCamera();
            capturedFace = null;
            dialogStage.close();
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.setOnCloseRequest(e -> stopCamera());
    }

    private void startCamera(Label faceStatus, Label eyesStatus, Label confidenceStatus) {
        if (cameraUtil.openCamera()) {
            statusLabel.setText("✅ Camera ready! Position your face in the center");
            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");

            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    Image frame = cameraUtil.captureJavaFXImage();
                    if (frame != null) {
                        cameraView.setImage(frame);
                        processFrameForDetection(frame, faceStatus, eyesStatus, confidenceStatus);
                    }
                }
            };
            animationTimer.start();
        } else {
            statusLabel.setText("❌ Failed to open camera!");
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px; -fx-font-weight: bold;");
        }
    }

    private void processFrameForDetection(Image frame, Label faceStatus, Label eyesStatus, Label confidenceStatus) {
        if (!detectorsLoaded) return;

        try {
            // Convert JavaFX Image to BufferedImage
            java.awt.image.BufferedImage awtImage = SwingFXUtils.fromFXImage(frame, null);
            if (awtImage == null) {
                return;
            }

            // Convert BufferedImage to Mat
            int width = awtImage.getWidth();
            int height = awtImage.getHeight();

            Mat mat = new Mat(height, width, CV_8UC3);
            BytePointer dataPointer = mat.data();
            byte[] data = new byte[width * height * 3];

            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    java.awt.Color color = new java.awt.Color(awtImage.getRGB(x, y));
                    data[idx++] = (byte) color.getBlue();
                    data[idx++] = (byte) color.getGreen();
                    data[idx++] = (byte) color.getRed();
                }
            }

            dataPointer.put(data);

            // Release previous frame
            if (currentFrame != null && !currentFrame.isNull()) {
                currentFrame.release();
            }
            currentFrame = mat.clone();

            // Convert to grayscale for detection
            Mat gray = new Mat();
            cvtColor(mat, gray, COLOR_BGR2GRAY);
            equalizeHist(gray, gray);

            // Detect faces
            int minFaceSize = Math.min(width, height) / 8;
            RectVector faceDetections = new RectVector();
            faceDetector.detectMultiScale(gray, faceDetections, 1.1, 3, 0,
                    new Size(minFaceSize, minFaceSize), new Size(width, height));

            // Clear previous drawings
            GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());

            if (!faceDetections.empty()) {
                detectedFace = faceDetections.get(0);

                // Calculate confidence based on face size and position
                double frameArea = overlayCanvas.getWidth() * overlayCanvas.getHeight();
                double faceArea = detectedFace.width() * detectedFace.height();
                double sizeScore = Math.min(faceArea / (frameArea * 0.25), 1.0);

                double centerX = overlayCanvas.getWidth() / 2;
                double centerY = overlayCanvas.getHeight() / 2;
                double faceCenterX = detectedFace.x() + detectedFace.width() / 2;
                double faceCenterY = detectedFace.y() + detectedFace.height() / 2;
                double distanceFromCenter = Math.sqrt(
                        Math.pow(faceCenterX - centerX, 2) +
                                Math.pow(faceCenterY - centerY, 2)
                ) / (Math.min(overlayCanvas.getWidth(), overlayCanvas.getHeight()) / 3);
                double positionScore = 1.0 - Math.min(distanceFromCenter, 1.0);

                detectionConfidence = (sizeScore * 0.6 + positionScore * 0.4) * 100;

                // Draw face rectangle
                int scaledX = detectedFace.x();
                int scaledY = detectedFace.y();
                int scaledWidth = detectedFace.width();
                int scaledHeight = detectedFace.height();

                gc.setStroke(Color.rgb(15, 165, 162, 0.8));
                gc.setLineWidth(3);
                gc.strokeRect(scaledX, scaledY, scaledWidth, scaledHeight);

                // Draw corners
                gc.setStroke(Color.rgb(254, 199, 76, 0.9));
                gc.setLineWidth(2);
                int cornerSize = Math.min(30, scaledWidth / 4);

                // Top-left
                gc.strokeLine(scaledX, scaledY, scaledX + cornerSize, scaledY);
                gc.strokeLine(scaledX, scaledY, scaledX, scaledY + cornerSize);
                // Top-right
                gc.strokeLine(scaledX + scaledWidth, scaledY,
                        scaledX + scaledWidth - cornerSize, scaledY);
                gc.strokeLine(scaledX + scaledWidth, scaledY,
                        scaledX + scaledWidth, scaledY + cornerSize);
                // Bottom-left
                gc.strokeLine(scaledX, scaledY + scaledHeight,
                        scaledX + cornerSize, scaledY + scaledHeight);
                gc.strokeLine(scaledX, scaledY + scaledHeight,
                        scaledX, scaledY + scaledHeight - cornerSize);
                // Bottom-right
                gc.strokeLine(scaledX + scaledWidth, scaledY + scaledHeight,
                        scaledX + scaledWidth - cornerSize, scaledY + scaledHeight);
                gc.strokeLine(scaledX + scaledWidth, scaledY + scaledHeight,
                        scaledX + scaledWidth, scaledY + scaledHeight - cornerSize);

                // Detect eyes within face region - FIXED with more tolerant parameters
                Rect faceRect = new Rect(detectedFace.x(), detectedFace.y(),
                        detectedFace.width(), detectedFace.height());
                Mat faceROI = new Mat(gray, faceRect);
                RectVector eyeDetections = new RectVector();

                eyeDetector.detectMultiScale(
                        faceROI,
                        eyeDetections,
                        1.1,      // scale factor
                        3,        // min neighbors
                        0,
                        new Size(15, 15),  // smaller min size
                        new Size(60, 60)   // max size
                );

                // Filter eyes - more tolerant
                ArrayList<Rect> validEyes = new ArrayList<>();
                int faceMidY = detectedFace.height() / 2;

                for (int i = 0; i < eyeDetections.size(); i++) {
                    Rect eye = eyeDetections.get(i);
                    // Eyes should be in upper half of face (more tolerant)
                    if (eye.y() + eye.height()/2 < faceMidY * 1.3) {
                        double aspectRatio = (double) eye.width() / eye.height();
                        // More tolerant aspect ratio
                        if (aspectRatio > 0.3 && aspectRatio < 2.5) {
                            validEyes.add(eye);
                        }
                    }
                }

                // Take only the two most likely eyes
                validEyes.sort((a, b) -> Integer.compare(b.width() * b.height(), a.width() * a.height()));
                detectedEyes = validEyes.size() > 2 ?
                        new Rect[]{validEyes.get(0), validEyes.get(1)} :
                        validEyes.toArray(new Rect[0]);

                // Draw eyes
                for (Rect eye : detectedEyes) {
                    int eyeX = scaledX + eye.x();
                    int eyeY = scaledY + eye.y();
                    int eyeWidth = eye.width();
                    int eyeHeight = eye.height();

                    if (eyeWidth > 5 && eyeHeight > 5) {
                        gc.setFill(Color.rgb(254, 199, 76, 0.3));
                        gc.fillOval(eyeX - 3, eyeY - 3, eyeWidth + 6, eyeHeight + 6);

                        gc.setStroke(Color.rgb(254, 199, 76, 0.9));
                        gc.setLineWidth(2);
                        gc.strokeOval(eyeX, eyeY, eyeWidth, eyeHeight);

                        gc.setFill(Color.rgb(254, 199, 76, 0.8));
                        gc.fillOval(eyeX + eyeWidth/2 - 2, eyeY + eyeHeight/2 - 2, 4, 4);
                    }
                }

                // Update status labels
                double finalConfidence = detectionConfidence;
                int eyeCount = detectedEyes.length;
                javafx.application.Platform.runLater(() -> {
                    faceStatus.setText("👤 Face: Detected");
                    faceStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;");

                    eyesStatus.setText("👀 Eyes: " + eyeCount + " detected");
                    eyesStatus.setStyle(eyeCount >= 1 ?
                            "-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;" :
                            "-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");

                    confidenceStatus.setText("📊 Confidence: " + String.format("%.1f%%", finalConfidence));
                    confidenceStatus.setStyle(finalConfidence > 50 ?
                            "-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;" :
                            "-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");
                });

                // Clean up
                faceROI.close();

            } else {
                detectedFace = null;
                detectedEyes = new Rect[0];
                detectionConfidence = 0;

                // Draw guide frame
                gc.setStroke(Color.rgb(255, 94, 98, 0.5));
                gc.setLineWidth(2);
                gc.setLineDashes(10);

                double centerX = overlayCanvas.getWidth() / 2;
                double centerY = overlayCanvas.getHeight() / 2;
                double size = Math.min(overlayCanvas.getWidth(), overlayCanvas.getHeight()) / 3;

                gc.strokeOval(centerX - size/2, centerY - size/2, size, size);

                gc.setLineDashes(5);
                gc.strokeLine(centerX - 40, centerY, centerX + 40, centerY);
                gc.strokeLine(centerX, centerY - 40, centerX, centerY + 40);

                javafx.application.Platform.runLater(() -> {
                    faceStatus.setText("👤 Face: Not detected");
                    faceStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                    eyesStatus.setText("👀 Eyes: --");
                    eyesStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                    confidenceStatus.setText("📊 Confidence: 0%");
                    confidenceStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                });
            }

            gray.close();

        } catch (Exception e) {
            System.err.println("Error in face detection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        cameraUtil.closeCamera();
        if (currentFrame != null && !currentFrame.isNull()) {
            currentFrame.close();
        }
    }

    private void captureFaceForSetup(ProgressIndicator progressIndicator, Button captureBtn, Button cancelBtn) {
        statusLabel.setText("📸 Capturing face...");

        new Thread(() -> {
            try {
                // Wait a moment for the current frame to be ready
                Thread.sleep(500);

                if (currentFrame != null && !currentFrame.isNull()) {
                    System.out.println("Current frame size: " + currentFrame.cols() + "x" + currentFrame.rows());

                    // Convert Mat to byte array using JavaCV
                    MatVector buf = new MatVector();
                    boolean success = imencode(".jpg", currentFrame, buf.asByteBuffer());

                    if (success && !buf.empty()) {
                        Mat encoded = buf.get(0);
                        if (encoded != null && !encoded.empty()) {
                            long total = encoded.total();
                            int channels = encoded.channels();
                            int size = (int) (total * channels);

                            if (size > 0) {
                                byte[] imageBytes = new byte[size];
                                encoded.data().get(imageBytes);
                                System.out.println("Captured image size: " + imageBytes.length + " bytes");

                                // Extract face features
                                FaceRecognitionUtil faceUtil = new FaceRecognitionUtil();
                                byte[] faceFeatures = faceUtil.extractFaceFeatures(imageBytes);

                                javafx.application.Platform.runLater(() -> {
                                    progressIndicator.setVisible(false);
                                    captureBtn.setDisable(false);
                                    cancelBtn.setDisable(false);

                                    if (faceFeatures != null && faceFeatures.length > 0) {
                                        capturedFace = faceFeatures;
                                        statusLabel.setText("✅ Face captured successfully!");
                                        statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");

                                        // Close dialog after successful capture
                                        new Thread(() -> {
                                            try {
                                                Thread.sleep(1500);
                                                javafx.application.Platform.runLater(() -> {
                                                    stopCamera();
                                                    dialogStage.close();
                                                });
                                            } catch (InterruptedException ex) {
                                                ex.printStackTrace();
                                            }
                                        }).start();
                                    } else {
                                        statusLabel.setText("❌ No face detected! Please ensure your face is clearly visible.");
                                        statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px; -fx-font-weight: bold;");
                                        System.out.println("Face features extraction failed - returned null or empty");
                                    }
                                });
                            } else {
                                javafx.application.Platform.runLater(() -> {
                                    progressIndicator.setVisible(false);
                                    captureBtn.setDisable(false);
                                    cancelBtn.setDisable(false);
                                    statusLabel.setText("❌ Failed to capture image");
                                });
                            }
                        }
                    }
                } else {
                    javafx.application.Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        captureBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                        statusLabel.setText("❌ No camera frame available");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    captureBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    statusLabel.setText("❌ Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void captureFaceForLogin(ProgressIndicator progressIndicator, Button captureBtn, Button cancelBtn) {
        new Thread(() -> {
            try {
                java.awt.image.BufferedImage bestImage = null;
                double bestSharpness = 0;
                int bestConfidence = 0;

                for (int i = 0; i < 5; i++) {
                    if (currentFrame != null && !currentFrame.isNull()) {
                        Mat gray = new Mat();
                        cvtColor(currentFrame, gray, COLOR_BGR2GRAY);
                        Mat laplacian = new Mat();
                        Laplacian(gray, laplacian, CV_64F);

                        // Calculate mean of laplacian for sharpness
                        Mat meanMat = new Mat();
                        Mat stddevMat = new Mat();
                        meanStdDev(laplacian, meanMat, stddevMat);

                        double sharpness = stddevMat.ptr(0, 0).getDouble();
                        int currentConfidence = (int) detectionConfidence;

                        if (sharpness > bestSharpness && currentConfidence > 50) {
                            bestSharpness = sharpness;
                            bestImage = matToBufferedImage(currentFrame);
                            bestConfidence = currentConfidence;
                        }

                        gray.close();
                        laplacian.close();
                        meanMat.close();
                        stddevMat.close();
                    }
                    Thread.sleep(200);
                }

                if (bestImage != null) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(bestImage, "jpg", baos);
                    capturedFace = baos.toByteArray();

                    System.out.println("✅ Best image selected - Sharpness: " + bestSharpness +
                            ", Confidence: " + bestConfidence + "%");
                }

                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    stopCamera();
                    dialogStage.close();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    captureBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    statusLabel.setText("❌ Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private java.awt.image.BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        BytePointer dataPointer = mat.data();
        byte[] data = new byte[width * height * channels];
        dataPointer.get(data);

        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);

        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte b = data[idx++];
                byte g = data[idx++];
                byte r = data[idx++];
                int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }
}