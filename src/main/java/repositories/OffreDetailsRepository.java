package repositories;

import entities.ServiceDetails;
import utils.MyDBConnexion;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OffreDetailsRepository {

    private final Connection cnx;

    public OffreDetailsRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }


    public List<ServiceDetails> findServicesDetailsByOffre(int idOffre) {
        List<ServiceDetails> list = new ArrayList<>();

        String sql =
                "SELECT " +
                        "  s.idService, s.nom, s.description, s.prix, s.disponibilite, s.capacite, s.idAgence, " +
                        "  os.quantite, os.prixOverride, " +
                        "  v.numeroVol, v.villeDepart, v.villeArrivee, v.dateDepart, v.dateArrivee, " +
                        "  h.nombreEtoiles, h.localisation, h.typeChambre " +
                        "FROM offre_service os " +
                        "JOIN service s ON s.idService = os.idService " +
                        "LEFT JOIN vol v ON v.idService = s.idService " +
                        "LEFT JOIN hotel h ON h.idService = s.idService " +
                        "WHERE os.idOffre = ? " +
                        "ORDER BY s.idService";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServiceDetails d = new ServiceDetails();

                    // ---- base service
                    d.setIdService(rs.getInt("idService"));
                    d.setNom(rs.getString("nom"));
                    d.setDescription(rs.getString("description"));
                    d.setPrix(rs.getBigDecimal("prix"));
                    d.setDisponibilite(rs.getBoolean("disponibilite"));
                    d.setCapacite(rs.getInt("capacite"));
                    d.setIdAgence(rs.getInt("idAgence"));

                    // ---- pivot
                    d.setQuantite(rs.getInt("quantite"));
                    d.setPrixOverride(rs.getBigDecimal("prixOverride")); // can be null

                    // ---- VOL
                    String numeroVol = rs.getString("numeroVol"); // null if not a vol
                    d.setNumeroVol(numeroVol);
                    d.setVilleDepart(rs.getString("villeDepart"));
                    d.setVilleArrivee(rs.getString("villeArrivee"));

                    Timestamp tsDep = rs.getTimestamp("dateDepart");
                    Timestamp tsArr = rs.getTimestamp("dateArrivee");
                    d.setDateDepart(tsDep != null ? tsDep.toLocalDateTime() : null);
                    d.setDateArrivee(tsArr != null ? tsArr.toLocalDateTime() : null);

                    // ---- HOTEL

                    Integer etoiles = (Integer) rs.getObject("nombreEtoiles");
                    d.setNombreEtoiles(etoiles);
                    d.setLocalisation(rs.getString("localisation"));
                    d.setTypeChambre(rs.getString("typeChambre"));

                    // ---- infer kind
                    if (numeroVol != null && !numeroVol.isBlank()) {
                        d.setKind("VOL");
                    } else if (etoiles != null) {
                        d.setKind("HOTEL");
                    } else {
                        d.setKind("SERVICE");
                    }

                    list.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findServicesDetailsByOffre: " + e.getMessage(), e);
        }

        return list;
    }

}
