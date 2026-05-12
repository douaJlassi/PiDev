package services;

import entities.Agency;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AgencyService — persistence for the Agency entity.
 *
 * Implements CRUD<Agency> and adds:
 *   login(email, password) — returns the Agency if credentials match, null otherwise.
 *   selectAll()            — used to populate the agency ComboBox in the create-post dialog.
 *
 * Password note:
 *   Passwords are stored as plain text for now to keep the project simple.
 *   In production, hash with BCrypt before insert and compare hashes on login.
 */
public class AgencyService implements CRUD<Agency> {

    private final Connection cnx;

    public AgencyService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public void insertOne(Agency a) throws SQLException {
        String sql = "INSERT INTO agency (name, email, password, description, logo_path, phone, address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getName());
            ps.setString(2, a.getEmail());
            ps.setString(3, a.getPassword());   // hash before passing in production
            ps.setString(4, a.getDescription());
            ps.setString(5, a.getLogoPath());
            ps.setString(6, a.getPhone());
            ps.setString(7, a.getAddress());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) a.setAgencyID(rs.getInt(1));
            }
        }
    }

    @Override
    public void updateOne(Agency a) throws SQLException {
        String sql = "UPDATE agency SET name=?, email=?, description=?, logo_path=?, phone=?, address=? " +
                "WHERE agencyID=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getName());
            ps.setString(2, a.getEmail());
            ps.setString(3, a.getDescription());
            ps.setString(4, a.getLogoPath());
            ps.setString(5, a.getPhone());
            ps.setString(6, a.getAddress());
            ps.setInt(7, a.getAgencyID());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteOne(Agency a) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM agency WHERE agencyID=?")) {
            ps.setInt(1, a.getAgencyID());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Agency> selectALL() throws SQLException {
        List<Agency> list = new ArrayList<>();
        String sql = "SELECT * FROM agency ORDER BY name ASC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Attempt agency login.
     * @return the Agency object on success, null if email/password don't match.
     */
    public Agency login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM agency WHERE email = ? AND password = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, password);   // compare hash in production
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Agency mapRow(ResultSet rs) throws SQLException {
        Agency a = new Agency(
                rs.getInt("agencyID"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("description"),
                rs.getString("logo_path"),
                rs.getString("phone"),
                rs.getString("address")
        );
        a.setPassword(rs.getString("password"));
        return a;
    }
}