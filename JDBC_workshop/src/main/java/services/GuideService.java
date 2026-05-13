package services;

import entities.Person;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuideService {

    // Get a fresh connection each time
    private Connection getConnection() throws SQLException {
        return MyDBConnexion.getInstance().getCnx();
    }

    // Get all users with role 'guide'
    public List<Person> getAllGuides() throws SQLException {
        List<Person> guideList = new ArrayList<>();

        String req = "SELECT id, name, last_name, email, telephone, username, role, status, date " +
                "FROM `user` WHERE role = 'guide' OR role = 'admin'";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Person user = new Person(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        null, // password - don't retrieve for security
                        rs.getDate("date"),
                        rs.getString("role"),
                        rs.getString("username"),
                        rs.getString("telephone")
                );
                user.setStatus(rs.getString("status"));
                guideList.add(user);
            }
        }
        return guideList;
    }

    // Get available guides (you can define availability logic)
    public List<Person> getAvailableGuides() throws SQLException {
        List<Person> guideList = new ArrayList<>();

        // Example: Guides who have fewer than 5 active activities
        String req = "SELECT u.id, u.name, u.last_name, u.email, u.telephone, u.username, u.role, u.status, u.date, " +
                "COUNT(a.idActivite) as activityCount " +
                "FROM `user` u " +
                "LEFT JOIN `activite` a ON u.id = a.idGuide " +
                "WHERE u.role = 'guide' OR u.role = 'admin' " +
                "GROUP BY u.id, u.name, u.last_name, u.email, u.telephone, u.username, u.role, u.status, u.date " +
                "HAVING activityCount < 5";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Person user = new Person(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        null, // password
                        rs.getDate("date"),
                        rs.getString("role"),
                        rs.getString("username"),
                        rs.getString("telephone")
                );
                user.setStatus(rs.getString("status"));
                guideList.add(user);
            }
        }
        return guideList;
    }

    // Get guide by ID
    public Person getGuideById(int id) throws SQLException {
        String req = "SELECT id, name, last_name, email, telephone, username, role, status, date " +
                "FROM `user` WHERE id = ? AND (role = 'guide' OR role = 'admin')";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Person user = new Person(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            null, // password
                            rs.getDate("date"),
                            rs.getString("role"),
                            rs.getString("username"),
                            rs.getString("telephone")
                    );
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        }
        return null;
    }

    // Get guides with their activities
    public List<Person> getGuidesWithActivities() throws SQLException {
        List<Person> guideList = new ArrayList<>();

        String req = "SELECT u.id, u.name, u.last_name, u.email, u.telephone, u.username, u.role, u.status, u.date, " +
                "a.idActivite, a.titre, a.description, a.lieu, a.dateActivite, " +
                "a.prix, a.statut as activity_status, a.placesDisponibles " +
                "FROM `user` u " +
                "LEFT JOIN `activite` a ON u.id = a.idGuide " +
                "WHERE u.role = 'guide' OR u.role = 'admin' " +
                "ORDER BY u.id, a.dateActivite";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            Person currentGuide = null;
            while (rs.next()) {
                int guideId = rs.getInt("id");

                if (currentGuide == null || currentGuide.getId() != guideId) {
                    currentGuide = new Person(
                            guideId,
                            rs.getString("name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            null, // password
                            rs.getDate("date"),
                            rs.getString("role"),
                            rs.getString("username"),
                            rs.getString("telephone")
                    );
                    currentGuide.setStatus(rs.getString("status"));
                    guideList.add(currentGuide);
                }

                // You can add activities to a list in the Person class if needed
                // currentGuide.addActivity(...);
            }
        }
        return guideList;
    }

    // Search guides by name
    public List<Person> searchGuides(String searchTerm) throws SQLException {
        List<Person> guideList = new ArrayList<>();

        String req = "SELECT id, name, last_name, email, telephone, username, role, status, date " +
                "FROM `user` " +
                "WHERE (role = 'guide' OR role = 'admin') AND (name LIKE ? OR last_name LIKE ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            String pattern = "%" + searchTerm + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Person user = new Person(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            null, // password
                            rs.getDate("date"),
                            rs.getString("role"),
                            rs.getString("username"),
                            rs.getString("telephone")
                    );
                    user.setStatus(rs.getString("status"));
                    guideList.add(user);
                }
            }
        }
        return guideList;
    }

    // Count activities per guide
    public int countActivitiesByGuide(int guideId) throws SQLException {
        String req = "SELECT COUNT(*) as count FROM `activite` WHERE idGuide = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, guideId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    // Update guide status
    public void updateGuideStatus(int guideId, String status) throws SQLException {
        String req = "UPDATE `user` SET status = ? WHERE id = ? AND (role = 'guide' OR role = 'admin')";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, status);
            ps.setInt(2, guideId);
            ps.executeUpdate();
        }
    }

    // Get guides by availability based on their status
    public List<Person> getGuidesByStatus(String status) throws SQLException {
        List<Person> guideList = new ArrayList<>();

        String req = "SELECT id, name, last_name, email, telephone, username, role, status, date " +
                "FROM `user` WHERE (role = 'guide' OR role = 'admin') AND status = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, status);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Person user = new Person(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            null, // password
                            rs.getDate("date"),
                            rs.getString("role"),
                            rs.getString("username"),
                            rs.getString("telephone")
                    );
                    user.setStatus(rs.getString("status"));
                    guideList.add(user);
                }
            }
        }
        return guideList;
    }
}