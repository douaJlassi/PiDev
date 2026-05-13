package repositories;

import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreServiceRepository {

    private final Connection cnx;

    private static final String TABLE_NAME = "offer_service";

    public OffreServiceRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public void addServiceToOffre(int offerId, int serviceId, int quantity) {
        String sql =
                "INSERT INTO " + TABLE_NAME + " (created_at, offer_id, service_id) " +
                        "VALUES (NOW(), ?, ?) " +
                        "ON DUPLICATE KEY UPDATE created_at = NOW()";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, offerId);
            ps.setInt(2, serviceId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error addServiceToOffre: " + e.getMessage(),
                    e
            );
        }
    }

    public boolean removeServiceFromOffre(int offerId, int serviceId) {
        String sql =
                "DELETE FROM " + TABLE_NAME + " " +
                        "WHERE offer_id = ? AND service_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, offerId);
            ps.setInt(2, serviceId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error removeServiceFromOffre: " + e.getMessage(),
                    e
            );
        }
    }

    public List<Integer> findServiceIdsByOffre(int offerId) {
        List<Integer> ids = new ArrayList<>();

        String sql =
                "SELECT service_id " +
                        "FROM " + TABLE_NAME + " " +
                        "WHERE offer_id = ? " +
                        "ORDER BY service_id";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, offerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("service_id"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error findServiceIdsByOffre: " + e.getMessage(),
                    e
            );
        }

        return ids;
    }

    public void replaceServices(int offerId, List<Integer> serviceIds) {
        String deleteSql =
                "DELETE FROM " + TABLE_NAME + " WHERE offer_id = ?";

        String insertSql =
                "INSERT INTO " + TABLE_NAME + " (created_at, offer_id, service_id) " +
                        "VALUES (NOW(), ?, ?)";

        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement ps = cnx.prepareStatement(deleteSql)) {
                ps.setInt(1, offerId);
                ps.executeUpdate();
            }

            if (serviceIds != null && !serviceIds.isEmpty()) {
                try (PreparedStatement ps = cnx.prepareStatement(insertSql)) {

                    for (Integer serviceId : serviceIds) {
                        ps.setInt(1, offerId);
                        ps.setInt(2, serviceId);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                }
            }

            cnx.commit();

        } catch (SQLException e) {

            try {
                cnx.rollback();
            } catch (SQLException ignored) {
            }

            throw new RuntimeException(
                    "Error replaceServices: " + e.getMessage(),
                    e
            );

        } finally {

            try {
                cnx.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }
}