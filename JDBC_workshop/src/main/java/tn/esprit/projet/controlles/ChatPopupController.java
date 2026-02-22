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
import javafx.stage.Stage;
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
    private Person selectedContact;
    private String currentConversationId;
    private MessageService messageService;
    private PersonService personService;
    private Timeline refreshTimeline;
    private ObservableList<String> conversations;
    private Popup popup;
    private String userRole; // "admin", "guider", or "user"

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
    }

    public void setUserData(Person user, Popup parentPopup) {
        this.currentUser = user;
        this.popup = parentPopup;
        this.userRole = user.getRole() != null ? user.getRole().toLowerCase().trim() : "user";

        System.out.println("User role detected: " + userRole);

        // Configure UI based on role
        configureUIBasedOnRole();

        loadConversations();
        startRefreshTimer();
        updateUnreadCount();
    }

    private void configureUIBasedOnRole() {
        if (isAdmin()) {
            // Admins can see all conversations and start new ones
            newChatBtn.setVisible(true);
            newChatBtn.setManaged(true);
            newChatBtn.setText("+ New Support Chat");
            System.out.println("Admin mode: can see all conversations");
        } else {
            // Regular users and guiders can only start support chats
            newChatBtn.setVisible(true);
            newChatBtn.setManaged(true);
            newChatBtn.setText("+ Contact Support");
            System.out.println("User/Guider mode: can only contact support");
        }
    }

    private boolean isAdmin() {
        return userRole.contains("admin");
    }

    private boolean isGuider() {
        return userRole.contains("guider");
    }

    private boolean isRegularUser() {
        return !isAdmin() && !isGuider();
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
                // Admin sees ALL conversations (between any users and admins)
                userConversations = messageService.getAllConversations();
                System.out.println("Loading all conversations for admin");
            } else {
                // Regular users and guiders see only conversations where they are participants
                userConversations = messageService.getUserConversations(currentUser.getId());
                System.out.println("Loading personal conversations for user/guider");
            }
            conversations.setAll(userConversations);
        } catch (SQLException e) {
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

            // Determine the other participant (for display purposes)
            String title = getConversationTitle(conversationId, messages);

            // Determine if this conversation involves the current user
            boolean isUserParticipant = messages.stream()
                    .anyMatch(m -> m.getSenderId() == currentUser.getId() ||
                            (m.getReceiverId() != null && m.getReceiverId() == currentUser.getId()));

            // Avatar
            StackPane avatarStack = new StackPane();
            Circle avatarCircle = new Circle(18);

            // Different colors for different conversation types
            if (title.contains("Support")) {
                avatarCircle.setFill(Color.web("#FEC74C")); // Support chats in yellow
            } else {
                avatarCircle.setFill(Color.web("#0FA5A2")); // Regular chats in teal
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

            // Add sender indicator for admins
            if (isAdmin() && lastMessage.getSenderId() != currentUser.getId()) {
                try {
                    Person sender = personService.getUserById(lastMessage.getSenderId());
                    if (sender != null) {
                        preview = sender.getUsername() + ": " + preview;
                    }
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

            // Check for unread messages (only for messages where current user is receiver)
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

            cell.setOnMouseClicked(e -> loadConversation(conversationId));

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

    private String getConversationTitle(String conversationId, List<Message> messages) {
        // For support chats (receiver_id is null)
        if (messages.stream().anyMatch(m -> m.getReceiverId() == null)) {
            // Find the user who started the support chat (not an admin)
            for (Message msg : messages) {
                try {
                    Person sender = personService.getUserById(msg.getSenderId());
                    if (sender != null && !sender.getRole().toLowerCase().contains("admin")) {
                        return "Support - " + sender.getUsername();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return "Support Chat";
        }

        // For direct chats between users
        for (Message msg : messages) {
            if (msg.getSenderId() != currentUser.getId() && msg.getReceiverId() != null) {
                try {
                    Person otherUser = personService.getUserById(msg.getSenderId());
                    if (otherUser != null) {
                        return otherUser.getUsername();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else if (msg.getReceiverId() != null && msg.getReceiverId() != currentUser.getId()) {
                try {
                    Person otherUser = personService.getUserById(msg.getReceiverId());
                    if (otherUser != null) {
                        return otherUser.getUsername();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "Unknown";
    }

    private void loadConversation(String conversationId) {
        this.currentConversationId = conversationId;

        try {
            // Load messages to determine participants
            List<Message> messages = messageService.getConversationMessages(conversationId);

            // Determine the other participant for display
            String title = getConversationTitle(conversationId, messages);
            conversationTitle.setText(title);

            // ALWAYS enable message input for normal users in their own conversations
            // They should be able to send messages to support
            messageInput.setDisable(false);
            sendMessageBtn.setDisable(false);
            messageInput.setPromptText("Type your message...");

            // Mark messages as read
            messageService.markConversationAsRead(conversationId, currentUser.getId());

            displayMessages(messages);

            // Switch to messages tab
            chatTabPane.getSelectionModel().select(messagesTab);

            updateUnreadCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayMessages(List<Message> messages) {
        chatArea.getChildren().clear();

        for (Message msg : messages) {
            HBox messageRow = new HBox(10);
            messageRow.setPadding(new Insets(5));
            messageRow.setAlignment(msg.getSenderId() == currentUser.getId()
                    ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox messageBubble = new VBox(3);
            messageBubble.setPadding(new Insets(8));
            messageBubble.setMaxWidth(220);

            // Different colors for different message types
            String bubbleStyle;
            if (msg.getSenderId() == currentUser.getId()) {
                // Current user's messages
                bubbleStyle = "-fx-background-color: #0FA5A2; -fx-background-radius: 15 15 5 15;";
            } else if (msg.getReceiverId() == null) {
                // Support message to all admins
                bubbleStyle = "-fx-background-color: #FEC74C; -fx-background-radius: 15 15 15 5;";
            } else {
                // Regular message from other user
                bubbleStyle = "-fx-background-color: #e9ecef; -fx-background-radius: 15 15 15 5;";
            }
            messageBubble.setStyle(bubbleStyle);

            // Add sender name for admins in group chats
            if (isAdmin() && msg.getSenderId() != currentUser.getId()) {
                try {
                    Person sender = personService.getUserById(msg.getSenderId());
                    if (sender != null) {
                        Label senderLabel = new Label(sender.getUsername());
                        senderLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #666;");
                        messageBubble.getChildren().add(senderLabel);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            Label messageLabel = new Label(msg.getMessage());
            messageLabel.setWrapText(true);

            String textColor;
            if (msg.getSenderId() == currentUser.getId()) {
                textColor = "white";
            } else if (msg.getReceiverId() == null) {
                textColor = "#1D4D7C"; // Dark blue for support messages
            } else {
                textColor = "#333";
            }
            messageLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 12px;");

            Label timeLabel = new Label(formatTime(msg.getTimestamp()));
            timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #999;");
            timeLabel.setAlignment(Pos.CENTER_RIGHT);

            messageBubble.getChildren().addAll(messageLabel, timeLabel);
            messageRow.getChildren().add(messageBubble);

            chatArea.getChildren().add(messageRow);
        }

        // Auto-scroll to bottom
        javafx.application.Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        if (currentUser == null || currentConversationId == null) return;

        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty()) return;

        try {
            Message message = new Message();
            message.setSenderId(currentUser.getId());

            // Get conversation messages to determine type
            List<Message> messages = messageService.getConversationMessages(currentConversationId);

            // Check if this is a support chat
            boolean isSupportChat = messages.isEmpty() || messages.stream().anyMatch(m -> m.getReceiverId() == null);

            if (isSupportChat) {
                // Support chat - send to all admins (receiver_id = null)
                message.setReceiverId(null);
                System.out.println("Sending support message to all admins");
            } else {
                // Direct chat - find the other participant
                Integer otherParticipantId = findOtherParticipant(messages);
                message.setReceiverId(otherParticipantId);
                System.out.println("Sending direct message to user ID: " + otherParticipantId);
            }

            message.setMessage(messageText);
            message.setTimestamp(LocalDateTime.now());
            message.setRead(false);
            message.setConversationId(currentConversationId);

            messageService.sendMessage(message);
            messageInput.clear();

            // Refresh conversation
            loadConversation(currentConversationId);

            // Refresh conversations list
            loadConversations();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Integer findOtherParticipant(List<Message> messages) {
        for (Message msg : messages) {
            if (msg.getSenderId() != currentUser.getId()) {
                return msg.getSenderId();
            }
            if (msg.getReceiverId() != null && msg.getReceiverId() != currentUser.getId()) {
                return msg.getReceiverId();
            }
        }
        return null;
    }

    @FXML
    private void startNewConversation() {
        if (currentUser == null) return;

        String newConversationId;

        if (isAdmin()) {
            // For admins, you might want to show a user selection dialog
            // For now, create a support chat
            newConversationId = "support_" + System.currentTimeMillis();
        } else {
            // Regular users and guiders start support chats with their ID
            newConversationId = "support_" + currentUser.getId() + "_" + System.currentTimeMillis();
        }

        try {
            conversations.add(0, newConversationId);
            loadConversation(newConversationId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void backToConversations() {
        chatTabPane.getSelectionModel().select(0);
        loadConversations();
    }

    @FXML
    private void handleClose() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        popup.hide();
    }

    private void updateUnreadCount() {
        try {
            List<Message> unread = messageService.getUnreadMessagesForUser(currentUser.getId());
            int count = unread.size();

            if (count > 0) {
                unreadCountLabel.setText(String.valueOf(count));
                unreadCountLabel.setVisible(true);
            } else {
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
                        ex.printStackTrace();
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
}