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
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

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

    public EnhancedFaceCaptureDialog() {
        this(0);
    }

    public EnhancedFaceCaptureDialog(int cameraIndex) {
        this.cameraIndex = cameraIndex;
        this.cameraUtil = new CameraUtil(cameraIndex);
        loadDetectors();
    }

    private void loadDetectors() {
        try {
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
            detectorsLoaded = true;
            System.out.println("✅ Face and eye detectors loaded successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to load detectors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void downloadCascade(String savePath, String url) throws IOException {
        System.out.println("📥 Downloading cascade: " + savePath);
        try (java.io.InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(savePath));
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
        root.setPrefWidth(800);
        root.setPrefHeight(700);
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

        Label cameraInfoLabel = new Label(cameraUtil.getCameraName());
        cameraInfoLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");

        cameraInfoBox.getChildren().addAll(cameraDot, cameraInfoLabel);

        // Camera view with overlay
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
        cameraView.setFitWidth(700);
        cameraView.setFitHeight(500);
        cameraView.setPreserveRatio(true);

        overlayCanvas = new Canvas(700, 500);
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

        // Buttons with cool effects
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
            // Convert JavaFX Image to Mat directly without using ImageIO
            java.awt.image.BufferedImage awtImage = SwingFXUtils.fromFXImage(frame, null);
            if (awtImage == null) {
                return;
            }

            // Convert BufferedImage to Mat using pixel manipulation
            int width = awtImage.getWidth();
            int height = awtImage.getHeight();

            // Create Mat with 3 channels (BGR)
            Mat mat = new Mat(height, width, CvType.CV_8UC3);
            byte[] data = new byte[width * height * 3];

            // Fill the Mat with pixel data
            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    java.awt.Color color = new java.awt.Color(awtImage.getRGB(x, y));
                    // OpenCV uses BGR order
                    data[idx++] = (byte) color.getBlue();
                    data[idx++] = (byte) color.getGreen();
                    data[idx++] = (byte) color.getRed();
                }
            }

            mat.put(0, 0, data);

            // Release previous frame
            if (!currentFrame.empty()) {
                currentFrame.release();
            }
            currentFrame = mat.clone();

            // Convert to grayscale for detection
            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            // Detect faces
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(gray, faceDetections, 1.1, 3, 0, new Size(100, 100), new Size(500, 500));

            Rect[] faces = faceDetections.toArray();

            // Clear previous drawings
            GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());

            if (faces.length > 0) {
                detectedFace = faces[0];

                // Calculate confidence based on face size and position
                double frameArea = overlayCanvas.getWidth() * overlayCanvas.getHeight();
                double faceArea = detectedFace.width * detectedFace.height;
                double sizeScore = Math.min(faceArea / (frameArea * 0.2), 1.0);

                // Center position score
                double centerX = overlayCanvas.getWidth() / 2;
                double centerY = overlayCanvas.getHeight() / 2;
                double faceCenterX = detectedFace.x + detectedFace.width / 2;
                double faceCenterY = detectedFace.y + detectedFace.height / 2;
                double distanceFromCenter = Math.sqrt(
                        Math.pow(faceCenterX - centerX, 2) +
                                Math.pow(faceCenterY - centerY, 2)
                ) / (Math.min(overlayCanvas.getWidth(), overlayCanvas.getHeight()) / 2);
                double positionScore = 1.0 - Math.min(distanceFromCenter, 1.0);

                detectionConfidence = (sizeScore * 0.6 + positionScore * 0.4) * 100;

                // Scale factor for drawing (camera view might be scaled)
                double scaleX = overlayCanvas.getWidth() / frame.getWidth();
                double scaleY = overlayCanvas.getHeight() / frame.getHeight();

                int scaledX = (int)(detectedFace.x * scaleX);
                int scaledY = (int)(detectedFace.y * scaleY);
                int scaledWidth = (int)(detectedFace.width * scaleX);
                int scaledHeight = (int)(detectedFace.height * scaleY);

                // Draw face rectangle with gradient
                gc.setStroke(Color.rgb(15, 165, 162, 0.8));
                gc.setLineWidth(3);
                gc.strokeRect(scaledX, scaledY, scaledWidth, scaledHeight);

                // Draw corners for cool effect
                gc.setStroke(Color.rgb(254, 199, 76, 0.9));
                gc.setLineWidth(2);
                int cornerSize = 30;
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

                // Detect eyes within face region - IMPROVED PARAMETERS
                Mat faceROI = gray.submat(detectedFace);
                MatOfRect eyeDetections = new MatOfRect();

                // More strict parameters to reduce false positives
                eyeDetector.detectMultiScale(
                        faceROI,
                        eyeDetections,
                        1.05,           // Smaller scale factor = more accurate
                        5,              // Higher min neighbors = fewer false positives
                        0,
                        new Size(30, 30),   // Min eye size
                        new Size(80, 80)    // Max eye size
                );

                Rect[] eyes = eyeDetections.toArray();

                // Filter eyes - they should be in the upper half of the face
                ArrayList<Rect> validEyes = new ArrayList<>();
                int faceMidY = detectedFace.height / 2;

                for (Rect eye : eyes) {
                    // Eyes should be in upper half of face
                    if (eye.y + eye.height/2 < faceMidY) {
                        // Eyes should have reasonable aspect ratio
                        double aspectRatio = (double) eye.width / eye.height;
                        if (aspectRatio > 0.8 && aspectRatio < 1.5) {
                            validEyes.add(eye);
                        }
                    }
                }

                // Take only the two most likely eyes (largest)
                validEyes.sort((a, b) -> Integer.compare(b.width * b.height, a.width * a.height));
                Rect[] filteredEyes = validEyes.size() > 2 ?
                        new Rect[]{validEyes.get(0), validEyes.get(1)} :
                        validEyes.toArray(new Rect[0]);

                detectedEyes = filteredEyes;

                // Draw eyes - only draw filtered eyes
                for (Rect eye : filteredEyes) {
                    // Adjust eye coordinates to original image and scale
                    int eyeX = scaledX + (int)(eye.x * scaleX);
                    int eyeY = scaledY + (int)(eye.y * scaleY);
                    int eyeWidth = (int)(eye.width * scaleX);
                    int eyeHeight = (int)(eye.height * scaleY);

                    // Only draw if within bounds
                    if (eyeWidth > 10 && eyeHeight > 10) {
                        // Draw eye circles with glow effect
                        gc.setFill(Color.rgb(254, 199, 76, 0.3));
                        gc.fillOval(eyeX - 5, eyeY - 5, eyeWidth + 10, eyeHeight + 10);

                        gc.setStroke(Color.rgb(254, 199, 76, 0.9));
                        gc.setLineWidth(2);
                        gc.strokeOval(eyeX, eyeY, eyeWidth, eyeHeight);

                        // Draw pupil
                        gc.setFill(Color.rgb(254, 199, 76, 0.8));
                        gc.fillOval(eyeX + eyeWidth/2 - 3, eyeY + eyeHeight/2 - 3, 6, 6);
                    }
                }

                // Update status labels
                double finalConfidence = detectionConfidence;
                int eyeCount = filteredEyes.length;
                javafx.application.Platform.runLater(() -> {
                    faceStatus.setText("👤 Face: Detected");
                    faceStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;");

                    eyesStatus.setText("👀 Eyes: " + eyeCount + " detected");
                    eyesStatus.setStyle(eyeCount >= 2 ?
                            "-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;" :
                            "-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");

                    confidenceStatus.setText("📊 Confidence: " + String.format("%.1f%%", finalConfidence));
                    confidenceStatus.setStyle(finalConfidence > 70 ?
                            "-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;" :
                            "-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");
                });

                // Clean up
                faceROI.release();
                eyeDetections.release();

            } else {
                detectedFace = null;
                detectedEyes = new Rect[0];
                detectionConfidence = 0;

                // Draw guide frame when no face detected
                gc.setStroke(Color.rgb(255, 94, 98, 0.5));
                gc.setLineWidth(2);
                gc.setLineDashes(10);

                double centerX = overlayCanvas.getWidth() / 2;
                double centerY = overlayCanvas.getHeight() / 2;
                double size = 200;

                gc.strokeOval(centerX - size/2, centerY - size/2, size, size);

                // Draw crosshair
                gc.setLineDashes(5);
                gc.strokeLine(centerX - 50, centerY, centerX + 50, centerY);
                gc.strokeLine(centerX, centerY - 50, centerX, centerY + 50);

                // Update status labels
                javafx.application.Platform.runLater(() -> {
                    faceStatus.setText("👤 Face: Not detected");
                    faceStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");

                    eyesStatus.setText("👀 Eyes: --");
                    eyesStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");

                    confidenceStatus.setText("📊 Confidence: 0%");
                    confidenceStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                });
            }

            // Clean up
            gray.release();
            faceDetections.release();

        } catch (Exception e) {
            System.err.println("Error in face detection: " + e.getMessage());
        }
    }

    private void stopCamera() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        cameraUtil.closeCamera();
        if (!currentFrame.empty()) {
            currentFrame.release();
        }
    }

    private void captureFaceForSetup(ProgressIndicator progressIndicator, Button captureBtn, Button cancelBtn) {
        statusLabel.setText("📸 Capturing face...");

        new Thread(() -> {
            try {
                // Capture image from the current frame
                if (!currentFrame.empty()) {
                    // Convert Mat to byte array
                    MatOfByte mob = new MatOfByte();
                    Imgcodecs.imencode(".jpg", currentFrame, mob);
                    byte[] imageBytes = mob.toArray();

                    FaceRecognitionUtil faceUtil = new FaceRecognitionUtil();
                    byte[] faceFeatures = faceUtil.extractFaceFeatures(imageBytes);

                    javafx.application.Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        captureBtn.setDisable(false);
                        cancelBtn.setDisable(false);

                        if (faceFeatures != null) {
                            capturedFace = faceFeatures;
                            statusLabel.setText("✅ Face captured successfully!");
                            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");

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
                            statusLabel.setText("❌ No face detected! Please try again.");
                            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px; -fx-font-weight: bold;");
                        }
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
                    if (!currentFrame.empty()) {
                        Mat gray = new Mat();
                        Imgproc.cvtColor(currentFrame, gray, Imgproc.COLOR_BGR2GRAY);
                        Mat laplacian = new Mat();
                        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
                        Core.MinMaxLocResult mmr = Core.minMaxLoc(laplacian);
                        double sharpness = mmr.maxVal - mmr.minVal;

                        int currentConfidence = (int) detectionConfidence;

                        if (sharpness > bestSharpness && currentConfidence > 50) {
                            bestSharpness = sharpness;
                            // Convert Mat to BufferedImage
                            bestImage = matToBufferedImage(currentFrame);
                            bestConfidence = currentConfidence;
                        }

                        gray.release();
                        laplacian.release();
                    }
                    Thread.sleep(200);
                }

                if (bestImage != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bestImage, "jpg", baos);
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
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();

        byte[] data = new byte[width * height * channels];
        mat.get(0, 0, data);

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