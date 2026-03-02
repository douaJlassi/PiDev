package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entities.Person;
import entities.Profile;
import entities.Purchase;
import entities.Shop;
import services.ProfileService;
import services.PurchaseService;
import services.ShopService;
import utils.EmailService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ShopController implements Initializable {

    @FXML
    private GridPane productsGrid;
    @FXML
    private Label coinsLabel;
    @FXML
    private Label welcomeLabel;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchBtn;
    @FXML
    private Button refreshBtn;
    @FXML
    private Button backBtn;
    @FXML
    private Button historyBtn;
    @FXML
    private Label statusLabel;

    private Person currentUser;
    private Profile userProfile;
    private ShopService shopService;
    private ProfileService profileService;
    private PurchaseService purchaseService;
    private ObservableList<Shop> productsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        shopService = new ShopService();
        profileService = new ProfileService();
        purchaseService = new PurchaseService();

        setupCategoryFilter();
        setupSearch();
        setupHistoryButton();
    }

    public void setUserData(Person user) {
        this.currentUser = user;
        loadUserProfile();
        loadProducts();
    }

    private void loadUserProfile() {
        try {
            userProfile = profileService.getProfileByUserId(currentUser.getId());
            if (userProfile != null) {
                welcomeLabel.setText("Welcome, " + currentUser.getUsername() + "!");
                coinsLabel.setText(userProfile.getCoins() + " 🪙");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load user profile: " + e.getMessage());
        }
    }

    private void loadProducts() {
        try {
            List<Shop> products = shopService.getAvailableProducts();
            productsList = FXCollections.observableArrayList(products);
            displayProducts(productsList);
            setupCategoryFilter(); // Refresh categories when products load
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load products: " + e.getMessage());
        }
    }

    private void displayProducts(ObservableList<Shop> products) {
        productsGrid.getChildren().clear();

        int column = 0;
        int row = 0;

        for (Shop product : products) {
            if (product != null) {
                VBox productCard = createProductCard(product);
                productsGrid.add(productCard, column, row);

                column++;
                if (column >= 3) {
                    column = 0;
                    row++;
                }
            }
        }
    }

    private VBox createProductCard(Shop product) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 15;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 15;"
        );
        card.setPrefWidth(250);
        card.setPrefHeight(350);

        // Product Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);

        if (product.getImage() != null && product.getImage().length > 0) {
            try {
                Image image = new Image(new ByteArrayInputStream(product.getImage()));
                imageView.setImage(image);
            } catch (Exception e) {
                // If image fails to load, use placeholder
                setPlaceholderImage(imageView);
            }
        } else {
            setPlaceholderImage(imageView);
        }

        // Product Name
        Label nameLabel = new Label(product.getName() != null ? product.getName() : "Unknown Product");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");
        nameLabel.setWrapText(true);

        // Product Description
        Label descLabel = new Label(product.getDescription() != null ? product.getDescription() : "No description available");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // Price and Quantity
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label priceLabel = new Label(product.getPriceCoins() + " 🪙");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FEC74C;");

        Label quantityLabel = new Label("Stock: " + product.getQuantity());
        quantityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");

        priceBox.getChildren().addAll(priceLabel, quantityLabel);

        // Buy Button
        Button buyBtn = new Button("BUY NOW");
        buyBtn.setStyle(
                "-fx-background-color: #0FA5A2;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 25;" +
                        "-fx-cursor: hand;"
        );
        buyBtn.setMaxWidth(Double.MAX_VALUE);

        // Check if user can afford
        if (userProfile != null && userProfile.getCoins() < product.getPriceCoins()) {
            buyBtn.setDisable(true);
            buyBtn.setText("INSUFFICIENT COINS");
            buyBtn.setStyle(
                    "-fx-background-color: #ccc;" +
                            "-fx-text-fill: #666;" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 10 20;" +
                            "-fx-background-radius: 25;"
            );
        } else if (product.getQuantity() <= 0) {
            buyBtn.setDisable(true);
            buyBtn.setText("OUT OF STOCK");
            buyBtn.setStyle(
                    "-fx-background-color: #ccc;" +
                            "-fx-text-fill: #666;" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 10 20;" +
                            "-fx-background-radius: 25;"
            );
        }

        buyBtn.setOnAction(e -> showPurchaseDialog(product));

        card.getChildren().addAll(imageView, nameLabel, descLabel, priceBox, buyBtn);

        return card;
    }

    private void setPlaceholderImage(ImageView imageView) {
        try {
            imageView.setImage(new Image(getClass().getResourceAsStream("/image/placeholder.png")));
        } catch (Exception e) {
            // If placeholder not found, leave image empty
            imageView.setImage(null);
        }
    }




    private void showPurchaseDialog(Shop product) {
        try {
            // Load the new purchase dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PurchaseDialog.fxml"));
            DialogPane dialogPane = loader.load();

            // Get controller and set product/user details
            PurchaseDialogController dialogController = loader.getController();
            dialogController.setProductDetails(product.getName(), product.getPriceCoins());
            dialogController.setUserDetails(currentUser.getUsername(), currentUser.getEmail());

            // Create and show dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Complete Your Purchase");

            Optional<ButtonType> result = dialog.showAndWait();

            // Find the confirm button type from dialog pane
            ButtonType confirmButton = dialogPane.getButtonTypes().stream()
                    .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    .findFirst()
                    .orElse(null);

            if (result.isPresent() && result.get() == confirmButton) {
                // Validate inputs
                if (!dialogController.isAddressSelected()) {
                    showAlert("Error", "Please select a delivery address on the map");
                    return;
                }

                if (dialogController.getBuyerAddress() == null ||
                        dialogController.getBuyerAddress().trim().isEmpty()) {
                    showAlert("Error", "Please select a delivery address");
                    return;
                }

                // Process purchase with map coordinates
                processPurchaseWithMap(
                        product,
                        dialogController.getBuyerName(),
                        dialogController.getBuyerEmail(),
                        dialogController.getBuyerAddress(),
                        dialogController.getDeliveryInstructions(),
                        dialogController.getSelectedLat(),
                        dialogController.getSelectedLng()
                );
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open purchase dialog: " + e.getMessage());
        }
    }

    private void processPurchaseWithMap(Shop product, String name, String email,
                                        String address, String instructions,
                                        double lat, double lng) {
        try {
            if (userProfile == null) {
                showAlert("Error", "User profile not found!");
                return;
            }

            int userCoins = userProfile.getCoins();
            int price = product.getPriceCoins();

            if (userCoins < price) {
                showAlert("Error", "Insufficient coins!");
                return;
            }

            if (product.getQuantity() <= 0) {
                showAlert("Error", "Product out of stock!");
                return;
            }

            // Format address with coordinates and instructions
            StringBuilder fullAddress = new StringBuilder(address);
            fullAddress.append("\n📍 Coordinates: ").append(String.format("%.6f, %.6f", lat, lng));

            if (instructions != null && !instructions.trim().isEmpty()) {
                fullAddress.append("\n📝 Instructions: ").append(instructions);
            }

            // Deduct coins
            userProfile.setCoins(userCoins - price);
            profileService.updateOne(userProfile);

            // Decrease product quantity
            shopService.decreaseQuantity(product.getId(), 1);

            // Create purchase record
            Purchase purchase = new Purchase();
            purchase.setUserId(currentUser.getId());
            purchase.setShopId(product.getId());
            purchase.setQuantity(1);
            purchase.setTotalCoins(price);
            purchase.setBuyerName(name);
            purchase.setBuyerEmail(email);
            purchase.setBuyerAddress(fullAddress.toString());

            purchaseService.insertPurchase(purchase);

            // Send confirmation email with map link
            sendPurchaseConfirmationWithMap(product, name, email, fullAddress.toString(),
                    purchase.getId(), lat, lng, instructions);

            // Update UI
            coinsLabel.setText(userProfile.getCoins() + " 🪙");
            loadProducts(); // Refresh products

            showAlert("Success", "Purchase completed successfully!\n\n" +
                    "Product: " + product.getName() + "\n" +
                    "Order ID: " + purchase.getId() + "\n" +
                    "Delivery Location: Selected on map\n" +
                    "Confirmation email sent to: " + email);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Purchase failed: " + e.getMessage());
        }
    }

    private void sendPurchaseConfirmationWithMap(Shop product, String name, String email,
                                                 String address, int orderId,
                                                 double lat, double lng, String instructions) {
        String subject = "✅ Purchase Confirmation with Map - Rehletna.tn Shop";

        String mapLink = String.format("https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=15/%f/%f",
                lat, lng, lat, lng);

        String emailContent = "<html>" +
                "<head><style>" +
                "body { font-family: Arial, sans-serif; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "h1 { color: #0FA5A2; }" +
                ".details { background-color: #f8f9fa; padding: 15px; border-radius: 10px; }" +
                ".order-id { font-size: 24px; color: #FEC74C; font-weight: bold; }" +
                ".map-link { background-color: #1D4D7C; color: white; padding: 10px 20px; " +
                "text-decoration: none; border-radius: 5px; display: inline-block; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "<h1>✨ Purchase Confirmation with Map ✨</h1>" +
                "<p>Dear " + name + ",</p>" +
                "<p>Thank you for your purchase from Rehletna.tn Shop!</p>" +
                "<div class='details'>" +
                "<h3>Order Details:</h3>" +
                "<p><strong>Order ID:</strong> <span class='order-id'>#" + orderId + "</span></p>" +
                "<p><strong>Product:</strong> " + product.getName() + "</p>" +
                "<p><strong>Price:</strong> " + product.getPriceCoins() + " coins</p>" +
                "<p><strong>Delivery Address:</strong><br>" + address.replace("\n", "<br>") + "</p>" +
                "<p><strong>Delivery Location:</strong><br>" +
                "<a href='" + mapLink + "' class='map-link' target='_blank'>🗺️ View on Map</a></p>" +
                "</div>" +
                "<p>Your order has been confirmed and will be processed soon.</p>" +
                "<hr>" +
                "<p style='color: #999; font-size: 12px;'>© 2025 Rehletna.tn - All rights reserved</p>" +
                "</div>" +
                "</body>" +
                "</html>";

        EmailService.sendEmail(email, subject, emailContent);
    }

    private void setupCategoryFilter() {
        if (categoryFilter == null) return;

        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All Categories");

        try {
            List<Shop> products = shopService.getAvailableProducts();
            if (products != null) {
                products.stream()
                        .map(Shop::getCategory)
                        .filter(Objects::nonNull)
                        .filter(cat -> !cat.trim().isEmpty())
                        .distinct()
                        .forEach(cat -> categoryFilter.getItems().add(cat));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Set default value safely
        categoryFilter.setValue("All Categories");

        // Add event handler with null check
        categoryFilter.setOnAction(e -> {
            if (categoryFilter.getValue() != null) {
                filterProducts();
            }
        });
    }

    private void setupSearch() {
        if (searchBtn != null) {
            searchBtn.setOnAction(e -> filterProducts());
        }

        if (refreshBtn != null) {
            refreshBtn.setOnAction(e -> {
                searchField.clear();
                categoryFilter.setValue("All Categories");
                loadProducts();
            });
        }

        if (backBtn != null) {
            backBtn.setOnAction(e -> goBackToMain());
        }
    }

    private void setupHistoryButton() {
        if (historyBtn != null) {
            historyBtn.setOnAction(e -> viewPurchaseHistory());
        }
    }

    private void filterProducts() {
        String searchText = searchField.getText();
        final String finalSearchText = searchText != null ? searchText.toLowerCase().trim() : "";

        String selectedCategory = categoryFilter.getValue();
        // Safety check for null category
        final String finalSelectedCategory = selectedCategory != null ? selectedCategory : "All Categories";

        try {
            List<Shop> allProducts = shopService.getAvailableProducts();
            if (allProducts == null) {
                allProducts = List.of();
            }

            List<Shop> filtered = allProducts.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> {
                        // Category filter with null safety
                        String productCategory = p.getCategory();
                        boolean matchCategory = finalSelectedCategory.equals("All Categories") ||
                                (productCategory != null && finalSelectedCategory.equals(productCategory));

                        // Search filter with null safety
                        boolean matchSearch = finalSearchText.isEmpty() ||
                                (p.getName() != null && p.getName().toLowerCase().contains(finalSearchText)) ||
                                (p.getDescription() != null && p.getDescription().toLowerCase().contains(finalSearchText));

                        return matchCategory && matchSearch;
                    })
                    .collect(Collectors.toList());

            displayProducts(FXCollections.observableArrayList(filtered));

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to filter products: " + e.getMessage());
        }
    }

    private void viewPurchaseHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PurchaseHistory.fxml"));
            Parent root = loader.load();

            PurchaseHistoryController historyController = loader.getController();
            historyController.setUserData(currentUser);
            historyController.setAdminView(false); // false for user view, true for admin

            Stage stage = (Stage) historyBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Purchase History - " + currentUser.getUsername());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open purchase history: " + e.getMessage());
        }
    }

    private void goBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainPage.fxml"));
            Parent mainRoot = loader.load();

            MainPageController mainController = loader.getController();
            mainController.setUserData(currentUser);

            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(mainRoot));
            stage.setTitle("Main Page - " + currentUser.getUsername());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to main page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}