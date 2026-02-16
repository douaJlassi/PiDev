package projet.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import projet.entites.Hotel;
import projet.services.HotelService;


import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private VBox pnItems; // The container in FXML

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Fetch data from your service
        List<Hotel> recentServices = getAllServices();


        // Populate the table
        for (Hotel hotel : recentServices) {
            HBox row = createServiceRow(hotel);
            pnItems.getChildren().add(row);
        }
    }
    private List<Hotel> getAllServices() {
        HotelService hotelService = new HotelService();
        List<Hotel> list = new ArrayList<>();
        try {
            list=hotelService.selectALL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }


    private HBox createServiceRow(Hotel service) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("table-row");


        // 1. Name Label
        Label nameLbl = new Label(service.getNom());
        nameLbl.setMinWidth(200);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        // 2. Type Label
        Label typeLbl = new Label("hotel");
        typeLbl.setMinWidth(150);

        // 3. Price Label (Styled with Teal)
        Label priceLbl = new Label(service.getPrix() + " TND");
        priceLbl.setMinWidth(100);
        priceLbl.setStyle("-fx-font-weight:bold; -fx-text-fill: #4ba3a1;");

        // 4. Capacity Label
        Label capLbl = new Label(String.valueOf(service.getCapacite()));
        capLbl.setMinWidth(150);

        // 5. Status Label (Dynamic Styling)
        if (service.getDisponibilite()){
        Label statusLbl = new Label("Disponible");
        statusLbl.setMinWidth(90);
        statusLbl.getStyleClass().add("status-active");
            row.getChildren().addAll(nameLbl, typeLbl, priceLbl, capLbl, statusLbl);
        }
        else {
            Label statusLbl = new Label("Non Disponible");
            statusLbl.setMinWidth(90);
            statusLbl.getStyleClass().add("status-pending");
            row.getChildren().addAll(nameLbl, typeLbl, priceLbl, capLbl, statusLbl);

        }



        // Add all to row


        return row;
    }
}