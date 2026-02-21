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

public class DashboardController implements Initializable {
  //  user connectedUser=new user("achref","souli","admin");
    user connectedUser=new user("achref","souli","admin");
    @FXML
    private VBox pnItems;
    @FXML
    private VBox pnItemsReservation;
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
    private Button Reservations;
    @FXML
    private ComboBox<String> cbFilter;
    @FXML
    private Button searchServiceBtn;
    @FXML
    private TextField searchBar;
    @FXML
    private Label Vols;
    @FXML
    private Label Hotels;
    @FXML
    private Label ReservationNb;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (connectedUser.getType().equals("user")) {
            setupUserView();
        }
        cbFilter.getItems().addAll("Price", "Capacity", "Availability");
        searchServiceBtn.setOnMouseClicked(event -> {handleSearchAction();});
        List<service> recentServices = getAllServices();
        List<reservation> recentReservations = getAllReservations();
        Vols.setText(String.valueOf(recentServices.stream().filter(s -> s.getType().equals("vol")).count()));
        Hotels.setText(String.valueOf(recentServices.stream().filter(s -> s.getType().equals("hotel")).count()));
        ReservationNb.setText(String.valueOf(recentReservations.size()));
        for (service s : recentServices) {
            HBox row = createServiceRow(s);
            pnItems.getChildren().add(row);
        }
        for (reservation r : recentReservations) {
            HBox row = createReservationRow(r);
            pnItemsReservation.getChildren().add(row);
        }
    }
    private void refreshData() {
        System.out.println("Refreshing Dashboard Data...");
        pnItems.getChildren().clear();
        List<service> recentServices = getAllServices();
        for (service service : recentServices) {
            HBox row = createServiceRow(service);
            pnItems.getChildren().add(row);
        }
    }
    private List<service> getAllServices() {
        ServiceService Service = new ServiceService();
        List<service> list;
        try {
            list=Service.selectALL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    private List<reservation> getAllReservations() {
        ReservationService reservations = new ReservationService();
        List<reservation> list;
        try {
            list=reservations.selectALL();
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
        capLbl.setMinWidth(100);

        // 5. Status Label (Dynamic Styling)
        if (service.getDisponibilite()){
        Label statusLbl = new Label("Disponible");
        statusLbl.setMinWidth(100);
        statusLbl.getStyleClass().add("status-active");
            row.getChildren().addAll(nameLbl, typeLbl, priceLbl, capLbl, statusLbl);
        }
        else {
            Label statusLbl = new Label("Non Disponible");
            statusLbl.setMinWidth(100);
            statusLbl.getStyleClass().add("status-pending");
            row.getChildren().addAll(nameLbl, typeLbl, priceLbl, capLbl, statusLbl);

        }
       row.setOnMouseClicked(e -> {showDetails(service);});
        searchServiceBtn.setOnMouseClicked(e -> {});


        // Add all to row


        return row;
    }
    private HBox createReservationRow(reservation reservation) {
        ServiceService Service = new ServiceService();

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("table-row");



        Label nameLbl = new Label(reservation.getNom());
        nameLbl.setMinWidth(100);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);


        Label DateLbl = new Label(reservation.getDateReservation().toString());
        DateLbl.setMinWidth(200);


        Label PaymentLbl = new Label(reservation.getModePaiement());
        PaymentLbl.setMinWidth(150);
        PaymentLbl.setStyle("-fx-font-weight:bold; -fx-text-fill: #4ba3a1;");

        Label ServiceLbl = new Label();
        ServiceLbl.setMinWidth(150);


        if (reservation.getStatut().equals("acceptee")){
            Label statusLbl = new Label("Accepted");
            statusLbl.setMinWidth(100);
            statusLbl.getStyleClass().add("status-active");
            row.getChildren().addAll(nameLbl, DateLbl, PaymentLbl, ServiceLbl, statusLbl);
        }
        else {
            Label statusLbl = new Label("Not Accepted");
            statusLbl.setMinWidth(100);
            statusLbl.getStyleClass().add("status-pending");
            row.getChildren().addAll(nameLbl, DateLbl, PaymentLbl, ServiceLbl, statusLbl);

        }
       // row.setOnMouseClicked(e -> {showDetailsReservation(reservation);});
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
        searchBox.setVisible(false);
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
        Reservations.getStyleClass().removeAll("active-nav-btn");
        Services.getStyleClass().removeAll("active-nav-btn");
        // Remove any forms (anything that is NOT pnlOverview)
        contentArea.getChildren().removeIf(node -> node != pnlOverview);

        // Show the stats again
        pnlOverview.setVisible(true);
        refreshData();
    }
    @FXML
    private void handleShowServices(ActionEvent event) {
        searchBox.setVisible(false);
        try {
            Services.getStyleClass().add("active-nav-btn");
            Dashboard.getStyleClass().removeAll("active-nav-btn");
            Reservations.getStyleClass().removeAll("active-nav-btn");
            Parent HotelForm = FXMLLoader.load(getClass().getResource("/Services.fxml"));
            contentArea.getChildren().removeAll(); // Clears everything? No, we want to keep logic simple.
            pnlOverview.setVisible(false);
            contentArea.getChildren().add(HotelForm);
            refreshData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleShowReservations(ActionEvent event) {
        searchBox.setVisible(false);
        try {
            Services.getStyleClass().removeAll("active-nav-btn");
            Dashboard.getStyleClass().removeAll("active-nav-btn");
            Reservations.getStyleClass().add("active-nav-btn");
            Parent Reservations = FXMLLoader.load(getClass().getResource("/Reservations.fxml"));
            contentArea.getChildren().removeAll();
            pnlOverview.setVisible(false);
            contentArea.getChildren().add(Reservations);
            refreshData();
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
        String searchText = searchBar.getText().trim();
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
    private void setupUserView() {
        searchBox.setVisible(false);
        // 1. Cacher les boutons et panneaux non autorisés
        Dashboard.setVisible(false);
        Dashboard.setManaged(false); // Très important: retire le bouton du layout

       /* btnAddVol.setVisible(false);
        btnAddVol.setManaged(false);

        btnAddHotel.setVisible(false);
        btnAddHotel.setManaged(false);*/

        // 2. Afficher directement la page des services
        try {
            Parent servicesView = FXMLLoader.load(getClass().getResource("/Services.fxml"));
            contentArea.getChildren().setAll(servicesView);
        } catch (IOException e) {
            e.printStackTrace();
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