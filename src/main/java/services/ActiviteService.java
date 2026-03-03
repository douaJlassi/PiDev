package services;

import gestion_activite.Activite;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService implements CRUD<Activite> {

    @Override
    public void insertOne(Activite a) throws SQLException {
        String req = "INSERT INTO `activite` (`titre`, `description`, `lieu`, `dateActivite`, `dureParJour`, `prix`, `idGuide`, `image`, `statut`, `placesDisponibles`, `categorie`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, a.getTitre());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getLieu());
            ps.setTimestamp(4, a.getDateActivite());
            ps.setInt(5, a.getDureParJour());
            ps.setDouble(6, a.getPrix());
            ps.setInt(7, a.getIdGuide());
            ps.setString(8, a.getImage());
            ps.setString(9, a.getStatut());
            ps.setInt(10, a.getPlacesDisponibles());
            ps.setString(11, a.getCategorie());

            ps.executeUpdate();
        }
    }

    @Override
    public void updateOne(Activite activite) throws SQLException {
        String req = "UPDATE `activite` SET `titre`=?, `description`=?, `lieu`=?, `dateActivite`=?, `dureParJour`=?, `prix`=?, `idGuide`=?, `image`=?, `statut`=?, `placesDisponibles`=?, `categorie`=? " +
                "WHERE `idActivite`=?";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, activite.getTitre());
            ps.setString(2, activite.getDescription());
            ps.setString(3, activite.getLieu());
            ps.setTimestamp(4, activite.getDateActivite());
            ps.setInt(5, activite.getDureParJour());
            ps.setDouble(6, activite.getPrix());
            ps.setInt(7, activite.getIdGuide());
            ps.setString(8, activite.getImage());
            ps.setString(9, activite.getStatut());
            ps.setInt(10, activite.getPlacesDisponibles());
            ps.setString(11, activite.getCategorie());
            ps.setInt(12, activite.getIdActivite());

            ps.executeUpdate();
        }
    }

    @Override
    public void deleteOne(Activite activite) throws SQLException {
        String req = "DELETE FROM `activite` WHERE `idActivite`=?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, activite.getIdActivite());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Activite> selectALL() throws SQLException {
        List<Activite> activiteList = new ArrayList<>();
        String req = "SELECT * FROM `activite`";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                activiteList.add(mapResultSetToActivite(rs));
            }
        }
        return activiteList;
    }

    public Activite selectById(int idActivite) throws SQLException {
        String req = "SELECT * FROM `activite` WHERE `idActivite`=?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, idActivite);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToActivite(rs);
                }
            }
        }
        return null;
    }

    public List<Activite> selectByLieu(String lieu) throws SQLException {
        List<Activite> activiteList = new ArrayList<>();
        String req = "SELECT * FROM `activite` WHERE `lieu` LIKE ?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, "%" + lieu + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    activiteList.add(mapResultSetToActivite(rs));
                }
            }
        }
        return activiteList;
    }

    public List<Activite> selectByGuide(int idGuide) throws SQLException {
        List<Activite> activiteList = new ArrayList<>();
        String req = "SELECT * FROM `activite` WHERE `idGuide`=?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, idGuide);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    activiteList.add(mapResultSetToActivite(rs));
                }
            }
        }
        return activiteList;
    }

    public List<Activite> selectByPrixRange(double minPrix, double maxPrix) throws SQLException {
        List<Activite> activiteList = new ArrayList<>();
        String req = "SELECT * FROM `activite` WHERE `prix` BETWEEN ? AND ?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setDouble(1, minPrix);
            ps.setDouble(2, maxPrix);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    activiteList.add(mapResultSetToActivite(rs));
                }
            }
        }
        return activiteList;
    }

    private Activite mapResultSetToActivite(ResultSet rs) throws SQLException {
        Activite a = new Activite(
                rs.getInt("idActivite"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("lieu"),
                rs.getTimestamp("dateActivite"),
                rs.getInt("dureParJour"),
                rs.getDouble("prix"),
                rs.getInt("idGuide"),
                rs.getString("image"),
                rs.getString("statut"),
                rs.getInt("placesDisponibles"),
                rs.getString("categorie")
        );
        // new
        a.setDateCreation(rs.getTimestamp("dateCreation"));
        return a;
    }

    public void updatePlaces(int idActivite, int newPlaces) throws SQLException {
        String req = "UPDATE activite SET placesDisponibles = ? WHERE idActivite = ?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, newPlaces);
            ps.setInt(2, idActivite);
            ps.executeUpdate();
        }
    }



    public void deleteExpiredActivities() throws SQLException
    {
        String sql = "DELETE FROM activite WHERE dateActivite < DATE_SUB(NOW(), INTERVAL 2 DAY)";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println(deleted + " activité(s) expirée(s) supprimée(s).");
            }
        }
    }


    public int countReservationsForActivity(int idActivite) throws SQLException {
        String sql = "SELECT COALESCE(SUM(nbPlaces), 0) FROM achat WHERE idActivite = ? AND statut != 'Annulé'";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idActivite);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

}