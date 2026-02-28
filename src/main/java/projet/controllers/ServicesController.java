package projet.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import projet.entites.Hotel;
import projet.entites.service;
import projet.entites.user;
import projet.entites.vol;
import projet.services.HotelService;
import projet.services.ServiceService;
import projet.services.SupabaseStorageService;
import projet.services.VolService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ServicesController implements Initializable {
   // user connectedUser=new user("achref","souli","admin");
    user connectedUser=new user("achref","souli","admin");

    @FXML
    private FlowPane cardsContainer;

    @FXML
    private TextField txtSearch;

    private List<service> allServices;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        allServices = getDummyData();
        renderServices(allServices);
    }
    public void refreshServices() {
        allServices = getDummyData();
        renderServices(allServices);
    }

    @FXML
    private void handleSearch() {
        String query = txtSearch.getText().toLowerCase();
        List<service> filtered = allServices.stream()
                .filter(s -> s.getNom().toLowerCase().contains(query)
                        )
                .toList();
        renderServices(filtered);
    }

    private void renderServices(List<service> services) {
        cardsContainer.getChildren().clear();

        for (service s : services) {
            VBox card = createServiceCard(s);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createServiceCard(service s) {
        ServiceService Service = new ServiceService();

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
        imageView.setPreserveRatio(false);


        try {
            SupabaseStorageService storageService = new SupabaseStorageService();
            String signedUrl = storageService.getSignedUrl(s.getImage(), 3600);
            System.out.println(signedUrl);
            Image img = new Image(signedUrl);
            imageView.setImage(img);
        } catch (Exception e) {
            imgContainer.setStyle("-fx-background-color: #e0e0e0;");
        }

        imgContainer.getChildren().add(imageView);


        VBox details = new VBox();
        details.setPadding(new Insets(10));
        details.setSpacing(5);

        Label type = new Label(s.getType());
        type.getStyleClass().add("card-type");

        Label name = new Label(s.getNom());
        name.getStyleClass().add("card-title-text");
        name.setWrapText(true);

        Label price = new Label(s.getPrix() + " TND");
        price.getStyleClass().add("card-price");



        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setSpacing(10);
        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("btn-card-action");
        btnEdit.setOnAction(e -> {
         handleEditAction(s);
        });


        Button btnDelete = new Button("🗑");
        btnDelete.setOnAction(event -> {
            try {
                Service.deleteOne(s);
                refreshServices();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        if (connectedUser.getType().equals("user")) {
            btnDelete.setVisible(false);
            btnEdit.setVisible(false);
        }
        btnDelete.getStyleClass().addAll("btn-card-action", "btn-card-delete");
        card.setOnMouseClicked(event -> {showDetails(s);});
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(price, spacer, btnEdit, btnDelete);

        details.getChildren().addAll(type, name, actions);
        card.getChildren().addAll(imgContainer, details);

        return card;
    }

    // Inside ServicesController.java

    private void handleEditAction(service service) {

        Parent root;
        vol v;
        Hotel h;
        // === CASE 1: IT IS A VOL ===
        if (service.getType().equals("vol")) {
            // 1. Load the Vol FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/updateVol.fxml"));
            try {
                VolService volService = new VolService();
                try {
                     v = volService.selectByNom(service.getNom());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                root = loader.load();
                UpdateVolController controller = loader.getController();
                controller.setServiceData(v);
                cardsContainer.getScene().setRoot(root);
            } catch (IOException ex) {
                throw new RuntimeException(ex);

        }}

        // === CASE 2: IT IS A HOTEL ===
        else if (service.getType().equals("hotel")) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/updateHotel.fxml"));
            try {
                HotelService hotelService = new HotelService();
                try {
                    h=hotelService.selectOne(service.getNom());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                root = loader.load();
                UpdateHotelController controller = loader.getController();
                controller.setServiceData(h);
                controller.setServiceData(h);
                cardsContainer.getScene().setRoot(root);
            } catch (IOException ex) {
                throw new RuntimeException(ex);

            }
        }

        // === CASE 3: UNKNOWN TYPE ===
        else {
            System.out.println("Unknown service type");
            return;
        }
    }
    private List<service> getDummyData() {
        ServiceService Service = new ServiceService();
        List<service> list = new ArrayList<>();
        try {
            list=Service.selectALL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    private void showDetails(service s) {
        FXMLLoader loader ;
        Parent root;
        vol v;
        Hotel h;
        if (s.getType().equals("hotel")) {
           loader = new FXMLLoader(getClass().getResource("/hotelDetails.fxml"));
            try {
                HotelService hotelService = new HotelService();
                try {
                    h=hotelService.selectOne(s.getNom());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                root = loader.load();
                HotelDetailsController controller = loader.getController();
                controller.setHotelData(h);
                controller.setHotelData(h);
                cardsContainer.getScene().setRoot(root);
            } catch (IOException ex) {
                throw new RuntimeException(ex);

            }
        }
        else if (s.getType().equals("vol")) {
            loader = new FXMLLoader(getClass().getResource("/volsDetails.fxml"));
            try {
                VolService volService = new VolService();
                try {
                    v = volService.selectByNom(s.getNom());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                root = loader.load();
                VolDetailsController controller = loader.getController();
                controller.setVolData(v);
                cardsContainer.getScene().setRoot(root);
            } catch (IOException ex) {
                throw new RuntimeException(ex);

            }
        }
        else {
            return;
        }


    }
}