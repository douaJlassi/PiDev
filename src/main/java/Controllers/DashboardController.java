package Controllers;


import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;


import gestion_activite.Activite; // à créer si pas encore fait
import Services.ActiviteService; // adaptez le package selon votre structure
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class DashboardController {

    @FXML
    private FlowPane activitiesFlowPane;

    private ActiviteService activiteService;

    public DashboardController() {
        activiteService = new ActiviteService();
    }

    @FXML
    public void initialize() {
        try {
            List<Activite> activites = activiteService.selectALL();
            afficherActivites(activites);
        } catch (SQLException e) {
            e.printStackTrace();
            // Afficher un message d'erreur à l'utilisateur
        }
    }

    private void afficherActivites(List<Activite> activites) {
        activitiesFlowPane.getChildren().clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");

        for (Activite a : activites) {
            VBox card = new VBox();
            card.setPrefWidth(300);
            card.getStyleClass().add("card");

            // 1. Create the ImageView
            ImageView imageView = new ImageView();
            imageView.setFitWidth(300);
            imageView.setFitHeight(160);

            // Load image from your folder
            try {
                String imageName = a.getImage();

                // 1. If null or empty, use the string "default.jpg"
                if (imageName == null || imageName.trim().isEmpty()) {
                    imageName = "default.jpg";
                }

                // 2. Automatically add .jpg if the extension is missing (e.g., "safari" -> "safari.jpg")
                if (!imageName.contains(".")) {
                    imageName += ".jpg";
                }

                String path = "/images/" + imageName;
                java.net.URL resource = getClass().getResource(path);

                if (resource != null) {
                    // Specific image found
                    imageView.setImage(new Image(resource.toExternalForm(), true));
                } else {
                    // Specific image NOT found, try to load the default.jpg
                    System.out.println("⚠️ Image not found: " + path + ". Trying default.jpg...");
                    java.net.URL defaultUrl = getClass().getResource("/images/default.jpg");

                    if (defaultUrl != null) {
                        imageView.setImage(new Image(defaultUrl.toExternalForm()));
                    } else {
                        // This means the file "default.jpg" is physically missing from src/main/resources/images/
                        System.err.println("❌ ERROR: default.jpg is missing from resources/images folder!");
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load image for: " + a.getTitre());
            }
            // 2. Add rounded corners to the image (Clip)
            Rectangle clip = new Rectangle(300, 160);
            clip.setArcWidth(30);  // Adjust to match your CSS card corners
            clip.setArcHeight(30);
            imageView.setClip(clip);

            // StackPane with image and badge
            StackPane imageStack = new StackPane();
            imageStack.setPrefHeight(160);
            imageStack.getStyleClass().add("card-image");

            // Add the image FIRST, then the badge so it stays on top
            imageStack.getChildren().addAll(imageView);

            String status = a.getStatut();
            Label badge = new Label(status != null ? status : "Activité");

            if (status != null && status.equalsIgnoreCase("Annulé")) {
                badge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            }



            badge.getStyleClass().add("badge-category");
            StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
            imageStack.getChildren().add(badge);

            // ... rest of your content (titre, lieu, date, prixRow, reserver) ...
            VBox content = new VBox(8);
            content.getStyleClass().add("card-content");

            Text titre = new Text(a.getTitre());
            titre.getStyleClass().add("card-title");
            Label lieu = new Label("📍 " + a.getLieu());
            lieu.getStyleClass().add("card-location");
            Label date = new Label("📅 " + dateFormat.format(a.getDateActivite()));
            date.getStyleClass().add("card-date");

            HBox priceRow = new HBox();
            priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Text prix = new Text(String.format("%.0f DT", a.getPrix()));
            prix.getStyleClass().add("card-price");
            Label note = new Label("★ 4.9");
            note.getStyleClass().add("card-rating");
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            Label duree = new Label(a.getDureParJour() + (a.getDureParJour() > 1 ? " jours" : " jour"));
            duree.getStyleClass().add("card-duration");
            priceRow.getChildren().addAll(prix, note, spacer, duree);

            Button reserver = new Button("Réserver");
            reserver.getStyleClass().add("btn-reserve");
            reserver.setMaxWidth(Double.MAX_VALUE);

            content.getChildren().addAll(titre, lieu, date, priceRow, reserver);
            card.getChildren().addAll(imageStack, content);

            activitiesFlowPane.getChildren().add(card);
        }
    }
}