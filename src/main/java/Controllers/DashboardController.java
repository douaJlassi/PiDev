package Controllers;


import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.scene.control.ButtonType;
import java.io.IOException;
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
        }
    }








    private void openReservationWindow(Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReservationView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            ReservationController controller = loader.getController();
            controller.setActivite(activite);
            controller.setStage(stage);


            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());


            stage.setTitle("Réserver - " + activite.getTitre());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);

            controller.setStage(stage);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }










    private void afficherActivites(List<Activite> activites) {
        activitiesFlowPane.getChildren().clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");

        for (Activite a : activites) {
            VBox card = new VBox();
            card.setPrefWidth(300);
            card.getStyleClass().add("card");


            ImageView imageView = new ImageView();
            imageView.setFitWidth(300);
            imageView.setFitHeight(160);


            try {
                String imageName = a.getImage();

                if (imageName == null || imageName.trim().isEmpty()) {
                    imageName = "default.jpg";
                }


                if (!imageName.contains(".")) {
                    imageName += ".jpg";
                }

                String path = "/images/" + imageName;
                java.net.URL resource = getClass().getResource(path);

                if (resource != null) {

                    imageView.setImage(new Image(resource.toExternalForm(), true));
                } else {

                    System.out.println("⚠️ Image not found: " + path + ". Trying default.jpg...");
                    java.net.URL defaultUrl = getClass().getResource("/images/default.jpg");

                    if (defaultUrl != null) {
                        imageView.setImage(new Image(defaultUrl.toExternalForm()));
                    } else {

                        System.err.println("❌ ERROR: default.jpg is missing from resources/images folder!");
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load image for: " + a.getTitre());
            }

            Rectangle clip = new Rectangle(300, 160);
            clip.setArcWidth(30);
            clip.setArcHeight(30);
            imageView.setClip(clip);

            StackPane imageStack = new StackPane();
            imageStack.setPrefHeight(160);
            imageStack.getStyleClass().add("card-image");


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
            reserver.setOnAction(event -> openReservationWindow(a));

            content.getChildren().addAll(titre, lieu, date, priceRow, reserver);
            card.getChildren().addAll(imageStack, content);

            activitiesFlowPane.getChildren().add(card);
        }
    }
}