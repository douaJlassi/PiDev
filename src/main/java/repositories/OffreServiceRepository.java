package repositories;

import utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreServiceRepository {

    private final Connection cnx;

    public OffreServiceRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public void addServiceToOffre(int idOffre, int idService, int quantite) {
        String sql = "INSERT INTO offre_service (idOffre, idService, quantite) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantite = VALUES(quantite)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idService);
            ps.setInt(3, quantite);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error addServiceToOffre: " + e.getMessage(), e);
        }
    }

    public boolean removeServiceFromOffre(int idOffre, int idService) {
        String sql = "DELETE FROM offre_service WHERE idOffre=? AND idService=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idService);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error removeServiceFromOffre: " + e.getMessage(), e);
        }
    }

    public List<Integer> findServiceIdsByOffre(int idOffre) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT idService FROM offre_service WHERE idOffre=? ORDER BY idService";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("idService"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findServiceIdsByOffre: " + e.getMessage(), e);
        }
        return ids;
    }
    public void replaceServices(int idOffre, List<Integer> serviceIds) {
        String del = "DELETE FROM offre_service WHERE idOffre=?";
        String ins = "INSERT INTO offre_service(idOffre, idService) VALUES(?, ?)";

        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement ps = cnx.prepareStatement(del)) {
                ps.setInt(1, idOffre);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = cnx.prepareStatement(ins)) {
                for (Integer idService : serviceIds) {
                    ps.setInt(1, idOffre);
                    ps.setInt(2, idService);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            cnx.commit();
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Error replaceServices: " + e.getMessage(), e);
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }



}
