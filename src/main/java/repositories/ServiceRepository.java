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

    // ✅ only what you need for OffreForm: list services of the logged-in agency
    public List<ServiceEntity> findAllByAgency(int idAgence) {
        List<ServiceEntity> list = new ArrayList<>();

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
                    ServiceEntity s = new ServiceEntity();
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
    public ServiceEntityDetails findDetailsByIdService(int idService) {
        String sql =
                "SELECT " +
                        "  s.idService, s.nom, s.description, s.prix, s.disponibilite, s.capacite, s.idAgence, " +
                        "  v.numeroVol, v.villeDepart, v.villeArrivee, v.dateDepart, v.dateArrivee, " +
                        "  h.nombreEtoiles, h.localisation, h.typeChambre " +
                        "FROM service s " +
                        "LEFT JOIN vol v ON v.idService = s.idService " +
                        "LEFT JOIN hotel h ON h.idService = s.idService " +
                        "WHERE s.idService = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idService);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                ServiceEntityDetails d = new ServiceEntityDetails();
                d.setIdService(rs.getInt("idService"));
                d.setNom(rs.getString("nom"));
                d.setDescription(rs.getString("description"));
                d.setPrix(rs.getBigDecimal("prix"));

                // if disponibilite is enum('true','false') you may need rs.getString and convert:
                try { d.setDisponibilite(rs.getBoolean("disponibilite")); } catch (Exception ignored) {}

                d.setCapacite(rs.getInt("capacite"));
                d.setIdAgence(rs.getInt("idAgence"));

                // VOL
                String numeroVol = rs.getString("numeroVol");
                d.setNumeroVol(numeroVol);
                d.setVilleDepart(rs.getString("villeDepart"));
                d.setVilleArrivee(rs.getString("villeArrivee"));

                Timestamp tsDep = rs.getTimestamp("dateDepart");
                Timestamp tsArr = rs.getTimestamp("dateArrivee");
                d.setDateDepart(tsDep != null ? tsDep.toLocalDateTime() : null);
                d.setDateArrivee(tsArr != null ? tsArr.toLocalDateTime() : null);

                // HOTEL
                Integer etoiles = (Integer) rs.getObject("nombreEtoiles");
                d.setNombreEtoiles(etoiles);
                d.setLocalisation(rs.getString("localisation"));
                d.setTypeChambre(rs.getString("typeChambre"));

                // kind
                if (numeroVol != null && !numeroVol.isBlank()) d.setKind("VOL");
                else if (etoiles != null) d.setKind("HOTEL");
                else d.setKind("SERVICE");

                // for OfferForm preview: qty = selection count
                d.setQuantite(1);

                return d;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findDetailsByIdService: " + e.getMessage(), e);
        }
    }
}
