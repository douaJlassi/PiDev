package utils;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import entities.Person;

public class QRCodeDialog {

    public void show(Person user) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        // Main container
        VBox root = new VBox(20);
        root.setStyle(
                "-fx-background-color: rgba(20, 20, 30, 0.95);" +
                        "-fx-background-radius: 30;" +
                        "-fx-border-color: rgba(102,126,234,0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 30;" +
                        "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.5), 20, 0, 0, 0);" +
                        "-fx-padding: 30;"
        );
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(450);
        root.setPrefHeight(600);

        // Title
        Label titleLabel = new Label("📱 Your QR Code");
        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, #667eea, 10, 0, 0, 0);"
        );

        // Subtitle
        Label subtitleLabel = new Label("Scan to get account information");
        subtitleLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px;");

        // Generate QR Code
        Image qrImage = QRCodeUtil.generateUserQRCode(
                user.getUsername(),
                user.getEmail(),
                user.getPassword()
        );

        ImageView qrImageView = new ImageView(qrImage);
        qrImageView.setFitWidth(250);
        qrImageView.setFitHeight(250);
        qrImageView.setPreserveRatio(true);
        qrImageView.setStyle(
                "-fx-effect: dropshadow(gaussian, white, 10, 0, 0, 0);" +
                        "-fx-background-color: white;" +
                        "-fx-padding: 10;"
        );

        // User info
        VBox infoBox = new VBox(10);
        infoBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 15;"
        );
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label usernameLabel = new Label("👤 Username: " + user.getUsername());
        usernameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label emailLabel = new Label("📧 Email: " + user.getEmail());
        emailLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label passwordLabel = new Label("🔑 Password: " + maskPassword(user.getPassword()));
        passwordLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(usernameLabel, emailLabel, passwordLabel);

        // Buttons
        Button sendEmailBtn = new Button("📧 Send to Email");
        sendEmailBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #0FA5A2, #1D4D7C);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 25;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,165,162,0.5), 10, 0, 0, 0);"
        );

        Button closeBtn = new Button("❌ Close");
        closeBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff5e62, #d43f3f);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 25;" +
                        "-fx-cursor: hand;"
        );

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(sendEmailBtn, closeBtn);

        root.getChildren().addAll(titleLabel, subtitleLabel, qrImageView, infoBox, buttonBox);

        // Send email action
        sendEmailBtn.setOnAction(e -> {
            boolean sent = EmailService.sendQRCodeEmail(
                    user.getEmail(),
                    user.getUsername(),
                    QRCodeUtil.generateUserQRCodeBytes(user.getUsername(), user.getEmail(), user.getPassword())
            );

            if (sent) {
                showAlert("Success", "QR Code sent to your email!");
            } else {
                showAlert("Error", "Failed to send QR Code email.");
            }
        });

        // Close action
        closeBtn.setOnAction(e -> dialogStage.close());

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) return "";
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                content.contains("Success") ?
                        javafx.scene.control.Alert.AlertType.INFORMATION :
                        javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}