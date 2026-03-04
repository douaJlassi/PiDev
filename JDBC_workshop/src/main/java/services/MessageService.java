package services;

import entities.Message;
import utils.MyDBConnexion;

import java.sql.*;
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
        String req = "INSERT INTO messages (sender_id, receiver_id, message, timestamp, is_read, conversation_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
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

            int rowsAffected = ps.executeUpdate();
            System.out.println("✅ Message inserted, rows affected: " + rowsAffected);
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("❌ Foreign key constraint violation: " + e.getMessage());
            throw new SQLException("Cannot send message: The sender user does not exist in the database.", e);
        }
    }

    /**
     * Get all conversations for a user
     */
    public List<String> getUserConversations(int userId) throws SQLException {
        List<String> conversations = new ArrayList<>();
        String req = "SELECT DISTINCT conversation_id FROM messages " +
                "WHERE sender_id = ? OR receiver_id = ? " +
                "GROUP BY conversation_id " +
                "ORDER BY MAX(timestamp) DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String convId = rs.getString("conversation_id");
                conversations.add(convId);
            }
        }
        return conversations;
    }

    /**
     * Get all conversations for admin
     */
    public List<String> getAllAdminConversations() throws SQLException {
        List<String> conversations = new ArrayList<>();
        String req = "SELECT DISTINCT conversation_id FROM messages " +
                "GROUP BY conversation_id " +
                "ORDER BY MAX(timestamp) DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                conversations.add(rs.getString("conversation_id"));
            }
        }
        return conversations;
    }

    /**
     * Get messages for a specific conversation
     */
    public List<Message> getConversationMessages(String conversationId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String req = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY timestamp ASC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, conversationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getInt("id"));
                message.setSenderId(rs.getInt("sender_id"));
                message.setReceiverId(rs.getInt("receiver_id"));
                if (rs.wasNull()) message.setReceiverId(null);
                message.setMessage(rs.getString("message"));
                message.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                message.setRead(rs.getBoolean("is_read"));
                message.setConversationId(rs.getString("conversation_id"));
                messages.add(message);
            }
        }
        return messages;
    }

    /**
     * Mark conversation as read for a user
     */
    public void markConversationAsRead(String conversationId, int userId) throws SQLException {
        String req = "UPDATE messages SET is_read = 1 WHERE conversation_id = ? AND receiver_id = ? AND is_read = 0";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, conversationId);
            ps.setInt(2, userId);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Marked " + rowsAffected + " messages as read");
            }
        }
    }

    /**
     * Get unread messages for a user
     */
    public List<Message> getUnreadMessagesForUser(int userId) throws SQLException {
        List<Message> unreadMessages = new ArrayList<>();
        String req = "SELECT * FROM messages WHERE receiver_id = ? AND is_read = 0 ORDER BY timestamp DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getInt("id"));
                message.setSenderId(rs.getInt("sender_id"));
                message.setReceiverId(rs.getInt("receiver_id"));
                message.setMessage(rs.getString("message"));
                message.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                message.setRead(rs.getBoolean("is_read"));
                message.setConversationId(rs.getString("conversation_id"));
                unreadMessages.add(message);
            }
        }
        return unreadMessages;
    }

    /**
     * Get user role by ID
     */
    public String getUserRole(int userId) throws SQLException {
        String req = "SELECT role FROM user WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        }
        return "USER";
    }

    /**
     * Get username by ID
     */
    public String getUsername(int userId) throws SQLException {
        String req = "SELECT username FROM user WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        }
        return "Unknown";
    }
}