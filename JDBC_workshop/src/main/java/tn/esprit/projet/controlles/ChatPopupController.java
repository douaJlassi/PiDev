package tn.esprit.projet.controlles;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.util.Duration;
import tn.esprit.projet.entities.Message;
import tn.esprit.projet.entities.Person;
import tn.esprit.projet.services.MessageService;
import tn.esprit.projet.services.PersonService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ChatPopupController {

    @FXML
    private TabPane chatTabPane;
    @FXML
    private Tab messagesTab;
    @FXML
    private ListView<String> conversationsList;
    @FXML
    private VBox chatArea;
    @FXML
    private TextArea messageInput;
    @FXML
    private Button sendMessageBtn;
    @FXML
    private Label conversationTitle;
    @FXML
    private Label unreadCountLabel;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private TextField searchField;
    @FXML
    private Button newChatBtn;
    @FXML
    private Button backToConversationsBtn;
    @FXML
    private Button closeButton;

    private Person currentUser;
    private String currentConversationId;
    private MessageService messageService;
    private PersonService personService;
    private Timeline refreshTimeline;
    private ObservableList<String> conversations;
    private Popup popup;
    private String userRole;

    @FXML
    public void initialize() {
        messageService = new MessageService();
        personService = new PersonService();
        conversations = FXCollections.observableArrayList();
        conversationsList.setItems(conversations);

        // Initially show conversations tab
        chatTabPane.getSelectionModel().select(0);

        // Custom cell factory for conversations
        conversationsList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = createConversationCell(item);
                    setGraphic(cell);
                }
            }
        });

        // Search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterConversations(newVal));

        // Send message on Enter key
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && !event.isShiftDown()) {
                event.consume();
                handleSendMessage();
            }
        });

        // Verify that all FXML fields are injected
        System.out.println("=== FXML Injection Check ===");
        System.out.println("conversationTitle injected: " + (conversationTitle != null));
        System.out.println("chatTabPane injected: " + (chatTabPane != null));
        System.out.println("messagesTab injected: " + (messagesTab != null));
        System.out.println("conversationsList injected: " + (conversationsList != null));
        System.out.println("chatArea injected: " + (chatArea != null));
        System.out.println("messageInput injected: " + (messageInput != null));
        System.out.println("sendMessageBtn injected: " + (sendMessageBtn != null));
        System.out.println("unreadCountLabel injected: " + (unreadCountLabel != null));
        System.out.println("chatScrollPane injected: " + (chatScrollPane != null));
        System.out.println("searchField injected: " + (searchField != null));
        System.out.println("newChatBtn injected: " + (newChatBtn != null));
        System.out.println("backToConversationsBtn injected: " + (backToConversationsBtn != null));
        System.out.println("closeButton injected: " + (closeButton != null));
    }

    public void setUserData(Person user, Popup parentPopup) {
        this.currentUser = user;
        this.popup = parentPopup;
        this.userRole = user.getRole() != null ? user.getRole().toLowerCase().trim() : "user";

        System.out.println("User role detected: " + userRole);

        configureUIBasedOnRole();
        loadConversations();
        startRefreshTimer();
        updateUnreadCount();
    }

    private void configureUIBasedOnRole() {
        if (isAdmin()) {
            newChatBtn.setVisible(true);
            newChatBtn.setManaged(true);
            newChatBtn.setText("+ New Support Chat");
            System.out.println("Admin mode: can see all conversations");
        } else {
            newChatBtn.setVisible(true);
            newChatBtn.setManaged(true);
            newChatBtn.setText("+ Contact Support");
            System.out.println("User/Guider mode: can only contact support");
        }
    }

    private boolean isAdmin() {
        return userRole != null && userRole.contains("admin");
    }

    private void filterConversations(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            conversationsList.setItems(conversations);
        } else {
            List<String> filtered = conversations.stream()
                    .filter(conv -> conv.toLowerCase().contains(searchText.toLowerCase()))
                    .collect(Collectors.toList());
            conversationsList.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    private void loadConversations() {
        if (currentUser == null) return;

        try {
            List<String> userConversations;
            if (isAdmin()) {
                userConversations = messageService.getAllAdminConversations();
                System.out.println("Loading all conversations for admin: " + userConversations.size());
            } else {
                userConversations = messageService.getUserConversations(currentUser.getId());
                System.out.println("Loading personal conversations for user/guider: " + userConversations.size());
            }

            if (!conversations.equals(userConversations)) {
                conversations.setAll(userConversations);
            }
        } catch (SQLException e) {
            System.err.println("Error loading conversations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HBox createConversationCell(String conversationId) {
        HBox cell = new HBox(10);
        cell.setPadding(new Insets(10));
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
        cell.setPrefHeight(60);

        try {
            List<Message> messages = messageService.getConversationMessages(conversationId);
            if (messages.isEmpty()) return cell;

            Message lastMessage = messages.get(messages.size() - 1);
            String title = getConversationTitle(conversationId, messages);

            // Avatar
            StackPane avatarStack = new StackPane();
            Circle avatarCircle = new Circle(18);

            if (title.contains("Support") || title.contains("GUIDER") || title.contains("USER")) {
                avatarCircle.setFill(Color.web("#FEC74C")); // Yellow for support chats
            } else {
                avatarCircle.setFill(Color.web("#0FA5A2")); // Teal for regular chats
            }
            avatarCircle.setOpacity(0.2);

            Label avatarLabel = new Label(title.substring(0, 1).toUpperCase());
            avatarLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");
            avatarStack.getChildren().addAll(avatarCircle, avatarLabel);

            // Conversation info
            VBox infoBox = new VBox(3);
            infoBox.setMaxWidth(150);

            Label nameLabel = new Label(title);
            nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1D4D7C;");

            String preview = lastMessage.getMessage();
            if (preview.length() > 20) preview = preview.substring(0, 17) + "...";

            if (isAdmin() && lastMessage.getSenderId() != currentUser.getId() && lastMessage.getSenderId() != 0) {
                try {
                    String senderRole = messageService.getUserRole(lastMessage.getSenderId());
                    String senderName = messageService.getUsername(lastMessage.getSenderId());
                    preview = senderName + " (" + senderRole + "): " + preview;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            Label previewLabel = new Label(preview);
            previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

            infoBox.getChildren().addAll(nameLabel, previewLabel);

            // Timestamp and unread
            VBox rightBox = new VBox(3);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            Label timeLabel = new Label(formatTime(lastMessage.getTimestamp()));
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

            boolean hasUnread = messages.stream()
                    .anyMatch(m -> !m.isRead() && m.getReceiverId() != null
                            && m.getReceiverId() == currentUser.getId());

            if (hasUnread) {
                Circle unreadDot = new Circle(4, Color.web("#0FA5A2"));
                rightBox.getChildren().add(unreadDot);
            }
            rightBox.getChildren().add(timeLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            cell.getChildren().addAll(avatarStack, infoBox, spacer, rightBox);

            cell.setOnMouseClicked(e -> {
                if (conversationId != null) {
                    loadConversation(conversationId);
                }
            });

            // Hover effect
            cell.setOnMouseEntered(e ->
                    cell.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 8; -fx-cursor: hand;"));
            cell.setOnMouseExited(e ->
                    cell.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;"));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cell;
    }

    private void loadConversation(String conversationId) {
        if (conversationId == null) {
            System.err.println("Cannot load conversation: conversationId is null");
            return;
        }

        // Check if conversationTitle is null (should not happen if FXML is correct)
        if (conversationTitle == null) {
            System.err.println("FATAL: conversationTitle is null! Check FXML injection.");
            return;
        }

        this.currentConversationId = conversationId;

        try {
            List<Message> messages = messageService.getConversationMessages(conversationId);
            String title = getConversationTitle(conversationId, messages);
            conversationTitle.setText(title);

            if (messageInput != null && sendMessageBtn != null) {
                messageInput.setDisable(false);
                sendMessageBtn.setDisable(false);
                messageInput.setPromptText("Type your message...");
            }

            messageService.markConversationAsRead(conversationId, currentUser.getId());

            displayMessages(messages);

            if (chatTabPane != null && messagesTab != null) {
                chatTabPane.getSelectionModel().select(messagesTab);
            }

            updateUnreadCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayMessages(List<Message> messages) {
        if (chatArea == null) return;

        chatArea.getChildren().clear();

        for (Message msg : messages) {
            HBox messageRow = new HBox(10);
            messageRow.setPadding(new Insets(5));
            messageRow.setAlignment(msg.getSenderId() == currentUser.getId()
                    ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox messageBubble = new VBox(3);
            messageBubble.setPadding(new Insets(8));
            messageBubble.setMaxWidth(220);

            String bubbleStyle;
            if (msg.getSenderId() == 0) {
                bubbleStyle = "-fx-background-color: #ff5e62; -fx-background-radius: 15; -fx-opacity: 0.8;";
            } else if (msg.getSenderId() == currentUser.getId()) {
                bubbleStyle = "-fx-background-color: #0FA5A2; -fx-background-radius: 15 15 5 15;";
            } else {
                bubbleStyle = "-fx-background-color: #e9ecef; -fx-background-radius: 15 15 15 5;";
            }
            messageBubble.setStyle(bubbleStyle);

            if (isAdmin() && msg.getSenderId() != currentUser.getId() && msg.getSenderId() != 0) {
                try {
                    String senderRole = messageService.getUserRole(msg.getSenderId());
                    String senderName = messageService.getUsername(msg.getSenderId());
                    Label senderLabel = new Label(senderName + " (" + senderRole + ")");
                    senderLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #666;");
                    messageBubble.getChildren().add(senderLabel);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            Label messageLabel = new Label(msg.getMessage());
            messageLabel.setWrapText(true);

            String textColor;
            if (msg.getSenderId() == 0) {
                textColor = "white";
            } else if (msg.getSenderId() == currentUser.getId()) {
                textColor = "white";
            } else {
                textColor = "#333";
            }
            messageLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 12px;");

            Label timeLabel = new Label(formatTime(msg.getTimestamp()));
            timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: " +
                    (msg.getSenderId() == currentUser.getId() ? "#e0e0e0" : "#999") + ";");
            timeLabel.setAlignment(Pos.CENTER_RIGHT);

            messageBubble.getChildren().addAll(messageLabel, timeLabel);
            messageRow.getChildren().add(messageBubble);

            chatArea.getChildren().add(messageRow);
        }

        javafx.application.Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        if (currentUser == null) {
            showAlert("Error", "You must be logged in to send messages.");
            return;
        }

        if (currentConversationId == null) {
            showAlert("Warning", "Please select a conversation first.");
            return;
        }

        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty()) {
            return;
        }

        try {
            Message message = new Message();
            message.setSenderId(currentUser.getId());
            message.setMessage(messageText);
            message.setTimestamp(LocalDateTime.now());
            message.setRead(false);
            message.setConversationId(currentConversationId);

            if (isAdmin()) {
                Integer recipientId = findConversationStarter(currentConversationId);
                message.setReceiverId(recipientId);
                System.out.println("📤 Admin sending to user: " + recipientId);
            } else {
                message.setReceiverId(null); // Send to all admins
                System.out.println("📤 User sending to admins");
            }

            messageService.sendMessage(message);
            System.out.println("✅ Message sent successfully");

            messageInput.clear();
            loadConversation(currentConversationId);
            loadConversations();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to send message: " + e.getMessage());
        }
    }

    private Integer findConversationStarter(String conversationId) throws SQLException {
        List<Message> messages = messageService.getConversationMessages(conversationId);
        for (Message msg : messages) {
            if (msg.getSenderId() != 0) {
                String role = messageService.getUserRole(msg.getSenderId());
                if (!role.toLowerCase().contains("admin")) {
                    return msg.getSenderId();
                }
            }
        }
        return null;
    }

    @FXML
    private void startNewConversation() {
        if (currentUser == null) return;

        String newConversationId;

        if (isAdmin()) {
            newConversationId = "chat_" + System.currentTimeMillis();
            System.out.println("Admin starting new conversation: " + newConversationId);
        } else {
            newConversationId = "support_" + currentUser.getId() + "_" + System.currentTimeMillis();
            System.out.println("User starting support conversation: " + newConversationId);
        }

        try {
            conversations.add(0, newConversationId);
            loadConversation(newConversationId);
            messageInput.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to start new conversation: " + e.getMessage());
        }
    }

    private String getConversationTitle(String conversationId, List<Message> messages) {
        if (messages.stream().anyMatch(m -> m.getReceiverId() == null)) {
            for (Message msg : messages) {
                try {
                    String role = messageService.getUserRole(msg.getSenderId());
                    if (!role.toLowerCase().contains("admin") && msg.getSenderId() != 0) {
                        String username = messageService.getUsername(msg.getSenderId());
                        return role.toUpperCase() + ": " + username;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return "Support Chat";
        }

        for (Message msg : messages) {
            if (msg.getSenderId() != currentUser.getId() && msg.getSenderId() != 0) {
                try {
                    String username = messageService.getUsername(msg.getSenderId());
                    return username;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "Unknown";
    }

    @FXML
    private void backToConversations() {
        if (chatTabPane != null) {
            chatTabPane.getSelectionModel().select(0);
        }
        currentConversationId = null;
        loadConversations();
    }

    @FXML
    private void handleClose() {
        cleanup();
        if (popup != null) {
            popup.hide();
        }
    }

    public void cleanup() {
        System.out.println("Cleaning up ChatPopupController");
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }

    private void updateUnreadCount() {
        try {
            List<Message> unread = messageService.getUnreadMessagesForUser(currentUser.getId());
            int count = unread.size();

            if (count > 0 && unreadCountLabel != null) {
                unreadCountLabel.setText(String.valueOf(count));
                unreadCountLabel.setVisible(true);
            } else if (unreadCountLabel != null) {
                unreadCountLabel.setVisible(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startRefreshTimer() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (currentUser != null) {
                if (currentConversationId != null) {
                    try {
                        List<Message> messages = messageService.getConversationMessages(currentConversationId);
                        displayMessages(messages);
                    } catch (SQLException ex) {
                        System.err.println("Error refreshing messages: " + ex.getMessage());
                    }
                }
                loadConversations();
                updateUnreadCount();
            }
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private String formatTime(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}