package tn.esprit.projet.utils;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class FaceCaptureDialog {

    private CameraUtil cameraUtil;
    private ImageView cameraView;
    private Stage dialogStage;
    private byte[] capturedFace;
    private Label statusLabel;
    private AnimationTimer animationTimer;
    private int cameraIndex;

    public FaceCaptureDialog() {
        this(0); // Default to first camera
    }

    public FaceCaptureDialog(int cameraIndex) {
        this.cameraIndex = cameraIndex;
        this.cameraUtil = new CameraUtil(cameraIndex);
    }

    public byte[] showAndWait(String mode) {
        createDialog(mode);
        dialogStage.showAndWait();
        return capturedFace;
    }

    private void createDialog(String mode) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);

        // Main container
        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20; -fx-background-radius: 15;");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(650);
        root.setPrefHeight(600);

        // Title
        Label titleLabel = new Label(mode.equals("setup") ? "Face ID Setup" : "Face ID Login");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Camera info
        Label cameraInfoLabel = new Label("Using: " + cameraUtil.getCameraName());
        cameraInfoLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 12px;");

        // Camera view with frame
        VBox cameraFrame = new VBox(10);
        cameraFrame.setStyle("-fx-background-color: #16213e; -fx-background-radius: 15; -fx-padding: 15;");
        cameraFrame.setAlignment(Pos.CENTER);

        cameraView = new ImageView();
        cameraView.setFitWidth(550);
        cameraView.setFitHeight(380);
        cameraView.setPreserveRatio(true);
        cameraView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,255,255,0.3), 10, 0, 0, 0); -fx-background-radius: 10;");

        // Face detection guide overlay
        Label guideLabel = new Label("Position your face in the center");
        guideLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px; -fx-font-weight: bold;");

        cameraFrame.getChildren().addAll(cameraView, guideLabel);

        // Status label
        statusLabel = new Label("Initializing camera...");
        statusLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px;");

        // Progress indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(50, 50);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button captureBtn = new Button(mode.equals("setup") ? "Capture Face" : "Verify Face");
        captureBtn.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 40; -fx-background-radius: 25; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 40; -fx-background-radius: 25; -fx-cursor: hand;");

        buttonBox.getChildren().addAll(captureBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, cameraInfoLabel, cameraFrame, statusLabel, progressIndicator, buttonBox);

        // Start camera with animation timer
        startCamera();

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
        dialogStage.setScene(scene);

        // Handle window close
        dialogStage.setOnCloseRequest(e -> stopCamera());
    }

    private void startCamera() {
        if (cameraUtil.openCamera()) {
            statusLabel.setText("Camera opened. Look at the camera...");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");

            // Use AnimationTimer for smooth video rendering
            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    Image frame = cameraUtil.captureJavaFXImage();
                    if (frame != null) {
                        cameraView.setImage(frame);
                    }
                }
            };
            animationTimer.start();
        } else {
            statusLabel.setText("Failed to open camera!");
            statusLabel.setStyle("-fx-text-fill: #ff5e62;");
        }
    }

    private void stopCamera() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        cameraUtil.closeCamera();
    }

    private void captureFaceForSetup(ProgressIndicator progressIndicator, Button captureBtn, Button cancelBtn) {
        statusLabel.setText("Capturing face...");

        // Run face detection in background thread
        new Thread(() -> {
            try {
                // Capture image
                java.awt.image.BufferedImage bufferedImage = cameraUtil.captureImage();
                if (bufferedImage != null) {
                    // Convert to bytes
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "jpg", baos);
                    byte[] imageBytes = baos.toByteArray();

                    // Extract face features
                    FaceRecognitionUtil faceUtil = new FaceRecognitionUtil();
                    byte[] faceFeatures = faceUtil.extractFaceFeatures(imageBytes);

                    javafx.application.Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        captureBtn.setDisable(false);
                        cancelBtn.setDisable(false);

                        if (faceFeatures != null) {
                            capturedFace = faceFeatures;
                            statusLabel.setText("✓ Face captured successfully!");
                            statusLabel.setStyle("-fx-text-fill: #2ecc71;");

                            // Close after 1 second
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                    javafx.application.Platform.runLater(() -> {
                                        stopCamera();
                                        dialogStage.close();
                                    });
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }).start();
                        } else {
                            statusLabel.setText("✗ No face detected! Please try again.");
                            statusLabel.setStyle("-fx-text-fill: #ff5e62;");
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    captureBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    statusLabel.setText("Error capturing face: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ff5e62;");
                });
            }
        }).start();
    }
    private void captureFaceForLogin(ProgressIndicator progressIndicator, Button captureBtn, Button cancelBtn) {
        new Thread(() -> {
            try {
                // Capture multiple images and take the best one
                java.awt.image.BufferedImage bestImage = null;
                double bestSharpness = 0;

                for (int i = 0; i < 5; i++) {
                    java.awt.image.BufferedImage image = cameraUtil.captureImage();
                    if (image != null) {
                        // Calculate sharpness (simple variance of Laplacian)
                        Mat mat = bufferedImageToMat(image);
                        Mat gray = new Mat();
                        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
                        Mat laplacian = new Mat();
                        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
                        Core.MinMaxLocResult mmr = Core.minMaxLoc(laplacian);
                        double sharpness = mmr.maxVal - mmr.minVal;

                        System.out.println("📸 Image " + i + " sharpness: " + sharpness);

                        if (sharpness > bestSharpness) {
                            bestSharpness = sharpness;
                            bestImage = image;
                        }
                    }
                    Thread.sleep(200);
                }

                if (bestImage != null) {
                    // Enhance image
                    bestImage = enhanceImage(bestImage);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bestImage, "jpg", baos);
                    capturedFace = baos.toByteArray();

                    System.out.println("✅ Best image selected with sharpness: " + bestSharpness);
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
                    statusLabel.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private java.awt.image.BufferedImage enhanceImage(java.awt.image.BufferedImage image) {
        // Convert to Mat
        Mat mat = bufferedImageToMat(image);

        // Apply Gaussian blur to reduce noise
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(mat, blurred, new Size(3, 3), 0);

        // Increase contrast using CLAHE
        Mat lab = new Mat();
        Imgproc.cvtColor(blurred, lab, Imgproc.COLOR_BGR2Lab);
        java.util.List<Mat> labChannels = new java.util.ArrayList<>();
        Core.split(lab, labChannels);

        org.opencv.core.Size size = new org.opencv.core.Size(8, 8);
        CLAHE clahe = Imgproc.createCLAHE(2.0, size);
        clahe.apply(labChannels.get(0), labChannels.get(0));

        Core.merge(labChannels, lab);
        Mat result = new Mat();
        Imgproc.cvtColor(lab, result, Imgproc.COLOR_Lab2BGR);

        // Convert back to BufferedImage
        return matToBufferedImage(result);
    }

    private Mat bufferedImageToMat(java.awt.image.BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] bytes = baos.toByteArray();
            return Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            e.printStackTrace();
            return new Mat();
        }
    }

    private java.awt.image.BufferedImage matToBufferedImage(Mat mat) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, mob);
        byte[] byteArray = mob.toArray();
        try {
            return ImageIO.read(new ByteArrayInputStream(byteArray));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}