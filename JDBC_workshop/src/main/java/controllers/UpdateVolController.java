package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import entities.vol;
import services.SupabaseStorageService;
import services.VolService;


import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate; // If you have dates

public class UpdateVolController {
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
    @FXML private Button retourBtn;
    private File selectedImageFile;
    String imageUrl;

    private vol currentService;

    public void setServiceData(vol service) {
        this.currentService = service;

        tfNom.setText(service.getNom());
        tfPrix.setText(String.valueOf(service.getPrix()));
        tfCapacite.setText(String.valueOf(service.getCapacite()));
        taDescription.setText(service.getDescription());
        cbDisponibilite.setSelected(service.getDisponibilite());
        tfNumeroVol.setText(String.valueOf(service.getNumeroVol()));
        tfVilleDepart.setText(String.valueOf(service.getVilleDepart()));
        tfVilleArrivee.setText(String.valueOf(service.getVilleArrivee()));
    }

    @FXML
    private void handleUpdate() {
        resetStyles();
        if (isNomValid() && isPrixValid() && isCapaciteValid() && isDescriptionValid() && isNumeroVolValid() && isVilleDepartValid() && isVilleArriveValid() && isImage()  ){
        String newName = tfNom.getText();
        double newPrice = Double.parseDouble(tfPrix.getText());
        String newdescription=taDescription.getText();
        String newnumeroVol = tfNumeroVol.getText();
        int newcapacite=Integer.parseInt(tfCapacite.getText());
        boolean newdisponibilite=cbDisponibilite.isSelected();
        String newVilleDepart=tfVilleDepart.getText();
        String newVilleArrivee=tfVilleArrivee.getText();
        LocalDate localDateArrivee = dpDateArrivee.getValue();
        LocalDate localDateDepart = dpDateDepart.getValue();
        java.sql.Date sqlDateArrive = java.sql.Date.valueOf(localDateArrivee);
        java.sql.Date sqlDateDepart = java.sql.Date.valueOf(localDateDepart);
        VolService vs=new VolService();
        vol v = new vol(newName,newdescription,newPrice,newdisponibilite,newcapacite,newnumeroVol,newVilleDepart,newVilleArrivee,sqlDateDepart,sqlDateArrive,"vol",imageUrl);
        try {
            vs.updateOne(currentService.getNumeroVol(),v);
            Parent dashboardView = null;
            try {
                dashboardView = FXMLLoader.load(getClass().getResource("/views/mainpage.fxml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            StackPane contentArea = (StackPane) tfNom.getScene().lookup("#contentArea");

            if (contentArea != null) {

                contentArea.getChildren().setAll(dashboardView);
            } else {

                tfNom.getScene().setRoot(dashboardView);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }}
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
        }
    }

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
            messageErrorDateArrive="ville Arrive est requise.";
            return false;
        }
        else if (tfVilleArrivee.getText().trim().length()<3) {
            messageErrorDateArrive="ville Arrive doit être au moins 3 caracteres" ;
            return false;
        }
        return true;
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
    @FXML private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DashboardServices.fxml"));
            Parent root = loader.load();
            retourBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}