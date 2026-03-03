package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.json.JSONObject;
import entities.Hotel;
import entities.user;
import services.ServiceService;
// Ensure this matches your package structure

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;

public class HotelDetailsController {
    user connectedUser=new user("achref","souli","user");
    //user connectedUser=new user("achref","souli","user");
    // Link to FXML IDs defined in hotelDetails.fxml
    @FXML private Label lblNom;
    @FXML private Label lblLocalisation;
    @FXML private Label lblTemp;
    @FXML private Label lblPrix;
    @FXML private Label lblEtoiles;
    @FXML private Label lblChambre;
    @FXML private Label lblCapacite;
    @FXML private Label lblStatus;
    @FXML private Label lblDescription;
    @FXML private Button btnReserver;
    @FXML
    private Button retourBtn;
    int id;
    ServiceService HotelService = new ServiceService();
    private String weatherapiKey;

    public void loadConfig() {
        try (InputStream input = getClass()
                .getResourceAsStream("/config.properties")) {

            Properties prop = new Properties();
            prop.load(input);
            weatherapiKey = prop.getProperty("weather.api.key");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setHotelData(Hotel hotel) {
        // 1. Set Basic Text
        lblNom.setText(hotel.getNom());
        lblLocalisation.setText(hotel.getLocalisation());
        loadConfig();
        loadWeather(hotel.getLocalisation());
        lblPrix.setText(hotel.getPrix() + " TND");
        lblDescription.setText(hotel.getDescription());
        lblCapacite.setText(String.valueOf(hotel.getCapacite()) + " Personnes");
        lblChambre.setText(hotel.getChambre());

        // 2. Generate Star Visuals (e.g., 5 -> ★★★★★)
        StringBuilder stars = new StringBuilder();
        int starCount = hotel.getNbEtoiles(); // Assuming getter is getNbEtoiles()
        for(int i = 0; i < starCount; i++) {
            stars.append("★");
        }
        lblEtoiles.setText(stars.toString());
        // Style the stars yellow/gold
        lblEtoiles.setStyle("-fx-text-fill: #f6c750; -fx-font-size: 16px;");

        // 3. Status Logic & Coloring
        // Assuming your model has getStatus() returning "Active", "Pending", etc.
        if (hotel.getDisponibilite()){
            String status = "disponible";
            lblStatus.setText(status);
            lblStatus.setStyle("-fx-text-fill: #4ba3a1; -fx-font-weight: bold;"); // Brand Teal
        }
        else {
            String status = "non disponible";
            lblStatus.setText(status);
            lblStatus.setStyle("-fx-text-fill: #f6c750; -fx-font-weight: bold;");
        }
        retourBtn.setOnAction(e -> {handleBack();});
        if (connectedUser.getType().equals("admin")) {
            btnReserver.setVisible(false);
        }
        btnReserver.setOnAction(e -> {handleReserver();});
        try {
            id=HotelService.getId(hotel.getNom());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    public void loadWeather(String city) {
        new Thread(() -> {
            try {
                String urlString =
                        "https://api.openweathermap.org/data/2.5/weather?q="
                                + city + "&appid=" + weatherapiKey + "&units=metric";

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                reader.close();

                JSONObject obj = new JSONObject(json.toString());
                double temp = obj.getJSONObject("main").getDouble("temp");

                Platform.runLater(() -> {
                    lblTemp.setText(temp + " °C");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblTemp.setText("N/A");
                });
                e.printStackTrace();
            }
        }).start();
    }
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardServices.fxml"));
            Parent root = loader.load();
            retourBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleReserver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationForm.fxml"));
            Parent root = loader.load();
            AddReservationController addReservationController = loader.getController();
            addReservationController.setIdService(id);
            addReservationController.setType("hotel");
            retourBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}