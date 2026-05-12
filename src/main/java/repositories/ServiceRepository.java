package repositories;

import entities.ServiceEntity;
import entities.ServiceEntityDetails;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRepository {

    private final Connection cnx;

    public ServiceRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public List<ServiceEntity> findAllByAgency(int agencyId) {
        List<ServiceEntity> list = new ArrayList<>();

        String sql =
                "SELECT id, name, type " +
                        "FROM service " +
                        "WHERE agency_id = ? OR agency_id IS NULL " +
                        "ORDER BY id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, agencyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServiceEntity s = new ServiceEntity();

                    s.setId(rs.getInt("id"));
                    s.setName(rs.getString("name"));
                    s.setType(rs.getString("type"));

                    list.add(s);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error findAllByAgency service: " + e.getMessage(),
                    e
            );
        }

        return list;
    }

    public ServiceEntityDetails findDetailsByIdService(int serviceId) {
        String sql =
                "SELECT " +
                        " s.id, " +
                        " s.name, " +
                        " s.type, " +
                        " s.description, " +
                        " s.base_price, " +
                        " s.is_available, " +
                        " s.capacity, " +
                        " s.agency_id, " +
                        " s.image_url, " +
                        " s.created_at, " +

                        " h.stars, " +
                        " h.location AS hotel_location, " +
                        " h.room_type " +

                        "FROM service s " +
                        "LEFT JOIN hotel h ON h.id = s.id " +
                        "WHERE s.id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, serviceId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                ServiceEntityDetails d = new ServiceEntityDetails();

                // -----------------------------
                // SERVICE BASE INFO
                // -----------------------------

                d.setId(rs.getInt("id"));
                d.setIdService(rs.getInt("id"));

                d.setName(rs.getString("name"));
                d.setNom(rs.getString("name"));

                String type = rs.getString("type");

                d.setDescription(rs.getString("description"));

                d.setBasePrice(rs.getBigDecimal("base_price"));
                d.setPrix(rs.getBigDecimal("base_price"));

                d.setAvailable(rs.getBoolean("is_available"));
                d.setDisponibilite(rs.getBoolean("is_available"));

                Object capacityObj = rs.getObject("capacity");
                Integer capacity = capacityObj == null
                        ? null
                        : ((Number) capacityObj).intValue();

                d.setCapacity(capacity);
                d.setCapacite(capacity == null ? 0 : capacity);

                Object agencyObj = rs.getObject("agency_id");
                Integer agencyId = agencyObj == null
                        ? null
                        : ((Number) agencyObj).intValue();

                d.setAgencyId(agencyId);
                d.setIdAgence(agencyId == null ? 0 : agencyId);

                d.setImageUrl(rs.getString("image_url"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                d.setCreatedAt(
                        createdAt == null
                                ? null
                                : createdAt.toLocalDateTime()
                );

                // -----------------------------
                // HOTEL DETAILS
                // -----------------------------

                Object starsObj = rs.getObject("stars");
                Integer stars = starsObj == null
                        ? null
                        : ((Number) starsObj).intValue();

                d.setNombreEtoiles(stars);
                d.setLocalisation(rs.getString("hotel_location"));
                d.setTypeChambre(rs.getString("room_type"));

                // If type is empty, infer it from hotel data
                if (type == null || type.isBlank()) {
                    type = stars != null ? "HOTEL" : "SERVICE";
                }

                d.setType(type);
                d.setKind(type);

                // -----------------------------
                // DEFAULT VALUES
                // -----------------------------

                d.setQuantity(1);
                d.setQuantite(1);

                d.setOverridePrice(null);
                d.setPrixOverride(null);

                return d;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error findDetailsByIdService: " + e.getMessage(),
                    e
            );
        }
    }
}