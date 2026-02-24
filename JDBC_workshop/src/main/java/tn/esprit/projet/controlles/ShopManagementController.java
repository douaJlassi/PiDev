package tn.esprit.projet.controlles;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.entities.Shop;
import tn.esprit.projet.services.ShopService;

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

        shopTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedShop = newSelection;
                displayShopDetails(selectedShop);
            }
        });
    }

    private void loadShopData() {
        try {
            List<Shop> shops = shopService.selectALL();
            shopList = FXCollections.observableArrayList(shops);
            shopTable.setItems(shopList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load shop data: " + e.getMessage());
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

        addBtn.setDisable(true);
        updateBtn.setDisable(false);
        deleteBtn.setDisable(false);
    }

    private void setupButtons() {
        chooseImageBtn.setOnAction(e -> chooseImage());
        addBtn.setOnAction(e -> addProduct());
        updateBtn.setOnAction(e -> updateProduct());
        deleteBtn.setOnAction(e -> deleteProduct());
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

    private void addProduct() {
        if (!validateInputs()) return;

        try {
            Shop shop = new Shop();
            shop.setName(nameField.getText().trim());
            shop.setDescription(descriptionArea.getText().trim());
            shop.setPriceCoins(Integer.parseInt(priceField.getText().trim()));
            shop.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            shop.setCategory(categoryField.getText().trim());
            shop.setImage(selectedImageBytes);

            shopService.insertOne(shop);

            loadShopData();
            clearForm();

            statusLabel.setText("Product added successfully!");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            statusLabel.setText("Failed to add product: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #ff5e62;");
        }
    }

    private void updateProduct() {
        if (selectedShop == null) {
            showAlert("Error", "Please select a product to update");
            return;
        }

        if (!validateInputs()) return;

        try {
            selectedShop.setName(nameField.getText().trim());
            selectedShop.setDescription(descriptionArea.getText().trim());
            selectedShop.setPriceCoins(Integer.parseInt(priceField.getText().trim()));
            selectedShop.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            selectedShop.setCategory(categoryField.getText().trim());
            if (selectedImageBytes != null) {
                selectedShop.setImage(selectedImageBytes);
            }

            shopService.updateOne(selectedShop);

            loadShopData();
            clearForm();

            statusLabel.setText("Product updated successfully!");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            statusLabel.setText("Failed to update product: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #ff5e62;");
        }
    }

    private void deleteProduct() {
        if (selectedShop == null) {
            showAlert("Error", "Please select a product to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Product");
        confirm.setContentText("Are you sure you want to delete " + selectedShop.getName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                shopService.deleteOne(selectedShop);
                loadShopData();
                clearForm();

                statusLabel.setText("Product deleted successfully!");
                statusLabel.setStyle("-fx-text-fill: #2ecc71;");

            } catch (SQLException e) {
                e.printStackTrace();
                statusLabel.setText("Failed to delete product: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: #ff5e62;");
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

        addBtn.setDisable(false);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);

        shopTable.getSelectionModel().clearSelection();
    }

    private boolean validateInputs() {
        if (nameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Product name is required");
            return false;
        }

        try {
            Integer.parseInt(priceField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Price must be a valid number");
            return false;
        }

        try {
            Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Quantity must be a valid number");
            return false;
        }

        return true;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}