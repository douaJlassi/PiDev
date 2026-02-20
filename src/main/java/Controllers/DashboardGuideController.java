package Controllers;


import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;

import gestion_activite.Activite;
import Services.ActiviteService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import utils.MyDBConnexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import javafx.scene.image.Image;
import java.io.File;
import java.io.InputStream;
import javafx.scene.image.ImageView;
import javafx.scene.control.*;

import javafx.fxml.FXMLLoader;
import java.io.IOException;


public class DashboardGuideController {

    @FXML
    private ListView<Activite> activitiesListView;

    @FXML
    private Label pageInfoLabel;

    private ActiviteService activiteService;
    private List<Activite> toutesActivites;
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;


    private int guideId = 1;

    @FXML
    private BorderPane borderPane;

    @FXML
    private Text userNameText;
    public DashboardGuideController() {
        activiteService = new ActiviteService();
    }

    @FXML
    public void initialize() {
        chargerActivites();
        setupListView();
        loadGuideProfile();
    }

    private void loadGuideProfile() {
        String guideName = getGuideNameFromDatabase(guideId);
        if (guideName != null && !guideName.isEmpty()) {
            userNameText.setText(guideName);
        } else {
            userNameText.setText("Guide inconnu");
        }
    }







    private Image loadActivityImage(Activite activite) {
        String imageName = activite.getImage();

        if (imageName == null || imageName.trim().isEmpty()) {
            imageName = "default.jpg";
        }
        if (!imageName.contains(".")) {
            imageName += ".jpg";
        }

        String resourcePath = "/images/" + imageName;
        URL resourceUrl = getClass().getResource(resourcePath);

        if (resourceUrl != null) {
            return new Image(resourceUrl.toExternalForm(), true);
        } else {
            System.out.println("⚠️ Image non trouvée : " + resourcePath + " - Chargement de default.jpg...");
            URL defaultUrl = getClass().getResource("/images/default.jpg");
            if (defaultUrl != null) {
                return new Image(defaultUrl.toExternalForm());
            } else {
                System.err.println("❌ ERREUR : default.jpg est manquant !");
                return null;
            }
        }
    }





    private String getGuideNameFromDatabase(int guideId) {
        String query = "SELECT nom FROM guide WHERE idUser = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = MyDBConnexion.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, guideId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nom");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {

            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        return null;
    }

    private void chargerActivites() {
        try {

            toutesActivites = activiteService.selectByGuide(guideId);
            totalPages = (int) Math.ceil((double) toutesActivites.size() / itemsPerPage);
            if (totalPages == 0) totalPages = 1;
            mettreAJourPage();
        } catch (SQLException e) {
            e.printStackTrace();

        }
    }

    private void mettreAJourPage() {
        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, toutesActivites.size());
        List<Activite> activitesPage = toutesActivites.subList(fromIndex, toIndex);
        activitiesListView.getItems().setAll(activitesPage);
        pageInfoLabel.setText("Page " + currentPage + " sur " + totalPages);
    }

    private void setupListView() {
        activitiesListView.setCellFactory(param -> new ListCell<Activite>() {
            @Override
            protected void updateItem(Activite activite, boolean empty) {
                super.updateItem(activite, empty);
                if (empty || activite == null) {
                    setGraphic(null);
                } else {
                    setGraphic(creerCarteActivite(activite));
                }
            }
        });
    }

    private VBox creerCarteActivite(Activite a) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");

        VBox card = new VBox();
        card.setPrefWidth(300);
        card.getStyleClass().add("card-modern");

        StackPane imageStack = new StackPane();
        imageStack.setPrefHeight(160);
        imageStack.getStyleClass().add("card-image-modern");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(300);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        Image img = loadActivityImage(a);
        if (img != null) {
            imageView.setImage(img);
        }

        String status = a.getStatut();
        Label badge = new Label(status != null ? status : "Activité");
        badge.getStyleClass().add("badge-category-modern");
        if (status != null && status.equalsIgnoreCase("Indisponible"))
        {
            badge.setStyle("-fx-background-color: #e74c3c;");
        }
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
        imageStack.getChildren().addAll(imageView, badge);

        VBox content = new VBox(8);
        content.getStyleClass().add("card-content-modern");
        content.setPadding(new Insets(15));

        Text titre = new Text(a.getTitre());
        titre.getStyleClass().add("card-title-modern");

        Label lieu = new Label("📍 " + a.getLieu());
        lieu.getStyleClass().add("card-meta-modern");

        Label date = new Label("📅 " + dateFormat.format(a.getDateActivite()));
        date.getStyleClass().add("card-meta-modern");

        HBox priceRow = new HBox();
        priceRow.setAlignment(Pos.CENTER_LEFT);

        Text prix = new Text(String.format("%.0f DT", a.getPrix()));
        prix.getStyleClass().add("card-price-modern");

        Label duree = new Label(a.getDureParJour() + (a.getDureParJour() > 1 ? " jours" : " jour"));
        duree.getStyleClass().add("card-duration-modern");

        Label note = new Label("★ 4.9");
        note.getStyleClass().add("card-rating-modern");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        priceRow.getChildren().addAll(prix, note, spacer, duree);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 15, 15, 15));

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("btn-modifier-modern");

        Button btnVoirResa = new Button("Réservations");
        btnVoirResa.getStyleClass().add("btn-reservations-modern");

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.getStyleClass().add("btn-supprimer-modern");

        btnModifier.setOnAction(e -> modifierActivite(a));

        btnSupprimer.setOnAction(e -> supprimerActivite(a));

        actions.getChildren().addAll(btnModifier, btnVoirResa, btnSupprimer);
        content.getChildren().addAll(titre, lieu, date, priceRow);
        card.getChildren().addAll(imageStack, content, actions);

        return card;
    }



    private void modifierActivite(Activite a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddActivite.fxml"));
            Node addActiviteView = loader.load();
            AddActiviteController controller = loader.getController();

            Node currentCenter = borderPane.getCenter();
            controller.setPreviousView(currentCenter);
            controller.setMainBorderPane(borderPane);
            controller.setActivite(a);

            borderPane.setCenter(addActiviteView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void supprimerActivite(Activite a) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer activité");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette activité ?");
        alert.setContentText(a.getTitre());
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    activiteService.deleteOne(a);
                    chargerActivites();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void pagePrecedente() {
        if (currentPage > 1) {
            currentPage--;
            mettreAJourPage();
        }
    }

    @FXML
    private void pageSuivante() {
        if (currentPage < totalPages) {
            currentPage++;
            mettreAJourPage();
        }
    }





    @FXML
    private void handleNouvelleActivite() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddActivite.fxml"));
            Node addActiviteView = loader.load();


            AddActiviteController controller = loader.getController();


            Node currentCenter = borderPane.getCenter();
            controller.setPreviousView(currentCenter);
            controller.setMainBorderPane(borderPane);
            borderPane.setCenter(addActiviteView);

        } catch (IOException e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire.", ButtonType.OK);
            alert.show();
        }
    }



}