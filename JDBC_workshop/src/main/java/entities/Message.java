package entities;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message — a chat message between two {@link Person} users.
 *
 * <p>{@code senderId} and {@code receiverId} are foreign keys to {@link Person#getId()}.
 * {@code receiverId} may be {@code null} if the message is broadcast to all admins.</p>
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private int            id;
    /** FK → {@link Person#getId()} */
    private int            senderId;
    /** FK → {@link Person#getId()}, or {@code null} for broadcast to all admins. */
    private Integer        receiverId;
    private String         message;
    private LocalDateTime  timestamp;
    private boolean        isRead;
    private String         conversationId;

    public Message() {}

    public Message(int id, int senderId, Integer receiverId, String message,
                   LocalDateTime timestamp, boolean isRead, String conversationId) {
        this.id             = id;
        this.senderId       = senderId;
        this.receiverId     = receiverId;
        this.message        = message;
        this.timestamp      = timestamp;
        this.isRead         = isRead;
        this.conversationId = conversationId;
    }

    // Getters and Setters
    public int            getId()              { return id; }
    public void           setId(int id)        { this.id = id; }

    public int            getSenderId()                  { return senderId; }
    public void           setSenderId(int senderId)      { this.senderId = senderId; }

    public Integer        getReceiverId()                       { return receiverId; }
    public void           setReceiverId(Integer receiverId)     { this.receiverId = receiverId; }

    public String         getMessage()                   { return message; }
    public void           setMessage(String message)     { this.message = message; }

    public LocalDateTime  getTimestamp()                         { return timestamp; }
    public void           setTimestamp(LocalDateTime timestamp)  { this.timestamp = timestamp; }

    public boolean        isRead()                    { return isRead; }
    public void           setRead(boolean read)       { isRead = read; }

    public String         getConversationId()                        { return conversationId; }
    public void           setConversationId(String conversationId)   { this.conversationId = conversationId; }
}