package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import entities.Person;
import entities.Shop;
import services.ShopService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ShopManagementController implements Initializable {

    @FXML
    private TableView<Shop> shopTable;
    @FXML
    private TableColumn<Shop, Integer> idColumn;
    @FXML
    private TableColumn<Shop, String> nameColumn;
    @FXML
    private TableColumn<Shop, Integer> priceColumn;
    @FXML
    private TableColumn<Shop, Integer> quantityColumn;
    @FXML
    private TableColumn<Shop, String> categoryColumn;

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField priceField;
    @FXML
    private TextField quantityField;
    @FXML
    private TextField categoryField;
    @FXML
    private ImageView productImageView;
    @FXML
    private Button chooseImageBtn;
    @FXML
    private Button addBtn;
    @FXML
    private Button updateBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button clearBtn;
    @FXML
    private Label statusLabel;

    private ShopService shopService;
    private ObservableList<Shop> shopList;
    private Shop selectedShop;
    private byte[] selectedImageBytes;
    private Person currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        shopService = new ShopService();

        setupTable();
        loadShopData();
        setupButtons();

        // Clear form initially
        clearForm();
    }

    public void setUserData(Person user) {
        this.currentUser = user;
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("priceCoins"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        // Add selection listener
        shopTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedShop = newSelection;
                displayShopDetails(selectedShop);
                addBtn.setDisable(true);
                updateBtn.setDisable(false);
                deleteBtn.setDisable(false);
            }
        });
    }

    private void loadShopData() {
        try {
            List<Shop> shops = shopService.selectALL();
            shopList = FXCollections.observableArrayList(shops);
            shopTable.setItems(shopList);
            statusLabel.setText("Loaded " + shops.size() + " products");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Failed to load products: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #ff5e62;");
        }
    }

    private void displayShopDetails(Shop shop) {
        nameField.setText(shop.getName());
        descriptionArea.setText(shop.getDescription());
        priceField.setText(String.valueOf(shop.getPriceCoins()));
        quantityField.setText(String.valueOf(shop.getQuantity()));
        categoryField.setText(shop.getCategory());

        if (shop.getImage() != null && shop.getImage().length > 0) {
            Image image = new Image(new ByteArrayInputStream(shop.getImage()));
            productImageView.setImage(image);
            selectedImageBytes = shop.getImage();
        } else {
            productImageView.setImage(null);
            selectedImageBytes = null;
        }
    }

    private void setupButtons() {
        // Choose Image Button
        chooseImageBtn.setOnAction(e -> chooseImage());

        // Add Button - This is what you asked for
        addBtn.setOnAction(e -> addProduct());

        // Update Button
        updateBtn.setOnAction(e -> updateProduct());

        // Delete Button
        deleteBtn.setOnAction(e -> deleteProduct());

        // Clear Button
        clearBtn.setOnAction(e -> clearForm());
    }

    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(chooseImageBtn.getScene().getWindow());
        if (selectedFile != null) {
            try {
                selectedImageBytes = Files.readAllBytes(selectedFile.toPath());
                Image image = new Image(new ByteArrayInputStream(selectedImageBytes));
                productImageView.setImage(image);
                statusLabel.setText("Image selected: " + selectedFile.getName());
                statusLabel.setStyle("-fx-text-fill: #2ecc71;");
            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Failed to load image: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: #ff5e62;");
            }
        }
    }

    /**
     * Add Product - This method handles adding a new product
     * Triggered when user clicks the Add button after filling the form
     */
    private void addProduct() {
        // Validate all inputs first
        if (!validateInputs()) {
            return;
        }

        try {
            // Create new Shop object with form data
            Shop shop = new Shop();
            shop.setName(nameField.getText().trim());
            shop.setDescription(descriptionArea.getText().trim());
            shop.setPriceCoins(Integer.parseInt(priceField.getText().trim()));
            shop.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            shop.setCategory(categoryField.getText().trim());
            shop.setImage(selectedImageBytes); // May be null if no image selected

            // Save to database
            shopService.insertOne(shop);

            // Refresh the table to show new product
            loadShopData();

            // Clear the form for next entry
            clearForm();

            // Show success message
            statusLabel.setText("✅ Product added successfully: " + shop.getName());
            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

            // Optional: Show alert for confirmation
            showAlert("Success", "Product \"" + shop.getName() + "\" added successfully!", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to add product: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-weight: bold;");
            showAlert("Error", "Failed to add product: " + e.getMessage(), Alert.AlertType.ERROR);

        } catch (NumberFormatException e) {
            statusLabel.setText("❌ Invalid number format");
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-weight: bold;");
        }
    }

    /**
     * Validate all input fields before adding/updating
     */
    private boolean validateInputs() {
        // Check required fields
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Product name is required", Alert.AlertType.WARNING);
            nameField.requestFocus();
            return false;
        }

        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Product description is required", Alert.AlertType.WARNING);
            descriptionArea.requestFocus();
            return false;
        }

        if (categoryField.getText() == null || categoryField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Product category is required", Alert.AlertType.WARNING);
            categoryField.requestFocus();
            return false;
        }

        // Validate price
        try {
            int price = Integer.parseInt(priceField.getText().trim());
            if (price <= 0) {
                showAlert("Validation Error", "Price must be greater than 0", Alert.AlertType.WARNING);
                priceField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Price must be a valid number", Alert.AlertType.WARNING);
            priceField.requestFocus();
            return false;
        }

        // Validate quantity
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 0) {
                showAlert("Validation Error", "Quantity cannot be negative", Alert.AlertType.WARNING);
                quantityField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Quantity must be a valid number", Alert.AlertType.WARNING);
            quantityField.requestFocus();
            return false;
        }

        return true;
    }

    private void updateProduct() {
        if (selectedShop == null) {
            showAlert("Error", "Please select a product to update", Alert.AlertType.WARNING);
            return;
        }

        if (!validateInputs()) {
            return;
        }

        try {
            selectedShop.setName(nameField.getText().trim());
            selectedShop.setDescription(descriptionArea.getText().trim());
            selectedShop.setPriceCoins(Integer.parseInt(priceField.getText().trim()));
            selectedShop.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            selectedShop.setCategory(categoryField.getText().trim());

            // Only update image if a new one was selected
            if (selectedImageBytes != null) {
                selectedShop.setImage(selectedImageBytes);
            }

            shopService.updateOne(selectedShop);
            loadShopData();
            clearForm();

            statusLabel.setText("✅ Product updated successfully: " + selectedShop.getName());
            statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            showAlert("Success", "Product updated successfully!", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to update product: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-weight: bold;");
        }
    }

    private void deleteProduct() {
        if (selectedShop == null) {
            showAlert("Error", "Please select a product to delete", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Product");
        confirm.setContentText("Are you sure you want to delete \"" + selectedShop.getName() + "\"?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                shopService.deleteOne(selectedShop);
                loadShopData();
                clearForm();

                statusLabel.setText("✅ Product deleted successfully");
                statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                showAlert("Success", "Product deleted successfully!", Alert.AlertType.INFORMATION);

            } catch (SQLException e) {
                e.printStackTrace();
                statusLabel.setText("❌ Failed to delete product: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: #ff5e62; -fx-font-weight: bold;");
            }
        }
    }

    private void clearForm() {
        nameField.clear();
        descriptionArea.clear();
        priceField.clear();
        quantityField.clear();
        categoryField.clear();
        productImageView.setImage(null);
        selectedImageBytes = null;
        selectedShop = null;

        // Reset button states
        addBtn.setDisable(false);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);

        // Clear table selection
        shopTable.getSelectionModel().clearSelection();

        statusLabel.setText("Ready to add new product");
        statusLabel.setStyle("-fx-text-fill: #666;");
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}