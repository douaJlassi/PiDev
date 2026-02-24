package tn.esprit.projet.services;

import tn.esprit.projet.entities.Person;
import tn.esprit.projet.utils.FaceRecognitionUtil;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonService implements CRUD<Person> {

    private Connection cnx;

    public PersonService(){
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    public Person login(String emailOrUsername, String password) throws SQLException {
        String req = "SELECT * FROM `user` WHERE (email=? OR username=?) AND password=?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, emailOrUsername);
        ps.setString(2, emailOrUsername);
        ps.setString(3, password);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Person person = new Person(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDate("date"),
                    rs.getString("role"),
                    rs.getString("username")
            );
            // Set status
            person.setStatus(rs.getString("status") != null ? rs.getString("status") : "offline");

            // Set two factor enabled
            try {
                person.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
            } catch (SQLException e) {
                person.setTwoFactorEnabled(false);
            }

            // Set face data
            try {
                person.setFaceData(rs.getBytes("face_data"));
            } catch (SQLException e) {
                person.setFaceData(null);
            }

            return person;
        }
        return null;
    }

    /**
     * Update user status (online/offline)
     */
    public void updateUserStatus(int userId, String status) throws SQLException {
        String req = "UPDATE `user` SET status=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, status);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    /**
     * Get all online users
     */
    public List<Person> getOnlineUsers() throws SQLException {
        List<Person> onlineUsers = new ArrayList<>();
        String req = "SELECT * FROM `user` WHERE status='online'";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Person p = new Person(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDate("date"),
                    rs.getString("role"),
                    rs.getString("username")
            );
            p.setStatus(rs.getString("status"));
            onlineUsers.add(p);
        }
        return onlineUsers;
    }

    /**
     * Get all offline users
     */
    public List<Person> getOfflineUsers() throws SQLException {
        List<Person> offlineUsers = new ArrayList<>();
        String req = "SELECT * FROM `user` WHERE status='offline' OR status IS NULL";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Person p = new Person(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDate("date"),
                    rs.getString("role"),
                    rs.getString("username")
            );
            p.setStatus(rs.getString("status") != null ? rs.getString("status") : "offline");
            offlineUsers.add(p);
        }
        return offlineUsers;
    }

    @Override
    public void insertOne(Person person) throws SQLException {
        String req = "INSERT INTO `user` (name, last_name, email, password, date, role, username, status) VALUES " +
                "('" + person.getName() + "', '" +
                person.getLastName() + "', '" +
                person.getEmail() + "', '" +
                person.getPassword() + "', '" +
                person.getDate() + "', '" +
                person.getRole() + "', '" +
                person.getUsername() + "', 'offline')";
        Statement st = cnx.createStatement();
        st.executeUpdate(req);
    }

    public void insertOneUpdated(Person person) throws SQLException {
        String req = "INSERT INTO `user` (name, last_name, email, password, date, role, username, status) VALUES "
                + "(?, ?, ?, ?, ?, ?, ?, 'offline')";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, person.getName());
        ps.setString(2, person.getLastName());
        ps.setString(3, person.getEmail());
        ps.setString(4, person.getPassword());
        ps.setDate(5, person.getDate());
        ps.setString(6, person.getRole());
        ps.setString(7, person.getUsername());

        ps.executeUpdate();
    }

    @Override
    public void updateOne(Person person) throws SQLException {
        String req = "UPDATE `user` SET name=?, last_name=?, email=?, password=?, date=?, role=?, username=?, status=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, person.getName());
        ps.setString(2, person.getLastName());
        ps.setString(3, person.getEmail());
        ps.setString(4, person.getPassword());
        ps.setDate(5, person.getDate());
        ps.setString(6, person.getRole());
        ps.setString(7, person.getUsername());
        ps.setString(8, person.getStatus() != null ? person.getStatus() : "offline");
        ps.setInt(9, person.getId());

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Person person) throws SQLException {
        String req = "DELETE FROM `user` WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, person.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Person> selectALL() throws SQLException {
        List<Person> userList = new ArrayList<>();

        String req = "SELECT * FROM `user`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Person p = new Person(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDate("date"),
                    rs.getString("role"),
                    rs.getString("username")
            );
            p.setStatus(rs.getString("status") != null ? rs.getString("status") : "offline");
            userList.add(p);
        }

        return userList;
    }

    /**
     * Get all admin users
     */
    public List<Person> getAdminUsers() throws SQLException {
        List<Person> adminList = new ArrayList<>();
        String req = "SELECT * FROM `user` WHERE LOWER(role) LIKE '%admin%'";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Person p = new Person(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDate("date"),
                    rs.getString("role"),
                    rs.getString("username")
            );
            p.setStatus(rs.getString("status") != null ? rs.getString("status") : "offline");
            adminList.add(p);
        }
        return adminList;
    }

    /**
     * Check if user has 2FA enabled
     */
    public boolean isTwoFactorEnabled(int userId) throws SQLException {
        String req = "SELECT two_factor_enabled FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getBoolean("two_factor_enabled");
        }
        return false;
    }

    /**
     * Save 2FA code for user
     */
    public void save2FACode(int userId, String code) throws SQLException {
        String req = "UPDATE `user` SET two_factor_code = ?, two_factor_expiry = DATE_ADD(NOW(), INTERVAL 5 MINUTE) WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, code);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    /**
     * Verify 2FA code
     */
    public boolean verify2FACode(int userId, String code) throws SQLException {
        String req = "SELECT * FROM `user` WHERE id = ? AND two_factor_code = ? AND two_factor_expiry > NOW()";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.setString(2, code);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            // Clear the code after successful verification
            clear2FACode(userId);
            return true;
        }
        return false;
    }

    /**
     * Clear 2FA code after verification
     */
    public void clear2FACode(int userId) throws SQLException {
        String req = "UPDATE `user` SET two_factor_code = NULL, two_factor_expiry = NULL WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    /**
     * Enable/disable 2FA for user
     */
    public void setTwoFactorEnabled(int userId, boolean enabled) throws SQLException {
        String req = "UPDATE `user` SET two_factor_enabled = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, enabled ? 1 : 0);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    /**
     * Get user by ID with all fields
     */
    public Person getUserById(int userId) throws SQLException {
        String req = "SELECT * FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Person person = new Person(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getDate("date"),
                    rs.getString("role"),
                    rs.getString("username")
            );
            person.setStatus(rs.getString("status") != null ? rs.getString("status") : "offline");

            // Set two factor enabled
            try {
                person.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
            } catch (SQLException e) {
                person.setTwoFactorEnabled(false);
            }

            // Set face data
            try {
                person.setFaceData(rs.getBytes("face_data"));
            } catch (SQLException e) {
                person.setFaceData(null);
            }

            return person;
        }
        return null;
    }

    /**
     * Save face data for a user
     */
    public void saveFaceData(int userId, byte[] faceData) throws SQLException {
        String query = "UPDATE `user` SET face_data = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setBytes(1, faceData);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("Face data saved for user ID: " + userId);
        }
    }

    /**
     * Get face data for a user
     */
    public byte[] getFaceData(int userId) throws SQLException {
        String query = "SELECT face_data FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBytes("face_data");
            }
        }
        return null;
    }

    /**
     * Find user by face data
     */
    public Person findUserByFaceData(byte[] capturedFace) throws SQLException {
        // First check if face_data column exists
        DatabaseMetaData metaData = cnx.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, "user", "face_data");
        boolean columnExists = columns.next();

        if (!columnExists) {
            System.out.println("face_data column does not exist in user table");
            return null;
        }

        // Get all users with face data
        String query = "SELECT * FROM `user` WHERE face_data IS NOT NULL";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            FaceRecognitionUtil faceUtil = new FaceRecognitionUtil();

            while (rs.next()) {
                byte[] storedFace = rs.getBytes("face_data");
                if (storedFace != null && storedFace.length > 0) {
                    // Compare faces
                    double similarity = faceUtil.compareFaces(capturedFace, storedFace);
                    System.out.println("Face similarity: " + similarity);

                    if (similarity > 0.51) { // Threshold for match
                        return mapPerson(rs);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Map ResultSet to Person object
     */
    private Person mapPerson(ResultSet rs) throws SQLException {
        Person person = new Person();
        person.setId(rs.getInt("id"));
        person.setName(rs.getString("name"));
        person.setLastName(rs.getString("last_name"));
        person.setEmail(rs.getString("email"));
        person.setPassword(rs.getString("password"));
        person.setDate(rs.getDate("date"));
        person.setRole(rs.getString("role"));
        person.setUsername(rs.getString("username"));
        person.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));

        // Set face data
        try {
            person.setFaceData(rs.getBytes("face_data"));
        } catch (SQLException e) {
            person.setFaceData(null);
        }

        return person;
    }


    public Person getUserByEmail(String email) throws SQLException {
        String req = "SELECT * FROM `user` WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapPerson(rs);
        }
        return null;
    }

    /**
     * Check if email exists in database
     */
    public boolean emailExists(String email) throws SQLException {
        String req = "SELECT COUNT(*) FROM `user` WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    /**
     * Find user by email or username
     */
    public Person findByEmailOrUsername(String emailOrUsername) throws SQLException {
        String req = "SELECT * FROM `user` WHERE email = ? OR username = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, emailOrUsername);
        ps.setString(2, emailOrUsername);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapPerson(rs);
        }
        return null;
    }

  
}
