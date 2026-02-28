package projet.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;
import projet.entites.vol;
import projet.services.SupabaseStorageService;
import projet.services.VolService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Properties;


public class addVolController {
    String messageErrorNom="";
    String messageErrorDescription="";
    String messageErrorPrix="";
    String messageErrorCapacite="";
    String messageErrorNumeroVol="";
    String messageErrorVilleDepart="";
    String messageErrorVilleArrive="";
    String messageErrorDateDepart="";
    String messageErrorDateArrive="";
    @FXML private CheckBox cbDisponibilite;
    @FXML private DatePicker dpDateArrivee;
    @FXML private DatePicker dpDateDepart;
    @FXML private TextArea taDescription;
    @FXML private TextField tfCapacite;
    @FXML private TextField tfNom;
    @FXML private TextField tfNumeroVol;
    @FXML private TextField tfPrix;
    @FXML private TextField tfVilleArrivee;
    @FXML private TextField tfVilleDepart;
    @FXML private Label lblErrorCapacite;
    @FXML private Label lblErrorDateArrive;
    @FXML private Label lblErrorDateDepart;
    @FXML private Label lblErrorDescription;
    @FXML private Label lblErrorNom;
    @FXML private Label lblErrorNumeroVol;
    @FXML private Label lblErrorPrix;
    @FXML private Label lblErrorVilleArrive;
    @FXML private Label lblErrorVilleDepart;
    @FXML private Button btnChoisirPhoto;
    @FXML private ImageView imgPreview;
    @FXML private Label lblErrorPhoto;
    private File selectedImageFile;
    private String AviationStackapiKey;
    String imageUrl;

    public void loadConfig() {
        try (InputStream input = getClass()
                .getResourceAsStream("/config.properties")) {

            Properties prop = new Properties();
            prop.load(input);
            AviationStackapiKey = prop.getProperty("aviation.api.key");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    void ajouterVol(ActionEvent event) {
        if (isInputValid()){
        String nom = tfNom.getText();
        String description=taDescription.getText();
        double prix = Double.parseDouble(tfPrix.getText());
        String numeroVol = tfNumeroVol.getText();
        int capacite=Integer.parseInt(tfCapacite.getText());
        String VilleDepart=tfVilleDepart.getText();
        String VilleArrivee=tfVilleArrivee.getText();
        boolean disponibilite=cbDisponibilite.isSelected();
        LocalDate localDateArrivee = dpDateArrivee.getValue();
        LocalDate localDateDepart = dpDateDepart.getValue();

        java.sql.Date sqlDateArrive = java.sql.Date.valueOf(localDateArrivee);
        java.sql.Date sqlDateDepart = java.sql.Date.valueOf(localDateDepart);
        VolService service = new VolService();
        vol vol = new vol(nom,description,prix,disponibilite,capacite,numeroVol,VilleDepart,VilleArrivee,sqlDateDepart,sqlDateArrive,"vol",imageUrl);
        try {
            service.insertOne(vol);
            Parent dashboardView = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            StackPane contentArea = (StackPane) tfNom.getScene().lookup("#contentArea");
            tfNom.getScene().setRoot(dashboardView);

        }catch (SQLException | IOException e){
            System.out.println(e.getMessage());
        }

    }}
    private  boolean isNomValid() {
        if (tfNom.getText() == null || tfNom.getText().trim().isEmpty()) {
            tfNom.getStyleClass().add("error");
            messageErrorNom="Le nom ne peut pas être vide" ;
            return false;
        } else if (tfNom.getText().trim().length()<3) {
            messageErrorNom="Le nom doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isPrixValid() {
        if (tfPrix.getText() == null || tfPrix.getText().isEmpty()) {
            messageErrorPrix="Le prix est requis.";
            tfPrix.getStyleClass().add("error");
            return false;
        }
        else {
            try {
                double prix = Double.parseDouble(tfPrix.getText());
                if (prix <= 0) {
                    messageErrorPrix="Le prix doit être un nombre positif.";
                    tfPrix.getStyleClass().add("error");
                    return false;
                }
            } catch (NumberFormatException e) {
                messageErrorPrix="Le prix doit être un nombre valide (ex: 150.50)";
                tfPrix.getStyleClass().add("error");
                return false;
            }
        }
        return true;
    }
    private  boolean isCapaciteValid() {
        if (tfCapacite.getText() == null || tfCapacite.getText().isEmpty()) {
            messageErrorCapacite="La capacité est requise.";
            tfCapacite.getStyleClass().add("error");
            return false;
        }
        else {
            try {
                int capacite = Integer.parseInt(tfCapacite.getText());
                if (capacite <= 0) {
                    messageErrorCapacite="La capacité doit être supérieure à 0.";
                    tfCapacite.getStyleClass().add("error");
                    return false;
                }
            } catch (NumberFormatException e) {
                messageErrorCapacite="La capacité doit être un nombre entier.\n";
                tfCapacite.getStyleClass().add("error");
            }
        }
        return true;
    }
    private  boolean isDescriptionValid() {
        if (taDescription.getText() == null || taDescription.getText().isEmpty()){
            taDescription.getStyleClass().add("error");
            messageErrorDescription="La Description est requise.";
            return false;
        }
        else if (taDescription.getText().trim().length()<5) {
            messageErrorDescription="Le nom doit être au moins 5 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isNumeroVolValid() {
        if (tfNumeroVol.getText() == null || tfNumeroVol.getText().isEmpty()){
            tfNumeroVol.getStyleClass().add("error");
            messageErrorNumeroVol="numerovol est requise.";
            return false;
        }
        else if (tfNumeroVol.getText().trim().length()<3) {
            messageErrorNumeroVol="numerovol doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isVilleDepartValid() {
        if (tfVilleDepart.getText() == null || tfVilleDepart.getText().isEmpty()){
            tfVilleDepart.getStyleClass().add("error");
            messageErrorVilleDepart="ville depart est requise.";
            return false;
        }
        else if (tfVilleDepart.getText().trim().length()<3) {
            messageErrorVilleDepart="ville depart doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private  boolean isVilleArriveValid() {
        if (tfVilleArrivee.getText() == null || tfVilleArrivee.getText().isEmpty()){
            tfVilleArrivee.getStyleClass().add("error");
            messageErrorVilleArrive="ville Arrive est requise.";
            return false;
        }
        else if (tfVilleArrivee.getText().trim().length()<3) {
            messageErrorVilleArrive="ville Arrive doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
    }
    private boolean isDateArriveValid() {
        if (dpDateArrivee.getValue() == null){
            dpDateArrivee.getStyleClass().add("error");
            messageErrorDateArrive="date Arrive est requise.";
            return false;
        }
        return true;
    }
    private boolean isDateDepartValid() {
        if (dpDateDepart.getValue() == null){
            dpDateDepart.getStyleClass().add("error");
            messageErrorDateDepart="date Arrive est requise.";
            return false;
        }
        return true;
    }
    private boolean isInputValid() {
        resetStyles();
        if (isNomValid() && isPrixValid() && isCapaciteValid() && isDescriptionValid() && isNumeroVolValid() && isVilleDepartValid() && isVilleArriveValid() && isImage() && isDateDepartValid() && isDateArriveValid()) {

            lblErrorNom.setVisible(false);
            lblErrorNom.setManaged(false);
            lblErrorDescription.setVisible(false);
            lblErrorDescription.setManaged(false);
            lblErrorCapacite.setVisible(false);
            lblErrorCapacite.setManaged(false);
            lblErrorNumeroVol.setVisible(false);
            lblErrorNumeroVol.setManaged(false);
            lblErrorPrix.setVisible(false);
            lblErrorPrix.setManaged(false);
            lblErrorVilleDepart.setVisible(false);
            lblErrorVilleDepart.setManaged(false);
            lblErrorVilleArrive.setVisible(false);
            lblErrorVilleArrive.setManaged(false);
            lblErrorPhoto.setVisible(false);
            lblErrorPhoto.setManaged(false);

            return true;
        }
        else {
            if (!isNomValid()){
                lblErrorNom.setText(messageErrorNom);
                lblErrorNom.setVisible(true);
                lblErrorNom.setManaged(true);
            }
            if (!isPrixValid()){
                lblErrorPrix.setVisible(true);
                lblErrorPrix.setManaged(true);
                lblErrorPrix.setText(messageErrorPrix);

            }
            if (!isCapaciteValid()){  lblErrorCapacite.setText(messageErrorCapacite);
                lblErrorCapacite.setVisible(true);
                lblErrorCapacite.setManaged(true);}
            if (!isNumeroVolValid()){

                lblErrorNumeroVol.setText(messageErrorNumeroVol);
                lblErrorNumeroVol.setVisible(true);
                lblErrorNumeroVol.setManaged(true);
            }
            if (!isDescriptionValid()){
                lblErrorDescription.setText(messageErrorDescription);
                lblErrorDescription.setVisible(true);
                lblErrorDescription.setManaged(true);
            }
            if (!isVilleDepartValid()){
                lblErrorVilleDepart.setText(messageErrorVilleDepart);
                lblErrorVilleDepart.setVisible(true);
                lblErrorVilleDepart.setManaged(true);
            }
            if (!isVilleArriveValid()){
                lblErrorVilleArrive.setText(messageErrorVilleArrive);
                lblErrorVilleArrive.setVisible(true);
                lblErrorVilleArrive.setManaged(true);
            }
            if (!isDateArriveValid()){
                lblErrorDateArrive.setText(messageErrorDateArrive);
                lblErrorDateArrive.setVisible(true);
                lblErrorDateArrive.setManaged(true);
            }
            if (!isDateDepartValid()){
                lblErrorDateDepart.setText(messageErrorDateDepart);
                lblErrorDateDepart.setVisible(true);
                lblErrorDateDepart.setManaged(true);
            }



            return false;
        }

    }
    private boolean isImage(){
        String photoUrl;

        if (selectedImageFile != null) {
            try {
                SupabaseStorageService storageService = new SupabaseStorageService();
                String fileName = "vol_" + tfNumeroVol.getText() + "_" + System.currentTimeMillis()
                        + selectedImageFile.getName().substring(selectedImageFile.getName().lastIndexOf('.'));
                photoUrl = storageService.uploadImage(selectedImageFile.toPath(), fileName);
                imageUrl = photoUrl;
                if (photoUrl == null) {
                    lblErrorPhoto.setText("Échec de l'upload de l'image.");
                    lblErrorPhoto.setVisible(true);
                    lblErrorPhoto.setManaged(true);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                lblErrorPhoto.setText("Erreur lors de l'upload.");
                lblErrorPhoto.setVisible(true);
                lblErrorPhoto.setManaged(true);
                return false;
            }
        }
        return  true;
    }
    private void resetStyles() {
        lblErrorNom.setVisible(false);
        lblErrorNom.setManaged(false);
        lblErrorDescription.setVisible(false);
        lblErrorDescription.setManaged(false);
        lblErrorCapacite.setVisible(false);
        lblErrorCapacite.setManaged(false);

        lblErrorPrix.setVisible(false);
        lblErrorPrix.setManaged(false);
        lblErrorNumeroVol.setVisible(false);
        lblErrorNumeroVol.setManaged(false);
        lblErrorVilleDepart.setVisible(false);
        lblErrorVilleDepart.setManaged(false);
        lblErrorVilleArrive.setVisible(false);
        lblErrorVilleArrive.setManaged(false);

        tfNom.getStyleClass().remove("error");
        tfPrix.getStyleClass().remove("error");
        tfCapacite.getStyleClass().remove("error");
        taDescription.getStyleClass().remove("error");
        tfNumeroVol.getStyleClass().remove("error");
        tfVilleDepart.getStyleClass().remove("error");
        tfVilleArrivee.getStyleClass().remove("error");

    }
    @FXML void choisirPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        selectedImageFile = fileChooser.showOpenDialog(btnChoisirPhoto.getScene().getWindow());
        if (selectedImageFile != null) {
            imgPreview.setImage(new Image(selectedImageFile.toURI().toString()));
            lblErrorPhoto.setVisible(false);
            lblErrorPhoto.setManaged(false);
        }
    }
    @FXML void chercherVolAPI(ActionEvent event) {

        if (isNumeroVolValid()){
        String numeroVol = tfNumeroVol.getText().trim();

        if (numeroVol.isEmpty()) {
            lblErrorNumeroVol.setText("Veuillez saisir un numéro de vol (ex: AF123)");
            lblErrorNumeroVol.setVisible(true);
            lblErrorNumeroVol.setManaged(true);
            return;
        }

        // On utilise un Thread séparé pour ne pas bloquer (figer) l'interface graphique (JavaFX) pendant la requête
        new Thread(() -> {
            try {
                // REMPLACEZ VOTRE CLÉ API ICI
                loadConfig();
                String urlString = "http://api.aviationstack.com/v1/flights?access_key=" + AviationStackapiKey + "&flight_iata=" + numeroVol;

                // Création et envoi de la requête HTTP
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlString))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Analyse de la réponse JSON
                JSONObject jsonResponse = new JSONObject(response.body());

                if (jsonResponse.has("data") && jsonResponse.getJSONArray("data").length() > 0) {
                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                    JSONObject flightData = dataArray.getJSONObject(0);

                    // Extraction des données utiles
                    String nomCompagnie = flightData.getJSONObject("airline").getString("name");
                    String villeDepart = flightData.getJSONObject("departure").getString("airport"); // ou "timezone"
                    String villeArrivee = flightData.getJSONObject("arrival").getString("airport");

                    // L'heure de l'API est sous ce format : 2023-12-15T14:30:00+00:00
                    String dateDepartStr = flightData.getJSONObject("departure").getString("scheduled");
                    String dateArriveeStr = flightData.getJSONObject("arrival").getString("scheduled");

                    // Convertir en LocalDate (on prend juste les 10 premiers caractères : YYYY-MM-DD)
                    LocalDate dateDepart = LocalDate.parse(dateDepartStr.substring(0, 10));
                    LocalDate dateArrivee = LocalDate.parse(dateArriveeStr.substring(0, 10));

                    // Mettre à jour l'interface graphique (Toujours utiliser Platform.runLater dans un Thread)
                    Platform.runLater(() -> {
                        tfNom.setText(nomCompagnie + " " + numeroVol);
                        tfVilleDepart.setText(villeDepart);
                        tfVilleArrivee.setText(villeArrivee);
                        dpDateDepart.setValue(dateDepart);
                        dpDateArrivee.setValue(dateArrivee);
                        taDescription.setText("Vol opéré par " + nomCompagnie + " depuis " + villeDepart + " vers " + villeArrivee + ".");

                        // Nettoyer les erreurs potentielles
                        lblErrorNumeroVol.setVisible(false);
                        lblErrorNumeroVol.setManaged(false);
                    });

                } else {
                    Platform.runLater(() -> {
                        lblErrorNumeroVol.setText("Vol introuvable via l'API.");
                        lblErrorNumeroVol.setVisible(true);
                        lblErrorNumeroVol.setManaged(true);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblErrorNumeroVol.setText("Erreur de connexion à l'API. Vérifiez votre clé.");
                    lblErrorNumeroVol.setVisible(true);
                    lblErrorNumeroVol.setManaged(true);
                });
            }
        }).start();
    }
        else{
            lblErrorNumeroVol.setText(messageErrorNumeroVol);
            lblErrorNumeroVol.setVisible(true);
            lblErrorNumeroVol.setManaged(true);
        }
    }

}
