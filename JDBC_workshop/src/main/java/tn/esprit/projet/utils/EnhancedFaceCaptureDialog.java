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
    private int framesWithFace = 0;
    private boolean faceStable = false;

    // Auto-capture timer
    private long stableStartTime = 0;
    private static final long AUTO_CAPTURE_DELAY = 3000; // 3 seconds
    private boolean autoCaptureTriggered = false;

    // For debugging
    private int totalFramesProcessed = 0;

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

        // Force VGA resolution for better compatibility
        if (cameraUtil.setFixedResolution(640, 480)) {
            this.frameWidth = 640;
            this.frameHeight = 480;
            System.out.println("📷 Using fixed VGA resolution: 640x480");
        } else {
            int[] bestResolution = cameraUtil.getBestResolution();
            if (bestResolution != null && bestResolution.length >= 2) {
                this.frameWidth = bestResolution[0];
                this.frameHeight = bestResolution[1];
                System.out.println("📷 Using camera resolution: " + frameWidth + "x" + frameHeight);
            }
        }

        loadDetectors();
    }

    private void loadDetectors() {
        try {
            File resourcesDir = new File("src/main/resources");
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs();
            }

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

            if (faceDetector.empty()) {
                System.err.println("❌ Failed to load face detector");
                detectorsLoaded = false;
            } else {
                System.out.println("✅ Face detector loaded: " + faceCascadePath);
                detectorsLoaded = true;
            }

            if (eyeDetector.empty()) {
                System.out.println("⚠️ Eye detector not loaded (optional)");
            } else {
                System.out.println("✅ Eye detector loaded");
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

        int containerWidth = Math.min(frameWidth + 100, 1000);
        int containerHeight = Math.min(frameHeight + 200, 800); // Reduced height since we removed button
        root.setPrefWidth(containerWidth);
        root.setPrefHeight(containerHeight);
        root.setPadding(new Insets(25));

        Label titleLabel = new Label(mode.equals("setup") ? "✨ Face ID Setup" : "🔐 Face ID Login");
        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, #0FA5A2, 10, 0, 0, 0);"
        );

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

        // Auto-capture countdown label
        Label countdownLabel = new Label("");
        countdownLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 24px; -fx-font-weight: bold;");

        HBox detectionInfo = new HBox(20);
        detectionInfo.setAlignment(Pos.CENTER);

        Label faceStatus = new Label("👤 Face: --");
        faceStatus.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px;");

        Label eyesStatus = new Label("👀 Eyes: --");
        eyesStatus.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px;");

        Label confidenceStatus = new Label("📊 Confidence: --");
        confidenceStatus.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px;");

        detectionInfo.getChildren().addAll(faceStatus, eyesStatus, confidenceStatus);
        statusCard.getChildren().addAll(statusLabel, countdownLabel, detectionInfo);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(60, 60);
        progressIndicator.setStyle(
                "-fx-progress-color: #0FA5A2;" +
                        "-fx-effect: dropshadow(gaussian, #0FA5A2, 10, 0, 0, 0);"
        );

        // Only Cancel button - NO CAPTURE BUTTON
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

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

        buttonBox.getChildren().addAll(cancelBtn);

        root.getChildren().addAll(titleLabel, cameraInfoBox, cameraContainer, statusCard, progressIndicator, buttonBox);

        startCamera(faceStatus, eyesStatus, confidenceStatus, countdownLabel, progressIndicator);

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

    private void startCamera(Label faceStatus, Label eyesStatus, Label confidenceStatus,
                             Label countdownLabel, ProgressIndicator progressIndicator) {
        if (cameraUtil.openCamera()) {
            statusLabel.setText("✅ Camera ready! Look at the camera");
            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");

            framesWithFace = 0;
            faceStable = false;
            autoCaptureTriggered = false;
            stableStartTime = 0;
            totalFramesProcessed = 0;

            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    Image frame = cameraUtil.captureJavaFXImage();
                    if (frame != null) {
                        cameraView.setImage(frame);
                        processFrameForDetection(frame, faceStatus, eyesStatus, confidenceStatus,
                                countdownLabel, progressIndicator);
                    }
                }
            };
            animationTimer.start();
        } else {
            statusLabel.setText("❌ Failed to open camera!");
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px; -fx-font-weight: bold;");
        }
    }

    private void processFrameForDetection(Image frame, Label faceStatus, Label eyesStatus,
                                          Label confidenceStatus, Label countdownLabel,
                                          ProgressIndicator progressIndicator) {
        if (!detectorsLoaded) return;

        try {
            totalFramesProcessed++;

            java.awt.image.BufferedImage awtImage = SwingFXUtils.fromFXImage(frame, null);
            if (awtImage == null) return;

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

            if (currentFrame != null && !currentFrame.isNull()) {
                currentFrame.release();
            }
            currentFrame = mat.clone();

            // Convert to grayscale
            Mat gray = new Mat();
            cvtColor(mat, gray, COLOR_BGR2GRAY);

            // Detect face
            boolean faceFound = detectFaceWithMultipleMethods(gray);

            GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());

            if (faceFound && detectedFace != null) {
                framesWithFace++;

                // Calculate confidence
                double frameArea = frameWidth * frameHeight;
                double faceArea = detectedFace.width() * detectedFace.height();
                double idealFaceArea = frameArea * 0.3;
                double sizeRatio = faceArea / idealFaceArea;
                double sizeScore = Math.min(sizeRatio, 1.5) / 1.5;

                double centerX = frameWidth / 2;
                double centerY = frameHeight / 2;
                double faceCenterX = detectedFace.x() + detectedFace.width() / 2;
                double faceCenterY = detectedFace.y() + detectedFace.height() / 2;

                double maxDist = Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2));
                double distance = Math.sqrt(Math.pow(faceCenterX - centerX, 2) + Math.pow(faceCenterY - centerY, 2));
                double positionScore = 1.0 - (distance / maxDist);

                double stabilityScore = Math.min((double) framesWithFace / 30, 1.0);

                detectionConfidence = (sizeScore * 0.4 + positionScore * 0.3 + stabilityScore * 0.3) * 100;

                // Draw ONLY the guide oval (no blue rectangle)
                gc.setStroke(Color.rgb(255, 255, 255, 0.3));
                gc.setLineWidth(1);
                gc.setLineDashes(5);

                double idealWidth = Math.sqrt(idealFaceArea) * 1.2;
                double idealHeight = idealWidth * 1.2;
                gc.strokeOval(centerX - idealWidth/2, centerY - idealHeight/2, idealWidth, idealHeight);

                // Draw face center point (small dot)
                gc.setFill(Color.rgb(254, 199, 76, 0.8));
                gc.fillOval(faceCenterX - 2, faceCenterY - 2, 4, 4);

                // Check if face is stable enough for auto-capture
                if (detectionConfidence > 60 && !autoCaptureTriggered) {
                    if (framesWithFace > 15) {
                        if (stableStartTime == 0) {
                            stableStartTime = System.currentTimeMillis();
                        } else {
                            long stableDuration = System.currentTimeMillis() - stableStartTime;
                            long remainingSeconds = (AUTO_CAPTURE_DELAY - stableDuration) / 1000;

                            if (stableDuration >= AUTO_CAPTURE_DELAY) {
                                // Time's up - trigger auto-capture
                                autoCaptureTriggered = true;
                                javafx.application.Platform.runLater(() -> {
                                    progressIndicator.setVisible(true);
                                    captureFace(progressIndicator);
                                });
                            } else {
                                // Update countdown
                                String countdownText = String.format("⏱️ Capturing in %d...", remainingSeconds + 1);
                                javafx.application.Platform.runLater(() -> {
                                    countdownLabel.setText(countdownText);
                                });
                            }
                        }
                    }
                } else if (detectionConfidence <= 60) {
                    stableStartTime = 0;
                    javafx.application.Platform.runLater(() -> {
                        countdownLabel.setText("");
                    });
                }

                // Update status
                double finalConfidence = detectionConfidence;

                javafx.application.Platform.runLater(() -> {
                    faceStatus.setText("👤 Face: Detected");
                    faceStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;");

                    eyesStatus.setText("👀 Eyes: " + (detectedEyes != null ? detectedEyes.length : 0) + " detected");
                    eyesStatus.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");

                    confidenceStatus.setText("📊 Confidence: " + String.format("%.1f%%", finalConfidence));
                    confidenceStatus.setStyle(finalConfidence > 60 ?
                            "-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold;" :
                            "-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");

                    if (!autoCaptureTriggered) {
                        if (finalConfidence > 60 && framesWithFace > 15) {
                            statusLabel.setText("✅ Face stable - Auto-capture in 3s");
                        } else if (faceFound) {
                            statusLabel.setText("👤 Hold still...");
                        }
                    }
                });

            } else {
                framesWithFace = Math.max(0, framesWithFace - 1);
                stableStartTime = 0;
                detectedFace = null;
                detectionConfidence = 0;

                // Draw only the guide oval
                gc.setStroke(Color.rgb(255, 255, 255, 0.3));
                gc.setLineWidth(1);
                gc.setLineDashes(5);

                double centerX = frameWidth / 2;
                double centerY = frameHeight / 2;
                double size = Math.min(frameWidth, frameHeight) / 2.5;

                gc.strokeOval(centerX - size/2, centerY - size/2, size, size);

                javafx.application.Platform.runLater(() -> {
                    faceStatus.setText("👤 Face: Not detected");
                    faceStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                    eyesStatus.setText("👀 Eyes: --");
                    eyesStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                    confidenceStatus.setText("📊 Confidence: 0%");
                    confidenceStatus.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold;");
                    statusLabel.setText("👤 Position your face in the oval");
                    countdownLabel.setText("");
                });
            }

            gray.close();

        } catch (Exception e) {
            System.err.println("Error in face detection: " + e.getMessage());
        }
    }

    private boolean detectFaceWithMultipleMethods(Mat grayImage) {
        if (faceDetector.empty()) return false;

        int[][] paramSets = {
                {3, 100, 1},  // min neighbors 3, min size 100, scale 1.1
                {2, 80, 1},   // min neighbors 2, min size 80, scale 1.1
                {1, 60, 1},   // min neighbors 1, min size 60, scale 1.1
                {2, 60, 2},   // min neighbors 2, min size 60, scale 1.05
                {1, 40, 2}    // min neighbors 1, min size 40, scale 1.05
        };

        for (int[] params : paramSets) {
            RectVector detections = new RectVector();

            double scaleFactor = params[2] == 1 ? 1.1 : 1.05;

            faceDetector.detectMultiScale(
                    grayImage,
                    detections,
                    scaleFactor,
                    params[0],
                    0,
                    new Size(params[1], params[1]),
                    new Size(grayImage.cols(), grayImage.rows())
            );

            if (!detections.empty()) {
                detectedFace = detections.get(0);
                for (int i = 1; i < detections.size(); i++) {
                    Rect r = detections.get(i);
                    if (r.width() * r.height() > detectedFace.width() * detectedFace.height()) {
                        detectedFace = r;
                    }
                }
                return true;
            }
        }

        return false;
    }

    private void captureFace(ProgressIndicator progressIndicator) {
        statusLabel.setText("📸 Capturing face...");

        new Thread(() -> {
            try {
                if (currentFrame != null && !currentFrame.isNull() && detectedFace != null) {
                    System.out.println("Auto-capturing face region...");

                    // Use the face that was already detected by the dialog
                    int margin = 30;
                    int x = Math.max(0, detectedFace.x() - margin);
                    int y = Math.max(0, detectedFace.y() - margin);
                    int w = Math.min(currentFrame.cols() - x, detectedFace.width() + 2 * margin);
                    int h = Math.min(currentFrame.rows() - y, detectedFace.height() + 2 * margin);

                    System.out.println("Face region: x=" + x + " y=" + y + " w=" + w + " h=" + h);

                    Rect faceRect = new Rect(x, y, w, h);
                    Mat faceROI = new Mat(currentFrame, faceRect);

                    // Convert the face ROI directly to bytes - THIS IS THE FACE, no need to re-detect
                    byte[] imageBytes = matToByteArray(faceROI);

                    if (imageBytes != null && imageBytes.length > 0) {
                        System.out.println("Face captured: " + imageBytes.length + " bytes");

                        // Save directly - this IS the face data
                        capturedFace = imageBytes;

                        javafx.application.Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
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
                        });
                    } else {
                        System.out.println("❌ Failed to convert face region to bytes");
                        resetAfterFailedCapture(progressIndicator);
                    }
                    faceROI.close();
                } else {
                    System.out.println("❌ No face detected in current frame");
                    resetAfterFailedCapture(progressIndicator);
                }
            } catch (Exception e) {
                e.printStackTrace();
                resetAfterFailedCapture(progressIndicator);
            }
        }).start();
    }

    private void resetAfterFailedCapture(ProgressIndicator progressIndicator) {
        javafx.application.Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            statusLabel.setText("❌ Capture failed. Try again.");
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-size: 16px; -fx-font-weight: bold;");
            autoCaptureTriggered = false;
            stableStartTime = 0;
        });
    }

    private byte[] matToByteArray(Mat mat) {
        try {
            // Method 1: Try using imencode
            MatVector buf = new MatVector();
            boolean success = imencode(".jpg", mat, buf.asByteBuffer());

            if (success && !buf.empty()) {
                Mat encoded = buf.get(0);
                if (encoded != null && !encoded.empty()) {
                    int size = (int) (encoded.total() * encoded.channels());
                    if (size > 0) {
                        byte[] result = new byte[size];
                        encoded.data().get(result);
                        System.out.println("✅ Method 1 succeeded: " + result.length + " bytes");
                        return result;
                    }
                }
            }

            // Method 2: Fallback to BufferedImage
            System.out.println("⚠️ Method 1 failed, trying Method 2...");

            int width = mat.cols();
            int height = mat.rows();
            int channels = mat.channels();

            BytePointer dataPointer = mat.data();
            byte[] data = new byte[width * height * channels];
            dataPointer.get(data);

            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                    width, height, java.awt.image.BufferedImage.TYPE_3BYTE_BGR);

            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte b = data[idx++];
                    byte g = data[idx++];
                    byte r = data[idx++];
                    int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                    bufferedImage.setRGB(x, y, rgb);
                }
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "jpg", baos);
            byte[] result = baos.toByteArray();

            System.out.println("✅ Method 2 succeeded: " + result.length + " bytes");
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error in matToByteArray: " + e.getMessage());
            e.printStackTrace();
            return null;
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
}