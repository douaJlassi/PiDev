package repositories;

import entities.Offre;
import utils.MyDBConnexion;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OffreRepository implements IOffreRepository {

    private final Connection cnx;

    public OffreRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public List<Offre> findAllAdmin() {
        List<Offre> list = new ArrayList<>();
        String sql = "SELECT o.idOffre, o.titre, o.description, o.prixPromo, o.dateDebut, o.dateFin, o.idAgence, o.imageUrl,\n" +
                "       a.nomAgence\n" +
                "FROM offre o\n" +
                "JOIN agence a ON a.idUser = o.idAgence\n" +
                "ORDER BY o.idOffre DESC\n";


        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));

        } catch (SQLException e) {
            throw new RuntimeException("Error findAll offre: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Offre findById(int idOffre) {
        String sql =
                "SELECT o.idOffre, o.titre, o.description, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, a.nomAgence " +
                        "FROM offre o " +
                        "JOIN agence a ON a.idUser = o.idAgence " +
                        "WHERE o.idOffre = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findById offre: " + e.getMessage(), e);
        }
    }

    @Override
    public int insert(Offre o) {
        String sql = "INSERT INTO offre (titre, description, prixPromo, dateDebut, dateFin, idAgence, imageUrl ) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setBigDecimal(3, o.getPrixPromo());
            ps.setDate(4, Date.valueOf(o.getDateDebut()));
            ps.setDate(5, Date.valueOf(o.getDateFin()));
            ps.setInt(6, o.getIdAgence());
            ps.setString(7, o.getImageUrl());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error insert offre: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Offre o) {
        String sql = "UPDATE offre SET titre=?, description=?, prixPromo=?, dateDebut=?, dateFin=?, idAgence=?, imageUrl=? " +
                "WHERE idOffre=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setBigDecimal(3, o.getPrixPromo());
            ps.setDate(4, Date.valueOf(o.getDateDebut()));
            ps.setDate(5, Date.valueOf(o.getDateFin()));
            ps.setInt(6, o.getIdAgence());
            ps.setString(7, o.getImageUrl());   // ✅ imageUrl
            ps.setInt(8, o.getIdOffre());       // ✅ idOffre

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error update offre: " + e.getMessage(), e);
        }
    }


    /**
     * Safe delete: refuse delete if offer is used in lignepanier.
     */
    @Override
    public boolean deleteSafe(int idOffre) {
        if (isUsedInLignePanier(idOffre)) return false;

        String sql = "DELETE FROM offre WHERE idOffre = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error delete offre: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isUsedInLignePanier(int idOffre) {
        String sql = "SELECT COUNT(*) FROM lignepanier WHERE idOffre = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking lignepanier: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Offre> findAllByAgency(int idAgence) {
        List<Offre> list = new ArrayList<>();
        String sql = "SELECT o.idOffre, o.titre, o.description, o.prixPromo, o.dateDebut, o.dateFin, o.idAgence, o.imageUrl,\n" +
                "       a.nomAgence\n" +
                "FROM offre o\n" +
                "JOIN agence a ON a.idUser = o.idAgence\n" +
                "WHERE o.idAgence = ?\n" +
                "ORDER BY o.idOffre DESC\n";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findAllByAgency offre: " + e.getMessage(), e);
        }
        return list;
    }
    @Override
    public boolean isOwnedByAgency(int idOffre, int idAgence) {
        String sql = "SELECT COUNT(*) FROM offre WHERE idOffre=? AND idAgence=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error isOwnedByAgency: " + e.getMessage(), e);
        }
    }

    public boolean deleteSafeForAgency(int idOffre, int idAgence) {
        if (!isOwnedByAgency(idOffre, idAgence)) return false;
        return deleteSafe(idOffre);
    }
    @Override
    public Offre findByIdForAgency(int idOffre, int idAgence) {
        String sql =
                "SELECT o.idOffre, o.titre, o.description, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, a.nomAgence " +
                        "FROM offre o " +
                        "JOIN agence a ON a.idUser = o.idAgence " +
                        "WHERE o.idOffre = ? AND o.idAgence = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findByIdForAgency: " + e.getMessage(), e);
        }
    }
    @Override
    public boolean updateForAgency(Offre o, int idAgence) {
        String sql =
                "UPDATE offre SET titre=?, description=?, prixPromo=?, dateDebut=?, dateFin=?, imageUrl=? " +
                        "WHERE idOffre=? AND idAgence=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setBigDecimal(3, o.getPrixPromo());
            ps.setDate(4, Date.valueOf(o.getDateDebut()));
            ps.setDate(5, Date.valueOf(o.getDateFin()));
            ps.setString(6, o.getImageUrl());
            ps.setInt(7, o.getIdOffre());
            ps.setInt(8, idAgence);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error updateForAgency: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Offre> findAllAdminByAgency(Integer agencyId) {
        List<Offre> list = new ArrayList<>();

        String base =
                "SELECT o.idOffre, o.titre, o.description, o.prixPromo, o.dateDebut, o.dateFin, o.idAgence, o.imageUrl, " +
                        "       a.nomAgence " +
                        "FROM offre o " +
                        "JOIN agence a ON a.idUser = o.idAgence ";

        String sql = (agencyId == null)
                ? base + "ORDER BY o.idOffre DESC"
                : base + "WHERE o.idAgence = ? ORDER BY o.idOffre DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (agencyId != null) ps.setInt(1, agencyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findAllAdminByAgency: " + e.getMessage(), e);
        }
        return list;
    }







    private Offre map(ResultSet rs) throws SQLException {
        Offre o = new Offre();
        o.setIdOffre(rs.getInt("idOffre"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        o.setPrixPromo(rs.getBigDecimal("prixPromo"));

        Date db = rs.getDate("dateDebut");
        Date df = rs.getDate("dateFin");
        o.setDateDebut(db != null ? db.toLocalDate() : LocalDate.now());
        o.setDateFin(df != null ? df.toLocalDate() : LocalDate.now());

        o.setIdAgence(rs.getInt("idAgence"));
        o.setImageUrl(rs.getString("imageUrl"));
        o.setNomAgence(rs.getString("nomAgence"));


        return o;
    }
}
