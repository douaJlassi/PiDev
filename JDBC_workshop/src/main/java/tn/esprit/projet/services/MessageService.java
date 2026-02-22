package tn.esprit.projet.services;

import tn.esprit.projet.entities.Message;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private Connection cnx;

    public MessageService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    /**
     * Send a new message
     */
    public void sendMessage(Message message) throws SQLException {
        String req = "INSERT INTO messages (sender_id, receiver_id, message, timestamp, is_read, conversation_id) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, message.getSenderId());
        if (message.getReceiverId() != null) {
            ps.setInt(2, message.getReceiverId());
        } else {
            ps.setNull(2, Types.INTEGER);
        }
        ps.setString(3, message.getMessage());
        ps.setTimestamp(4, Timestamp.valueOf(message.getTimestamp()));
        ps.setBoolean(5, message.isRead());
        ps.setString(6, message.getConversationId());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            message.setId(rs.getInt(1));
        }
    }

    /**
     * Get all messages in a conversation
     */
    public List<Message> getConversationMessages(String conversationId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String req = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY timestamp ASC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, conversationId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            messages.add(extractMessageFromResultSet(rs));
        }
        return messages;
    }

    /**
     * Get all conversations for a specific user
     */
    public List<String> getUserConversations(int userId) throws SQLException {
        List<String> conversations = new ArrayList<>();
        String req = "SELECT DISTINCT conversation_id FROM messages " +
                "WHERE sender_id = ? OR receiver_id = ? " +
                "ORDER BY MAX(timestamp) DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.setInt(2, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            conversations.add(rs.getString("conversation_id"));
        }
        return conversations;
    }

    /**
     * Get ALL conversations (for admin)
     */
    public List<String> getAllConversations() throws SQLException {
        List<String> conversations = new ArrayList<>();
        String req = "SELECT DISTINCT conversation_id FROM messages ORDER BY MAX(timestamp) DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            conversations.add(rs.getString("conversation_id"));
        }
        return conversations;
    }

    /**
     * Get unread messages for a user
     */
    public List<Message> getUnreadMessagesForUser(int userId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String req = "SELECT m.* FROM messages m " +
                "WHERE (m.receiver_id = ? OR (m.receiver_id IS NULL AND ? IN " +
                "(SELECT id FROM user WHERE LOWER(role) LIKE '%admin%'))) " +
                "AND m.is_read = false " +
                "AND m.sender_id != ? " +
                "ORDER BY m.timestamp DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.setInt(2, userId);
        ps.setInt(3, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            messages.add(extractMessageFromResultSet(rs));
        }
        return messages;
    }

    /**
     * Mark a single message as read
     */
    public void markAsRead(int messageId) throws SQLException {
        String req = "UPDATE messages SET is_read = true WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, messageId);
        ps.executeUpdate();
    }

    /**
     * Mark all messages in a conversation as read for a specific user
     */
    public void markConversationAsRead(String conversationId, int userId) throws SQLException {
        String req = "UPDATE messages SET is_read = true WHERE conversation_id = ? " +
                "AND (receiver_id = ? OR (receiver_id IS NULL AND ? IN " +
                "(SELECT id FROM user WHERE LOWER(role) LIKE '%admin%')))";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, conversationId);
        ps.setInt(2, userId);
        ps.setInt(3, userId);
        ps.executeUpdate();
    }

    /**
     * Get all support conversations (where receiver_id is null)
     */
    public List<String> getSupportConversations() throws SQLException {
        List<String> conversations = new ArrayList<>();
        String req = "SELECT DISTINCT conversation_id FROM messages WHERE receiver_id IS NULL ORDER BY MAX(timestamp) DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            conversations.add(rs.getString("conversation_id"));
        }
        return conversations;
    }

    /**
     * Get the original user of a support conversation
     */
    public Integer getSupportConversationUser(String conversationId) throws SQLException {
        String req = "SELECT sender_id FROM messages WHERE conversation_id = ? AND receiver_id IS NULL ORDER BY timestamp ASC LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, conversationId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("sender_id");
        }
        return null;
    }

    /**
     * Check if a user has an existing support conversation
     */
    public boolean hasExistingSupportConversation(int userId) throws SQLException {
        String req = "SELECT COUNT(*) FROM messages WHERE conversation_id LIKE 'support_%' AND sender_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    /**
     * Get the existing support conversation ID for a user
     */
    public String getExistingSupportConversationId(int userId) throws SQLException {
        String req = "SELECT DISTINCT conversation_id FROM messages WHERE conversation_id LIKE 'support_%' AND sender_id = ? ORDER BY timestamp DESC LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getString("conversation_id");
        }
        return null;
    }

    /**
     * Extract a Message object from ResultSet
     */
    private Message extractMessageFromResultSet(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setSenderId(rs.getInt("sender_id"));
        int receiverId = rs.getInt("receiver_id");
        message.setReceiverId(rs.wasNull() ? null : receiverId);
        message.setMessage(rs.getString("message"));
        message.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        message.setRead(rs.getBoolean("is_read"));
        message.setConversationId(rs.getString("conversation_id"));
        return message;
    }

    /**
     * Delete old messages (optional cleanup method)
     */
    public void deleteOldMessages(int daysOld) throws SQLException {
        String req = "DELETE FROM messages WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, daysOld);
        ps.executeUpdate();
    }
}