package tn.esprit.projet.controlles;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.entities.Purchase;
import tn.esprit.projet.services.PurchaseHistoryService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class PurchaseHistoryController implements Initializable {

    @FXML
    private TableView<Purchase> purchaseTable;
    @FXML
    private TableColumn<Purchase, Integer> idColumn;
    @FXML
    private TableColumn<Purchase, Timestamp> dateColumn;
    @FXML
    private TableColumn<Purchase, String> productColumn;
    @FXML
    private TableColumn<Purchase, Integer> quantityColumn;
    @FXML
    private TableColumn<Purchase, Integer> priceColumn;
    @FXML
    private TableColumn<Purchase, String> statusColumn;
    @FXML
    private TableColumn<Purchase, String> buyerColumn;
    @FXML
    private TableColumn<Purchase, Void> actionColumn;

    @FXML
    private Button backBtn;
    @FXML
    private Button refreshBtn;

    @FXML
    private Label totalPurchasesLabel;
    @FXML
    private Label totalSpentLabel;
    @FXML
    private Label uniqueProductsLabel;
    @FXML
    private Label emptyStateLabel;

    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchBtn;

    private Person currentUser;
    private PurchaseHistoryService historyService;
    private ObservableList<Purchase> purchaseList;
    private boolean isAdminView = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        historyService = new PurchaseHistoryService();
        setupTableColumns();
        setupTableStyle();
        setupSortComboBox();
        setupSearch();
    }

    public void setUserData(Person user) {
        this.currentUser = user;
        loadPurchaseHistory();
    }

    public void setAdminView(boolean isAdmin) {
        this.isAdminView = isAdmin;
        if (isAdmin) {
            buyerColumn.setVisible(true);
            actionColumn.setVisible(true);
            loadAllPurchases();
        } else {
            buyerColumn.setVisible(false);
            actionColumn.setVisible(false);
        }
    }

    private void setupTableColumns() {
        // Order ID Column
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        idColumn.setCellFactory(column -> new TableCell<Purchase, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                } else {
                    setText("#" + String.format("%04d", id));
                    setStyle("-fx-text-fill: #1D4D7C; -fx-font-weight: bold;");
                }
            }
        });

        // Date Column
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        dateColumn.setStyle("-fx-alignment: CENTER;");
        dateColumn.setCellFactory(column -> new TableCell<Purchase, Timestamp>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

            @Override
            protected void updateItem(Timestamp timestamp, boolean empty) {
                super.updateItem(timestamp, empty);
                if (empty || timestamp == null) {
                    setText(null);
                } else {
                    setText(timestamp.toLocalDateTime().format(formatter));
                    setStyle("-fx-text-fill: #666;");
                }
            }
        });

        // Product Column with icon
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        productColumn.setCellFactory(column -> new TableCell<Purchase, String>() {
            @Override
            protected void updateItem(String product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    Label iconLabel = new Label("🛍️");
                    iconLabel.setStyle("-fx-font-size: 16px;");

                    Label nameLabel = new Label(product);
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

                    hbox.getChildren().addAll(iconLabel, nameLabel);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        // Quantity Column
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setStyle("-fx-alignment: CENTER;");
        quantityColumn.setCellFactory(column -> new TableCell<Purchase, Integer>() {
            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null);
                } else {
                    setText("x" + qty);
                    setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 12; -fx-padding: 2 8;");
                }
            }
        });

        // Price Column
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("totalCoins"));
        priceColumn.setStyle("-fx-alignment: CENTER;");
        priceColumn.setCellFactory(column -> new TableCell<Purchase, Integer>() {
            @Override
            protected void updateItem(Integer price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    Label priceLabel = new Label(price + " 🪙");
                    priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FEC74C;");
                    setGraphic(priceLabel);
                    setText(null);
                }
            }
        });

        // Status Column with custom styling
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setCellFactory(column -> new TableCell<Purchase, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Label statusLabel = new Label(status.toUpperCase());
                    statusLabel.setStyle(getStatusStyle(status));
                    statusLabel.setAlignment(Pos.CENTER);
                    statusLabel.setPrefWidth(80);
                    statusLabel.setPrefHeight(24);
                    setGraphic(statusLabel);
                    setText(null);
                }
            }

            private String getStatusStyle(String status) {
                switch (status.toLowerCase()) {
                    case "pending":
                        return "-fx-background-color: #FFF3CD; -fx-text-fill: #856404; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 2 8;";
                    case "confirmed":
                    case "completed":
                        return "-fx-background-color: #D4EDDA; -fx-text-fill: #155724; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 2 8;";
                    case "cancelled":
                        return "-fx-background-color: #F8D7DA; -fx-text-fill: #721C24; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 2 8;";
                    default:
                        return "-fx-background-color: #E2E3E5; -fx-text-fill: #383D41; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 2 8;";
                }
            }
        });

        // Buyer Column
        buyerColumn.setCellValueFactory(new PropertyValueFactory<>("buyerUsername"));
        buyerColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        buyerColumn.setCellFactory(column -> new TableCell<Purchase, String>() {
            @Override
            protected void updateItem(String buyer, boolean empty) {
                super.updateItem(buyer, empty);
                if (empty || buyer == null) {
                    setText(null);
                } else {
                    HBox hbox = new HBox(8);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    Label iconLabel = new Label("👤");
                    iconLabel.setStyle("-fx-font-size: 14px;");

                    Label nameLabel = new Label(buyer);
                    nameLabel.setStyle("-fx-text-fill: #666;");

                    hbox.getChildren().addAll(iconLabel, nameLabel);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        // Action Column (View Details)
        actionColumn.setCellFactory(param -> new TableCell<Purchase, Void>() {
            private final Button viewBtn = new Button("👁️ View");

            {
                viewBtn.setStyle(
                        "-fx-background-color: #0FA5A2;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 11px;" +
                                "-fx-padding: 5 10;" +
                                "-fx-background-radius: 15;" +
                                "-fx-cursor: hand;"
                );
                viewBtn.setOnAction(event -> {
                    Purchase purchase = getTableView().getItems().get(getIndex());
                    showPurchaseDetails(purchase);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewBtn);
                }
            }
        });

        backBtn.setOnAction(e -> goBack());
    }

    private void setupTableStyle() {
        purchaseTable.setStyle(
                "-fx-font-family: 'Segoe UI';" +
                        "-fx-font-size: 13px;"
        );

        // Style the table header
        purchaseTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            // Adjust column widths proportionally
            double totalWidth = newVal.doubleValue() - 20; // Subtract some padding
            idColumn.setPrefWidth(totalWidth * 0.08);
            dateColumn.setPrefWidth(totalWidth * 0.15);
            productColumn.setPrefWidth(totalWidth * 0.20);
            quantityColumn.setPrefWidth(totalWidth * 0.06);
            priceColumn.setPrefWidth(totalWidth * 0.10);
            statusColumn.setPrefWidth(totalWidth * 0.10);
            buyerColumn.setPrefWidth(totalWidth * 0.15);
            actionColumn.setPrefWidth(totalWidth * 0.10);
        });
    }

    private void setupSortComboBox() {
        sortComboBox.getItems().addAll(
                "Most Recent",
                "Oldest First",
                "Highest Price",
                "Lowest Price"
        );
        sortComboBox.setValue("Most Recent");
        sortComboBox.setOnAction(e -> sortPurchases(sortComboBox.getValue()));
    }

    private void setupSearch() {
        searchBtn.setOnAction(e -> searchPurchases(searchField.getText()));
        searchField.setOnAction(e -> searchPurchases(searchField.getText()));
    }

    private void sortPurchases(String sortBy) {
        if (purchaseList == null || purchaseList.isEmpty()) return;

        switch (sortBy) {
            case "Most Recent":
                purchaseList.sort((p1, p2) -> p2.getPurchaseDate().compareTo(p1.getPurchaseDate()));
                break;
            case "Oldest First":
                purchaseList.sort((p1, p2) -> p1.getPurchaseDate().compareTo(p2.getPurchaseDate()));
                break;
            case "Highest Price":
                purchaseList.sort((p1, p2) -> Integer.compare(p2.getTotalCoins(), p1.getTotalCoins()));
                break;
            case "Lowest Price":
                purchaseList.sort((p1, p2) -> Integer.compare(p1.getTotalCoins(), p2.getTotalCoins()));
                break;
        }
        purchaseTable.setItems(purchaseList);
    }

    private void searchPurchases(String query) {
        if (query == null || query.trim().isEmpty()) {
            purchaseTable.setItems(purchaseList);
            return;
        }

        String searchLower = query.toLowerCase().trim();
        List<Purchase> filtered = purchaseList.stream()
                .filter(p ->
                        (p.getProductName() != null && p.getProductName().toLowerCase().contains(searchLower)) ||
                                (p.getBuyerName() != null && p.getBuyerName().toLowerCase().contains(searchLower)) ||
                                String.valueOf(p.getId()).contains(searchLower)
                )
                .toList();

        purchaseTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showPurchaseDetails(Purchase purchase) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Order Details");
        dialog.setHeaderText("Order #" + String.format("%04d", purchase.getId()));

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-background-radius: 10;");

        // Order details
        Label orderIdLabel = new Label("Order ID: #" + String.format("%04d", purchase.getId()));
        orderIdLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        // Product info
        HBox productBox = new HBox(10);
        productBox.setAlignment(Pos.CENTER_LEFT);
        Label productIcon = new Label("🛍️");
        productIcon.setStyle("-fx-font-size: 24px;");
        Label productName = new Label(purchase.getProductName());
        productName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        productBox.getChildren().addAll(productIcon, productName);

        // Details grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);
        detailsGrid.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 8;");

        int row = 0;
        detailsGrid.add(new Label("Quantity:"), 0, row);
        detailsGrid.add(new Label(String.valueOf(purchase.getQuantity())), 1, row++);

        detailsGrid.add(new Label("Total Price:"), 0, row);
        Label priceLabel = new Label(purchase.getTotalCoins() + " 🪙");
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FEC74C;");
        detailsGrid.add(priceLabel, 1, row++);

        detailsGrid.add(new Label("Status:"), 0, row);
        Label statusLabel = new Label(purchase.getStatus());
        statusLabel.setStyle(getStatusDetailStyle(purchase.getStatus()));
        detailsGrid.add(statusLabel, 1, row++);

        detailsGrid.add(new Label("Purchase Date:"), 0, row);
        detailsGrid.add(new Label(purchase.getPurchaseDate().toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))), 1, row++);

        // Buyer info
        if (purchase.getBuyerName() != null) {
            detailsGrid.add(new Label("Buyer:"), 0, row);
            detailsGrid.add(new Label(purchase.getBuyerName()), 1, row++);
        }

        if (purchase.getBuyerEmail() != null) {
            detailsGrid.add(new Label("Email:"), 0, row);
            detailsGrid.add(new Label(purchase.getBuyerEmail()), 1, row++);
        }

        if (purchase.getBuyerAddress() != null) {
            detailsGrid.add(new Label("Address:"), 0, row);
            detailsGrid.add(new Label(purchase.getBuyerAddress()), 1, row++);
        }

        content.getChildren().addAll(orderIdLabel, productBox, detailsGrid);
        dialogPane.setContent(content);

        dialog.showAndWait();
    }

    private String getStatusDetailStyle(String status) {
        switch (status.toLowerCase()) {
            case "pending":
                return "-fx-background-color: #FFF3CD; -fx-text-fill: #856404; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12;";
            case "confirmed":
            case "completed":
                return "-fx-background-color: #D4EDDA; -fx-text-fill: #155724; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12;";
            case "cancelled":
                return "-fx-background-color: #F8D7DA; -fx-text-fill: #721C24; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12;";
            default:
                return "-fx-background-color: #E2E3E5; -fx-text-fill: #383D41; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12;";
        }
    }

    private void loadPurchaseHistory() {
        try {
            List<Purchase> purchases = historyService.getUserPurchaseHistory(currentUser.getId());

            if (purchases.isEmpty()) {
                showEmptyState(true);
            } else {
                showEmptyState(false);
                purchaseList = FXCollections.observableArrayList(purchases);
                sortPurchases(sortComboBox.getValue());

                PurchaseHistoryService.UserPurchaseSummary summary =
                        historyService.getUserPurchaseSummary(currentUser.getId());
                if (summary != null) {
                    totalPurchasesLabel.setText("Total: " + summary.getTotalPurchases());
                    totalSpentLabel.setText("Spent: " + summary.getTotalSpent() + " 🪙");
                    uniqueProductsLabel.setText("Unique: " + summary.getUniqueProductsBought());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load purchase history: " + e.getMessage());
        }
    }

    private void loadAllPurchases() {
        try {
            List<Purchase> purchases = historyService.getAllPurchases(null, null, null);

            if (purchases.isEmpty()) {
                showEmptyState(true);
            } else {
                showEmptyState(false);
                purchaseList = FXCollections.observableArrayList(purchases);
                purchaseTable.setItems(purchaseList);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load all purchases: " + e.getMessage());
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(show);
            emptyStateLabel.setManaged(show);
        }
        purchaseTable.setVisible(!show);
        purchaseTable.setManaged(!show);
    }

    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Shop.fxml"));
            Parent root = loader.load();

            ShopController shopController = loader.getController();
            shopController.setUserData(currentUser);

            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Shop - " + currentUser.getUsername());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to shop: " + e.getMessage());
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