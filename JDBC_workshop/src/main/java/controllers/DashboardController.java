package controllers;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import entities.Person;
import entities.Profile;
import entities.Todo;
import services.PersonService;
import services.ProfileService;
import services.TodoService;
import utils.SessionManager;
import utils.UserStatusManager;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class DashboardController {

    @FXML
    private Label dashboardMenuItem;
    @FXML
    private Label usersMenuItem;
    @FXML
    private Label todoMenuItem;
    @FXML
    private Label statsMenuItem;
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

    @FXML
    private Label shopMenuItem;

    // Services
    private PersonService personService;
    private ProfileService profileService;
    private TodoService todoService;

    // User data
    private Person currentUser;
    private Profile userProfile;
    private Map<Integer, Image> profileImageCache = new HashMap<>();

    // Timer variables
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

    // User lists
    private ObservableList<Person> usersList;
    private ObservableList<Person> filteredList;
    private ObservableList<Person> onlineUsers;
    private ObservableList<Person> offlineUsers;

    // Todo lists
    private ObservableList<Todo> todoList;
    private ObservableList<Todo> inProgressList;
    private ObservableList<Todo> doneList;

    // Pagination variables
    private int currentPage = 0;
    private int itemsPerPage = 5;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        personService = new PersonService();
        profileService = new ProfileService();
        todoService = new TodoService();

        usersList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();
        onlineUsers = FXCollections.observableArrayList();
        offlineUsers = FXCollections.observableArrayList();

        // Initialize todo lists
        todoList = FXCollections.observableArrayList();
        inProgressList = FXCollections.observableArrayList();
        doneList = FXCollections.observableArrayList();

        // Create todo table if not exists
        try {
            todoService.createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        setupMenuItems();
        showDashboard();
    }

    @FXML
    private void handleBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mainpage.fxml"));
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

        if (shopMenuItem != null) {
            shopMenuItem.setOnMouseClicked(this::handleShopMenuClick);
            shopMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }


        if (usersMenuItem != null) {
            usersMenuItem.setOnMouseClicked(this::handleUsersMenuClick);
            usersMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (statsMenuItem != null) {
            statsMenuItem.setOnMouseClicked(this::handleStatsMenuClick);
            statsMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        if (todoMenuItem != null) {
            todoMenuItem.setOnMouseClicked(this::handleTodoMenuClick);
            todoMenuItem.setStyle("-fx-padding: 12 15; -fx-background-radius: 10; -fx-cursor: hand;");
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
        addMenuHoverEffect(todoMenuItem);
        addMenuHoverEffect(statsMenuItem);
        addMenuHoverEffect(myTicketsMenuItem);
        addMenuHoverEffect(favouriteMenuItem);
        addMenuHoverEffect(messageMenuItem);
        addMenuHoverEffect(transactionMenuItem);
        addMenuHoverEffect(bookingsMenuItem);
        addMenuHoverEffect(settingsMenuItem);
        addMenuHoverEffect(shopMenuItem);
    }

    private void handleStatsMenuClick(MouseEvent event) {
        showStatsView();
        updateMenuStyles(statsMenuItem);
    }

    private void handleUsersMenuClick(MouseEvent event) {
        isUsersViewActive = true;
        showUsersView();
        updateMenuStyles(usersMenuItem);
    }

    private void handleTodoMenuClick(MouseEvent event) {
        showTodoView();
        updateMenuStyles(todoMenuItem);
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
            case "Todo List":
                showTodoView();
                break;
            case "Stats":
                showStatsView();
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

    /**
     * Show Todo List View with drag and drop
     */
    private void showTodoView() {
        try {
            // Load todos from database
            loadTodosFromDatabase();

            VBox todoView = createTodoView();
            contentArea.getChildren().setAll(todoView);
            updateMenuStyles(todoMenuItem);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load todo view: " + e.getMessage());
        }
    }

    /**
     * Create the Todo view with drag and drop columns
     */
    private VBox createTodoView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f5f5f5;");

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📋 Todo List");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Add Todo Button
        Button addTodoBtn = new Button("+ Add New Task");
        addTodoBtn.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        addTodoBtn.setOnAction(e -> showAddTodoDialog());

        header.getChildren().addAll(title, spacer, addTodoBtn);

        // Kanban Board (3 columns)
        HBox kanbanBoard = new HBox(20);
        kanbanBoard.setAlignment(Pos.TOP_CENTER);
        kanbanBoard.setPrefHeight(600);

        // To Do Column
        VBox todoColumn = createKanbanColumn("📝 To Do", "#0FA5A2", todoList, "To Do");
        todoColumn.setPrefWidth(350);

        // In Progress Column
        VBox inProgressColumn = createKanbanColumn("⚡ In Progress", "#FEC74C", inProgressList, "In Progress");
        inProgressColumn.setPrefWidth(350);

        // Done Column
        VBox doneColumn = createKanbanColumn("✅ Done", "#2ecc71", doneList, "Done");
        doneColumn.setPrefWidth(350);

        kanbanBoard.getChildren().addAll(todoColumn, inProgressColumn, doneColumn);

        view.getChildren().addAll(header, kanbanBoard);
        return view;
    }

    /**
     * Create a Kanban column with drag and drop support
     */
    private VBox createKanbanColumn(String columnTitle, String color, ObservableList<Todo> items, String status) {
        VBox column = new VBox(10);
        column.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

        // Column Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(columnTitle);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label countLabel = new Label("(" + items.size() + ")");
        countLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #666;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, countLabel, spacer);

        // Items Container (with drag drop target)
        VBox itemsContainer = new VBox(10);
        itemsContainer.setPrefHeight(500);
        itemsContainer.setStyle("-fx-padding: 5;");

        // Add drag drop target to column
        setupDropTarget(itemsContainer, status, countLabel);

        // Populate items
        for (Todo todo : items) {
            HBox itemCard = createTodoCard(todo, itemsContainer, countLabel);
            itemsContainer.getChildren().add(itemCard);
        }

        // Make container scrollable
        ScrollPane scrollPane = new ScrollPane(itemsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        column.getChildren().addAll(header, scrollPane);
        return column;
    }

    /**
     * Create a draggable todo card
     */
    private HBox createTodoCard(Todo todo, VBox container, Label countLabel) {
        HBox card = new HBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
        card.setPrefHeight(80);
        card.setMaxWidth(Double.MAX_VALUE);

        // Priority indicator
        Rectangle priorityIndicator = new Rectangle(5, 60);
        priorityIndicator.setFill(Color.web(todo.getPriorityColor()));
        priorityIndicator.setArcWidth(5);
        priorityIndicator.setArcHeight(5);

        // Content
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(todo.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        Label descLabel = new Label(todo.getDescription() != null ?
                (todo.getDescription().length() > 30 ? todo.getDescription().substring(0, 27) + "..." : todo.getDescription()) : "");
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label(todo.getCategory() != null ? todo.getCategory() : "General");
        categoryLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-text-fill: #666;");

        Label priorityLabel = new Label(todo.getPriorityText());
        priorityLabel.setStyle("-fx-background-color: " + todo.getPriorityColor() + "20; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-text-fill: " + todo.getPriorityColor() + "; -fx-font-weight: bold;");

        footer.getChildren().addAll(categoryLabel, priorityLabel);

        content.getChildren().addAll(titleLabel, descLabel, footer);

        // Action buttons
        VBox actions = new VBox(5);
        actions.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✎");
        editBtn.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        editBtn.setPrefSize(25, 25);
        editBtn.setOnAction(e -> showEditTodoDialog(todo));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        deleteBtn.setPrefSize(25, 25);
        deleteBtn.setOnAction(e -> handleDeleteTodo(todo, container, countLabel));

        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(priorityIndicator, content, actions);

        // Make card draggable
        setupDraggable(card, todo, container);

        return card;
    }

    /**
     * Setup draggable functionality for todo cards
     */
    private void setupDraggable(HBox card, Todo todo, VBox sourceContainer) {
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(todo.getId() + ":" + todo.getStatus());
            db.setContent(content);

            // Store reference to the card for removal
            db.setDragView(card.snapshot(null, null));
            event.consume();
        });

        card.setOnDragDone(event -> {
            if (event.getTransferMode() == TransferMode.MOVE) {
                // Remove from source container after successful drop
                javafx.application.Platform.runLater(() -> {
                    sourceContainer.getChildren().remove(card);
                });
            }
            event.consume();
        });
    }

    /**
     * Setup drop target for todo columns
     */
    private void setupDropTarget(VBox targetContainer, String targetStatus, Label countLabel) {
        targetContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != targetContainer && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        targetContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                String[] data = db.getString().split(":");
                int todoId = Integer.parseInt(data[0]);
                String sourceStatus = data[1];

                if (!sourceStatus.equals(targetStatus)) {
                    try {
                        // Update in database
                        todoService.moveTodo(todoId, targetStatus);

                        // Find and move the todo in lists
                        moveTodoBetweenLists(todoId, sourceStatus, targetStatus);

                        success = true;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showAlert("Error", "Failed to move todo: " + e.getMessage());
                    }
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Move todo between ObservableLists
     */
    private void moveTodoBetweenLists(int todoId, String sourceStatus, String targetStatus) {
        ObservableList<Todo> sourceList = getListByStatus(sourceStatus);
        ObservableList<Todo> targetList = getListByStatus(targetStatus);

        for (Todo todo : sourceList) {
            if (todo.getId() == todoId) {
                sourceList.remove(todo);
                todo.setStatus(targetStatus);
                targetList.add(todo);
                break;
            }
        }

        // Refresh the view
        refreshTodoView();
    }

    /**
     * Get ObservableList by status
     */
    private ObservableList<Todo> getListByStatus(String status) {
        switch (status) {
            case "To Do": return todoList;
            case "In Progress": return inProgressList;
            case "Done": return doneList;
            default: return todoList;
        }
    }

    /**
     * Refresh the todo view
     */
    private void refreshTodoView() {
        // This will trigger a UI update
        showTodoView();
    }

    /**
     * Load todos from database
     */
    private void loadTodosFromDatabase() {
        if (currentUser == null) return;

        try {
            List<Todo> allTodos = todoService.getTodosByUserId(currentUser.getId());

            todoList.clear();
            inProgressList.clear();
            doneList.clear();

            for (Todo todo : allTodos) {
                switch (todo.getStatus()) {
                    case "To Do":
                        todoList.add(todo);
                        break;
                    case "In Progress":
                        inProgressList.add(todo);
                        break;
                    case "Done":
                        doneList.add(todo);
                        break;
                }
            }

            System.out.println("✅ Loaded todos: " + allTodos.size());

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load todos: " + e.getMessage());
        }
    }

    /**
     * Show dialog to add a new todo
     */
    private void showAddTodoDialog() {
        Dialog<Todo> dialog = new Dialog<>();
        dialog.setTitle("Add New Task");
        dialog.setHeaderText("Create a new todo item");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Task title");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Task description");
        descArea.setPrefRowCount(3);

        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("High", "Medium", "Low");
        priorityCombo.setValue("Medium");

        TextField categoryField = new TextField();
        categoryField.setPromptText("Category (e.g., Work, Personal)");

        int row = 0;
        grid.add(new Label("Title:"), 0, row);
        grid.add(titleField, 1, row++);
        grid.add(new Label("Description:"), 0, row);
        grid.add(descArea, 1, row++);
        grid.add(new Label("Priority:"), 0, row);
        grid.add(priorityCombo, 1, row++);
        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryField, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Todo todo = new Todo();
                todo.setTitle(titleField.getText());
                todo.setDescription(descArea.getText());
                todo.setStatus("To Do");
                todo.setUserId(currentUser.getId());
                todo.setCreatedAt(LocalDateTime.now());
                todo.setUpdatedAt(LocalDateTime.now());

                // Set priority
                String priority = priorityCombo.getValue();
                if (priority.equals("High")) todo.setPriority(1);
                else if (priority.equals("Medium")) todo.setPriority(2);
                else todo.setPriority(3);

                todo.setCategory(categoryField.getText());
                return todo;
            }
            return null;
        });

        Optional<Todo> result = dialog.showAndWait();
        result.ifPresent(todo -> {
            try {
                todoService.addTodo(todo);
                todoList.add(todo);
                refreshTodoView();
                showAlert("Success", "Task added successfully!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to add task: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Show dialog to edit a todo
     */
    private void showEditTodoDialog(Todo todo) {
        Dialog<Todo> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Edit todo item");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField(todo.getTitle());
        TextArea descArea = new TextArea(todo.getDescription());

        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("High", "Medium", "Low");
        priorityCombo.setValue(todo.getPriorityText());

        TextField categoryField = new TextField(todo.getCategory());

        int row = 0;
        grid.add(new Label("Title:"), 0, row);
        grid.add(titleField, 1, row++);
        grid.add(new Label("Description:"), 0, row);
        grid.add(descArea, 1, row++);
        grid.add(new Label("Priority:"), 0, row);
        grid.add(priorityCombo, 1, row++);
        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryField, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                todo.setTitle(titleField.getText());
                todo.setDescription(descArea.getText());

                String priority = priorityCombo.getValue();
                if (priority.equals("High")) todo.setPriority(1);
                else if (priority.equals("Medium")) todo.setPriority(2);
                else todo.setPriority(3);

                todo.setCategory(categoryField.getText());
                todo.setUpdatedAt(LocalDateTime.now());
                return todo;
            }
            return null;
        });

        Optional<Todo> result = dialog.showAndWait();
        result.ifPresent(updatedTodo -> {
            try {
                todoService.updateTodo(updatedTodo);
                refreshTodoView();
                showAlert("Success", "Task updated successfully!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to update task: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Handle delete todo
     */
    private void handleDeleteTodo(Todo todo, VBox container, Label countLabel) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete: " + todo.getTitle() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                todoService.deleteTodo(todo.getId());

                // Remove from list
                ObservableList<Todo> list = getListByStatus(todo.getStatus());
                list.remove(todo);

                // Update count
                countLabel.setText("(" + list.size() + ")");

                refreshTodoView();
                showAlert("Success", "Task deleted successfully!", Alert.AlertType.INFORMATION);

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete task: " + e.getMessage(), Alert.AlertType.ERROR);
            }
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
        usersSection.setAlignment(Pos.TOP_LEFT);

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
        summaryBox.setAlignment(Pos.CENTER);
        summaryBox.setPadding(new Insets(10, 0, 10, 0));
        summaryBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 10;");

        // Online Summary
        VBox onlineSummary = new VBox(5);
        onlineSummary.setAlignment(Pos.CENTER);
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
        offlineSummary.setAlignment(Pos.CENTER);
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
        header.setAlignment(Pos.CENTER_LEFT);
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
        container.setAlignment(Pos.CENTER);

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
        dropdownTimer.setAlignment(Pos.CENTER);
        dropdownTimer.setMinWidth(250);
        dropdownTimer.setTranslateY(50);

        VBox lineContainer = new VBox(5);
        lineContainer.setAlignment(Pos.CENTER);

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
        earningsBox.setAlignment(Pos.CENTER);

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
        cards.setAlignment(Pos.CENTER);

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
        card.setAlignment(Pos.CENTER);
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

    private VBox createUsersView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f5f5f5;");

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

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
        pagination.setAlignment(Pos.CENTER);
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
        editLabel.setOnMouseClicked(e -> showBeautifulEditUserDialog(user));

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
        badge.setAlignment(Pos.CENTER_LEFT);
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
        nameContainer.setAlignment(Pos.CENTER_LEFT);

        Label usernameLabel = new Label(user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        Label nameLabel = new Label(user.getName() + " " + user.getLastName());
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        nameContainer.getChildren().addAll(usernameLabel, nameLabel);
        grid.add(nameContainer, 1, 0);

        // Email with icon
        HBox emailBox = new HBox(5);
        emailBox.setAlignment(Pos.CENTER_LEFT);

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
        statusBox.setAlignment(Pos.CENTER_LEFT);

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
        actionBox.setAlignment(Pos.CENTER_LEFT);

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
        editBtn.setOnMouseClicked(e -> showBeautifulEditUserDialog(user));

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
        badge.setAlignment(Pos.CENTER_LEFT);
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

    // ==================== BEAUTIFUL ADD USER DIALOG ====================

    private void handleAddUser() {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("✨ Add New User");
        dialog.setHeaderText(null);

        // Set dialog background and styling
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #0FA5A2, #1D4D7C); -fx-background-radius: 30; -fx-padding: 20;");
        dialogPane.setPrefWidth(600);
        dialogPane.setPrefHeight(700);

        // Create header with icon
        HBox headerBox = createDialogHeader("👤", "Create New Account", "Fill in the details to add a new user");

        // Create form fields with validation
        TextField usernameField = createStyledTextField("Username", "");
        TextField emailField = createStyledTextField("Email", "");
        PasswordField passwordField = createStyledPasswordField("Password", "");
        PasswordField confirmPasswordField = createStyledPasswordField("Confirm Password", "");
        TextField firstNameField = createStyledTextField("First Name", "");
        TextField lastNameField = createStyledTextField("Last Name", "");
        TextField dateField = createStyledTextField("Birth Date", "");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select date");
        datePicker.setPrefHeight(45);
        datePicker.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-font-size: 14px;");

        usernameField.setPromptText("Enter username");
        emailField.setPromptText("Enter email address");
        passwordField.setPromptText("Enter password");
        confirmPasswordField.setPromptText("Confirm password");
        firstNameField.setPromptText("Enter first name");
        lastNameField.setPromptText("Enter last name");
        dateField.setPromptText("DD/MM/YYYY");

        // Add real-time validation
        Label usernameError = createValidationLabel();
        Label emailError = createValidationLabel();
        Label passwordError = createValidationLabel();
        Label confirmError = createValidationLabel();
        Label dateError = createValidationLabel();

        // Username validation
        usernameField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) {
                usernameError.setText("Username is required");
                usernameError.setVisible(true);
                usernameField.setStyle(getFieldErrorStyle());
            } else if (val.length() < 3) {
                usernameError.setText("Username must be at least 3 characters");
                usernameError.setVisible(true);
                usernameField.setStyle(getFieldErrorStyle());
            } else if (!val.matches("[A-Za-z0-9_]+")) {
                usernameError.setText("Only letters, numbers and underscore");
                usernameError.setVisible(true);
                usernameField.setStyle(getFieldErrorStyle());
            } else {
                usernameError.setVisible(false);
                usernameField.setStyle(getFieldValidStyle());
            }
        });

        // Email validation
        emailField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) {
                emailError.setText("Email is required");
                emailError.setVisible(true);
                emailField.setStyle(getFieldErrorStyle());
            } else if (!val.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                emailError.setText("Enter a valid email address");
                emailError.setVisible(true);
                emailField.setStyle(getFieldErrorStyle());
            } else {
                emailError.setVisible(false);
                emailField.setStyle(getFieldValidStyle());
            }
        });

        // Password validation
        passwordField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) {
                passwordError.setText("Password is required");
                passwordError.setVisible(true);
                passwordField.setStyle(getFieldErrorStyle());
            } else if (val.length() < 8) {
                passwordError.setText("Password must be at least 8 characters");
                passwordError.setVisible(true);
                passwordField.setStyle(getFieldErrorStyle());
            } else {
                passwordError.setVisible(false);
                passwordField.setStyle(getFieldValidStyle());
            }
            validateConfirmPassword(passwordField, confirmPasswordField, confirmError);
        });

        // Confirm password validation
        confirmPasswordField.textProperty().addListener((obs, old, val) -> {
            validateConfirmPassword(passwordField, confirmPasswordField, confirmError);
        });

        // Date validation
        dateField.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !val.matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19[4-9][0-9]|20[01][0-9]|202[0-6])$")) {
                dateError.setText("Use format: DD/MM/YYYY");
                dateError.setVisible(true);
                dateField.setStyle(getFieldErrorStyle());
            } else {
                dateError.setVisible(false);
                dateField.setStyle(getFieldValidStyle());
            }
        });

        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(350);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;
        addFormFieldWithValidation(grid, "👤 Username:", usernameField, usernameError, row++);
        addFormFieldWithValidation(grid, "📧 Email:", emailField, emailError, row++);
        addFormFieldWithValidation(grid, "🔒 Password:", passwordField, passwordError, row++);
        addFormFieldWithValidation(grid, "✓ Confirm:", confirmPasswordField, confirmError, row++);
        addFormFieldWithValidation(grid, "📛 First Name:", firstNameField, null, row++);
        addFormFieldWithValidation(grid, "📛 Last Name:", lastNameField, null, row++);

        // Date row with both text field and date picker
        HBox dateBox = new HBox(10);
        dateBox.getChildren().addAll(dateField, datePicker);
        HBox.setHgrow(dateField, Priority.ALWAYS);

        Label dateLabel = new Label("📅 Birth Date:");
        dateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        VBox dateContainer = new VBox(3);
        dateContainer.getChildren().addAll(dateBox, dateError);

        grid.add(dateLabel, 0, row);
        grid.add(dateContainer, 1, row++);

        // Role selection
        HBox roleBox = createRoleSelector();

        // Membership selection with cards
        VBox membershipBox = createMembershipSelector();

        // Create content container
        VBox content = new VBox(20);
        content.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 20; -fx-padding: 25;");
        content.setEffect(new DropShadow(20, Color.web("#00000040")));
        content.getChildren().addAll(grid, roleBox, membershipBox);

        // Button Bar
        ButtonType saveButtonType = new ButtonType("✨ Create Account", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle("-fx-background-color: linear-gradient(to right, #0FA5A2, #1D4D7C); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand;");
        saveButton.setEffect(new DropShadow(10, Color.web("#0FA5A280")));

        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand;");
        cancelButton.setEffect(new DropShadow(10, Color.web("#ff5e6280")));

        // Add hover effects
        addButtonHoverEffect(saveButton, "linear-gradient(to right, #0FA5A2, #1D4D7C)", "#0FA5A2");
        addButtonHoverEffect(cancelButton, "#ff5e62", "#ff5e62");

        // Disable save button initially
        saveButton.setDisable(true);

        // Enable save button only when all validations pass
        Runnable updateSaveButton = () -> {
            boolean valid = !usernameField.getText().isEmpty() && !usernameError.isVisible() &&
                    !emailField.getText().isEmpty() && !emailError.isVisible() &&
                    !passwordField.getText().isEmpty() && !passwordError.isVisible() &&
                    !confirmPasswordField.getText().isEmpty() && !confirmError.isVisible();
            saveButton.setDisable(!valid);
        };

        usernameField.textProperty().addListener((obs, old, val) -> updateSaveButton.run());
        emailField.textProperty().addListener((obs, old, val) -> updateSaveButton.run());
        passwordField.textProperty().addListener((obs, old, val) -> updateSaveButton.run());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> updateSaveButton.run());

        // Combine everything
        VBox mainContent = new VBox(20);
        mainContent.getChildren().addAll(headerBox, content);

        dialogPane.setContent(mainContent);

        // Get selected role and membership
        ToggleGroup roleGroup = (ToggleGroup) roleBox.getUserData();

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Person person = new Person();
                person.setUsername(usernameField.getText().trim());
                person.setEmail(emailField.getText().trim().toLowerCase());
                person.setPassword(passwordField.getText());
                person.setName(firstNameField.getText().trim());
                person.setLastName(lastNameField.getText().trim());

                // Get selected role
                RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle();
                String role = "USER";
                if (selectedRole != null) {
                    String roleText = selectedRole.getText();
                    if (roleText.contains("Guider")) role = "GUIDER";
                    else if (roleText.contains("Admin")) role = "ADMIN";
                }
                person.setRole(role);

                // Parse date from either field or picker
                if (!dateField.getText().isEmpty()) {
                    try {
                        String[] dateParts = dateField.getText().split("/");
                        String sqlDateStr = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];
                        person.setDate(java.sql.Date.valueOf(sqlDateStr));
                    } catch (Exception e) {
                        // Invalid date
                    }
                } else if (datePicker.getValue() != null) {
                    person.setDate(java.sql.Date.valueOf(datePicker.getValue()));
                }

                // Get selected membership from the first selected card
                String membership = "Standard";
                HBox membershipCards = (HBox) membershipBox.getChildren().get(1);
                for (Node node : membershipCards.getChildren()) {
                    if (node instanceof VBox) {
                        VBox card = (VBox) node;
                        Rectangle indicator = (Rectangle) card.getChildren().get(3);
                        if (indicator.isVisible()) {
                            membership = (String) card.getUserData();
                            break;
                        }
                    }
                }

                return new Object[]{person, membership};
            }
            return null;
        });

        Optional<Object[]> result = dialog.showAndWait();

        result.ifPresent(data -> {
            Person person = (Person) data[0];
            String membership = (String) data[1];
            saveNewUser(person, membership);
        });
    }

    // ==================== BEAUTIFUL EDIT USER DIALOG ====================

    private void showBeautifulEditUserDialog(Person user) {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("✏️ Edit User");
        dialog.setHeaderText(null);

        // Set dialog background and styling
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #FEC74C, #0FA5A2); -fx-background-radius: 30; -fx-padding: 20;");
        dialogPane.setPrefWidth(600);
        dialogPane.setPrefHeight(700);

        // Get current profile
        Profile userProfile = null;
        try {
            userProfile = profileService.getProfileByUserId(user.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String currentMembership = (userProfile != null && userProfile.getMemberPremium() != null)
                ? userProfile.getMemberPremium() : "Standard";

        // Create header with icon
        HBox headerBox = createDialogHeader("✏️", "Edit User: " + user.getUsername(), "Update user information");

        // Create form fields with existing data
        TextField usernameField = createStyledTextField("Username", user.getUsername());
        TextField emailField = createStyledTextField("Email", user.getEmail());
        TextField firstNameField = createStyledTextField("First Name", user.getName());
        TextField lastNameField = createStyledTextField("Last Name", user.getLastName());

        // Format date for display
        String dateString = "";
        DatePicker datePicker = new DatePicker();
        if (user.getDate() != null) {
            LocalDate date = user.getDate().toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            dateString = date.format(formatter);
            datePicker.setValue(date);
        }
        datePicker.setPromptText("Select date");
        datePicker.setPrefHeight(45);
        datePicker.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-font-size: 14px;");

        TextField dateField = createStyledTextField("Birth Date", dateString);

        usernameField.setPromptText("Enter username");
        emailField.setPromptText("Enter email address");
        firstNameField.setPromptText("Enter first name");
        lastNameField.setPromptText("Enter last name");
        dateField.setPromptText("DD/MM/YYYY");

        // Add validation labels
        Label usernameError = createValidationLabel();
        Label emailError = createValidationLabel();
        Label dateError = createValidationLabel();

        // Username validation
        usernameField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) {
                usernameError.setText("Username is required");
                usernameError.setVisible(true);
                usernameField.setStyle(getFieldErrorStyle());
            } else if (val.length() < 3) {
                usernameError.setText("Username must be at least 3 characters");
                usernameError.setVisible(true);
                usernameField.setStyle(getFieldErrorStyle());
            } else if (!val.matches("[A-Za-z0-9_]+")) {
                usernameError.setText("Only letters, numbers and underscore");
                usernameError.setVisible(true);
                usernameField.setStyle(getFieldErrorStyle());
            } else {
                usernameError.setVisible(false);
                usernameField.setStyle(getFieldValidStyle());
            }
        });

        // Email validation
        emailField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) {
                emailError.setText("Email is required");
                emailError.setVisible(true);
                emailField.setStyle(getFieldErrorStyle());
            } else if (!val.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                emailError.setText("Enter a valid email address");
                emailError.setVisible(true);
                emailField.setStyle(getFieldErrorStyle());
            } else {
                emailError.setVisible(false);
                emailField.setStyle(getFieldValidStyle());
            }
        });

        // Date validation
        dateField.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !val.matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19[4-9][0-9]|20[01][0-9]|202[0-6])$")) {
                dateError.setText("Use format: DD/MM/YYYY");
                dateError.setVisible(true);
                dateField.setStyle(getFieldErrorStyle());
            } else {
                dateError.setVisible(false);
                dateField.setStyle(getFieldValidStyle());
            }
        });

        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(350);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;
        addFormFieldWithValidation(grid, "👤 Username:", usernameField, usernameError, row++);
        addFormFieldWithValidation(grid, "📧 Email:", emailField, emailError, row++);
        addFormFieldWithValidation(grid, "📛 First Name:", firstNameField, null, row++);
        addFormFieldWithValidation(grid, "📛 Last Name:", lastNameField, null, row++);

        // Date row with both text field and date picker
        HBox dateBox = new HBox(10);
        dateBox.getChildren().addAll(dateField, datePicker);
        HBox.setHgrow(dateField, Priority.ALWAYS);

        Label dateLabel = new Label("📅 Birth Date:");
        dateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        VBox dateContainer = new VBox(3);
        dateContainer.getChildren().addAll(dateBox, dateError);

        grid.add(dateLabel, 0, row);
        grid.add(dateContainer, 1, row++);

        // Role selection - preselect current role
        HBox roleBox = createRoleSelectorWithSelection(user.getRole());

        // Membership selection with cards - preselect current membership
        VBox membershipBox = createMembershipSelectorWithSelection(currentMembership);

        // Set default image option
        boolean hasImage = (userProfile != null && userProfile.getImage() != null && userProfile.getImage().length > 0);
        CheckBox setDefaultImageCheckBox = new CheckBox(hasImage ? "Replace with default image" : "Set default image");
        setDefaultImageCheckBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
        if (!hasImage) {
            setDefaultImageCheckBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff5e62; -fx-font-weight: bold;");
        }

        HBox imageOptionBox = new HBox(setDefaultImageCheckBox);
        imageOptionBox.setAlignment(Pos.CENTER_LEFT);
        imageOptionBox.setPadding(new Insets(10, 0, 0, 0));

        // Create content container
        VBox content = new VBox(20);
        content.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 20; -fx-padding: 25;");
        content.setEffect(new DropShadow(20, Color.web("#00000040")));
        content.getChildren().addAll(grid, roleBox, membershipBox, imageOptionBox);

        // Button Bar
        ButtonType saveButtonType = new ButtonType("💾 Save Changes", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style buttons
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.setStyle("-fx-background-color: linear-gradient(to right, #0FA5A2, #1D4D7C); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand;");
        saveButton.setEffect(new DropShadow(10, Color.web("#0FA5A280")));

        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand;");
        cancelButton.setEffect(new DropShadow(10, Color.web("#ff5e6280")));

        // Add hover effects
        addButtonHoverEffect(saveButton, "linear-gradient(to right, #0FA5A2, #1D4D7C)", "#0FA5A2");
        addButtonHoverEffect(cancelButton, "#ff5e62", "#ff5e62");

        // Disable save button initially if validation fails
        saveButton.setDisable(false);

        // Validate on load
        usernameField.setText(user.getUsername()); // Trigger validation
        emailField.setText(user.getEmail());

        // Combine everything
        VBox mainContent = new VBox(20);
        mainContent.getChildren().addAll(headerBox, content);

        dialogPane.setContent(mainContent);

        // Get selected role and membership
        ToggleGroup roleGroup = (ToggleGroup) roleBox.getUserData();

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                user.setUsername(usernameField.getText().trim());
                user.setEmail(emailField.getText().trim().toLowerCase());
                user.setName(firstNameField.getText().trim());
                user.setLastName(lastNameField.getText().trim());

                // Get selected role
                RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle();
                if (selectedRole != null) {
                    String roleText = selectedRole.getText();
                    if (roleText.contains("Guider")) user.setRole("GUIDER");
                    else if (roleText.contains("Admin")) user.setRole("ADMIN");
                    else user.setRole("USER");
                }

                // Parse date if provided
                if (!dateField.getText().isEmpty()) {
                    try {
                        String[] dateParts = dateField.getText().split("/");
                        String sqlDateStr = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];
                        user.setDate(java.sql.Date.valueOf(sqlDateStr));
                    } catch (Exception e) {
                        // Keep existing date
                    }
                } else if (datePicker.getValue() != null) {
                    user.setDate(java.sql.Date.valueOf(datePicker.getValue()));
                }

                // Get selected membership from the first selected card
                String membership = "Standard";
                HBox membershipCards = (HBox) membershipBox.getChildren().get(1);
                for (Node node : membershipCards.getChildren()) {
                    if (node instanceof VBox) {
                        VBox card = (VBox) node;
                        Rectangle indicator = (Rectangle) card.getChildren().get(3);
                        if (indicator.isVisible()) {
                            membership = (String) card.getUserData();
                            break;
                        }
                    }
                }

                return new Object[]{user, membership, setDefaultImageCheckBox.isSelected()};
            }
            return null;
        });

        Optional<Object[]> result = dialog.showAndWait();

        result.ifPresent(data -> {
            Person updatedUser = (Person) data[0];
            String newMembership = (String) data[1];
            boolean setDefaultImage = (boolean) data[2];
            updateExistingUser(updatedUser, newMembership, setDefaultImage);
        });
    }

    // ==================== HELPER METHODS FOR DIALOGS ====================

    private HBox createDialogHeader(String emoji, String title, String subtitle) {
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 20, 0));

        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 48px; -fx-background-color: #FEC74C; -fx-background-radius: 50; -fx-padding: 15; -fx-text-fill: #1D4D7C;");
        iconLabel.setEffect(new DropShadow(15, Color.web("#FEC74C80")));

        VBox titleBox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        headerBox.getChildren().addAll(iconLabel, titleBox);

        return headerBox;
    }

    private TextField createStyledTextField(String prompt, String text) {
        TextField field = new TextField(text);
        field.setPromptText(prompt);
        field.setPrefHeight(45);
        field.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px;");

        field.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                field.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #0FA5A2; -fx-border-width: 2; -fx-padding: 0 15; -fx-font-size: 14px; -fx-effect: dropshadow(gaussian, #0FA5A240, 10, 0, 0, 0);");
            } else {
                String currentStyle = field.getStyle();
                if (!currentStyle.contains("#ff5e62")) {
                    field.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px;");
                }
            }
        });

        return field;
    }

    private PasswordField createStyledPasswordField(String prompt, String text) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setText(text);
        field.setPrefHeight(45);
        field.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px;");

        field.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                field.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #0FA5A2; -fx-border-width: 2; -fx-padding: 0 15; -fx-font-size: 14px; -fx-effect: dropshadow(gaussian, #0FA5A240, 10, 0, 0, 0);");
            } else {
                String currentStyle = field.getStyle();
                if (!currentStyle.contains("#ff5e62")) {
                    field.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 0 15; -fx-font-size: 14px;");
                }
            }
        });

        return field;
    }

    private Label createValidationLabel() {
        Label label = new Label();
        label.setTextFill(Color.INDIANRED);
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        label.setVisible(false);
        label.setWrapText(true);
        return label;
    }

    private String getFieldErrorStyle() {
        return "-fx-background-color: #fff0f0; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #ff5e62; -fx-border-width: 2; -fx-padding: 0 15; -fx-font-size: 14px;";
    }

    private String getFieldValidStyle() {
        return "-fx-background-color: #f0fff0; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #2ecc71; -fx-border-width: 2; -fx-padding: 0 15; -fx-font-size: 14px;";
    }

    private void addFormFieldWithValidation(GridPane grid, String labelText, TextField field, Label errorLabel, int row) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        VBox fieldContainer = new VBox(3);
        fieldContainer.getChildren().add(field);
        if (errorLabel != null) {
            fieldContainer.getChildren().add(errorLabel);
        }

        grid.add(label, 0, row);
        grid.add(fieldContainer, 1, row);
    }

    private HBox createRoleSelector() {
        HBox roleBox = new HBox(20);
        roleBox.setAlignment(Pos.CENTER_LEFT);
        roleBox.setPadding(new Insets(10, 0, 0, 0));

        Label roleLabel = new Label("👥 Role:");
        roleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-min-width: 120;");

        ToggleGroup roleGroup = new ToggleGroup();

        RadioButton userRadio = createStyledRoleRadio("👤 User", "#0FA5A2", roleGroup);
        RadioButton guiderRadio = createStyledRoleRadio("🧭 Guider", "#FEC74C", roleGroup);
        RadioButton adminRadio = createStyledRoleRadio("👑 Admin", "#9b59b6", roleGroup);

        userRadio.setSelected(true);

        roleBox.getChildren().addAll(roleLabel, userRadio, guiderRadio, adminRadio);
        roleBox.setUserData(roleGroup);

        return roleBox;
    }

    private HBox createRoleSelectorWithSelection(String currentRole) {
        HBox roleBox = new HBox(20);
        roleBox.setAlignment(Pos.CENTER_LEFT);
        roleBox.setPadding(new Insets(10, 0, 0, 0));

        Label roleLabel = new Label("👥 Role:");
        roleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C; -fx-min-width: 120;");

        ToggleGroup roleGroup = new ToggleGroup();

        RadioButton userRadio = createStyledRoleRadio("👤 User", "#0FA5A2", roleGroup);
        RadioButton guiderRadio = createStyledRoleRadio("🧭 Guider", "#FEC74C", roleGroup);
        RadioButton adminRadio = createStyledRoleRadio("👑 Admin", "#9b59b6", roleGroup);

        // Select based on current role
        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            adminRadio.setSelected(true);
        } else if ("GUIDER".equalsIgnoreCase(currentRole)) {
            guiderRadio.setSelected(true);
        } else {
            userRadio.setSelected(true);
        }

        roleBox.getChildren().addAll(roleLabel, userRadio, guiderRadio, adminRadio);
        roleBox.setUserData(roleGroup);

        return roleBox;
    }

    private RadioButton createStyledRoleRadio(String text, String color, ToggleGroup group) {
        RadioButton radio = new RadioButton(text);
        radio.setToggleGroup(group);
        radio.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-font-weight: bold;");

        Circle circle = new Circle(8);
        circle.setFill(Color.web(color));
        circle.setEffect(new InnerShadow(3, Color.web(color + "80")));
        radio.setGraphic(circle);

        return radio;
    }

    private VBox createMembershipSelector() {
        VBox membershipBox = new VBox(10);
        membershipBox.setPadding(new Insets(10, 0, 0, 0));

        Label membershipTitle = new Label("💎 Membership Plan");
        membershipTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        HBox membershipCards = new HBox(15);
        membershipCards.setAlignment(Pos.CENTER);

        VBox standardCard = createMembershipCard("📋 STANDARD", "0 DT", "#95a5a6");
        VBox premiumCard = createMembershipCard("⭐ PREMIUM", "29 DT", "#0FA5A2");
        VBox vipCard = createMembershipCard("👑 VIP", "59 DT", "#FEC74C");
        VBox vipPlusCard = createMembershipCard("💎 VIP+", "99 DT", "#9b59b6");

        standardCard.setUserData("Standard");
        premiumCard.setUserData("Premium");
        vipCard.setUserData("VIP");
        vipPlusCard.setUserData("VIP+");

        // Set Standard as default selected
        Rectangle standardIndicator = (Rectangle) standardCard.getChildren().get(3);
        standardIndicator.setVisible(true);

        membershipCards.getChildren().addAll(standardCard, premiumCard, vipCard, vipPlusCard);
        membershipBox.getChildren().addAll(membershipTitle, membershipCards);

        return membershipBox;
    }

    private VBox createMembershipSelectorWithSelection(String currentMembership) {
        VBox membershipBox = new VBox(10);
        membershipBox.setPadding(new Insets(10, 0, 0, 0));

        Label membershipTitle = new Label("💎 Membership Plan");
        membershipTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

        HBox membershipCards = new HBox(15);
        membershipCards.setAlignment(Pos.CENTER);

        VBox standardCard = createMembershipCard("📋 STANDARD", "0 DT", "#95a5a6");
        VBox premiumCard = createMembershipCard("⭐ PREMIUM", "29 DT", "#0FA5A2");
        VBox vipCard = createMembershipCard("👑 VIP", "59 DT", "#FEC74C");
        VBox vipPlusCard = createMembershipCard("💎 VIP+", "99 DT", "#9b59b6");

        standardCard.setUserData("Standard");
        premiumCard.setUserData("Premium");
        vipCard.setUserData("VIP");
        vipPlusCard.setUserData("VIP+");

        // Select based on current membership
        Rectangle indicator;
        if ("VIP+".equalsIgnoreCase(currentMembership)) {
            indicator = (Rectangle) vipPlusCard.getChildren().get(3);
        } else if ("VIP".equalsIgnoreCase(currentMembership)) {
            indicator = (Rectangle) vipCard.getChildren().get(3);
        } else if ("Premium".equalsIgnoreCase(currentMembership)) {
            indicator = (Rectangle) premiumCard.getChildren().get(3);
        } else {
            indicator = (Rectangle) standardCard.getChildren().get(3);
        }
        indicator.setVisible(true);

        membershipCards.getChildren().addAll(standardCard, premiumCard, vipCard, vipPlusCard);
        membershipBox.getChildren().addAll(membershipTitle, membershipCards);

        return membershipBox;
    }

    private VBox createMembershipCard(String title, String price, String color) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(110);
        card.setPrefHeight(100);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 15; -fx-border-width: 1; -fx-cursor: hand;");
        card.setEffect(new DropShadow(5, Color.web("#00000020")));

        String[] parts = title.split(" ");
        String emoji = parts[0];
        String name = parts[1];

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 24px;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label priceLabel = new Label(price);
        priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        card.getChildren().addAll(emojiLabel, nameLabel, priceLabel);

        // Selection indicator
        Rectangle selectionIndicator = new Rectangle(90, 4, Color.web(color));
        selectionIndicator.setArcWidth(4);
        selectionIndicator.setArcHeight(4);
        selectionIndicator.setVisible(false);
        card.getChildren().add(selectionIndicator);

        // Mouse click handler
        card.setOnMouseClicked(e -> {
            // Deselect all cards first
            if (card.getParent() instanceof HBox) {
                HBox parent = (HBox) card.getParent();
                for (Node node : parent.getChildren()) {
                    if (node instanceof VBox) {
                        VBox otherCard = (VBox) node;
                        Rectangle otherIndicator = (Rectangle) otherCard.getChildren().get(3);
                        otherIndicator.setVisible(false);
                    }
                }
            }
            // Select this card
            selectionIndicator.setVisible(true);
        });

        // Hover effects
        card.setOnMouseEntered(e -> {
            if (!selectionIndicator.isVisible()) {
                card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 12; -fx-border-color: " + color + "; -fx-border-radius: 15; -fx-border-width: 2; -fx-cursor: hand;");
                card.setEffect(new DropShadow(10, Color.web(color + "80")));
            }
        });

        card.setOnMouseExited(e -> {
            if (!selectionIndicator.isVisible()) {
                card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 15; -fx-border-width: 1; -fx-cursor: hand;");
                card.setEffect(new DropShadow(5, Color.web("#00000020")));
            }
        });

        return card;
    }

    private void addButtonHoverEffect(Button button, String baseColor, String glowColor) {
        button.setOnMouseEntered(e -> {
            if (baseColor.startsWith("linear-gradient")) {
                button.setStyle("-fx-background-color: linear-gradient(to right, #1D4D7C, #0FA5A2); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + glowColor + ", 15, 0, 0, 0);");
            } else {
                button.setStyle("-fx-background-color: #ff3030; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + glowColor + ", 15, 0, 0, 0);");
            }
        });

        button.setOnMouseExited(e -> {
            if (baseColor.startsWith("linear-gradient")) {
                button.setStyle("-fx-background-color: linear-gradient(to right, #0FA5A2, #1D4D7C); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand;");
            } else {
                button.setStyle("-fx-background-color: #ff5e62; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 25; -fx-cursor: hand;");
            }
        });
    }

    private void validateConfirmPassword(PasswordField passwordField, PasswordField confirmField, Label errorLabel) {
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (confirm.isEmpty()) {
            errorLabel.setText("Please confirm your password");
            errorLabel.setVisible(true);
            confirmField.setStyle(getFieldErrorStyle());
        } else if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match");
            errorLabel.setVisible(true);
            confirmField.setStyle(getFieldErrorStyle());
        } else {
            errorLabel.setVisible(false);
            confirmField.setStyle(getFieldValidStyle());
        }
    }

    private void saveNewUser(Person person, String membership) {
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
    }

    private void updateExistingUser(Person user, String newMembership, boolean setDefaultImage) {
        try {
            personService.updateOne(user);

            Profile profile = profileService.getProfileByUserId(user.getId());

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
                            profileImageCache.put(user.getId(), image);
                        } catch (IOException e) {
                            System.err.println("Failed to load default image: " + e.getMessage());
                        }
                    }
                }

                profileService.updateOne(profile);
            } else {
                profile = new Profile();
                profile.setIdUser(user.getId());
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
                            profileImageCache.put(user.getId(), image);
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
    }

    // ==================== EXISTING METHODS (continued) ====================

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
        userInfo.setAlignment(Pos.CENTER_LEFT);

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

        Label[] menus = {dashboardMenuItem, usersMenuItem, todoMenuItem, statsMenuItem, shopMenuItem,
                myTicketsMenuItem, favouriteMenuItem, messageMenuItem, transactionMenuItem,
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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterPersonne.fxml"));
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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterPersonne.fxml"));
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

    // Stats methods
    private int calculateAge(java.sql.Date birthDate) {
        if (birthDate == null) return 0;
        LocalDate birthLocalDate = birthDate.toLocalDate();
        LocalDate currentDate = LocalDate.now();
        return Period.between(birthLocalDate, currentDate).getYears();
    }

    private Map<String, Integer> getAgeDistribution(List<Person> users) {
        Map<String, Integer> ageStats = new HashMap<>();
        ageStats.put("Under 18", 0);
        ageStats.put("18-25", 0);
        ageStats.put("26-35", 0);
        ageStats.put("36-50", 0);
        ageStats.put("Over 50", 0);

        for (Person user : users) {
            if (user.getDate() != null) {
                int age = calculateAge(user.getDate());
                if (age < 18) ageStats.put("Under 18", ageStats.get("Under 18") + 1);
                else if (age <= 25) ageStats.put("18-25", ageStats.get("18-25") + 1);
                else if (age <= 35) ageStats.put("26-35", ageStats.get("26-35") + 1);
                else if (age <= 50) ageStats.put("36-50", ageStats.get("36-50") + 1);
                else ageStats.put("Over 50", ageStats.get("Over 50") + 1);
            }
        }
        return ageStats;
    }

    private Map<String, Integer> getTwoFAStats(List<Person> users) {
        Map<String, Integer> twoFAStats = new HashMap<>();
        twoFAStats.put("Enabled", 0);
        twoFAStats.put("Disabled", 0);

        for (Person user : users) {
            if (user.isTwoFactorEnabled()) twoFAStats.put("Enabled", twoFAStats.get("Enabled") + 1);
            else twoFAStats.put("Disabled", twoFAStats.get("Disabled") + 1);
        }
        return twoFAStats;
    }

    private Map<String, Integer> getMembershipStats() {
        Map<String, Integer> membershipStats = new HashMap<>();
        membershipStats.put("Premium", 0);
        membershipStats.put("Standard", 0);
        membershipStats.put("VIP", 0);

        try {
            List<Person> users = personService.selectALL();
            for (Person user : users) {
                Profile profile = profileService.getProfileByUserId(user.getId());
                if (profile != null && profile.getMemberPremium() != null) {
                    String membership = profile.getMemberPremium();
                    membershipStats.put(membership, membershipStats.getOrDefault(membership, 0) + 1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return membershipStats;
    }

    private void showStatsView() {
        try {
            VBox statsView = createStatsView();
            contentArea.getChildren().setAll(statsView);
            updateMenuStyles(statsMenuItem);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load stats view: " + e.getMessage());
        }
    }

    private VBox createStatsView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f5f5f5;");

        // Header with title only
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("📊 Statistics Dashboard");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");
        header.getChildren().add(title);

        // Centered Refresh Button
        HBox refreshContainer = new HBox();
        refreshContainer.setAlignment(Pos.CENTER);
        Button refreshBtn = new Button("🔄 Refresh Data");
        refreshBtn.setStyle("-fx-background-color: #0FA5A2; -fx-text-fill: white; -fx-padding: 12 30; -fx-background-radius: 25; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #0FA5A280, 10, 0, 0, 0);");
        refreshBtn.setOnAction(e -> showStatsView());
        refreshContainer.getChildren().add(refreshBtn);

        try {
            List<Person> users = personService.selectALL();

            // Stats Grid
            GridPane statsGrid = new GridPane();
            statsGrid.setHgap(25);
            statsGrid.setVgap(25);
            statsGrid.setAlignment(Pos.CENTER);

            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(50);
            col1.setHgrow(Priority.ALWAYS);

            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(50);
            col2.setHgrow(Priority.ALWAYS);

            statsGrid.getColumnConstraints().addAll(col1, col2);

            // Age Distribution Card
            VBox ageCard = createModernStatCard(
                    "📊 Age Distribution",
                    "#0FA5A2",
                    getAgeDistribution(users),
                    users.size()
            );

            // 2FA Status Card
            VBox twoFACard = createModernStatCard(
                    "🔐 2FA Status",
                    "#FEC74C",
                    getTwoFAStats(users),
                    users.size()
            );

            // Membership Status Card
            VBox membershipCard = createModernStatCard(
                    "💎 Membership Status",
                    "#9C27B0",
                    getMembershipStats(),
                    users.size()
            );

            statsGrid.add(ageCard, 0, 0);
            statsGrid.add(twoFACard, 1, 0);
            statsGrid.add(membershipCard, 0, 1, 2, 1);

            view.getChildren().addAll(header, refreshContainer, statsGrid);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load statistics: " + e.getMessage());
        }

        return view;
    }

    private VBox createModernStatCard(String title, String color, Map<String, Integer> stats, int total) {
        VBox card = new VBox(20);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 0);");
        card.setPrefWidth(450);

        // Card Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label totalLabel = new Label("Total: " + total);
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #666; -fx-background-color: #f0f0f0; -fx-padding: 5 12; -fx-background-radius: 20;");

        header.getChildren().addAll(titleLabel, spacer, totalLabel);

        // Stats Content
        VBox content = new VBox(15);
        content.setPadding(new Insets(10, 0, 0, 0));

        // Sort stats by value
        List<Map.Entry<String, Integer>> sortedStats = new ArrayList<>(stats.entrySet());
        sortedStats.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sortedStats) {
            String key = entry.getKey();
            int value = entry.getValue();
            double percentage = total > 0 ? (double) value / total * 100 : 0;

            HBox statRow = new HBox(15);
            statRow.setAlignment(Pos.CENTER_LEFT);

            // Label with icon
            String icon = getIconForKey(key);
            Label keyLabel = new Label(icon + " " + key);
            keyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 120; -fx-text-fill: #333;");

            // Value badge
            Label valueBadge = new Label(String.valueOf(value));
            valueBadge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 15; -fx-min-width: 40; -fx-alignment: center;");

            // Progress bar container
            StackPane progressContainer = new StackPane();
            progressContainer.setPrefWidth(200);
            progressContainer.setPrefHeight(12);
            progressContainer.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10;");

            // Progress bar
            Rectangle progressBar = new Rectangle(200 * percentage / 100, 12);
            progressBar.setFill(getProgressGradient(color, percentage));
            progressBar.setArcWidth(10);
            progressBar.setArcHeight(10);

            progressContainer.getChildren().add(progressBar);

            // Percentage label
            Label percentLabel = new Label(String.format("%.1f%%", percentage));
            percentLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-min-width: 55;");

            statRow.getChildren().addAll(keyLabel, valueBadge, progressContainer, percentLabel);
            content.getChildren().add(statRow);
        }

        card.getChildren().addAll(header, content);
        return card;
    }

    private String getIconForKey(String key) {
        switch (key.toLowerCase()) {
            case "enabled": return "✅";
            case "disabled": return "❌";
            case "premium": return "💎";
            case "standard": return "📋";
            case "vip": return "👑";
            case "under 18": return "🧒";
            case "18-25": return "👤";
            case "26-35": return "👨";
            case "36-50": return "👨‍🦰";
            case "over 50": return "👴";
            default: return "📊";
        }
    }

    private LinearGradient getProgressGradient(String baseColor, double percentage) {
        Color startColor, endColor;
        if (percentage < 30) {
            startColor = Color.web("#ff5e62");
            endColor = Color.web("#ff9966");
        } else if (percentage < 60) {
            startColor = Color.web("#FEC74C");
            endColor = Color.web("#FFD966");
        } else {
            startColor = Color.web("#2ecc71");
            endColor = Color.web("#27ae60");
        }
        return new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, startColor), new Stop(1, endColor));
    }

    private void handleShopMenuClick(MouseEvent event) {
        showShopManagement();
        updateMenuStyles(shopMenuItem);
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

    private void showShopManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ShopManagement.fxml"));
            Parent shopRoot = loader.load();

            ShopManagementController shopController = loader.getController();
            shopController.setUserData(currentUser);

            contentArea.getChildren().setAll(shopRoot);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open shop management: " + e.getMessage());
        }
    }


    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private void showDashboardMessaging() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/BackofficeView.fxml"));
            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}