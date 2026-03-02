package utils;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CameraSelectionDialog {

    private int selectedCameraIndex = 0;
    private boolean confirmed = false;

    public int showAndWait() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 30; -fx-background-radius: 15;");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(400);
        root.setPrefHeight(250);

        Label titleLabel = new Label("Select Camera");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label instructionLabel = new Label("Choose which camera to use for Face ID:");
        instructionLabel.setStyle("-fx-text-fill: #FEC74C; -fx-font-size: 14px;");

        ComboBox<String> cameraCombo = new ComboBox<>();
        cameraCombo.getItems().addAll(CameraUtil.getCameraNames());
        cameraCombo.getSelectionModel().selectFirst();
        cameraCombo.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 350;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button selectBtn = new Button("Select Camera");
        selectBtn.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand;");

        buttonBox.getChildren().addAll(selectBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, instructionLabel, cameraCombo, buttonBox);

        selectBtn.setOnAction(e -> {
            selectedCameraIndex = cameraCombo.getSelectionModel().getSelectedIndex();
            confirmed = true;
            dialogStage.close();
        });

        cancelBtn.setOnAction(e -> {
            confirmed = false;
            dialogStage.close();
        });

        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return confirmed ? selectedCameraIndex : -1;
    }
}