package tn.esprit.projet.controlles;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.entities.Profile;
import tn.esprit.projet.services.PersonService;
import tn.esprit.projet.services.ProfileService;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.UserStatusManager;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public class DashboardController {

    @FXML
    private Label dashboardMenuItem;
    @FXML
    private Label usersMenuItem;
    @FXML
    private Label myTicketsMenuItem;
    @FXML
    private Label favouriteMenuItem;
    @FXML
    private Label messageMenuItem;
    @FXML
    private Label transactionMenuItem;
    @FXML
    private Label bookingsMenuItem;
    @FXML
    private Label settingsMenuItem;
    @FXML
    private Label logoutMenuItem;

    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;
    @FXML
    private Label welcomeNameLabel;

    // Stats labels
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalAdminsLabel;
    @FXML
    private Label totalGuidersLabel;
    @FXML
    private Label totalRegularLabel;

    // Containers
    @FXML
    private VBox recentUsersContainer;
    @FXML
    private VBox onlineUsersContainer;
    @FXML
    private VBox offlineUsersContainer;
    @FXML
    private StackPane contentArea;

    // Timer components
    @FXML
    private StackPane timerContainer;
    @FXML
    private Circle timerCircle;
    @FXML
    private Label timerLabel;
    @FXML
    private VBox dropdownTimer;
    @FXML
    private Label earningsLabel;
    @FXML
    private Line progressLine;
    @FXML
    private Label plusEarningsLabel;

    // Users view components
    @FXML
    private TextField searchField;
    @FXML
    private Button addUserBtn;
    @FXML
    private VBox usersContainer;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private Button prevPageBtn;
    @FXML
    private Button nextPageBtn;

    // Status labels
    @FXML
    private Label onlineCountLabel;
    @FXML
    private Label offlineCountLabel;
    @FXML
    private Circle userAvatar;
    @FXML
    private Button backToMainBtn;
    @FXML
    private ImageView userAvatarImage;

    private ProfileService profileService;
    private Profile userProfile;
    private Map<Integer, Image> profileImageCache = new HashMap<>();

    private Person currentUser;
    private Timeline timeline;
    private Timeline earningsTimeline;
    private Timeline statusUpdateTimeline;
    private long remainingSeconds = 48 * 60 * 60;
    private long totalSeconds = 48 * 60 * 60;
    private double totalEarnings = 0.0;
    private boolean isDropdownVisible = false;
    private boolean isUsersViewActive = false;

    private static final String TIMER_FILE_PREFIX = "timer_";
    private static final String EARNINGS_FILE_PREFIX = "earnings_";

    private PersonService personService;
    private ObservableList<Person> usersList;
    private ObservableList<Person> filteredList;
    private ObservableList<Person> onlineUsers;
    private ObservableList<Person> offlineUsers;

    // Pagination variables
    private int currentPage = 0;
    private int itemsPerPage = 5;
    private int totalPages = 0;

    @FXML
    private void handleBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainpage.fxml"));
            Parent mainRoot = loader.load();

            MainPageController mainController = loader.getController();
            mainController.setUserData(currentUser);

            Stage stage = (Stage) backToMainBtn.getScene().getWindow();
            stage.setScene(new Scene(mainRoot));
            stage.setTitle("Main Page");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to main page: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        personService = new PersonService();
        profileService = new ProfileService();
        usersList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();
        onlineUsers = FXCollections.observableArrayList();
        offlineUsers = FXCollections.observableArrayList();

        setupMenuItems();
        showDashboard();
    }

    private void setupMenuItems() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setOnMouseClicked((MouseEvent e) -> showDashboard());
            dashboardMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-background-color: #e6f7f5; -fx-font-weight: bold; -fx-cursor: hand;");
            dashboardMenuItem.setTextFill(Color.web("#0FA5A2"));
            if (userAvatar != null) {
                userAvatar.setOnMouseClicked(this::handleAvatarClick);
                userAvatar.setStyle("-fx-cursor: hand;");
            }
        }

        if (usersMenuItem != null) {
            usersMenuItem.setOnMouseClicked(this::handleUsersMenuClick);
            usersMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (myTicketsMenuItem != null) {
            myTicketsMenuItem.setOnMouseClicked(this::handleMenuClick);
            myTicketsMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (favouriteMenuItem != null) {
            favouriteMenuItem.setOnMouseClicked(this::handleMenuClick);
            favouriteMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (messageMenuItem != null) {
            messageMenuItem.setOnMouseClicked(this::handleMenuClick);
            messageMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (transactionMenuItem != null) {
            transactionMenuItem.setOnMouseClicked(this::handleMenuClick);
            transactionMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (bookingsMenuItem != null) {
            bookingsMenuItem.setOnMouseClicked(this::handleMenuClick);
            bookingsMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (settingsMenuItem != null) {
            settingsMenuItem.setOnMouseClicked(this::handleMenuClick);
            settingsMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (logoutMenuItem != null) {
            logoutMenuItem.setOnMouseClicked(this::handleLogout);
            logoutMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
            logoutMenuItem.setTextFill(Color.web("#ff5e62"));
        }

        addMenuHoverEffect(dashboardMenuItem);
        addMenuHoverEffect(usersMenuItem);
        addMenuHoverEffect(myTicketsMenuItem);
        addMenuHoverEffect(favouriteMenuItem);
        addMenuHoverEffect(messageMenuItem);
        addMenuHoverEffect(transactionMenuItem);
        addMenuHoverEffect(bookingsMenuItem);
        addMenuHoverEffect(settingsMenuItem);
    }

    private void handleUsersMenuClick(MouseEvent event) {
        isUsersViewActive = true;
        showUsersView();
        updateMenuStyles(usersMenuItem);
    }

    private void handleMenuClick(MouseEvent event) {
        Label clickedItem = (Label) event.getSource();
        String menuText = clickedItem.getText();
        System.out.println("Navigating to: " + menuText);

        updateMenuStyles(clickedItem);

        switch (menuText) {
            case "Dashboard":
                showDashboard();
                break;
            case "Users":
                showUsersView();
                break;
            default:
                showComingSoon(menuText);
                break;
        }
    }

    private void showDashboard() {
        isUsersViewActive = false;
        try {
            VBox dashboardContent = createDashboardContent();
            contentArea.getChildren().setAll(dashboardContent);
            updateMenuStyles(dashboardMenuItem);
            loadDashboardData();
            updateWelcomeName();

            // Start status update timer
            startStatusUpdateTimer();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load dashboard: " + e.getMessage());
        }
    }

    private void updateWelcomeName() {
        if (currentUser != null && welcomeNameLabel != null) {
            welcomeNameLabel.setText(currentUser.getName() + "!");
        }
    }

    private VBox createDashboardContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f5f5f5;");

        HBox header = createWelcomeHeader();
        HBox statsCards = createStatsCards();

        // Create a horizontal box for recent users and online/offline users
        HBox usersSection = new HBox(20);
        usersSection.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Recent Users Table (left side)
        VBox recentUsersTable = createRecentUsersTable();
        recentUsersTable.setPrefWidth(600);

        // Online/Offline Users Section (right side)
        VBox statusSection = createStatusSection();
        statusSection.setPrefWidth(380);

        usersSection.getChildren().addAll(recentUsersTable, statusSection);

        content.getChildren().addAll(header, statsCards, usersSection);
        return content;
    }

    private VBox createStatusSection() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

        // Title
        Label title = new Label("User Status");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        // Status Summary
        HBox summaryBox = new HBox(20);
        summaryBox.setAlignment(javafx.geometry.Pos.CENTER);
        summaryBox.setPadding(new Insets(10, 0, 10, 0));
        summaryBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 10;");

        // Online Summary
        VBox onlineSummary = new VBox(5);
        onlineSummary.setAlignment(javafx.geometry.Pos.CENTER);
        onlineSummary.setPrefWidth(150);

        Circle onlineDot = new Circle(8);
        onlineDot.setFill(Color.web("#2ecc71"));

        onlineCountLabel = new Label("0");
        onlineCountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        Label onlineText = new Label("Online");
        onlineText.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        onlineSummary.getChildren().addAll(onlineDot, onlineCountLabel, onlineText);

        // Offline Summary
        VBox offlineSummary = new VBox(5);
        offlineSummary.setAlignment(javafx.geometry.Pos.CENTER);
        offlineSummary.setPrefWidth(150);

        Circle offlineDot = new Circle(8);
        offlineDot.setFill(Color.web("#95a5a6"));

        offlineCountLabel = new Label("0");
        offlineCountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");

        Label offlineText = new Label("Offline");
        offlineText.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        offlineSummary.getChildren().addAll(offlineDot, offlineCountLabel, offlineText);

        summaryBox.getChildren().addAll(onlineSummary, offlineSummary);

        // Online Users List
        VBox onlineSection = new VBox(10);
        Label onlineTitle = new Label("🟢 Online Users");
        onlineTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        onlineUsersContainer = new VBox(5);
        onlineUsersContainer.setPrefHeight(150);
        ScrollPane onlineScroll = new ScrollPane(onlineUsersContainer);
        onlineScroll.setFitToWidth(true);
        onlineScroll.setPrefHeight(150);
        onlineScroll.setStyle("-fx-background-color: transparent; -fx-border-color: #f0f0f0; -fx-border-radius: 10;");

        // Offline Users List
        VBox offlineSection = new VBox(10);
        Label offlineTitle = new Label("⚫ Offline Users");
        offlineTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");

        offlineUsersContainer = new VBox(5);
        offlineUsersContainer.setPrefHeight(150);
        ScrollPane offlineScroll = new ScrollPane(offlineUsersContainer);
        offlineScroll.setFitToWidth(true);
        offlineScroll.setPrefHeight(150);
        offlineScroll.setStyle("-fx-background-color: transparent; -fx-border-color: #f0f0f0; -fx-border-radius: 10;");

        onlineSection.getChildren().addAll(onlineTitle, onlineScroll);
        offlineSection.getChildren().addAll(offlineTitle, offlineScroll);

        section.getChildren().addAll(title, summaryBox, onlineSection, offlineSection);

        return section;
    }

    private HBox createWelcomeHeader() {
        HBox header = new HBox(20);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

        VBox welcomeBox = new VBox(5);
        Label welcomeBack = new Label("Welcome back,");
        welcomeBack.setStyle("-fx-font-size: 18px; -fx-text-fill: #666;");

        if (welcomeNameLabel == null) {
            welcomeNameLabel = new Label("User!");
        }
        welcomeNameLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        welcomeBox.getChildren().addAll(welcomeBack, welcomeNameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane timerComp = createTimerComponent();

        header.getChildren().addAll(welcomeBox, spacer, timerComp);
        return header;
    }

    private StackPane createTimerComponent() {
        StackPane container = new StackPane();
        container.setAlignment(javafx.geometry.Pos.CENTER);

        timerCircle = new Circle(30);
        timerCircle.setStroke(Color.web("#0FA5A2"));
        timerCircle.setStrokeWidth(3);
        timerCircle.setFill(Color.TRANSPARENT);

        timerLabel = new Label("48:00:00");
        timerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        dropdownTimer = new VBox(10);
        dropdownTimer.setVisible(false);
        dropdownTimer.setManaged(false);
        dropdownTimer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); -fx-padding: 20;");
        dropdownTimer.setAlignment(javafx.geometry.Pos.CENTER);
        dropdownTimer.setMinWidth(250);
        dropdownTimer.setTranslateY(50);

        VBox lineContainer = new VBox(5);
        lineContainer.setAlignment(javafx.geometry.Pos.CENTER);

        Label lineLabel = new Label("Session Progress");
        lineLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        StackPane lineStack = new StackPane();
        lineStack.setPrefWidth(200);
        lineStack.setPrefHeight(30);

        Line bgLine = new Line(0, 0, 200, 0);
        bgLine.setStroke(Color.web("#e0e0e0"));
        bgLine.setStrokeWidth(3);
        bgLine.setTranslateY(0);

        progressLine = new Line(0, 0, 200, 0);
        progressLine.setStroke(Color.web("#0FA5A2"));
        progressLine.setStrokeWidth(3);
        progressLine.setTranslateY(0);

        lineStack.getChildren().addAll(bgLine, progressLine);

        HBox earningsBox = new HBox(10);
        earningsBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label earningsTitle = new Label("Total Earnings:");
        earningsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #666;");

        earningsLabel = new Label("0.00 DT");
        earningsLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0FA5A2;");

        plusEarningsLabel = new Label("");
        plusEarningsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FEC74C;");

        earningsBox.getChildren().addAll(earningsTitle, earningsLabel, plusEarningsLabel);

        lineContainer.getChildren().addAll(lineLabel, lineStack, earningsBox);
        dropdownTimer.getChildren().add(lineContainer);

        container.getChildren().addAll(timerCircle, timerLabel, dropdownTimer);

        timerCircle.setOnMouseClicked(e -> toggleDropdown());

        return container;
    }

    private HBox createStatsCards() {
        HBox cards = new HBox(20);
        cards.setAlignment(javafx.geometry.Pos.CENTER);

        VBox totalUsersCard = createStatCard("Total Users", "0", "#0FA5A2");
        totalUsersLabel = (Label) totalUsersCard.getChildren().get(1);

        VBox adminsCard = createStatCard("Admins", "0", "#FEC74C");
        totalAdminsLabel = (Label) adminsCard.getChildren().get(1);

        VBox guidersCard = createStatCard("Guiders", "0", "#9C27B0");
        totalGuidersLabel = (Label) guidersCard.getChildren().get(1);

        VBox regularCard = createStatCard("Regular Users", "0", "#1D4D7C");
        totalRegularLabel = (Label) regularCard.getChildren().get(1);

        cards.getChildren().addAll(totalUsersCard, adminsCard, guidersCard, regularCard);
        return cards;
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPrefWidth(180);
        card.setPrefHeight(120);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createRecentUsersTable() {
        VBox table = new VBox(10);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

        Label title = new Label("Recent Users");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);
        header.setStyle("-fx-padding: 10 0; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(30);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(20);
        ColumnConstraints col5 = new ColumnConstraints();
        col5.setPercentWidth(10);

        header.getColumnConstraints().addAll(col1, col2, col3, col4, col5);

        Label avatarHeader = new Label("Avatar");
        avatarHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        header.add(avatarHeader, 0, 0);

        Label usernameHeader = new Label("Username");
        usernameHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        header.add(usernameHeader, 1, 0);

        Label emailHeader = new Label("Email");
        emailHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        header.add(emailHeader, 2, 0);

        Label roleHeader = new Label("Role");
        roleHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        header.add(roleHeader, 3, 0);

        Label actionsHeader = new Label("Actions");
        actionsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        header.add(actionsHeader, 4, 0);

        recentUsersContainer = new VBox(5);

        table.getChildren().addAll(title, header, recentUsersContainer);
        return table;
    }

    private void showUsersView() {
        try {
            VBox usersView = createUsersView();
            contentArea.getChildren().setAll(usersView);
            loadUsersFromDatabase();

            if (searchField != null) {
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    filterUsers(newValue);
                });
            }

            if (addUserBtn != null) {
                addUserBtn.setOnAction(e -> handleAddUser());
            }

            if (prevPageBtn != null) {
                prevPageBtn.setOnAction(e -> previousPage());
            }
            if (nextPageBtn != null) {
                nextPageBtn.setOnAction(e -> nextPage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load users view: " + e.getMessage());
        }
    }

    private VBox createUsersView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f5f5f5;");

        HBox header = new HBox(20);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label("User Management");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-padding: 10; -fx-background-radius: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");

        addUserBtn = new Button("+ Add User");
        addUserBtn.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");

        header.getChildren().addAll(title, spacer, searchField, addUserBtn);

        VBox table = createUsersTable();

        view.getChildren().addAll(header, table);
        return view;
    }

    private VBox createUsersTable() {
        VBox table = new VBox(15);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

        GridPane tableHeader = new GridPane();
        tableHeader.setHgap(15);
        tableHeader.setVgap(10);
        tableHeader.setStyle("-fx-padding: 10 0; -fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(20);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(15);
        ColumnConstraints col5 = new ColumnConstraints();
        col5.setPercentWidth(15);
        ColumnConstraints col6 = new ColumnConstraints();
        col6.setPercentWidth(10);

        tableHeader.getColumnConstraints().addAll(col1, col2, col3, col4, col5, col6);

        Label avatarHeader = new Label("AVATAR");
        avatarHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-font-size: 14px;");
        tableHeader.add(avatarHeader, 0, 0);

        Label usernameHeader = new Label("USERNAME");
        usernameHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-font-size: 14px;");
        tableHeader.add(usernameHeader, 1, 0);

        Label emailHeader = new Label("EMAIL");
        emailHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-font-size: 14px;");
        tableHeader.add(emailHeader, 2, 0);

        Label roleHeader = new Label("ROLE");
        roleHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-font-size: 14px;");
        tableHeader.add(roleHeader, 3, 0);

        Label statusHeader = new Label("STATUS");
        statusHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-font-size: 14px;");
        tableHeader.add(statusHeader, 4, 0);

        Label actionsHeader = new Label("ACTIONS");
        actionsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-font-size: 14px;");
        tableHeader.add(actionsHeader, 5, 0);

        usersContainer = new VBox(10);

        HBox pagination = new HBox(10);
        pagination.setAlignment(javafx.geometry.Pos.CENTER);
        pagination.setPadding(new Insets(20, 0, 0, 0));

        prevPageBtn = new Button("← Previous");
        prevPageBtn.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");

        pageInfoLabel = new Label("Page 1 of 1");
        pageInfoLabel.setStyle("-fx-padding: 0 15; -fx-font-size: 14px; -fx-text-fill: #666;");

        nextPageBtn = new Button("Next →");
        nextPageBtn.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");

        pagination.getChildren().addAll(prevPageBtn, pageInfoLabel, nextPageBtn);

        table.getChildren().addAll(tableHeader, usersContainer, pagination);
        return table;
    }

    private void loadUsersFromDatabase() {
        try {
            List<Person> users = personService.selectALL();
            usersList.setAll(users);
            filteredList.setAll(users);

            totalPages = (int) Math.ceil((double) filteredList.size() / itemsPerPage);
            currentPage = 0;
            updatePaginationControls();

            // Preload profile images
            preloadProfileImages(users);

            displayCurrentPage();
            updateDashboardStats(users);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void preloadProfileImages(List<Person> users) {
        for (Person user : users) {
            try {
                Profile profile = profileService.getProfileByUserId(user.getId());
                if (profile != null && profile.getImage() != null) {
                    Image image = new Image(new ByteArrayInputStream(profile.getImage()));
                    profileImageCache.put(user.getId(), image);
                }
            } catch (SQLException e) {
                // Ignore errors
            }
        }
    }

    private void loadDashboardData() {
        try {
            List<Person> users = personService.selectALL();
            updateDashboardStats(users);
            displayRecentUsers(users);
            loadUserStatusFromDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void updateDashboardStats(List<Person> users) {
        if (totalUsersLabel != null) {
            totalUsersLabel.setText(String.valueOf(users.size()));
        }

        long adminCount = users.stream().filter(u -> "Admin".equalsIgnoreCase(u.getRole())).count();
        long guiderCount = users.stream().filter(u -> "Guider".equalsIgnoreCase(u.getRole())).count();
        long regularCount = users.stream().filter(u -> !"Admin".equalsIgnoreCase(u.getRole()) && !"Guider".equalsIgnoreCase(u.getRole())).count();

        if (totalAdminsLabel != null) {
            totalAdminsLabel.setText(String.valueOf(adminCount));
        }
        if (totalGuidersLabel != null) {
            totalGuidersLabel.setText(String.valueOf(guiderCount));
        }
        if (totalRegularLabel != null) {
            totalRegularLabel.setText(String.valueOf(regularCount));
        }
    }

    private void displayRecentUsers(List<Person> users) {
        if (recentUsersContainer == null) return;

        recentUsersContainer.getChildren().clear();

        int start = Math.max(0, users.size() - 5);
        for (int i = start; i < users.size(); i++) {
            Person user = users.get(i);
            HBox userRow = createRecentUserRow(user);
            recentUsersContainer.getChildren().add(userRow);
        }
    }

    private HBox createRecentUserRow(Person user) {
        HBox row = new HBox(10);
        row.setStyle("-fx-padding: 8; -fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-alignment: center-left;");
        row.setPrefHeight(50);

        // Avatar
        StackPane avatarStack = new StackPane();
        avatarStack.setPrefWidth(40);

        Circle avatarCircle = new Circle(16);
        Image userImage = profileImageCache.get(user.getId());

        if (userImage != null) {
            ImageView avatarView = new ImageView(userImage);
            avatarView.setFitWidth(32);
            avatarView.setFitHeight(32);
            Circle clip = new Circle(16);
            clip.setCenterX(16);
            clip.setCenterY(16);
            avatarView.setClip(clip);
            avatarStack.getChildren().add(avatarView);
        } else {
            // Fallback to colored circle with initial
            avatarCircle.setFill(getAvatarColor(user.getRole()));
            Label initialLabel = new Label(user.getUsername().substring(0, 1).toUpperCase());
            initialLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
            avatarStack.getChildren().addAll(avatarCircle, initialLabel);
        }

        // Username
        Label usernameLabel = new Label(user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-min-width: 120;");

        // Email
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 14px; -fx-min-width: 200;");

        // Role badge
        HBox roleBox = createRoleBadge(user.getRole());

        // Actions
        HBox actionBox = new HBox(10);

        Label editLabel = new Label("✏️");
        editLabel.setStyle("-fx-font-size: 18px; -fx-cursor: hand;");
        editLabel.setOnMouseClicked(e -> handleEditUser(user));

        Label deleteLabel = new Label("🗑️");
        deleteLabel.setStyle("-fx-font-size: 18px; -fx-cursor: hand;");
        deleteLabel.setOnMouseClicked(e -> handleDeleteUser(user));

        actionBox.getChildren().addAll(editLabel, deleteLabel);

        HBox.setHgrow(usernameLabel, Priority.NEVER);
        HBox.setHgrow(emailLabel, Priority.NEVER);
        HBox.setHgrow(roleBox, Priority.NEVER);
        HBox.setHgrow(actionBox, Priority.NEVER);

        row.getChildren().addAll(avatarStack, usernameLabel, emailLabel, roleBox, actionBox);
        return row;
    }

    private Color getAvatarColor(String role) {
        switch (role.toLowerCase()) {
            case "admin":
                return Color.web("#FEC74C");
            case "guider":
                return Color.web("#9C27B0");
            default:
                return Color.web("#0FA5A2");
        }
    }

    private HBox createRoleBadge(String role) {
        HBox badge = new HBox(5);
        badge.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        badge.setMinWidth(70);

        Circle indicator = new Circle(5);
        String color;
        switch (role.toLowerCase()) {
            case "admin":
                color = "#FEC74C";
                break;
            case "guider":
                color = "#9C27B0";
                break;
            default:
                color = "#0FA5A2";
        }
        indicator.setFill(Color.web(color));

        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        badge.getChildren().addAll(indicator, roleLabel);
        return badge;
    }

    private void displayCurrentPage() {
        if (usersContainer == null || filteredList == null) return;

        usersContainer.getChildren().clear();

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredList.size());

        for (int i = start; i < end; i++) {
            Person user = filteredList.get(i);
            HBox userRow = createUserRow(user);
            usersContainer.getChildren().add(userRow);
        }
    }

    private HBox createUserRow(Person user) {
        HBox row = new HBox(15);
        row.setStyle("-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-color: #e9ecef; -fx-border-radius: 12; -fx-alignment: center-left; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");
        row.setPrefHeight(80);

        // Add hover effect for the row
        row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 15; -fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #0FA5A2; -fx-border-radius: 12; -fx-border-width: 2; -fx-alignment: center-left; -fx-effect: dropshadow(three-pass-box, #0FA5A240, 10, 0, 0, 0);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-color: #e9ecef; -fx-border-radius: 12; -fx-border-width: 1; -fx-alignment: center-left; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);"));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(5);
        HBox.setHgrow(grid, Priority.ALWAYS);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(20);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(15);
        ColumnConstraints col5 = new ColumnConstraints();
        col5.setPercentWidth(15);
        ColumnConstraints col6 = new ColumnConstraints();
        col6.setPercentWidth(10);

        grid.getColumnConstraints().addAll(col1, col2, col3, col4, col5, col6);

        // Avatar with profile image - Enhanced design
        StackPane avatarStack = new StackPane();
        avatarStack.setPrefWidth(50);
        avatarStack.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 0);");

        Circle avatarCircle = new Circle(20);
        Image userImage = profileImageCache.get(user.getId());

        if (userImage != null) {
            ImageView avatarView = new ImageView(userImage);
            avatarView.setFitWidth(40);
            avatarView.setFitHeight(40);
            Circle clip = new Circle(20);
            clip.setCenterX(20);
            clip.setCenterY(20);
            avatarView.setClip(clip);
            avatarStack.getChildren().add(avatarView);
        } else {
            // Fallback to colored circle with gradient
            avatarCircle.setFill(getAvatarColor(user.getRole()));
            avatarCircle.setEffect(new InnerShadow(5, Color.web("#00000040")));
            Label initialLabel = new Label(user.getUsername().substring(0, 1).toUpperCase());
            initialLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 3, 0, 0, 0);");
            avatarStack.getChildren().addAll(avatarCircle, initialLabel);
        }
        grid.add(avatarStack, 0, 0);

        // Username with better styling
        VBox nameContainer = new VBox(2);
        nameContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label usernameLabel = new Label(user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        Label nameLabel = new Label(user.getName() + " " + user.getLastName());
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        nameContainer.getChildren().addAll(usernameLabel, nameLabel);
        grid.add(nameContainer, 1, 0);

        // Email with icon
        HBox emailBox = new HBox(5);
        emailBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label emailIcon = new Label("📧");
        emailIcon.setStyle("-fx-font-size: 14px;");

        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #495057;");

        emailBox.getChildren().addAll(emailIcon, emailLabel);
        grid.add(emailBox, 2, 0);

        // Role badge - Enhanced design
        HBox roleBox = createEnhancedRoleBadge(user.getRole());
        grid.add(roleBox, 3, 0);

        // Status with better styling
        boolean isOnline = "online".equalsIgnoreCase(user.getStatus());
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Circle statusDot = new Circle(6);
        if (isOnline) {
            statusDot.setFill(Color.web("#2ecc71"));
            statusDot.setEffect(new DropShadow(8, Color.web("#2ecc7180")));
        } else {
            statusDot.setFill(Color.web("#95a5a6"));
        }

        Label statusLabel = new Label(isOnline ? "Online" : "Offline");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isOnline ? "#2ecc71" : "#95a5a6") + ";");

        statusBox.getChildren().addAll(statusDot, statusLabel);
        grid.add(statusBox, 4, 0);

        // Action buttons with enhanced emoji design
        HBox actionBox = new HBox(12);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Edit Button with emoji - Circular design
        StackPane editBtn = new StackPane();
        editBtn.setPrefSize(36, 36);
        editBtn.setStyle("-fx-background-color: #0FA5A2; -fx-background-radius: 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #0FA5A280, 8, 0, 0, 0);");

        Label editIcon = new Label("✎");
        editIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");
        editIcon.setEffect(new DropShadow(3, Color.web("#00000040")));

        editBtn.getChildren().add(editIcon);

        // Tooltip
        Tooltip editTooltip = new Tooltip("Edit User");
        editTooltip.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        Tooltip.install(editBtn, editTooltip);

        // Hover effects for edit button
        editBtn.setOnMouseEntered(e -> {
            editBtn.setStyle("-fx-background-color: #0d8f8c; -fx-background-radius: 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #0FA5A2, 12, 0, 0, 0);");
            editBtn.setScaleX(1.1);
            editBtn.setScaleY(1.1);
        });
        editBtn.setOnMouseExited(e -> {
            editBtn.setStyle("-fx-background-color: #0FA5A2; -fx-background-radius: 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #0FA5A280, 8, 0, 0, 0);");
            editBtn.setScaleX(1.0);
            editBtn.setScaleY(1.0);
        });
        editBtn.setOnMouseClicked(e -> handleEditUser(user));

        // Delete Button with emoji - Circular design
        StackPane deleteBtn = new StackPane();
        deleteBtn.setPrefSize(36, 36);
        deleteBtn.setStyle("-fx-background-color: #ff5e62; -fx-background-radius: 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #ff5e6280, 8, 0, 0, 0);");

        Label deleteIcon = new Label("🗑");
        deleteIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteIcon.setEffect(new DropShadow(3, Color.web("#00000040")));

        deleteBtn.getChildren().add(deleteIcon);

        // Tooltip
        Tooltip deleteTooltip = new Tooltip("Delete User");
        deleteTooltip.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        Tooltip.install(deleteBtn, deleteTooltip);

        // Hover effects for delete button
        deleteBtn.setOnMouseEntered(e -> {
            deleteBtn.setStyle("-fx-background-color: #e04e52; -fx-background-radius: 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #ff5e62, 12, 0, 0, 0);");
            deleteBtn.setScaleX(1.1);
            deleteBtn.setScaleY(1.1);
        });
        deleteBtn.setOnMouseExited(e -> {
            deleteBtn.setStyle("-fx-background-color: #ff5e62; -fx-background-radius: 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #ff5e6280, 8, 0, 0, 0);");
            deleteBtn.setScaleX(1.0);
            deleteBtn.setScaleY(1.0);
        });
        deleteBtn.setOnMouseClicked(e -> handleDeleteUser(user));

        actionBox.getChildren().addAll(editBtn, deleteBtn);
        grid.add(actionBox, 5, 0);

        row.getChildren().add(grid);
        return row;
    }

    /**
     * Enhanced role badge with better design
     */
    private HBox createEnhancedRoleBadge(String role) {
        HBox badge = new HBox(8);
        badge.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        badge.setMinWidth(80);
        badge.setPadding(new Insets(4, 10, 4, 8));

        String color;
        String bgColor;
        switch (role.toLowerCase()) {
            case "admin":
                color = "#FEC74C";
                bgColor = "#FEC74C20";
                break;
            case "guider":
                color = "#9C27B0";
                bgColor = "#9C27B020";
                break;
            default:
                color = "#0FA5A2";
                bgColor = "#0FA5A220";
        }

        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 20; -fx-border-color: " + color + "; -fx-border-radius: 20; -fx-border-width: 1;");

        Circle indicator = new Circle(6);
        indicator.setFill(Color.web(color));
        indicator.setEffect(new InnerShadow(3, Color.web(color + "80")));

        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        badge.getChildren().addAll(indicator, roleLabel);
        return badge;
    }
    private void handleAddUser() {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Enter user details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        TextField nameField = new TextField();
        nameField.setPromptText("First Name");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Guider", "User");
        roleCombo.setValue("User");

        DatePicker datePicker = new DatePicker();

        ComboBox<String> membershipCombo = new ComboBox<>();
        membershipCombo.getItems().addAll("Standard", "Premium");
        membershipCombo.setValue("Standard");

        int row = 0;
        grid.add(new Label("Username:"), 0, row);
        grid.add(usernameField, 1, row++);
        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row++);
        grid.add(new Label("Password:"), 0, row);
        grid.add(passwordField, 1, row++);
        grid.add(new Label("First Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Last Name:"), 0, row);
        grid.add(lastNameField, 1, row++);
        grid.add(new Label("Role:"), 0, row);
        grid.add(roleCombo, 1, row++);
        grid.add(new Label("Membership:"), 0, row);
        grid.add(membershipCombo, 1, row++);
        grid.add(new Label("Date:"), 0, row);
        grid.add(datePicker, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Person person = new Person();
                person.setUsername(usernameField.getText());
                person.setEmail(emailField.getText());
                person.setPassword(passwordField.getText());
                person.setName(nameField.getText());
                person.setLastName(lastNameField.getText());
                person.setRole(roleCombo.getValue());
                if (datePicker.getValue() != null) {
                    person.setDate(java.sql.Date.valueOf(datePicker.getValue()));
                }
                return new Object[]{person, membershipCombo.getValue()};
            }
            return null;
        });

        Optional<Object[]> result = dialog.showAndWait();

        result.ifPresent(data -> {
            Person person = (Person) data[0];
            String membership = (String) data[1];

            try {
                personService.insertOneUpdated(person);
                Person createdUser = personService.login(person.getEmail(), person.getPassword());

                if (createdUser != null) {
                    Profile profile = new Profile();
                    profile.setIdUser(createdUser.getId());
                    profile.setMemberPremium(membership);
                    profile.setLanguage("English");
                    profile.setCoins(0);

                    String imagePath = "C:\\Users\\pyrox\\Downloads\\default_image.png";
                    File imageFile = new File(imagePath);

                    if (imageFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(imageFile)) {
                            byte[] defaultImage = fis.readAllBytes();
                            profile.setImage(defaultImage);
                        } catch (IOException e) {
                            System.err.println("Failed to load default image: " + e.getMessage());
                        }
                    }

                    profileService.insertOne(profile);

                    if (profile.getImage() != null) {
                        Image image = new Image(new ByteArrayInputStream(profile.getImage()));
                        profileImageCache.put(createdUser.getId(), image);
                    }
                }

                loadUsersFromDatabase();
                showAlert("Success", "User added successfully with " + membership + " membership!", Alert.AlertType.INFORMATION);

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to add user: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void handleEditUser(Person user) {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit user: " + user.getUsername());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField(user.getUsername());
        TextField emailField = new TextField(user.getEmail());
        TextField nameField = new TextField(user.getName());
        TextField lastNameField = new TextField(user.getLastName());

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Guider", "User");
        roleCombo.setValue(user.getRole());

        String currentMembership = "Standard";
        boolean hasImage = false;
        Profile existingProfile = null;

        try {
            existingProfile = profileService.getProfileByUserId(user.getId());
            if (existingProfile != null) {
                currentMembership = existingProfile.getMemberPremium();
                hasImage = existingProfile.getImage() != null && existingProfile.getImage().length > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ComboBox<String> membershipCombo = new ComboBox<>();
        membershipCombo.getItems().addAll("Standard", "Premium");
        membershipCombo.setValue(currentMembership);

        CheckBox setDefaultImageCheckBox = new CheckBox("Set default image");
        setDefaultImageCheckBox.setSelected(false);
        if (!hasImage) {
            setDefaultImageCheckBox.setText("No image found - Set default image");
            setDefaultImageCheckBox.setSelected(true);
            setDefaultImageCheckBox.setStyle("-fx-text-fill: #ff5e62; -fx-font-weight: bold;");
        }

        int row = 0;
        grid.add(new Label("Username:"), 0, row);
        grid.add(usernameField, 1, row++);
        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row++);
        grid.add(new Label("First Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Last Name:"), 0, row);
        grid.add(lastNameField, 1, row++);
        grid.add(new Label("Role:"), 0, row);
        grid.add(roleCombo, 1, row++);
        grid.add(new Label("Membership:"), 0, row);
        grid.add(membershipCombo, 1, row++);
        grid.add(new Label("Image:"), 0, row);
        grid.add(setDefaultImageCheckBox, 1, row++);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                user.setUsername(usernameField.getText());
                user.setEmail(emailField.getText());
                user.setName(nameField.getText());
                user.setLastName(lastNameField.getText());
                user.setRole(roleCombo.getValue());
                return new Object[]{user, membershipCombo.getValue(), setDefaultImageCheckBox.isSelected()};
            }
            return null;
        });

        Optional<Object[]> result = dialog.showAndWait();

        result.ifPresent(data -> {
            Person updatedUser = (Person) data[0];
            String newMembership = (String) data[1];
            boolean setDefaultImage = (boolean) data[2];

            try {
                personService.updateOne(updatedUser);

                Profile profile = profileService.getProfileByUserId(updatedUser.getId());

                if (profile != null) {
                    profile.setMemberPremium(newMembership);

                    if (setDefaultImage) {
                        String imagePath = "C:\\Users\\pyrox\\Downloads\\default_image.png";
                        File imageFile = new File(imagePath);

                        if (imageFile.exists()) {
                            try (FileInputStream fis = new FileInputStream(imageFile)) {
                                byte[] defaultImage = fis.readAllBytes();
                                profile.setImage(defaultImage);

                                Image image = new Image(new ByteArrayInputStream(defaultImage));
                                profileImageCache.put(updatedUser.getId(), image);
                            } catch (IOException e) {
                                System.err.println("Failed to load default image: " + e.getMessage());
                            }
                        }
                    }

                    profileService.updateOne(profile);
                } else {
                    profile = new Profile();
                    profile.setIdUser(updatedUser.getId());
                    profile.setMemberPremium(newMembership);
                    profile.setLanguage("English");
                    profile.setCoins(0);

                    if (setDefaultImage) {
                        String imagePath = "C:\\Users\\pyrox\\Downloads\\default_image.png";
                        File imageFile = new File(imagePath);

                        if (imageFile.exists()) {
                            try (FileInputStream fis = new FileInputStream(imageFile)) {
                                byte[] defaultImage = fis.readAllBytes();
                                profile.setImage(defaultImage);

                                Image image = new Image(new ByteArrayInputStream(defaultImage));
                                profileImageCache.put(updatedUser.getId(), image);
                            } catch (IOException e) {
                                System.err.println("Failed to load default image: " + e.getMessage());
                            }
                        }
                    }

                    profileService.insertOne(profile);
                }

                loadUsersFromDatabase();
                String imageMessage = setDefaultImage ? " with default image" : "";
                showAlert("Success", "User updated successfully! Membership: " + newMembership + imageMessage, Alert.AlertType.INFORMATION);

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to update user: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void handleDeleteUser(Person user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete user: " + user.getUsername() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                personService.deleteOne(user);
                profileImageCache.remove(user.getId());
                loadUsersFromDatabase();
                showAlert("Success", "User deleted successfully!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete user: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void updateUserStatusLists() {
        if (onlineUsersContainer == null || offlineUsersContainer == null) return;

        onlineUsers.clear();
        offlineUsers.clear();

        for (Person user : usersList) {
            if (UserStatusManager.isUserOnline(user.getId())) {
                onlineUsers.add(user);
            } else {
                offlineUsers.add(user);
            }
        }

        if (onlineCountLabel != null) {
            onlineCountLabel.setText(String.valueOf(onlineUsers.size()));
        }
        if (offlineCountLabel != null) {
            offlineCountLabel.setText(String.valueOf(offlineUsers.size()));
        }

        onlineUsersContainer.getChildren().clear();
        for (Person user : onlineUsers) {
            HBox userRow = createStatusUserRow(user, true);
            onlineUsersContainer.getChildren().add(userRow);
        }

        offlineUsersContainer.getChildren().clear();
        for (Person user : offlineUsers) {
            HBox userRow = createStatusUserRow(user, false);
            offlineUsersContainer.getChildren().add(userRow);
        }
    }

    private HBox createStatusUserRow(Person user, boolean isOnline) {
        HBox row = new HBox(10);
        row.setStyle("-fx-padding: 8; -fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-alignment: center-left;");
        row.setPrefHeight(40);
        row.setMaxWidth(Double.MAX_VALUE);

        StackPane avatarStack = new StackPane();
        avatarStack.setPrefWidth(30);

        Circle avatarCircle = new Circle(12);
        Image userImage = profileImageCache.get(user.getId());

        if (userImage != null) {
            ImageView avatarView = new ImageView(userImage);
            avatarView.setFitWidth(24);
            avatarView.setFitHeight(24);
            Circle clip = new Circle(12);
            clip.setCenterX(12);
            clip.setCenterY(12);
            avatarView.setClip(clip);
            avatarStack.getChildren().add(avatarView);
        } else {
            avatarCircle.setFill(getAvatarColor(user.getRole()));
            Label initialLabel = new Label(user.getUsername().substring(0, 1).toUpperCase());
            initialLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");
            avatarStack.getChildren().addAll(avatarCircle, initialLabel);
        }

        VBox userInfo = new VBox(2);
        userInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label usernameLabel = new Label(user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        Label roleLabel = new Label(user.getRole());
        roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        userInfo.getChildren().addAll(usernameLabel, roleLabel);

        HBox.setHgrow(userInfo, Priority.ALWAYS);
        row.getChildren().addAll(avatarStack, userInfo);

        Circle statusDot = new Circle(6);
        if (isOnline) {
            statusDot.setFill(Color.web("#2ecc71"));
        } else {
            statusDot.setFill(Color.web("#95a5a6"));
        }
        row.getChildren().add(statusDot);

        row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 8; -fx-background-color: #e9ecef; -fx-background-radius: 8; -fx-alignment: center-left;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding: 8; -fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-alignment: center-left;"));

        return row;
    }

    private void loadUserStatusFromDatabase() {
        try {
            List<Person> onlineUsers = personService.getOnlineUsers();
            List<Person> offlineUsers = personService.getOfflineUsers();

            if (onlineCountLabel != null) {
                onlineCountLabel.setText(String.valueOf(onlineUsers.size()));
            }
            if (offlineCountLabel != null) {
                offlineCountLabel.setText(String.valueOf(offlineUsers.size()));
            }

            onlineUsersContainer.getChildren().clear();
            for (Person user : onlineUsers) {
                HBox userRow = createStatusUserRow(user, true);
                onlineUsersContainer.getChildren().add(userRow);
            }

            offlineUsersContainer.getChildren().clear();
            for (Person user : offlineUsers) {
                HBox userRow = createStatusUserRow(user, false);
                offlineUsersContainer.getChildren().add(userRow);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startStatusUpdateTimer() {
        if (statusUpdateTimeline != null) {
            statusUpdateTimeline.stop();
        }

        statusUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            loadUserStatusFromDatabase();
        }));

        statusUpdateTimeline.setCycleCount(Animation.INDEFINITE);
        statusUpdateTimeline.play();
    }

    private void filterUsers(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredList.setAll(usersList);
        } else {
            ObservableList<Person> filtered = FXCollections.observableArrayList();
            String lowerSearch = searchText.toLowerCase();

            for (Person user : usersList) {
                if (user.getUsername().toLowerCase().contains(lowerSearch) ||
                        user.getEmail().toLowerCase().contains(lowerSearch) ||
                        user.getRole().toLowerCase().contains(lowerSearch) ||
                        user.getName().toLowerCase().contains(lowerSearch) ||
                        user.getLastName().toLowerCase().contains(lowerSearch)) {
                    filtered.add(user);
                }
            }
            filteredList.setAll(filtered);
        }

        currentPage = 0;
        totalPages = (int) Math.ceil((double) filteredList.size() / itemsPerPage);
        updatePaginationControls();
        displayCurrentPage();
    }

    private void updatePaginationControls() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
        }

        if (prevPageBtn != null) {
            prevPageBtn.setDisable(currentPage == 0);
        }

        if (nextPageBtn != null) {
            nextPageBtn.setDisable(currentPage >= totalPages - 1);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            displayCurrentPage();
            updatePaginationControls();
        }
    }

    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            displayCurrentPage();
            updatePaginationControls();
        }
    }

    private void showComingSoon(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(null);
        alert.setContentText(feature + " feature is coming soon!");
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        showAlert(title, content, Alert.AlertType.ERROR);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setUserData(Person user) {
        this.currentUser = user;

        try {
            personService.updateUserStatus(user.getId(), "online");
            user.setStatus("online");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateUserInterface();
        loadTimerState();
        loadEarningsState();
        startTimer();
        startEarningsTimer();

        loadProfileImage();

        loadDashboardData();
        if (isUsersViewActive) {
            loadUsersFromDatabase();
        }

        updateWelcomeName();
    }

    private void updateUserInterface() {
        if (currentUser != null) {
            if (userNameLabel != null) {
                userNameLabel.setText(currentUser.getName() + " " + currentUser.getLastName());
            }
            if (userRoleLabel != null) {
                userRoleLabel.setText(currentUser.getRole());
            }
            updateWelcomeName();
        }
    }

    private void updateMenuStyles(Label activeMenu) {
        Label[] menus = {dashboardMenuItem, usersMenuItem, myTicketsMenuItem,
                favouriteMenuItem, messageMenuItem, transactionMenuItem,
                bookingsMenuItem, settingsMenuItem};

        for (Label menu : menus) {
            if (menu != null) {
                menu.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
                menu.setTextFill(Color.BLACK);
            }
        }

        if (activeMenu != null) {
            activeMenu.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-background-color: #e6f7f5; -fx-cursor: hand;");
            activeMenu.setTextFill(Color.web("#0FA5A2"));
        }
    }

    private void addMenuHoverEffect(Label menuItem) {
        if (menuItem == null) return;

        menuItem.setOnMouseEntered(e -> {
            if (!menuItem.getStyle().contains("#e6f7f5")) {
                menuItem.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
            }
        });

        menuItem.setOnMouseExited(e -> {
            if (!menuItem.getStyle().contains("#e6f7f5")) {
                menuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
            }
        });
    }

    private void startTimer() {
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateTimerDisplay();
            } else {
                stopTimer();
                handleAutoLogout();
            }
        }));

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void startEarningsTimer() {
        if (earningsTimeline != null) {
            earningsTimeline.stop();
        }

        earningsTimeline = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            totalEarnings += 0.20;
            updateEarningsDisplay();
            showEarningsAnimation();
            saveEarningsState();
        }));

        earningsTimeline.setCycleCount(Animation.INDEFINITE);
        earningsTimeline.play();
    }

    private void stopTimer() {
        if (timeline != null) {
            timeline.stop();
        }
        if (earningsTimeline != null) {
            earningsTimeline.stop();
        }
        if (statusUpdateTimeline != null) {
            statusUpdateTimeline.stop();
        }
        saveTimerState();
        saveEarningsState();
    }

    private void updateTimerDisplay() {
        String timeStr = formatTime(remainingSeconds);
        if (timerLabel != null) {
            timerLabel.setText(timeStr);
        }

        if (progressLine != null) {
            double progress = (double) remainingSeconds / totalSeconds;
            progressLine.setEndX(200 * progress);
        }

        if (timerCircle != null) {
            if (remainingSeconds < 3600) {
                timerCircle.setStroke(Color.web("#ff5e62"));
            } else if (remainingSeconds < 21600) {
                timerCircle.setStroke(Color.web("#FEC74C"));
            } else {
                timerCircle.setStroke(Color.web("#0FA5A2"));
            }
        }
    }

    private void updateEarningsDisplay() {
        if (earningsLabel != null) {
            earningsLabel.setText(String.format("%.2f DT", totalEarnings));
        }
    }

    private void showEarningsAnimation() {
        if (plusEarningsLabel != null) {
            plusEarningsLabel.setText("+0.20 DT");
            plusEarningsLabel.setOpacity(1);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), plusEarningsLabel);
            fadeOut.setToValue(0);
            fadeOut.play();

            TranslateTransition moveUp = new TranslateTransition(Duration.seconds(2), plusEarningsLabel);
            moveUp.setByY(-20);
            moveUp.play();

            moveUp.setOnFinished(e -> {
                plusEarningsLabel.setText("");
                plusEarningsLabel.setTranslateY(0);
            });
        }
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    @FXML
    private void toggleDropdown() {
        isDropdownVisible = !isDropdownVisible;
        if (dropdownTimer != null) {
            dropdownTimer.setVisible(isDropdownVisible);
            dropdownTimer.setManaged(isDropdownVisible);
        }
    }

    private void saveTimerState() {
        if (currentUser == null) return;

        try {
            String filename = TIMER_FILE_PREFIX + currentUser.getId() + ".dat";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeLong(remainingSeconds);
                oos.writeObject(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveEarningsState() {
        if (currentUser == null) return;

        try {
            String filename = EARNINGS_FILE_PREFIX + currentUser.getId() + ".dat";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeDouble(totalEarnings);
                oos.writeObject(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTimerState() {
        if (currentUser == null) return;

        try {
            String filename = TIMER_FILE_PREFIX + currentUser.getId() + ".dat";
            File file = new File(filename);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    long savedSeconds = ois.readLong();
                    String savedTime = (String) ois.readObject();

                    LocalDateTime savedDateTime = LocalDateTime.parse(savedTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    LocalDateTime now = LocalDateTime.now();

                    long elapsedSeconds = java.time.Duration.between(savedDateTime, now).getSeconds();
                    remainingSeconds = Math.max(0, savedSeconds - elapsedSeconds);

                    if (remainingSeconds == 0) {
                        handleAutoLogout();
                    }
                }
            } else {
                remainingSeconds = 48 * 60 * 60;
            }
            updateTimerDisplay();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            remainingSeconds = 48 * 60 * 60;
        }
    }

    private void loadEarningsState() {
        if (currentUser == null) return;

        try {
            String filename = EARNINGS_FILE_PREFIX + currentUser.getId() + ".dat";
            File file = new File(filename);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    double savedEarnings = ois.readDouble();
                    String savedTime = (String) ois.readObject();

                    LocalDateTime savedDateTime = LocalDateTime.parse(savedTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    LocalDateTime now = LocalDateTime.now();

                    long elapsedMinutes = java.time.Duration.between(savedDateTime, now).toMinutes();
                    totalEarnings = savedEarnings + (elapsedMinutes * 0.20);
                }
            } else {
                totalEarnings = 0.0;
            }
            updateEarningsDisplay();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            totalEarnings = 0.0;
        }
    }

    private void handleAutoLogout() {
        try {
            if (currentUser != null) {
                personService.updateUserStatus(currentUser.getId(), "offline");
                SessionManager.clearSession();

                String filename = TIMER_FILE_PREFIX + currentUser.getId() + ".dat";
                new File(filename).delete();
                filename = EARNINGS_FILE_PREFIX + currentUser.getId() + ".dat";
                new File(filename).delete();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterPersonne.fxml"));
            Parent loginRoot = loader.load();

            Stage stage = (Stage) timerContainer.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("Login");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLogout(MouseEvent event) {
        stopTimer();

        try {
            if (currentUser != null) {
                personService.updateUserStatus(currentUser.getId(), "offline");
                SessionManager.clearSession();

                String filename = TIMER_FILE_PREFIX + currentUser.getId() + ".dat";
                new File(filename).delete();
                filename = EARNINGS_FILE_PREFIX + currentUser.getId() + ".dat";
                new File(filename).delete();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterPersonne.fxml"));
            Parent loginRoot = loader.load();

            Stage stage = (Stage) logoutMenuItem.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("Login");
            stage.show();

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAvatarClick(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/showprofile.fxml"));
            Parent profileRoot = loader.load();

            ShowprofileController profileController = loader.getController();
            profileController.setUserData(currentUser);

            Stage currentStage = (Stage) userAvatar.getScene().getWindow();
            currentStage.setScene(new Scene(profileRoot));
            currentStage.setTitle("My Profile");
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load profile page: " + e.getMessage());
        }
    }

    private void loadProfileImage() {
        if (currentUser == null) return;

        try {
            userProfile = profileService.getProfileByUserId(currentUser.getId());

            if (userProfile != null && userProfile.getImage() != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(userProfile.getImage());
                Image profileImage = new Image(bis);

                profileImageCache.put(currentUser.getId(), profileImage);

                ImagePattern pattern = new ImagePattern(profileImage);
                userAvatar.setFill(pattern);

            } else {
                userAvatar.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0FA5A2")),
                        new Stop(1, Color.web("#1D4D7C"))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            userAvatar.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#0FA5A2")),
                    new Stop(1, Color.web("#1D4D7C"))));
        }
    }
}