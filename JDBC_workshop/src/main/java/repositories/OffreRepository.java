package repositories;

import entities.Offre;
import entities.OffreStatus;
import utils.MyDBConnexion;

import java.math.BigDecimal;
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
        String sql =
                "SELECT o.idOffre, o.titre, o.description, o.prixOriginal, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.status, o.imageUrl, a.nomAgence " +
                        "FROM offre o " +
                        "JOIN agence a ON a.idUser = o.idAgence " +
                        "WHERE o.status = 'ACTIVE' " +
                        "ORDER BY o.idOffre DESC";


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
                "SELECT o.idOffre, o.titre, o.description, o.prixOriginal, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, o.status, a.nomAgence " +
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
        String sql =
                "INSERT INTO offre (titre, description, prixOriginal, prixPromo, dateDebut, dateFin, idAgence, imageUrl, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());

// default original to promo if null
            BigDecimal original = (o.getPrixOriginal() != null) ? o.getPrixOriginal() : o.getPrixPromo();
            ps.setBigDecimal(3, original);

            ps.setBigDecimal(4, o.getPrixPromo());
            ps.setDate(5, Date.valueOf(o.getDateDebut()));
            ps.setDate(6, Date.valueOf(o.getDateFin()));
            ps.setInt(7, o.getIdAgence());
            ps.setString(8, o.getImageUrl());

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
        String sql = "UPDATE offre SET titre=?, description=?, prixOriginal=?, prixPromo=?, dateDebut=?, dateFin=?, idAgence=?, imageUrl=? " +
                "WHERE idOffre=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setBigDecimal(3, o.getPrixOriginal());
            ps.setBigDecimal(4, o.getPrixPromo());
            ps.setDate(5, Date.valueOf(o.getDateDebut()));
            ps.setDate(6, Date.valueOf(o.getDateFin()));
            ps.setInt(7, o.getIdAgence());
            ps.setString(8, o.getImageUrl());
            ps.setInt(9, o.getIdOffre());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error update offre: " + e.getMessage(), e);
        }
    }



    @Override
    public boolean deleteSafe(int idOffre) {
    /* NEW LOGIC:
       When the user clicks 'Delete/Archive' in the main grid,
       we ALWAYS just move it to the Archive.
    */
        return archiveOffre(idOffre);
    }

    public boolean archiveOffre(int idOffre) {
        String sql = "UPDATE offre SET status = 'ARCHIVED' WHERE idOffre = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error archiving offer: " + e.getMessage(), e);
        }
    }

    /**
     * Call THIS method from your ArchivedOfferCardController (The Hard Delete button)
     * This follows your logic: Delete if unused, block if used.
     */
    public boolean confirmPermanentDelete(int idOffre) {
        // 1. If it IS used in history, we REFUSE to delete it (keep it archived)
        if (isUsedInLignePanier(idOffre)) {
            System.out.println("Cannot delete: Offer is linked to existing transactions.");
            return false;
        }

        // 2. If it's NOT used, we wipe it from the DB
        String sql = "DELETE FROM offre WHERE idOffre = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error during permanent deletion: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isUsedInLignePanier(int idOffre) {

        String sql = "SELECT COUNT(*) FROM lignepanier WHERE idOffre = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error checking usage: " + e.getMessage(), e);
        }

        return false;
    }

    @Override
    public List<Offre> findAllByAgency(int idAgence) {
        List<Offre> list = new ArrayList<>();

        String sql =
                "SELECT o.idOffre, o.titre, o.description, o.prixOriginal, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, o.status, a.nomAgence " +
                        "FROM offre o " +
                        "JOIN agence a ON a.idUser = o.idAgence " +
                        "WHERE o.idAgence = ? AND o.status = 'ACTIVE' " +
                        "ORDER BY o.idOffre DESC";

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
                "SELECT o.idOffre, o.titre, o.description, o.prixOriginal, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, o.status, a.nomAgence " +
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
                "UPDATE offre SET titre=?, description=?, prixOriginal=?, prixPromo=?, dateDebut=?, dateFin=?, imageUrl=? " +
                        "WHERE idOffre=? AND idAgence=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setBigDecimal(3, o.getPrixOriginal());
            ps.setBigDecimal(4, o.getPrixPromo());
            ps.setDate(5, Date.valueOf(o.getDateDebut()));
            ps.setDate(6, Date.valueOf(o.getDateFin()));
            ps.setString(7, o.getImageUrl());
            ps.setInt(8, o.getIdOffre());
            ps.setInt(9, idAgence);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error updateForAgency: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Offre> findAllAdminByAgency(Integer agencyId) {
        List<Offre> list = new ArrayList<>();

        String base =
                "SELECT o.idOffre, o.titre, o.description, o.prixPromo, o.dateDebut, o.prixOriginal, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, o.status, a.nomAgence " +
                        "FROM offre o " +
                        "JOIN agence a ON a.idUser = o.idAgence " +
                        "WHERE o.status='ACTIVE' ";

        String sql = (agencyId == null)
                ? base + "ORDER BY o.idOffre DESC"
                : base + "AND o.idAgence = ? ORDER BY o.idOffre DESC";

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
    public boolean archiveForAgency(int idOffre, int idAgence) {
        if (!isOwnedByAgency(idOffre, idAgence)) return false;
        if (isUsedInLignePanier(idOffre)) return false; // keep your integrity rule

        String sql = "UPDATE offre SET status='ARCHIVED' WHERE idOffre=? AND idAgence=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idAgence);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error archive offer: " + e.getMessage(), e);
        }
    }

    public boolean restoreForAgency(int idOffre, int idAgence) {
        if (!isOwnedByAgency(idOffre, idAgence)) return false;

        String sql = "UPDATE offre SET status='ACTIVE' WHERE idOffre=? AND idAgence=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idAgence);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error restore offer: " + e.getMessage(), e);
        }
    }

    public List<Offre> findArchivedByAgency(int idAgence) {
        List<Offre> list = new ArrayList<>();
        String sql =
                "SELECT o.idOffre, o.titre, o.description, o.prixOriginal, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, o.status, a.nomAgence " +
                        "FROM offre o JOIN agence a ON a.idUser=o.idAgence " +
                        "WHERE o.idAgence=? AND o.status='ARCHIVED' " +
                        "ORDER BY o.idOffre DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findArchivedByAgency: " + e.getMessage(), e);
        }
        return list;
    }
    @Override
    public boolean deleteHardAdmin(int idOffre) {
        String sql = "DELETE FROM offre WHERE idOffre=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleteHardAdmin: " + e.getMessage(), e);
        }
    }



    private Offre map(ResultSet rs) throws SQLException {
        Offre o = new Offre();
        o.setIdOffre(rs.getInt("idOffre"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        o.setPrixOriginal(rs.getBigDecimal("prixOriginal"));
        o.setPrixPromo(rs.getBigDecimal("prixPromo"));

        Date db = rs.getDate("dateDebut");
        Date df = rs.getDate("dateFin");
        o.setDateDebut(db != null ? db.toLocalDate() : LocalDate.now());
        o.setDateFin(df != null ? df.toLocalDate() : LocalDate.now());

        o.setIdAgence(rs.getInt("idAgence"));
        o.setImageUrl(rs.getString("imageUrl"));
        o.setNomAgence(rs.getString("nomAgence"));
        try {
            o.setStatus(OffreStatus.valueOf(rs.getString("status")));
        } catch (SQLException ex) {
            o.setStatus(OffreStatus.ACTIVE);
        }


        return o;
    }
    @Override
    public List<Offre> searchActiveOffers(entities.OfferFilter f) {
        List<Offre> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT o.idOffre, o.titre, o.description, o.prixOriginal, o.prixPromo, o.dateDebut, o.dateFin, " +
                        "       o.idAgence, o.imageUrl, o.status, a.nomAgence " +
                        "FROM offre o " +
                        "JOIN agence a ON a.idUser = o.idAgence " +
                        "WHERE o.status='ACTIVE' "
        );

        List<Object> params = new ArrayList<>();

        // keyword
        if (f != null && f.getKeyword() != null && !f.getKeyword().trim().isEmpty()) {
            sql.append("AND (o.titre LIKE ? OR a.nomAgence LIKE ?) ");
            String like = "%" + f.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
        }

        // min price
        if (f != null && f.getMinPrice() != null) {
            sql.append("AND o.prixPromo >= ? ");
            params.add(f.getMinPrice());
        }

        // max price
        if (f != null && f.getMaxPrice() != null) {
            sql.append("AND o.prixPromo <= ? ");
            params.add(f.getMaxPrice());
        }

        // date inside offer range
        if (f != null && f.getSelectedDate() != null) {
            sql.append("AND o.dateDebut <= ? AND o.dateFin >= ? ");
            params.add(Date.valueOf(f.getSelectedDate()));
            params.add(Date.valueOf(f.getSelectedDate()));
        }

        // agencies IN (...)
        if (f != null && f.getAgencyIds() != null && !f.getAgencyIds().isEmpty()) {
            sql.append("AND o.idAgence IN (");
            sql.append(String.join(",", java.util.Collections.nCopies(f.getAgencyIds().size(), "?")));
            sql.append(") ");
            for (Integer id : f.getAgencyIds()) params.add(id);
        }

        sql.append("ORDER BY o.idOffre DESC");

        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof BigDecimal bd) ps.setBigDecimal(i + 1, bd);
                else if (p instanceof Integer in) ps.setInt(i + 1, in);
                else if (p instanceof Date d) ps.setDate(i + 1, d);
                else ps.setString(i + 1, String.valueOf(p));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searchActiveOffers: " + e.getMessage(), e);
        }

        return list;
    }

}
