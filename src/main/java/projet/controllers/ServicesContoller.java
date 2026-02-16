package projet.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import projet.entites.Hotel;
import projet.services.HotelService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ServicesContoller implements Initializable {

    @FXML
    private FlowPane cardsContainer;

    @FXML
    private TextField txtSearch;

    private List<Hotel> allServices;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Fetch Data (Replace with ServiceService.getAll())
        allServices = getDummyData();

        // 2. Render Cards
        renderServices(allServices);
    }

    @FXML
    private void handleSearch() {
        String query = txtSearch.getText().toLowerCase();
        List<Hotel> filtered = allServices.stream()
                .filter(s -> s.getNom().toLowerCase().contains(query)
                        )
                .toList();
        renderServices(filtered);
    }

    private void renderServices(List<Hotel> services) {
        cardsContainer.getChildren().clear();

        for (Hotel s : services) {
            VBox card = createServiceCard(s);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createServiceCard(Hotel s) {
        // --- CONTAINER ---
        VBox card = new VBox();
        card.getStyleClass().add("service-card");
        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setSpacing(10);

        // --- IMAGE PLACEHOLDER ---
        // Since you don't have DB images yet, we use a placeholder logic
        StackPane imgContainer = new StackPane();
        imgContainer.getStyleClass().add("card-img-container");

        // Try to load image, otherwise standard icon
        ImageView imageView = new ImageView();
        imageView.setFitHeight(120);
        imageView.setFitWidth(220);
        imageView.setPreserveRatio(false); // Fill the box

        // Use a default image from resources if available, else a colored rect
        try {
            // Put a "default_service.jpg" in your images folder
            Image img = new Image(getClass().getResourceAsStream("/images/default_service.jpg"));
            imageView.setImage(img);
        } catch (Exception e) {
            // If no image found, just leave empty or set style
            imgContainer.setStyle("-fx-background-color: #e0e0e0;");
        }

        imgContainer.getChildren().add(imageView);

        // --- DETAILS ---
        VBox details = new VBox();
        details.setPadding(new Insets(10));
        details.setSpacing(5);

        Label type = new Label("hotel");
        type.getStyleClass().add("card-type");

        Label name = new Label(s.getNom());
        name.getStyleClass().add("card-title-text");
        name.setWrapText(true);

        Label price = new Label(s.getPrix() + " TND");
        price.getStyleClass().add("card-price");

        // --- ACTION BUTTONS ---
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setSpacing(10);

        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("btn-card-action");

        Button btnDelete = new Button("🗑");
        btnDelete.getStyleClass().addAll("btn-card-action", "btn-card-delete");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(price, spacer, btnEdit, btnDelete);

        details.getChildren().addAll(type, name, actions);
        card.getChildren().addAll(imgContainer, details);

        return card;
    }

    private List<Hotel> getDummyData() {
        HotelService hotelService = new HotelService();
        List<Hotel> list = new ArrayList<>();
        try {
            list=hotelService.selectALL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}