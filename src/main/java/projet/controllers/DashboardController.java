package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import projet.entites.Hotel;
import projet.entites.service;
import projet.entites.vol;
import projet.services.HotelService;
import projet.services.ServiceService;
import projet.services.VolService;


import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private VBox pnItems;
    @FXML
    private StackPane contentArea;
    @FXML
    private HBox searchBox;
    @FXML
    private ScrollPane pnlOverview;
    @FXML
    private Button Dashboard;
    @FXML
    private Button Services;
    @FXML
    private ComboBox<String> cbFilter;
    @FXML
    private Button searchServiceBtn;
    @FXML
    private TextField searchBar;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbFilter.getItems().addAll("Price", "Capacity", "Availability");
        // Fetch data from your service
        searchServiceBtn.setOnMouseClicked(event -> {handleSearchAction();});
        List<service> recentServices = getAllServices();
        // Populate the table
        for (service s : recentServices) {
            HBox row = createServiceRow(s);
            pnItems.getChildren().add(row);
        }
    }
    private void refreshData() {
        System.out.println("Refreshing Dashboard Data...");

        // STEP 1: CLEAR THE OLD DATA (Very Important!)
        pnItems.getChildren().clear();

        // STEP 2: Fetch fresh data from your Service/DB
        // List<Service> recentServices = serviceService.getAll();
        List<service> recentServices = getAllServices();


        // Populate the table
        for (service service : recentServices) {
            HBox row = createServiceRow(service);
            pnItems.getChildren().add(row);
        }


    }
    private List<service> getAllServices() {
        ServiceService Service = new ServiceService();
        List<service> list = new ArrayList<>();
        try {
            list=Service.selectALL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    private HBox createServiceRow(service service) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("table-row");


        // 1. Name Label
        Label nameLbl = new Label(service.getNom());
        nameLbl.setMinWidth(200);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        // 2. Type Label
        Label typeLbl = new Label(service.getType());
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
       row.setOnMouseClicked(e -> {showDetails(service);});
        searchServiceBtn.setOnMouseClicked(e -> {});


        // Add all to row


        return row;
    }
    @FXML
    private void handleShowVolForm(ActionEvent event) {
        try {
            // Load the addVol.fxml
            Parent volForm = FXMLLoader.load(getClass().getResource("/addVol.fxml"));
            searchBox.setVisible(false);
            // Clear current view and add the form
            contentArea.getChildren().removeAll(); // Clears everything? No, we want to keep logic simple.

            // Better approach: Make pnlOverview invisible and add form on top
            pnlOverview.setVisible(false);

            // Check if form is already added to avoid duplicates (Optional optimization)
            contentArea.getChildren().add(volForm);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleShowHotelForm(ActionEvent event) {
        try {

            Parent HotelForm = FXMLLoader.load(getClass().getResource("/AddHotel.fxml"));

            contentArea.getChildren().removeAll();


            pnlOverview.setVisible(false);


            contentArea.getChildren().add(HotelForm);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleShowDashboard(ActionEvent event) {
        searchBox.setVisible(true);
        Dashboard.getStyleClass().add("active-nav-btn");
        Services.getStyleClass().removeAll("active-nav-btn");
        // Remove any forms (anything that is NOT pnlOverview)
        contentArea.getChildren().removeIf(node -> node != pnlOverview);

        // Show the stats again
        pnlOverview.setVisible(true);
        refreshData();
    }
    @FXML
    private void handleShowServices(ActionEvent event) {
        try {
            Services.getStyleClass().add("active-nav-btn");
            Dashboard.getStyleClass().removeAll("active-nav-btn");
           searchBox.setVisible(false);
            // Load the addVol.fxml
            Parent HotelForm = FXMLLoader.load(getClass().getResource("/Services.fxml"));

            // Clear current view and add the form
            contentArea.getChildren().removeAll(); // Clears everything? No, we want to keep logic simple.

            // Better approach: Make pnlOverview invisible and add form on top
            pnlOverview.setVisible(false);

            // Check if form is already added to avoid duplicates (Optional optimization)
            contentArea.getChildren().add(HotelForm);
            refreshData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void handleShowUpdateVolForm(ActionEvent event) {
        try {
            // Load the addVol.fxml
            Parent volForm = FXMLLoader.load(getClass().getResource("/updateVol.fxml"));

            // Clear current view and add the form
            contentArea.getChildren().removeAll(); // Clears everything? No, we want to keep logic simple.

            // Better approach: Make pnlOverview invisible and add form on top
            pnlOverview.setVisible(false);

            // Check if form is already added to avoid duplicates (Optional optimization)
            contentArea.getChildren().add(volForm);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleFilterAction() {
        String selected = cbFilter.getValue();

    }
    @FXML
    private void handleSearchAction() {
        ServiceService service = new ServiceService();
        pnItems.getChildren().clear();
        String searchText = searchBar.getText().toLowerCase().trim();
        List<service> allServices = getAllServices();
        boolean found = false;
        for (service s : allServices) {
            if (s.getNom().equals(searchText)) {
                HBox row = createServiceRow(s);
                pnItems.getChildren().add(row);
                found = true;
            }
        }

        // Optional: Show a "No Results" label if nothing found
        if (!found) {
            Label noResult = new Label("No services found for: " + searchText);
            noResult.setStyle("-fx-text-fill: #555; -fx-padding: 20;");
            pnItems.getChildren().add(noResult);
        }

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
                contentArea.getScene().setRoot(root);
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
                contentArea.getScene().setRoot(root);
            } catch (IOException ex) {
                throw new RuntimeException(ex);

            }
        }
        else {
            return;
        }


    }

}