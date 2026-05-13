package services;

import entities.Person;
import utils.FaceRecognitionUtil;
import utils.MyDBConnexion;

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

            // Set two factor code
            try {
                person.setTwoFactorCode(rs.getString("two_factor_code"));
            } catch (SQLException e) {
                person.setTwoFactorCode(null);
            }

            // Set two factor expiry
            try {
                person.setTwoFactorExpiry(rs.getTimestamp("two_factor_expiry"));
            } catch (SQLException e) {
                person.setTwoFactorExpiry(null);
            }

            // Set face data
            try {
                person.setFaceData(rs.getBytes("face_data"));
            } catch (SQLException e) {
                person.setFaceData(null);
            }

            // Set fingerprint data
            try {
                person.setFingerprintData(rs.getBytes("fingerprint_data"));
            } catch (SQLException e) {
                person.setFingerprintData(null);
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
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                onlineUsers.add(mapPerson(rs));
            }
        }
        return onlineUsers;
    }

    /**
     * Get all offline users
     */
    public List<Person> getOfflineUsers() throws SQLException {
        List<Person> offlineUsers = new ArrayList<>();
        String req = "SELECT * FROM `user` WHERE status='offline' OR status IS NULL";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                offlineUsers.add(mapPerson(rs));
            }
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
                new java.sql.Date(person.getDate().getTime()) + "', '" +
                person.getRole() + "', '" +
                person.getUsername() + "', 'offline')";
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(req);
        }
    }

    public void insertOneUpdated(Person person) throws SQLException {
        String req = "INSERT INTO `user` (name, last_name, email, password, date, role, username, status) VALUES "
                + "(?, ?, ?, ?, ?, ?, ?, 'offline')";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, person.getName());
            ps.setString(2, person.getLastName());
            ps.setString(3, person.getEmail());
            ps.setString(4, person.getPassword());
            ps.setDate(5, new java.sql.Date(person.getDate().getTime()));
            ps.setString(6, person.getRole());
            ps.setString(7, person.getUsername());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateOne(Person person) throws SQLException {
        String req = "UPDATE `user` SET name=?, last_name=?, email=?, password=?, date=?, role=?, username=?, status=?, two_factor_enabled=?, two_factor_code=?, two_factor_expiry=?, face_data=?, fingerprint_data=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, person.getName());
            ps.setString(2, person.getLastName());
            ps.setString(3, person.getEmail());
            ps.setString(4, person.getPassword());
            ps.setDate(5, new java.sql.Date(person.getDate().getTime()));
            ps.setString(6, person.getRole());
            ps.setString(7, person.getUsername());
            ps.setString(8, person.getStatus() != null ? person.getStatus() : "offline");
            ps.setBoolean(9, person.isTwoFactorEnabled());
            ps.setString(10, person.getTwoFactorCode());
            ps.setTimestamp(11, person.getTwoFactorExpiry());
            ps.setBytes(12, person.getFaceData());
            ps.setBytes(13, person.getFingerprintData());
            ps.setInt(14, person.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void deleteOne(Person person) throws SQLException {
        String req = "DELETE FROM `user` WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, person.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Person> selectALL() throws SQLException {
        List<Person> userList = new ArrayList<>();
        String req = "SELECT * FROM `user`";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                userList.add(mapPerson(rs));
            }
        }
        return userList;
    }

    /**
     * Get all admin users
     */
    public List<Person> getAdminUsers() throws SQLException {
        List<Person> adminList = new ArrayList<>();
        String req = "SELECT * FROM `user` WHERE LOWER(role) LIKE '%admin%'";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                adminList.add(mapPerson(rs));
            }
        }
        return adminList;
    }

    /**
     * Check if user has 2FA enabled
     */
    public boolean isTwoFactorEnabled(int userId) throws SQLException {
        String req = "SELECT two_factor_enabled FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("two_factor_enabled");
                }
            }
        }
        return false;
    }

    /**
     * Save 2FA code for user
     */
    public void save2FACode(int userId, String code) throws SQLException {
        String req = "UPDATE `user` SET two_factor_code = ?, two_factor_expiry = DATE_ADD(NOW(), INTERVAL 5 MINUTE) WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, code);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Verify 2FA code
     */
    public boolean verify2FACode(int userId, String code) throws SQLException {
        String req = "SELECT * FROM `user` WHERE id = ? AND two_factor_code = ? AND two_factor_expiry > NOW()";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Clear the code after successful verification
                    clear2FACode(userId);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clear 2FA code after verification
     */
    public void clear2FACode(int userId) throws SQLException {
        String req = "UPDATE `user` SET two_factor_code = NULL, two_factor_expiry = NULL WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Enable/disable 2FA for user
     */
    public void setTwoFactorEnabled(int userId, boolean enabled) throws SQLException {
        String req = "UPDATE `user` SET two_factor_enabled = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Get user by ID with all fields
     */
    public Person getUserById(int userId) throws SQLException {
        String req = "SELECT * FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPerson(rs);
                }
            }
        }
        return null;
    }

    /**
     * Save face data for a user - simplified version
     */
    public void saveFaceData(int userId, byte[] faceData) throws SQLException {
        String query = "UPDATE `user` SET face_data = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            if (faceData != null && faceData.length > 0) {
                ps.setBytes(1, faceData);
                System.out.println("📸 Saving face data for user ID: " + userId + ", size: " + faceData.length + " bytes");
            } else {
                ps.setNull(1, Types.BLOB);
                System.out.println("❌ No face data to save or data is empty");
            }
            ps.setInt(2, userId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Face data saved successfully for user ID: " + userId);
            } else {
                System.err.println("❌ Failed to save face data - user ID " + userId + " not found");
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Error saving face data: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get face data for a user
     */
    public byte[] getFaceData(int userId) throws SQLException {
        String query = "SELECT face_data FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("face_data");
                }
            }
        }
        return null;
    }

    /**
     * Find user by face data
     */
    public Person findUserByFaceData(byte[] capturedFace) throws SQLException {
        // First check if face_data column exists
        if (!columnExists("face_data")) {
            System.out.println("face_data column does not exist in user table");
            return null;
        }

        // Get all users with face data
        String query = "SELECT * FROM `user` WHERE face_data IS NOT NULL";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            FaceRecognitionUtil faceUtil = new FaceRecognitionUtil();
            Person bestMatch = null;
            double bestSimilarity = 0;

            while (rs.next()) {
                byte[] storedFace = rs.getBytes("face_data");
                if (storedFace != null && storedFace.length > 0) {
                    try {
                        // Compare faces using the compareFaces method
                        double similarity = faceUtil.compareFaces(capturedFace, storedFace);
                        System.out.println("Face similarity with user ID " + rs.getInt("id") + ": " + similarity);

                        if (similarity > 0.51) { // Threshold for match
                            System.out.println("✅ Face match found for user ID: " + rs.getInt("id"));
                            return mapPerson(rs);
                        } else if (similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestMatch = mapPerson(rs);
                        }
                    } catch (Exception e) {
                        System.err.println("Error comparing faces for user ID " + rs.getInt("id") + ": " + e.getMessage());
                    }
                }
            }

            if (bestSimilarity > 0.4) {
                System.out.println("⚠️ Best match has similarity " + bestSimilarity + " (below threshold 0.51)");
                return bestMatch;
            }

            System.out.println("❌ No face match found");
            return null;
        }
    }

    /**
     * Check if a column exists in the user table
     */
    private boolean columnExists(String columnName) throws SQLException {
        DatabaseMetaData metaData = cnx.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, "user", columnName)) {
            return columns.next();
        }
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
        person.setStatus(rs.getString("status") != null ? rs.getString("status") : "offline");

        // Set two factor enabled
        try {
            person.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
        } catch (SQLException e) {
            person.setTwoFactorEnabled(false);
        }

        // Set fingerprint data
        try {
            person.setFingerprintData(rs.getBytes("fingerprint_data"));
        } catch (SQLException e) {
            person.setFingerprintData(null);
        }

        // Set fingerprint slot ID
        try {
            person.setFingerprintSlotId(rs.getInt("fingerprint_slot_id"));
        } catch (SQLException e) {
            person.setFingerprintSlotId(-1);
        }

        return person;
    }

    public Person getUserByEmail(String email) throws SQLException {
        String req = "SELECT * FROM `user` WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPerson(rs);
                }
            }
        }
        return null;
    }

    /**
     * Check if email exists in database
     */
    public boolean emailExists(String email) throws SQLException {
        String req = "SELECT COUNT(*) FROM `user` WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Find user by email or username
     */
    public Person findByEmailOrUsername(String emailOrUsername) throws SQLException {
        String req = "SELECT * FROM `user` WHERE email = ? OR username = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, emailOrUsername);
            ps.setString(2, emailOrUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPerson(rs);
                }
            }
        }
        return null;
    }

    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM `user` WHERE username = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        return false;
    }

    // ==================== FINGERPRINT METHODS ====================

    /**
     * Save fingerprint data for a user
     */
    /**
     * Save fingerprint data for a user with slot ID
     */
    public void saveFingerprintData(int userId, byte[] fingerprintData, int fingerprintSlotId) throws SQLException {
        // First check if fingerprint_data column exists
        if (!columnExists("fingerprint_data")) {
            addFingerprintColumn();
        }

        // Check if fingerprint_slot_id column exists
        if (!columnExists("fingerprint_slot_id")) {
            addFingerprintSlotColumn();
        }

        String query = "UPDATE `user` SET fingerprint_data = ?, fingerprint_slot_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            if (fingerprintData != null && fingerprintData.length > 0) {
                ps.setBytes(1, fingerprintData);
                ps.setInt(2, fingerprintSlotId);
                System.out.println("📸 Saving fingerprint for user ID: " + userId + " in slot #" + fingerprintSlotId);
            } else {
                ps.setNull(1, Types.BLOB);
                ps.setNull(2, Types.INTEGER);
                System.out.println("Removing fingerprint data for user ID: " + userId);
            }
            ps.setInt(3, userId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Fingerprint data saved successfully for user ID: " + userId);
            } else {
                System.err.println("❌ No rows affected - user ID " + userId + " not found");
            }
        }
    }

    /**
     * Add fingerprint_slot_id column if it doesn't exist
     */
    private void addFingerprintSlotColumn() throws SQLException {
        try {
            String sql = "ALTER TABLE `user` ADD COLUMN fingerprint_slot_id INT NULL";
            try (Statement st = cnx.createStatement()) {
                st.execute(sql);
                System.out.println("✅ fingerprint_slot_id column added to user table");
            }
        } catch (SQLException e) {
            // Column might already exist
            if (!e.getMessage().contains("Duplicate column")) {
                throw e;
            }
        }
    }

    /**
     * Get fingerprint data for a user
     */
    public byte[] getFingerprintData(int userId) throws SQLException {
        if (!columnExists("fingerprint_data")) {
            return null;
        }

        String query = "SELECT fingerprint_data FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("fingerprint_data");
                }
            }
        }
        return null;
    }

    /**
     * Get all users with fingerprint data
     */
    public List<Person> getUsersWithFingerprint() throws SQLException {
        List<Person> users = new ArrayList<>();

        if (!columnExists("fingerprint_data")) {
            System.out.println("fingerprint_data column does not exist");
            return users;
        }

        // Make sure we're selecting users that have both fingerprint_data AND fingerprint_slot_id
        String query = "SELECT * FROM `user` WHERE fingerprint_data IS NOT NULL";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                Person p = mapPerson(rs);
                users.add(p);
                System.out.println("📋 DB - Found user with fingerprint: ID=" + p.getId() +
                        ", email=" + p.getEmail() +
                        ", slot=" + p.getFingerprintSlotId());
            }
        }
        return users;
    }
    /**
     * Get user by fingerprint data ID (simulated - in real implementation,
     * this would search the fingerprint sensor's database)
     */
    public Person getUserByFingerprintId(int fingerprintId) throws SQLException {
        // In a real implementation, you would have a mapping between fingerprint IDs and user IDs
        // For simulation, we'll just return a random user with fingerprint data
        List<Person> usersWithFingerprint = getUsersWithFingerprint();

        if (!usersWithFingerprint.isEmpty()) {
            // For simulation, return the first user with fingerprint data
            // In reality, you'd match the fingerprintId to a specific user
            return usersWithFingerprint.get(0);
        }
        return null;
    }

    /**
     * Add fingerprint_data column to user table if it doesn't exist
     */
    private void addFingerprintColumn() throws SQLException {
        try {
            String sql = "ALTER TABLE `user` ADD COLUMN fingerprint_data LONGBLOB NULL";
            try (Statement st = cnx.createStatement()) {
                st.execute(sql);
                System.out.println("✅ fingerprint_data column added to user table");
            }
        } catch (SQLException e) {
            System.err.println("Failed to add fingerprint_data column: " + e.getMessage());
            // Column might already exist
        }
    }
}