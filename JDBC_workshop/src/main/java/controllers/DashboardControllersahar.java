package controllers;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import services.WeatherServiceActi;
import java.time.LocalDateTime;

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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import utils.EventBus;
import entities.Person;
import controllers.MainPageController;

public class DashboardControllersahar extends MainPageController {

    // Add currentUser field
    private Person currentUser;

    // Remove mesReservationsButton since it's not in the new FXML
    // @FXML
    // private Button mesReservationsButton;

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

    // New FXML elements from MainPage

    @FXML
    private Button activitiesBtn;





    @FXML
    private HBox coinCollectionBox;


    private WeatherServiceActi weatherServiceActi = new WeatherServiceActi();

    private int clientId = 5; // Default, will be updated from user data
    private Node dashboardContent;

    private List<Activite> allActivites;
    private String currentCategory = null;
    private String currentPlacesFilter = "Tous";

    public DashboardControllersahar() {
        super();
        activiteService = new ActiviteService();
    }

    /**
     * Set user data from MainPageController
     */
    public void setUserData(Person user) {
        this.currentUser = user;
        if (user != null) {
            // Update clientId with the actual user ID
            this.clientId = user.getId();
            System.out.println("DashboardControllersahar: User data set - " + user.getUsername() + " (ID: " + clientId + ")");
            updateUIWithUserData();
        }
    }

    /**
     * Update UI based on user data
     */
    private void updateUIWithUserData() {
        if (currentUser != null) {
            System.out.println("Welcome " + currentUser.getUsername() + " to Activities page!");

            String role = currentUser.getRole();
            if (role != null && role.toLowerCase().contains("admin")) {
                System.out.println("Admin user logged in");
            }
        }
    }

    public ScrollPane getContentScrollPane() {
        return contentScrollPane;
    }

    @FXML
    public void initialize() {
        try {
            System.out.println("DashboardControllersahar initialize started");

            dashboardContent = contentScrollPane.getContent();
            EventBus.getInstance().addListener(this::refreshActivities);

            // Load activities
            List<Activite> all = activiteService.selectALL();
            allActivites = all.stream()
                    .filter(a -> "Actif".equals(a.getStatut()))
                    .collect(Collectors.toList());

            afficherActivites(allActivites);

            // Setup filter controls
            setupFilterControls();

            // Setup navigation buttons


            // Setup coin collection (will be visible after user data is set)
            if (coinCollectionBox != null) {
                coinCollectionBox.setVisible(false);
            }

            System.out.println("DashboardControllersahar initialize completed");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les activités.");
        }
    }

    private void setupFilterControls() {
        // Places filter combo box
        placesFilterCombo.getItems().addAll("Tous", "Disponible");
        placesFilterCombo.setValue("Tous");
        placesFilterCombo.setOnAction(e -> {
            currentPlacesFilter = placesFilterCombo.getValue();
            applyFilters();
        });

        // Category buttons
        btnTous.setOnAction(e -> {
            customCategoryField.clear();
            setCategoryFilter(null);
        });
        btnAventure.setOnAction(e -> setCategoryFilter("Aventure"));
        btnSport.setOnAction(e -> setCategoryFilter("Sport"));
        updateCategoryButtonStyles(null);

        // Search button
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
    }



    private void addHoverEffect(Button button) {
        if (button == null) return;

        button.setOnMouseEntered(e -> {
            if (button == activitiesBtn) {
                button.setStyle("-fx-background-color: #ffe0e0; -fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand;");
            } else if (button == activitiesBtn) {
                button.setStyle("-fx-background-color: #d0f0f0; -fx-text-fill: #0FA5A2; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            } else {
                button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            }
        });

        button.setOnMouseExited(e -> {
            if (button == activitiesBtn) {
                button.setStyle("-fx-background-color: #fff0f0; -fx-text-fill: #ff5e62; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand;");
            } else if (button == activitiesBtn) {
                button.setStyle("-fx-background-color: #e6f7f5; -fx-text-fill: #0FA5A2; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            } else {
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            }
        });
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleLogout() {
        System.out.println("Logout clicked");
        // Implement logout logic
        showMessage("Logout - Coming soon!");
    }

    private void collectCoin() {
        System.out.println("Collect coin clicked");
        // Implement coin collection logic
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            ReservationController controller = loader.getController();
            controller.setActivite(activite);
            controller.setStage(stage);
            controller.setClientId(clientId);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

            stage.setTitle("Réserver - " + activite.getTitre());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

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
        String activeStyle = "filter-chip-active";
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
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

            // Image
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

            // Badge
            String status = a.getStatut();
            Timestamp creation = a.getDateCreation();
            boolean isNew = (creation != null) && (System.currentTimeMillis() - creation.getTime() < 24 * 60 * 60 * 1000);

            Label badge = new Label();
            if (isNew) {
                badge.setText("NEW!");
                badge.setStyle("-fx-background-color: #FEC74C; -fx-text-fill: #1D4D7C; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15;");
            } else {
                badge.setText(status != null ? status : "Activité");
                if (status != null && status.equalsIgnoreCase("Annulé")) {
                    badge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15;");
                } else {
                    badge.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15;");
                }
            }
            StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
            imageStack.getChildren().add(badge);

            // Content
            VBox content = new VBox(8);
            content.setPadding(new Insets(15));
            content.getStyleClass().add("card-content");

            Text titre = new Text(a.getTitre());
            titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #1D4D7C;");

            Label lieu = new Label("📍 " + a.getLieu());
            lieu.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

            int places = a.getPlacesDisponibles();
            String placesText = (places == 0) ? " Complet" : places + " place" + (places > 1 ? "s" : "") + " disponible" + (places > 1 ? "s" : "");
            Label nbPlaces = new Label(placesText);
            if (places == 0) {
                nbPlaces.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                nbPlaces.setStyle("-fx-text-fill: #0FA5A2; -fx-font-weight: bold; -fx-font-size: 14px;");
            }

            Label date = new Label("📅 " + dateFormat.format(a.getDateActivite()));
            date.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");

            // Price row
            HBox priceRow = new HBox();
            priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Text prix = new Text(String.format("%.0f DT", a.getPrix()));
            prix.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #0FA5A2;");

            Label note = new Label("★ 4.9");
            note.setStyle("-fx-text-fill: #FEC74C; -fx-font-weight: bold; -fx-font-size: 14px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Label duree = new Label(a.getDureParJour() + (a.getDureParJour() > 1 ? " jours" : " jour"));
            duree.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");

            priceRow.getChildren().addAll(prix, note, spacer, duree);

            // Weather box
            HBox weatherBox = new HBox(8);
            weatherBox.setAlignment(Pos.CENTER_LEFT);
            weatherBox.setPadding(new Insets(5, 8, 5, 8));
            weatherBox.setStyle("-fx-background-color: #f0f7f7; -fx-background-radius: 20;");
            weatherBox.setVisible(false);

            ImageView weatherIcon = new ImageView();
            weatherIcon.setFitWidth(28);
            weatherIcon.setFitHeight(28);

            VBox weatherText = new VBox(2);
            weatherText.setAlignment(Pos.CENTER_LEFT);
            Label tempLabel = new Label();
            tempLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0FA5A2;");
            Label descLabel = new Label();
            descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
            weatherText.getChildren().addAll(tempLabel, descLabel);

            weatherBox.getChildren().addAll(weatherIcon, weatherText);

            // Reserve button
            Button reserver = new Button("Réserver");
            reserver.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
            reserver.setMaxWidth(Double.MAX_VALUE);

            if (places == 0) {
                reserver.setDisable(true);
                reserver.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25;");
                card.setOpacity(0.6);
            } else {
                reserver.setOnAction(event -> openReservationWindow(a));
            }

            reserver.setOnMouseEntered(e -> {
                if (!reserver.isDisable()) {
                    reserver.setStyle("-fx-background-color: #1D4D7C; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
                }
            });
            reserver.setOnMouseExited(e -> {
                if (!reserver.isDisable()) {
                    reserver.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 25; -fx-cursor: hand;");
                }
            });

            // Assemble
            content.getChildren().addAll(titre, lieu, date, nbPlaces, priceRow, weatherBox, reserver);
            card.getChildren().addAll(imageStack, content);
            activitiesFlowPane.getChildren().add(card);

            // Load weather
            loadWeatherForActivity(a, tempLabel, descLabel, weatherIcon, weatherBox);
        }
    }

    private void loadWeatherForActivity(Activite a, Label tempLabel, Label descLabel, ImageView icon, HBox weatherBox) {
        LocalDateTime activityDateTime = a.getDateActivite().toLocalDateTime();
        if (activityDateTime.isAfter(LocalDateTime.now().plusDays(5))) {
            return;
        }
        weatherServiceActi.getWeatherForCityAndDateTime(a.getLieu(), activityDateTime)
                .thenAccept(weatherInfo -> {
                    javafx.application.Platform.runLater(() -> {
                        if (weatherInfo != null) {
                            tempLabel.setText(String.format("%.1f°C", weatherInfo.getTemperature()));
                            descLabel.setText(weatherInfo.getDescription());
                            icon.setImage(new Image(weatherInfo.getIconUrl(), true));
                            weatherBox.setVisible(true);
                        } else {
                            weatherBox.setVisible(false);
                        }
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> weatherBox.setVisible(false));
                    return null;
                });
    }

    public Person getCurrentUser() {
        return currentUser;
    }

    public int getClientId() {
        return clientId;
    }
}