package services;

import entities.Comment;
import entities.Client;
import entities.Publication;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentService implements CRUD<Comment> {

    private Connection cnx;

    public CommentService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(Comment comment) throws SQLException {
        if (comment == null) {
            throw new IllegalArgumentException("Comment cannot be null");
        }

        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }

        String sql = "INSERT INTO comment(publicationID, user_id, content, commentDate) VALUES(?, ?, ?, ?)";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, comment.getPublicationID());
            ps.setInt(2, comment.getClient().getClientID());
            ps.setString(3, comment.getContent());
            ps.setTimestamp(4, new Timestamp(comment.getCommentDate().getTime()));

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating comment failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    comment.setCommentID(rs.getInt(1));
                } else {
                    throw new SQLException("Creating comment failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void updateOne(Comment comment) throws SQLException {
        if (comment == null) {
            throw new IllegalArgumentException("Comment cannot be null");
        }

        if (comment.getCommentID() <= 0) {
            throw new IllegalArgumentException("Invalid comment ID");
        }

        String sql = "UPDATE comment SET content=? WHERE commentID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, comment.getContent());
            ps.setInt(2, comment.getCommentID());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Updating comment failed, no rows affected. Comment ID: " + comment.getCommentID());
            }
        }
    }

    @Override
    public void deleteOne(Comment comment) throws SQLException {
        if (comment == null) {
            throw new IllegalArgumentException("Comment cannot be null");
        }

        if (comment.getCommentID() <= 0) {
            throw new IllegalArgumentException("Invalid comment ID");
        }

        String sql = "DELETE FROM comment WHERE commentID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, comment.getCommentID());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Deleting comment failed, no rows affected. Comment ID: " + comment.getCommentID());
            }
        }
    }

    @Override
    public List<Comment> selectALL() throws SQLException {
        List<Comment> list = new ArrayList<>();

        String sql = "SELECT cm.*, u.username " +
                "FROM comment cm " +
                "LEFT JOIN user u ON cm.user_id = u.id " +
                "ORDER BY cm.commentDate DESC";

        try (Statement st = MyDBConnexion.getInstance().getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    /**
     * Get all comments for a specific publication
     */
    public List<Comment> getCommentsByPublication(int publicationID) throws SQLException {
        List<Comment> list = new ArrayList<>();

        String sql = "SELECT cm.*, u.username " +
                "FROM comment cm " +
                "LEFT JOIN user u ON cm.user_id = u.id " +
                "WHERE cm.publicationID=? ORDER BY cm.commentDate ASC";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    /**
     * Get comment count for a publication
     */
    public int getCommentCount(int publicationID) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM comment WHERE publicationID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }

        return 0;
    }

    /**
     * Get a single comment by ID
     */
    public Comment getCommentById(int commentID) throws SQLException {
        String sql = "SELECT cm.*, u.username " +
                "FROM comment cm " +
                "LEFT JOIN user u ON cm.user_id = u.id " +
                "WHERE cm.commentID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, commentID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    /**
     * Get comments by a specific user
     */
    public List<Comment> getCommentsByUser(int clientID) throws SQLException {
        List<Comment> list = new ArrayList<>();

        String sql = "SELECT cm.*, u.username " +
                "FROM comment cm " +
                "LEFT JOIN user u ON cm.user_id = u.id " +
                "WHERE cm.user_id=? ORDER BY cm.commentDate DESC";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, clientID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    /**
     * Delete all comments for a publication
     */
    public void deleteCommentsByPublication(int publicationID) throws SQLException {
        String sql = "DELETE FROM comment WHERE publicationID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationID);
            ps.executeUpdate();
        }
    }

    /**
     * Get recent comments (last N comments)
     */
    public List<Comment> getRecentComments(int limit) throws SQLException {
        List<Comment> list = new ArrayList<>();

        String sql = "SELECT cm.*, u.username " +
                "FROM comment cm " +
                "LEFT JOIN user u ON cm.user_id = u.id " +
                "ORDER BY cm.commentDate DESC LIMIT ?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    /**
     * Shared row mapper for Comment queries that join the user table.
     */
    private Comment mapRow(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setClientID(rs.getInt("user_id"));
        try {
            client.setUsername(rs.getString("username"));
        } catch (SQLException ignored) {
            // username column not in every query
        }

        Publication publication = new Publication();
        publication.setPublicationID(rs.getInt("publicationID"));

        return new Comment(
                client,
                publication,
                rs.getInt("commentID"),
                rs.getInt("publicationID"),
                rs.getString("content"),
                new Date(rs.getTimestamp("commentDate").getTime()));
    }
}
