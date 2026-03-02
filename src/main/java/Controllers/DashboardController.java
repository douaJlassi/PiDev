package Controllers;


import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.Node;
import java.io.IOException;
import gestion_activite.Activite;
import services.ActiviteService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import Services.ServiceMessage;
import utils.EventBus;
import utils.NotificationUtils;


public class DashboardController {


    @FXML
    private Button mesReservationsButton;
    @FXML
    private FlowPane activitiesFlowPane;
    @FXML
    private Button btnTous, btnAventure, btnSport;
    @FXML
    private ComboBox<String> placesFilterCombo;

    @FXML
    private ScrollPane contentScrollPane;

    private ActiviteService activiteService;

    @FXML
    private StackPane notificationPane;

    @FXML
    private TextField customCategoryField;
    @FXML
    private Button searchCategoryButton;

    private int clientId = 5;
    private Node dashboardContent;

    private List<Activite> allActivites;
    private String currentCategory = null;
    private String currentPlacesFilter = "Tous";
    @FXML
    private VBox mainVBox;
    @FXML
    private Label lblBadge;
    private ServiceMessage serMsg = new ServiceMessage();
    private int currentUserId =0;
    private static DashboardController instance;
    public static DashboardController getInstance() {
        return instance;
    }
    public DashboardController() {
        activiteService = new ActiviteService();

    }

    public ScrollPane getContentScrollPane() {      // ADDED
        return contentScrollPane;
    }


    public void showDashboardView() {
        if (dashboardContent != null) {
            Node currentContent = contentScrollPane.getContent();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentContent);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                contentScrollPane.setContent(dashboardContent);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dashboardContent);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    private void showMesAchats() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesAchats.fxml"));
            Node view = loader.load();
            Controllers.MesAchatsController controller = loader.getController();
            controller.setClientId(clientId);
            controller.setDashboardController(this);

            if (dashboardContent == null) {
                dashboardContent = contentScrollPane.getContent();
            }

            Node current = contentScrollPane.getContent();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), current);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                contentScrollPane.setContent(view);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), view);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la liste des réservations.");
        }
    }

    @FXML
    public void initialize() {
        instance = this;

        Platform.runLater(() -> {
            refreshBadge();
        });

        try {


            dashboardContent = contentScrollPane.getContent();
            EventBus.getInstance().addListener(this::refreshActivities);
            List<Activite> all = activiteService.selectALL();
            allActivites = all.stream()
                    .filter(a -> "Actif".equals(a.getStatut()))
                    .collect(Collectors.toList());

            afficherActivites(allActivites);

            placesFilterCombo.getItems().addAll("Tous", "Disponible");
            placesFilterCombo.setValue("Tous");
            placesFilterCombo.setOnAction(e -> {
                currentPlacesFilter = placesFilterCombo.getValue();
                applyFilters();
            });

            btnTous.setOnAction(e -> {
                customCategoryField.clear();
                setCategoryFilter(null);
            });
            btnAventure.setOnAction(e -> setCategoryFilter("Aventure"));
            btnSport.setOnAction(e -> setCategoryFilter("Sport"));
            updateCategoryButtonStyles(null);

            searchCategoryButton.setOnAction(e -> {
                String customCat = customCategoryField.getText().trim();
                if (!customCat.isEmpty()) {
                    setCategoryFilter(customCat);
                } else {
                    setCategoryFilter(null);
                }
            });

            customCategoryField.setOnAction(e -> searchCategoryButton.fire());
            updateCategoryButtonStyles(null);

            mesReservationsButton.setOnAction(e -> showMesAchats());


            EventBus.getInstance().addListener(() -> {
                javafx.application.Platform.runLater(this::refreshActivities);
            });

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les activités.");
        }
    }



    public void refreshActivities() {
        try {
            List<Activite> all = activiteService.selectALL();
            allActivites = all.stream()
                    .filter(a -> "Actif".equals(a.getStatut()))
                    .collect(Collectors.toList());
            afficherActivites(allActivites);
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void openReservationWindow(Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReservationView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            Controllers.ReservationController controller = loader.getController();
            controller.setActivite(activite);
            controller.setStage(stage);
            controller.setClientId(clientId);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());

            stage.setTitle("Réserver - " + activite.getTitre());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();   // ← bloque jusqu'à la fermeture

            // Après fermeture de la fenêtre de réservation, on rafraîchit
            refreshActivities();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private void setCategoryFilter(String category) {
        currentCategory = category;
        updateCategoryButtonStyles(category);
        applyFilters();
    }

    private void updateCategoryButtonStyles(String selectedCategory) {
        String activeStyle = "filter-chip-active"; // define this CSS class in style.css
        btnTous.getStyleClass().remove(activeStyle);
        btnAventure.getStyleClass().remove(activeStyle);
        btnSport.getStyleClass().remove(activeStyle);

        if (selectedCategory == null) {
            btnTous.getStyleClass().add(activeStyle);
        } else if ("Aventure".equals(selectedCategory)) {
            btnAventure.getStyleClass().add(activeStyle);
        } else if ("Sport".equals(selectedCategory)) {
            btnSport.getStyleClass().add(activeStyle);
        }
    }

    private void applyFilters() {
        List<Activite> filtered = allActivites.stream()
                .filter(a -> {

                    if (currentCategory != null) {
                        String cat = a.getCategorie();
                        if (cat == null || !cat.equalsIgnoreCase(currentCategory)) {
                            return false;
                        }
                    }

                    if (currentPlacesFilter != null) {
                        switch (currentPlacesFilter) {
                            case "Disponible":
                                return a.getPlacesDisponibles() > 0;
                            case "Complet":
                                return a.getPlacesDisponibles() == 0;
                            case "Tous":
                            default:
                                return true;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        afficherActivites(filtered);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.show();
    }









    private void afficherActivites(List<Activite> activites) {
        activitiesFlowPane.getChildren().clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");

        for (Activite a : activites) {
            VBox card = new VBox();
            card.setPrefWidth(300);
            card.getStyleClass().add("card");

            ImageView imageView = new ImageView();
            imageView.setFitWidth(300);
            imageView.setFitHeight(160);
            try {
                String imageName = a.getImage();
                if (imageName == null || imageName.trim().isEmpty()) imageName = "default.jpg";
                if (!imageName.contains(".")) imageName += ".jpg";
                String path = "/images/" + imageName;
                java.net.URL resource = getClass().getResource(path);
                if (resource != null) {
                    imageView.setImage(new Image(resource.toExternalForm(), true));
                } else {
                    System.out.println("⚠️ Image not found: " + path + ". Trying default.jpg...");
                    java.net.URL defaultUrl = getClass().getResource("/images/default.jpg");
                    if (defaultUrl != null) {
                        imageView.setImage(new Image(defaultUrl.toExternalForm()));
                    } else {
                        System.err.println("❌ ERROR: default.jpg is missing from resources/images folder!");
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load image for: " + a.getTitre());
            }

            Rectangle clip = new Rectangle(300, 160);
            clip.setArcWidth(30);
            clip.setArcHeight(30);
            imageView.setClip(clip);

            StackPane imageStack = new StackPane();
            imageStack.setPrefHeight(160);
            imageStack.getStyleClass().add("card-image");
            imageStack.getChildren().add(imageView);

            String status = a.getStatut();
            Label badge = new Label(status != null ? status : "Activité");
            if (status != null && status.equalsIgnoreCase("Annulé")) {
                badge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            }
            badge.getStyleClass().add("badge-category");
            StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
            imageStack.getChildren().add(badge);


            VBox content = new VBox(8);
            content.getStyleClass().add("card-content");

            Text titre = new Text(a.getTitre());
            titre.getStyleClass().add("card-title");
            Label lieu = new Label("📍 " + a.getLieu());
            lieu.getStyleClass().add("card-location");

            int places = a.getPlacesDisponibles();
            String placesText;
            if (places == 0) {
                placesText = " Complet";
            } else {
                placesText = places + " place" + (places > 1 ? "s" : "") + " disponible" + (places > 1 ? "s" : "");
            }
            Label nbPlaces = new Label(placesText);
            if (places == 0) {
                nbPlaces.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                nbPlaces.getStyleClass().add("card-places");
            }

            Label date = new Label("📅 " + dateFormat.format(a.getDateActivite()));
            date.getStyleClass().add("card-date");

            HBox priceRow = new HBox();
            priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Text prix = new Text(String.format("%.0f DT", a.getPrix()));
            prix.getStyleClass().add("card-price");
            Label note = new Label("★ 4.9");
            note.getStyleClass().add("card-rating");
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            Label duree = new Label(a.getDureParJour() + (a.getDureParJour() > 1 ? " jours" : " jour"));
            duree.getStyleClass().add("card-duration");
            priceRow.getChildren().addAll(prix, note, spacer, duree);

            Button reserver = new Button("view more");
            reserver.getStyleClass().add("btn-reserve");
            reserver.setMaxWidth(Double.MAX_VALUE);


            if (places == 0) {
                reserver.setDisable(true);
                reserver.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666;"); // gray button
                card.setOpacity(0.6);
            } else {
                reserver.setOnAction(event -> openReservationWindow(a));
            }

            content.getChildren().addAll(titre, lieu, date, nbPlaces, priceRow, reserver);
            card.getChildren().addAll(imageStack, content);

            activitiesFlowPane.getChildren().add(card);
        }
    }
    public void refreshBadge() {
        int count = 0;
        try {
            count = serMsg.countUnreadMessages(currentUserId);
            System.out.println("DEBUG BADGE - Nombre trouvé : " + count);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (count > 0) {
            lblBadge.setText(String.valueOf(count > 99 ? "99+" : count)); // On limite à 99+
            lblBadge.setVisible(true);
            lblBadge.setManaged(true);
        } else {
            lblBadge.setVisible(false);
            lblBadge.setManaged(false);
        }

    }
    @FXML
    private void showChatView() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ChatView.fxml"));
            mainVBox.getChildren().setAll(root);
            refreshBadge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}