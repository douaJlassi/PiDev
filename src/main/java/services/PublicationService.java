package services;

import entities.Client;
import entities.Publication;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PublicationService implements CRUD<Publication> {

    private Connection cnx;

    public PublicationService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(Publication p) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Publication cannot be null");
        }

        if (p.getContent() == null || p.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Publication content cannot be empty");
        }

        String sql = "INSERT INTO publication(content, datePublication, client_id, image_path, place) VALUES(?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getContent());
            ps.setTimestamp(2, new Timestamp(p.getDatePublication().getTime()));
            ps.setInt(3, p.getClient().getClientID());
            ps.setString(4, p.getImagePath());
            ps.setString(5, p.getPlace());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating publication failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setPublicationID(rs.getInt(1));
                } else {
                    throw new SQLException("Creating publication failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void updateOne(Publication p) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Publication cannot be null");
        }

        if (p.getPublicationID() <= 0) {
            throw new IllegalArgumentException("Invalid publication ID");
        }

        String sql = "UPDATE publication SET content=?, image_path=?, place=? WHERE publicationID=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, p.getContent());
            ps.setString(2, p.getImagePath());
            ps.setString(3, p.getPlace());
            ps.setInt(4, p.getPublicationID());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating publication failed, no rows affected. Publication ID: " + p.getPublicationID());
            }
        }
    }

    @Override
    public void deleteOne(Publication p) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Publication cannot be null");
        }

        if (p.getPublicationID() <= 0) {
            throw new IllegalArgumentException("Invalid publication ID");
        }

        String sql = "DELETE FROM publication WHERE publicationID=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getPublicationID());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Deleting publication failed, no rows affected. Publication ID: " + p.getPublicationID());
            }
        }
    }

    @Override
    public List<Publication> selectALL() throws SQLException {
        List<Publication> list = new ArrayList<>();

        String sql = "SELECT p.*, c.username, c.avatarPath " +
                "FROM publication p " +
                "LEFT JOIN client c ON p.client_id = c.clientID " +
                "ORDER BY p.datePublication DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Client c = new Client();
                c.setClientID(rs.getInt("client_id"));
                c.setUsername(rs.getString("username"));
                c.setAvatarPath(rs.getString("avatarPath"));

                Publication p = new Publication(
                        c,
                        rs.getInt("publicationID"),
                        rs.getString("content"),
                        new Date(rs.getTimestamp("datePublication").getTime()),
                        rs.getString("image_path"),
                        rs.getString("place")
                );

                list.add(p);
            }
        }

        return list;
    }

    /**
     * Get a single publication by ID
     */
    public Publication selectById(int publicationID) throws SQLException {
        String sql = "SELECT * FROM publication WHERE publicationID=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, publicationID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Client c = new Client();
                    c.setClientID(rs.getInt("client_id"));

                    return new Publication(
                            c,
                            rs.getInt("publicationID"),
                            rs.getString("content"),
                            new Date(rs.getTimestamp("datePublication").getTime()),
                            rs.getString("image_path"),
                            rs.getString("place")
                    );
                }
            }
        }

        return null;
    }

    /**
     * Get all publications by a specific client
     */
    public List<Publication> selectByClient(int clientID) throws SQLException {
        List<Publication> list = new ArrayList<>();

        String sql = "SELECT * FROM publication WHERE client_id=? ORDER BY datePublication DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, clientID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Client c = new Client();
                    c.setClientID(rs.getInt("client_id"));

                    Publication p = new Publication(
                            c,
                            rs.getInt("publicationID"),
                            rs.getString("content"),
                            new Date(rs.getTimestamp("datePublication").getTime()),
                            rs.getString("image_path"),
                            rs.getString("place")
                    );

                    list.add(p);
                }
            }
        }

        return list;
    }

    /**
     * Get publications with images only
     */
    public List<Publication> selectWithImages() throws SQLException {
        List<Publication> list = new ArrayList<>();

        String sql = "SELECT * FROM publication WHERE image_path IS NOT NULL AND image_path != '' ORDER BY datePublication DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Client c = new Client();
                c.setClientID(rs.getInt("client_id"));

                Publication p = new Publication(
                        c,
                        rs.getInt("publicationID"),
                        rs.getString("content"),
                        new Date(rs.getTimestamp("datePublication").getTime()),
                        rs.getString("image_path"),
                        rs.getString("place")
                );

                list.add(p);
            }
        }

        return list;
    }

    /**
     * Search publications by content or place
     */
    public List<Publication> searchPublications(String keyword) throws SQLException {
        List<Publication> list = new ArrayList<>();

        String sql = "SELECT * FROM publication WHERE content LIKE ? OR place LIKE ? ORDER BY datePublication DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Client c = new Client();
                    c.setClientID(rs.getInt("client_id"));

                    Publication p = new Publication(
                            c,
                            rs.getInt("publicationID"),
                            rs.getString("content"),
                            new Date(rs.getTimestamp("datePublication").getTime()),
                            rs.getString("image_path"),
                            rs.getString("place")
                    );

                    list.add(p);
                }
            }
        }

        return list;
    }

    /**
     * Get the count of all publications
     */
    public int getPublicationCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM publication";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }

        return 0;
    }

    /**
     * Get recent publications (last N posts)
     */
    public List<Publication> getRecentPublications(int limit) throws SQLException {
        List<Publication> list = new ArrayList<>();

        String sql = "SELECT * FROM publication ORDER BY datePublication DESC LIMIT ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Client c = new Client();
                    c.setClientID(rs.getInt("client_id"));

                    Publication p = new Publication(
                            c,
                            rs.getInt("publicationID"),
                            rs.getString("content"),
                            new Date(rs.getTimestamp("datePublication").getTime()),
                            rs.getString("image_path"),
                            rs.getString("place")
                    );

                    list.add(p);
                }
            }
        }

        return list;
    }
}