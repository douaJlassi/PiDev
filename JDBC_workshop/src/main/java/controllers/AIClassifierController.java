package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import entities.Person;
import utils.AIClassifierService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

public class AIClassifierController implements Initializable {

    @FXML
    private VBox mainContainer;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Button uploadBtn;
    @FXML
    private Button backBtn;
    @FXML
    private VBox uploadArea;
    @FXML
    private Label uploadPrompt;
    @FXML
    private ImageView previewImage;
    @FXML
    private Button analyzeBtn;
    @FXML
    private VBox resultContainer;
    @FXML
    private Label resultTitle;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label confidenceLabel;
    @FXML
    private Label placesTitle;
    @FXML
    private FlowPane placesGrid;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label statusLabel;

    private Person currentUser;
    private AIClassifierService aiService;
    private byte[] selectedImageBytes;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        aiService = AIClassifierService.getInstance();

        setupUI();
        setupDragAndDrop();
    }

    public void setUserData(Person user) {
        this.currentUser = user;
    }

    private void setupUI() {
        // Initially hide result container
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);
        loadingIndicator.setVisible(false);
        analyzeBtn.setDisable(true);

        // Style upload area
        uploadArea.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #0FA5A2;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-style: dashed;" +
                        "-fx-border-radius: 20;"
        );

        // Button actions
        uploadBtn.setOnAction(e -> chooseImage());
        analyzeBtn.setOnAction(e -> analyzeImage());
        backBtn.setOnAction(e -> goBack());
    }

    private void setupDragAndDrop() {
        uploadArea.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                uploadArea.setStyle(
                        "-fx-background-color: rgba(15,165,162,0.2);" +
                                "-fx-background-radius: 20;" +
                                "-fx-border-color: #0FA5A2;" +
                                "-fx-border-width: 3;" +
                                "-fx-border-style: solid;" +
                                "-fx-border-radius: 20;"
                );
            }
            event.consume();
        });

        uploadArea.setOnDragExited(event -> {
            uploadArea.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.1);" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: #0FA5A2;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-style: dashed;" +
                            "-fx-border-radius: 20;"
            );
            event.consume();
        });

        uploadArea.setOnDragDropped(event -> {
            var db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                loadImage(file);
                event.setDropCompleted(true);
            }
            event.consume();
        });
    }

    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an image of a place");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(uploadBtn.getScene().getWindow());
        if (selectedFile != null) {
            loadImage(selectedFile);
        }
    }

    private void loadImage(File file) {
        try {
            selectedImageBytes = Files.readAllBytes(file.toPath());
            Image image = new Image(new ByteArrayInputStream(selectedImageBytes));
            previewImage.setImage(image);
            previewImage.setFitWidth(300);
            previewImage.setFitHeight(200);
            previewImage.setPreserveRatio(true);

            uploadPrompt.setText("Image loaded: " + file.getName());
            analyzeBtn.setDisable(false);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load image: " + e.getMessage());
        }
    }

    private void analyzeImage() {
        if (selectedImageBytes == null) return;

        // Show loading
        loadingIndicator.setVisible(true);
        analyzeBtn.setDisable(true);
        resultContainer.setVisible(false);
        statusLabel.setText("Analyzing image...");

        // Run analysis in background
        new Thread(() -> {
            AIClassifierService.ClassificationResult result = aiService.classifyImage(selectedImageBytes);

            javafx.application.Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                analyzeBtn.setDisable(false);
                statusLabel.setText("");

                displayResults(result);
            });
        }).start();
    }

    private void displayResults(AIClassifierService.ClassificationResult result) {
        resultContainer.setVisible(true);
        resultContainer.setManaged(true);

        String category = result.getCategory();
        String emoji = result.getCategoryEmoji();

        resultTitle.setText("✨ Analysis Complete!");
        categoryLabel.setText(emoji + "  " + category);
        confidenceLabel.setText("Confidence: " + result.getFormattedConfidence());

        // Color code based on category
        String color = getCategoryColor(category);
        categoryLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");

        // Display places
        placesGrid.getChildren().clear();
        List<AIClassifierService.Place> places = result.getPlaces();

        if (places.isEmpty()) {
            Label noPlaces = new Label("No places found for this category.");
            noPlaces.setStyle("-fx-text-fill: #999; -fx-font-size: 16px;");
            placesGrid.getChildren().add(noPlaces);
        } else {
            for (AIClassifierService.Place place : places) {
                VBox placeCard = createPlaceCard(place);
                placesGrid.getChildren().add(placeCard);
            }
        }
    }

    private String getCategoryColor(String category) {
        switch (category) {
            case "FOREST": return "#2ecc71";
            case "SNOW": return "#3498db";
            case "DESERT": return "#e67e22";
            case "BEACH": return "#f1c40f";
            case "CITY": return "#9b59b6";
            default: return "#0FA5A2";
        }
    }

    private VBox createPlaceCard(AIClassifierService.Place place) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 15;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 15;"
        );
        card.setPrefWidth(280);
        card.setPrefHeight(180);

        // Placeholder for place image (you can add actual images later)
        Rectangle imagePlaceholder = new Rectangle(250, 100);
        imagePlaceholder.setFill(Color.web(getCategoryColor(place.getCategory())));
        imagePlaceholder.setArcWidth(10);
        imagePlaceholder.setArcHeight(10);

        Label nameLabel = new Label(place.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        Label locationLabel = new Label("📍 " + place.getLocation());
        locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Label descLabel = new Label(place.getDescription());
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        card.getChildren().addAll(imagePlaceholder, nameLabel, locationLabel, descLabel);

        // Add hover effect
        card.setOnMouseEntered(e ->
                card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, " + getCategoryColor(place.getCategory()) + "80, 15, 0, 0, 0); -fx-border-color: " + getCategoryColor(place.getCategory()) + "; -fx-border-radius: 15; -fx-border-width: 2;")
        );
        card.setOnMouseExited(e ->
                card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-border-color: #e0e0e0; -fx-border-radius: 15; -fx-border-width: 1;")
        );

        return card;
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainPage.fxml"));
            Parent mainRoot = loader.load();

            MainPageController mainController = loader.getController();
            mainController.setUserData(currentUser);

            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(mainRoot));
            stage.setTitle("Main Page - " + currentUser.getUsername());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to main page: " + e.getMessage());
        }
    }
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadPlaceImage(AIClassifierService.Place place, ImageView imageView) {
        if (place.getImageUrl() != null && !place.getImageUrl().isEmpty()) {
            new Thread(() -> {
                try {
                    URL url = new URL(place.getImageUrl());
                    BufferedImage img = ImageIO.read(url);
                    if (img != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(img, "jpg", baos);
                        Image fxImage = new Image(new ByteArrayInputStream(baos.toByteArray()));

                        javafx.application.Platform.runLater(() -> {
                            imageView.setImage(fxImage);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}