package services;

import entities.Client;
import entities.Like;
import entities.Publication;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LikeService implements CRUD<Like> {

    private Connection cnx;

    public LikeService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(Like like) throws SQLException {
        if (like == null) {
            throw new IllegalArgumentException("Like cannot be null");
        }

        String sql = "INSERT INTO `like`(publicationID, user_id) VALUES(?, ?)";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, like.getPublicationID());
            ps.setInt(2, like.getClient().getClientID());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating like failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    like.setLikeID(rs.getInt(1));
                } else {
                    throw new SQLException("Creating like failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void updateOne(Like like) throws SQLException {
        // Likes don't typically need updating
        throw new UnsupportedOperationException("Likes cannot be updated");
    }

    @Override
    public void deleteOne(Like like) throws SQLException {
        if (like == null) {
            throw new IllegalArgumentException("Like cannot be null");
        }

        if (like.getLikeID() <= 0) {
            throw new IllegalArgumentException("Invalid like ID");
        }

        String sql = "DELETE FROM `like` WHERE likeID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, like.getLikeID());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Deleting like failed, no rows affected. Like ID: " + like.getLikeID());
            }
        }
    }

    @Override
    public List<Like> selectALL() throws SQLException {
        List<Like> list = new ArrayList<>();

        String sql = "SELECT * FROM `like`";

        try (Statement st = MyDBConnexion.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Client client = new Client();
                client.setClientID(rs.getInt("user_id"));

                Publication publication = new Publication();
                publication.setPublicationID(rs.getInt("publicationID"));

                Like like = new Like(
                        client,
                        publication,
                        rs.getInt("likeID"),
                        rs.getInt("publicationID"),
                        rs.getInt("commentID")
                );

                list.add(like);
            }
        }

        return list;
    }

    /**
     * Get all likes for a specific publication
     */
    public List<Like> getLikesByPublication(int publicationID) throws SQLException {
        List<Like> list = new ArrayList<>();

        String sql = "SELECT * FROM `like` WHERE publicationID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Client client = new Client();
                    client.setClientID(rs.getInt("user_id"));

                    Publication publication = new Publication();
                    publication.setPublicationID(rs.getInt("publicationID"));

                    Like like = new Like(
                            client,
                            publication,
                            rs.getInt("likeID"),
                            rs.getInt("publicationID"),
                            rs.getInt("commentID")
                    );

                    list.add(like);
                }
            }
        }

        return list;
    }

    /**
     * Get like count for a publication
     */
    public int getLikeCount(int publicationID) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM `like` WHERE publicationID=?";

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
     * Check if a user has liked a publication
     */
    public boolean hasUserLiked(int publicationID, int clientID) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM `like` WHERE publicationID=? AND user_id=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationID);
            ps.setInt(2, clientID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }

        return false;
    }

    /**
     * Get the like object for a specific user and publication
     */
    public Like getUserLike(int publicationID, int clientID) throws SQLException {
        String sql = "SELECT * FROM `like` WHERE publicationID=? AND user_id=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationID);
            ps.setInt(2, clientID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Client client = new Client();
                    client.setClientID(rs.getInt("user_id"));

                    Publication publication = new Publication();
                    publication.setPublicationID(rs.getInt("publicationID"));

                    return new Like(
                            client,
                            publication,
                            rs.getInt("likeID"),
                            rs.getInt("publicationID"),
                            rs.getInt("commentID")
                    );
                }
            }
        }

        return null;
    }

    /**
     * Toggle like - if liked, unlike; if not liked, like
     */
    public boolean toggleLike(int publicationID, int clientID) throws SQLException {
        if (hasUserLiked(publicationID, clientID)) {
            // Unlike
            Like like = getUserLike(publicationID, clientID);
            if (like != null) {
                deleteOne(like);
                return false; // Now unliked
            }
        } else {
            // Like
            Client client = new Client();
            client.setClientID(clientID);

            Publication publication = new Publication();
            publication.setPublicationID(publicationID);

            Like like = new Like(client, publication, 0, publicationID, 0);
            insertOne(like);
            return true; // Now liked
        }
        return false;
    }

    /**
     * Get all likes by a specific user
     */
    public List<Like> getLikesByUser(int clientID) throws SQLException {
        List<Like> list = new ArrayList<>();

        String sql = "SELECT * FROM `like` WHERE user_id=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, clientID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Client client = new Client();
                    client.setClientID(rs.getInt("user_id"));

                    Publication publication = new Publication();
                    publication.setPublicationID(rs.getInt("publicationID"));

                    Like like = new Like(
                            client,
                            publication,
                            rs.getInt("likeID"),
                            rs.getInt("publicationID"),
                            rs.getInt("commentID")
                    );

                    list.add(like);
                }
            }
        }

        return list;
    }

    /**
     * Delete all likes for a publication
     */
    public void deleteLikesByPublication(int publicationID) throws SQLException {
        String sql = "DELETE FROM `like` WHERE publicationID=?";

        try (PreparedStatement ps = MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationID);
            ps.executeUpdate();
        }
    }
}
