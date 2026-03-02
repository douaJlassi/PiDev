package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.AiChatService;
import services.AiChatService.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * AiChatController — builds and manages the AI Travel Assistant chat panel entirely
 * in Java code (no FXML file). This eliminates all FXML controller-wiring risks.
 *
 * Entry point: call buildPanel() to get the VBox root node, then call close()
 * to trigger the close callback.
 *
 * Conversation flow:
 *   1. User types in inputField and presses Send (or Ctrl+Enter)
 *   2. User bubble appended; message added to history list
 *   3. AiChatService.chat() called on CompletableFuture background thread
 *   4. Typing indicator shown while waiting
 *   5. On reply: indicator removed; assistant bubble appended; history updated
 */
public class AiChatController {

    // ── Services & state ──────────────────────────────────────────────────────
    private final AiChatService  service  = new AiChatService();
    private final List<Message>  history  = new ArrayList<>();
    private       Runnable       onClose;
    private       boolean        waiting  = false;

    // ── Live node references (set during buildPanel) ──────────────────────────
    private VBox       messagesBox;
    private ScrollPane messagesScroll;
    private TextArea   inputField;
    private Button     sendBtn;
    private Label      statusLabel;
    private VBox       suggestionsBox;

    /** Provide the callback that fires when the user clicks ✕. */
    public void setOnClose(Runnable callback) { this.onClose = callback; }

    // ── Panel construction ────────────────────────────────────────────────────

    /**
     * Builds and returns the complete chat panel VBox.
     * Call once; the same node is reused for the panel's lifetime.
     */
    public VBox buildPanel() {
        VBox root = new VBox(0);
        root.getStyleClass().add("chat-panel-root");
        root.setPrefWidth(400);
        root.setMinWidth(400);
        root.setMaxWidth(400);

        root.getChildren().addAll(
                buildHeader(),
                buildMessagesArea(),
                buildSuggestions(),
                buildInputBar()
        );

        // Show welcome message
        appendAssistantBubble(
                "Marhba! 👋 I'm Rihla, your AI travel companion.\n\n" +
                        "Ask me anything — itineraries, packing lists, local food, " +
                        "hidden gems, or the best time to visit any destination. " +
                        "Where are we heading today?"
        );

        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("chat-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 16, 20));

        // AI avatar circle
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("chat-avatar");
        avatar.setMinSize(38, 38);
        avatar.setMaxSize(38, 38);
        Label avatarIcon = new Label("✦");
        avatarIcon.getStyleClass().add("chat-avatar-icon");
        avatar.getChildren().add(avatarIcon);

        // Name + status column
        VBox nameCol = new VBox(2);
        HBox.setHgrow(nameCol, Priority.ALWAYS);
        Label nameLabel = new Label("Rihla");
        nameLabel.getStyleClass().add("chat-title");
        statusLabel = new Label("AI Travel Assistant");
        statusLabel.getStyleClass().add("chat-subtitle");
        nameCol.getChildren().addAll(nameLabel, statusLabel);

        // Clear button
        Button clearBtn = new Button("⟳");
        clearBtn.getStyleClass().add("chat-icon-btn");
        clearBtn.setTooltip(new Tooltip("Clear conversation"));
        clearBtn.setOnAction(e -> clearConversation());

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("chat-close-btn");
        closeBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });

        header.getChildren().addAll(avatar, nameCol, clearBtn, closeBtn);
        return header;
    }

    // ── Messages scroll area ──────────────────────────────────────────────────

    private ScrollPane buildMessagesArea() {
        messagesBox = new VBox(12);
        messagesBox.getStyleClass().add("chat-messages-box");
        messagesBox.setPadding(new Insets(20, 16, 16, 16));

        messagesScroll = new ScrollPane(messagesBox);
        messagesScroll.getStyleClass().add("chat-scroll");
        messagesScroll.setFitToWidth(true);
        messagesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(messagesScroll, Priority.ALWAYS);

        return messagesScroll;
    }

    // ── Suggestion chips ──────────────────────────────────────────────────────

    private VBox buildSuggestions() {
        suggestionsBox = new VBox(8);
        suggestionsBox.getStyleClass().add("chat-suggestions-box");
        suggestionsBox.setPadding(new Insets(12, 16, 8, 16));

        Label tryLabel = new Label("Try asking:");
        tryLabel.getStyleClass().add("chat-suggestions-label");

        String[] chips = {
                "Plan 3 days in Djerba",
                "Best food in Tunis",
                "What to pack for Tunisia?",
                "Hidden gems in Sahara"
        };

        HBox row1 = new HBox(8);
        HBox row2 = new HBox(8);

        for (int i = 0; i < chips.length; i++) {
            Button chip = new Button(chips[i]);
            chip.getStyleClass().add("chat-suggestion-chip");
            final String prompt = chips[i];
            chip.setOnAction(e -> {
                hideSuggestions();
                sendMessage(prompt);
            });
            (i < 2 ? row1 : row2).getChildren().add(chip);
        }

        suggestionsBox.getChildren().addAll(tryLabel, row1, row2);
        return suggestionsBox;
    }

    // ── Input bar ─────────────────────────────────────────────────────────────

    private HBox buildInputBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("chat-input-bar");
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(12, 14, 14, 14));

        inputField = new TextArea();
        inputField.getStyleClass().add("chat-input-field");
        inputField.setPromptText("Ask Rihla anything about travel...");
        inputField.setWrapText(true);
        inputField.setPrefRowCount(1);
        inputField.setMaxHeight(80);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Ctrl+Enter to send (Enter alone adds newline — better for long questions)
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                onSend();
                e.consume();
            }
        });

        sendBtn = new Button("➤");
        sendBtn.getStyleClass().add("chat-send-btn");
        sendBtn.setOnAction(e -> onSend());

        bar.getChildren().addAll(inputField, sendBtn);
        return bar;
    }

    // ── Send flow ─────────────────────────────────────────────────────────────

    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || waiting) return;
        hideSuggestions();
        sendMessage(text);
    }

    private void sendMessage(String text) {
        inputField.clear();

        // Append user bubble and add to history
        appendUserBubble(text);
        history.add(new Message("user", text));

        // Lock UI and show typing indicator
        setWaiting(true);
        HBox typingRow = appendTypingIndicator();

        // Call AI on background thread
        service.chat(new ArrayList<>(history))
                .thenAccept(reply -> Platform.runLater(() -> {
                    messagesBox.getChildren().remove(typingRow);
                    history.add(new Message("assistant", reply));
                    appendAssistantBubble(reply);
                    setWaiting(false);
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> {
                        messagesBox.getChildren().remove(typingRow);
                        appendAssistantBubble("⚠ Something went wrong. Please try again.");
                        setWaiting(false);
                    });
                    return null;
                });
    }

    // ── Bubble builders ───────────────────────────────────────────────────────

    private void appendUserBubble(String text) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(300);
        msg.getStyleClass().add("chat-bubble-user");

        HBox row = new HBox(msg);
        row.setAlignment(Pos.CENTER_RIGHT);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void appendAssistantBubble(String text) {
        StackPane avatar = makeAvatarDot();

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(290);
        msg.getStyleClass().add("chat-bubble-assistant");

        HBox row = new HBox(10, avatar, msg);
        row.setAlignment(Pos.TOP_LEFT);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private HBox appendTypingIndicator() {
        StackPane avatar = makeAvatarDot();

        Label dots = new Label("Rihla is thinking ●");
        dots.getStyleClass().add("chat-bubble-typing");

        // Cycle dots animation
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(0),   e -> dots.setText("Rihla is thinking ●")),
                new KeyFrame(Duration.millis(500),  e -> dots.setText("Rihla is thinking ● ●")),
                new KeyFrame(Duration.millis(1000), e -> dots.setText("Rihla is thinking ● ● ●")),
                new KeyFrame(Duration.millis(1500), e -> dots.setText("Rihla is thinking ●"))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        HBox row = new HBox(10, avatar, dots);
        row.setAlignment(Pos.CENTER_LEFT);
        // Stop animation when the row is removed from the scene
        row.sceneProperty().addListener((obs, o, n) -> { if (n == null) timeline.stop(); });

        messagesBox.getChildren().add(row);
        scrollToBottom();
        return row;
    }

    private StackPane makeAvatarDot() {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("chat-bubble-avatar");
        avatar.setMinSize(28, 28);
        avatar.setMaxSize(28, 28);
        Label icon = new Label("✦");
        icon.getStyleClass().add("chat-bubble-avatar-icon");
        avatar.getChildren().add(icon);
        return avatar;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void scrollToBottom() {
        // Defer until layout pass so scroll position is accurate
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private void setWaiting(boolean isWaiting) {
        this.waiting = isWaiting;
        sendBtn.setDisable(isWaiting);
        inputField.setDisable(isWaiting);
        statusLabel.setText(isWaiting ? "● Thinking..." : "AI Travel Assistant");
    }

    private void hideSuggestions() {
        if (suggestionsBox != null) {
            suggestionsBox.setVisible(false);
            suggestionsBox.setManaged(false);
        }
    }

    private void clearConversation() {
        history.clear();
        messagesBox.getChildren().clear();
        if (suggestionsBox != null) {
            suggestionsBox.setVisible(true);
            suggestionsBox.setManaged(true);
        }
        setWaiting(false);
        appendAssistantBubble(
                "Marhba! 👋 I'm Rihla, your AI travel companion.\n\n" +
                        "Ask me anything — itineraries, packing lists, local food, " +
                        "hidden gems, or the best time to visit any destination. " +
                        "Where are we heading today?"
        );
    }
}