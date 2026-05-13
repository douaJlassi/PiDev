package entities;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private int id;
    private int senderId;
    private Integer receiverId; // null if message to all admins
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;
    private String conversationId;

    public Message() {}

    public Message(int id, int senderId, Integer receiverId, String message,
                   LocalDateTime timestamp, boolean isRead, String conversationId) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.conversationId = conversationId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public Integer getReceiverId() { return receiverId; }
    public void setReceiverId(Integer receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}