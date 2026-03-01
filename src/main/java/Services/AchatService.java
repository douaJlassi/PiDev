package Services;

import gestion_activite.Achat;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AchatService {

    // ❌ Remove: private Connection cnx;
    // ✅ Always get a fresh (validated) connection per method

    private Connection getConn() {
        return MyDBConnexion.getInstance().getConnection();
    }

    public void insert(Achat achat) throws SQLException {
        String sql = "INSERT INTO achat (dateAchat, montantTotal, statut, idClient, nbPlaces, idActivite) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, achat.getDateAchat());
            ps.setDouble(2, achat.getMontantTotal());
            ps.setString(3, achat.getStatut());
            ps.setInt(4, achat.getIdClient());
            ps.setInt(5, achat.getNbPlaces());
            ps.setInt(6, achat.getIdActivite());
            ps.executeUpdate();
        }
    }

    public List<Achat> selectAll() throws SQLException {
        List<Achat> list = new ArrayList<>();
        String sql = "SELECT * FROM achat";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToAchat(rs));
        }
        return list;
    }

    public Achat selectById(int id) throws SQLException {
        String sql = "SELECT * FROM achat WHERE idAchat = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToAchat(rs);
            }
        }
        return null;
    }

    public void update(Achat achat) throws SQLException {
        String sql = "UPDATE achat SET dateAchat=?, montantTotal=?, statut=?, idClient=?, nbPlaces=?, idActivite=? WHERE idAchat=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, achat.getDateAchat());
            ps.setDouble(2, achat.getMontantTotal());
            ps.setString(3, achat.getStatut());
            ps.setInt(4, achat.getIdClient());
            ps.setInt(5, achat.getNbPlaces());
            ps.setInt(6, achat.getIdActivite());
            ps.setInt(7, achat.getIdAchat());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM achat WHERE idAchat = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Achat> selectByClient(int idClient) throws SQLException {
        List<Achat> list = new ArrayList<>();
        String sql = "SELECT * FROM achat WHERE idClient = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToAchat(rs));
            }
        }
        return list;
    }

    private Achat mapResultSetToAchat(ResultSet rs) throws SQLException {
        return new Achat(
                rs.getInt("idAchat"),
                rs.getTimestamp("dateAchat"),
                rs.getDouble("montantTotal"),
                rs.getString("statut"),
                rs.getInt("idClient"),
                rs.getInt("nbPlaces"),
                rs.getInt("idActivite")
        );
    }
}