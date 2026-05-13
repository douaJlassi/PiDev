package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.json.JSONObject;
import entities.user;
import entities.vol;
import services.ServiceService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;

public class VolDetailsController {
    @FXML private Button retourBtn;
    @FXML private Button btnReserver;
    @FXML private Label lblTempDepart;
    @FXML private Label lblTempArrive;
    user connectedUser=new user("achref","souli","user");
    @FXML private Label lblNom, lblNumeroVol, lblPrix, lblVilleDepart, lblVilleArrivee,
            lblDateDepart, lblDateArrivee, lblCapacite, lblStatus, lblDescription;
    int id;
    ServiceService volService = new ServiceService();
    private String weatherapiKey;
    String username;
    String type;

    void setConnected(String username,String type) {
        this.username = username;
        this.type=type;
    }
    public void loadConfig() {
        try (InputStream input = getClass()
                .getResourceAsStream("/views/config.properties")) {

            Properties prop = new Properties();
            prop.load(input);
            weatherapiKey = prop.getProperty("weather.api.key");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setVolData(vol vol) {
        lblNom.setText(vol.getNom());
        lblNumeroVol.setText(vol.getNumeroVol());
        lblPrix.setText(vol.getPrix() + " TND");
        loadConfig();
        loadWeather(vol.getVilleArrivee(),"arrivee");
        loadWeather(vol.getVilleDepart(),"depart");
        lblVilleDepart.setText(vol.getVilleDepart());
        lblVilleArrivee.setText(vol.getVilleArrivee());
        lblDateArrivee.setText(vol.getDateArrivee().toString());
        lblDateDepart.setText(vol.getDateDepart().toString());
        lblCapacite.setText(String.valueOf(vol.getCapacite()));
        if (vol.getDisponibilite()){
            lblStatus.setText("disponible");}
        else {
            lblStatus.setText("no disponible");}
        lblDescription.setText(vol.getDescription());
retourBtn.setOnAction(e -> {handleBack();});
if (connectedUser.getType().equals("admin")) {
    btnReserver.setVisible(false);
}
btnReserver.setOnAction(e -> {handleReserver();});
        try {
            id=volService.getId(vol.getNom());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainpage.fxml"));
            Parent root = loader.load();
            retourBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML private void handleReserver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ReservationForm.fxml"));
            Parent root = loader.load();
            AddReservationController addReservationController = loader.getController();
            addReservationController.setIdService(id);
            addReservationController.setType("vol");
            addReservationController.setConnected(username,type);
            retourBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadWeather(String city,String DepOuRet) {
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
                    if (DepOuRet.equals("depart")){lblTempDepart.setText(temp + " °C");}
                    else {lblTempArrive.setText(temp + " °C");}

                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (DepOuRet.equals("depart")){lblTempDepart.setText("N/A");}
                    else {lblTempArrive.setText("N/A");}

                });
                e.printStackTrace();
            }
        }).start();
    }

}
