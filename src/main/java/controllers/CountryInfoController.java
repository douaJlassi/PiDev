package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import services.CountryInfoService;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class CountryInfoController implements Initializable {

    @FXML private ImageView imgFlag;
    @FXML private Label lblCountryName;
    @FXML private Label lblOfficialName;
    @FXML private Label lblCapital;
    @FXML private Label lblRegion;
    @FXML private Label lblPopulation;
    @FXML private Label lblCurrency;
    @FXML private Label lblLanguages;
    @FXML private Label lblTimezone;
    @FXML private Label lblCallingCode;
    @FXML private Label lblStatus;

    private String city;
    public void setCity(String city) {
        this.city = city;
        lblStatus.setText("Loading country info");
        fetchAsync();
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }




    private void fetchAsync() {
        Thread t = new Thread(() -> {
            try {
                CountryInfoService.CountryInfo info = CountryInfoService.fetchByCity(city);
                Platform.runLater(() -> populate(info));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("⚠ Could not load data: " + e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void populate(CountryInfoService.CountryInfo info) {
        lblStatus.setText("");

        lblCountryName.setText(info.flagEmoji + "  " + info.commonName);
        lblOfficialName.setText(info.officialName);
        lblCapital.setText(info.capital);
        lblRegion.setText(info.region + (info.subregion != null && !info.subregion.isBlank()
                ? " · " + info.subregion : ""));
        lblPopulation.setText(NumberFormat.getNumberInstance(Locale.US).format(info.population));
        lblCurrency.setText(info.currency
                + (info.currencySymbol != null && !info.currencySymbol.isBlank()
                ? "  (" + info.currencySymbol + ")" : ""));
        lblLanguages.setText(info.languages);
        lblTimezone.setText(info.timezone);
        lblCallingCode.setText(info.callingCode.isBlank() ? "N/A" : info.callingCode);

        if (info.flagPngUrl != null && !info.flagPngUrl.isBlank()) {
            try {
                Image flag = new Image(info.flagPngUrl, 90, 60, true, true, true);
                imgFlag.setImage(flag);
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblCountryName.getScene().getWindow();
        stage.close();
    }
}