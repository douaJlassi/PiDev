package repositories;

import entities.ServiceEntityDetails;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreDetailsRepository {

    private final Connection cnx;

    public OffreDetailsRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public List<ServiceEntityDetails> findServicesDetailsByOffre(int offerId) {

        List<ServiceEntityDetails> list = new ArrayList<>();

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

                        " os.id AS offerServiceId, " +
                        " os.created_at AS attachedAt, " +

                        " h.stars, " +
                        " h.location AS hotel_location, " +
                        " h.room_type " +

                        "FROM offer_service os " +
                        "JOIN service s ON s.id = os.service_id " +
                        "LEFT JOIN hotel h ON h.id = s.id " +
                        "WHERE os.offer_id = ? " +
                        "ORDER BY s.id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, offerId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    ServiceEntityDetails d = new ServiceEntityDetails();

                    d.setId(rs.getInt("id"));
                    d.setName(rs.getString("name"));

                    String type = rs.getString("type");

                    d.setDescription(rs.getString("description"));
                    d.setBasePrice(rs.getBigDecimal("base_price"));
                    d.setAvailable(rs.getBoolean("is_available"));

                    Object capacityObj = rs.getObject("capacity");
                    d.setCapacity(
                            capacityObj == null
                                    ? null
                                    : ((Number) capacityObj).intValue()
                    );

                    Object agencyObj = rs.getObject("agency_id");
                    d.setAgencyId(
                            agencyObj == null
                                    ? null
                                    : ((Number) agencyObj).intValue()
                    );

                    d.setImageUrl(rs.getString("image_url"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    d.setCreatedAt(
                            createdAt == null
                                    ? null
                                    : createdAt.toLocalDateTime()
                    );

                    // HOTEL DETAILS
                    Object starsObj = rs.getObject("stars");
                    Integer stars = starsObj == null
                            ? null
                            : ((Number) starsObj).intValue();

                    d.setNombreEtoiles(stars);
                    d.setLocalisation(rs.getString("hotel_location"));
                    d.setTypeChambre(rs.getString("room_type"));

                    // If service.type is empty, infer HOTEL if hotel data exists
                    if (type == null || type.isBlank()) {
                        type = stars != null ? "HOTEL" : "SERVICE";
                    }

                    d.setType(type);
                    d.setKind(type);

                    // offer_service has no quantity / override price
                    d.setQuantity(1);
                    d.setOverridePrice(null);

                    // Compatibility setters
                    d.setIdService(rs.getInt("id"));
                    d.setNom(rs.getString("name"));
                    d.setPrix(rs.getBigDecimal("base_price"));
                    d.setDisponibilite(rs.getBoolean("is_available"));

                    Object capObj = rs.getObject("capacity");
                    d.setCapacite(
                            capObj == null
                                    ? 0
                                    : ((Number) capObj).intValue()
                    );

                    Object agObj = rs.getObject("agency_id");
                    d.setIdAgence(
                            agObj == null
                                    ? 0
                                    : ((Number) agObj).intValue()
                    );

                    d.setQuantite(1);
                    d.setPrixOverride(null);

                    list.add(d);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Error findServicesDetailsByOffre: " + e.getMessage(),
                    e
            );
        }

        return list;
    }
}