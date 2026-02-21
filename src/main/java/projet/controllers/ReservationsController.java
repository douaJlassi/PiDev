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

import projet.entites.*;
import projet.services.HotelService;
import projet.services.ReservationService;
import projet.services.ServiceService;
import projet.services.VolService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ReservationsController implements Initializable {
    // user connectedUser=new user("achref","souli","admin");
    user connectedUser=new user("achref","souli","admin");
    @FXML
    private FlowPane cardsContainer;
    @FXML
    private TextField txtSearch;
    private List<reservation> allReservations;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        allReservations = getDummyData();
        renderServices(allReservations);
    }
    public void refreshServices() {
        allReservations = getDummyData();
        renderServices(allReservations);
    }

    @FXML
    private void handleSearch() {
        String query = txtSearch.getText().toLowerCase();
        List<reservation> filtered = allReservations.stream()
                .filter(s -> s.getNom().toLowerCase().contains(query)
                )
                .toList();
        renderServices(filtered);
    }

    private void renderServices(List<reservation> reservations) {
        cardsContainer.getChildren().clear();

        for (reservation r : reservations) {
            VBox card = createServiceCard(r);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createServiceCard(reservation r) {
        ReservationService Service = new ReservationService();

        // --- CONTAINER ---
        VBox card = new VBox();
        card.getStyleClass().add("service-card");
        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setSpacing(10);
        // --- DETAILS ---
        VBox details = new VBox();
        details.setPadding(new Insets(10));
        details.setSpacing(5);

        Label statut = new Label("Status: "+r.getStatut());
        statut.getStyleClass().add("card-type");

        Label paiement = new Label("payment method: "+r.getModePaiement());
        paiement.getStyleClass().add("card-type");

        Label date = new Label("Reservation Date: "+r.getDateReservation().toString());
        date.getStyleClass().add("card-type");

        Label name = new Label(r.getNom());
        name.getStyleClass().add("card-title-text");
        name.setWrapText(true);


        // --- ACTION BUTTONS ---

        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setSpacing(10);
        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("btn-card-action");



        Button btnDelete = new Button("🗑");
        btnDelete.setOnAction(event -> {
            try {
                Service.deleteOne(r);
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
        //card.setOnMouseClicked(event -> {showDetails(s);});
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll( spacer, btnEdit, btnDelete);

        details.getChildren().addAll(name,statut,paiement,date,actions);
        card.getChildren().addAll(details);

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
    private List<reservation> getDummyData() {
        ReservationService Service = new ReservationService();
        List<reservation> list = new ArrayList<>();
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