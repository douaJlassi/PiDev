package repositories;

import entities.Service;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRepository {

    private final Connection cnx;

    public ServiceRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    // ✅ only what you need for OffreForm: list services of the logged-in agency
    public List<Service> findAllByAgency(int idAgence) {
        List<Service> list = new ArrayList<>();

        String sql =
                "SELECT s.idService, s.nom, " +
                        "       CASE " +
                        "           WHEN v.idService IS NOT NULL THEN 'VOL' " +
                        "           WHEN h.idService IS NOT NULL THEN 'HOTEL' " +
                        "           ELSE 'SERVICE' " +
                        "       END AS kind " +
                        "FROM service s " +
                        "LEFT JOIN vol v ON v.idService = s.idService " +
                        "LEFT JOIN hotel h ON h.idService = s.idService " +
                        "WHERE s.idAgence = ? " +
                        "ORDER BY s.idService DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Service s = new Service();
                    s.setIdService(rs.getInt("idService"));
                    s.setNom(rs.getString("nom"));
                    s.setKind(rs.getString("kind"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findAllByAgency service: " + e.getMessage(), e);
        }

        return list;
    }
}
