package repositories;

import entities.ReservationSummary;
import utils.MyDBConnexion;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class ReservationRepository {

    private final Connection cnx;

    public ReservationRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }
    private String agencyStatus;


    public int getOrCreateDraftCart(int idClient) {
        // 1) find existing ENATTENTE reservation for client
        String find = "SELECT idReservation FROM reservation WHERE idClient=? AND statut='PANIER'";
        try (PreparedStatement ps = cnx.prepareStatement(find)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error find draft cart: " + e.getMessage(), e);
        }

        // 2) create new ENATTENTE reservation
        String insert =
                "INSERT INTO reservation(dateReservation, statut, modePaiement, montantTotal, idClient) " +
                        "VALUES (NOW(), 'PANIER', 'CASH', 0, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idClient);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error create draft cart: " + e.getMessage(), e);
        }
    }


    public Integer findDraftCartId(int idClient) {
        String sql =
                "SELECT idReservation FROM reservation " +
                        "WHERE idClient = ? AND statut = 'PANIER' " +
                        "ORDER BY dateReservation DESC LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findDraftCartId: " + e.getMessage(), e);
        }
    }

    /**
     * Recalculate and update reservation.montantTotal from lignepanier.
     */
    public void recomputeTotal(int idReservation) {
        String sumSql = "SELECT COALESCE(SUM(prixUnitaire),0) FROM lignepanier WHERE idReservation = ?";
        String updSql = "UPDATE reservation SET montantTotal = ? WHERE idReservation = ?";

        try (PreparedStatement psSum = cnx.prepareStatement(sumSql)) {
            psSum.setInt(1, idReservation);

            BigDecimal total;
            try (ResultSet rs = psSum.executeQuery()) {
                rs.next();
                total = rs.getBigDecimal(1);
            }

            try (PreparedStatement psUpd = cnx.prepareStatement(updSql)) {
                psUpd.setBigDecimal(1, total);
                psUpd.setInt(2, idReservation);
                psUpd.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error recomputeTotal: " + e.getMessage(), e);
        }
    }
    public java.math.BigDecimal getTotal(int idReservation) {
        String sql = "SELECT montantTotal FROM reservation WHERE idReservation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
                return java.math.BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getTotal: " + e.getMessage(), e);
        }
    }
    public boolean checkout(int idReservation, int idClient, String modePaiement) {
        String sql =
                "UPDATE reservation " +
                        "SET statut='CONFIRME', modePaiement=?, dateReservation=NOW() " +
                        "WHERE idReservation=? AND idClient=? AND statut='ENATTENTE'";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, modePaiement == null ? "CASH" : modePaiement);
            ps.setInt(2, idReservation);
            ps.setInt(3, idClient);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error checkout: " + e.getMessage(), e);
        }
    }
    public List<ReservationSummary> findByClient(int idClient) {
        List<ReservationSummary> list = new java.util.ArrayList<>();

        String sql =
                "SELECT idReservation, dateReservation, statut, modePaiement, montantTotal " +
                        "FROM reservation " +
                        "WHERE idClient = ? AND statut <> 'PANIER' " +
                        "ORDER BY dateReservation DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationSummary r = new ReservationSummary();
                    r.setIdReservation(rs.getInt("idReservation"));

                    Timestamp ts = rs.getTimestamp("dateReservation");
                    r.setDateReservation(ts != null ? ts.toLocalDateTime() : null);

                    r.setStatut(entities.ReservationStatut.valueOf(rs.getString("statut")));
                    r.setModePaiement(rs.getString("modePaiement"));
                    r.setMontantTotal(rs.getBigDecimal("montantTotal"));

                    list.add(r);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findByClient reservations: " + e.getMessage(), e);
        }

        return list;
    }
    /*public List<entities.CartItem> findReservationItems(int idReservation, int idClient) {
        List<entities.CartItem> list = new java.util.ArrayList<>();

        String sql =
                "SELECT lp.idReservation, lp.idOffre, lp.prixUnitaire, lp.agencyStatus, o.titre, o.imageUrl\n" +
                        "FROM lignepanier lp\n" +
                        "JOIN offre o ON o.idOffre = lp.idOffre\n" +
                        "JOIN reservation r ON r.idReservation = lp.idReservation\n" +
                        "WHERE lp.idReservation = ? AND r.idClient = ?\n" +
                        "ORDER BY lp.idOffre DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entities.CartItem it = new entities.CartItem();
                    it.setIdReservation(rs.getInt("idReservation"));
                    it.setIdOffre(rs.getInt("idOffre"));
                    it.setPrixUnitaire(rs.getBigDecimal("prixUnitaire"));
                    it.setTitre(rs.getString("titre"));
                    it.setImageUrl(rs.getString("imageUrl"));
                    it.setAgencyStatus(rs.getString("agencyStatus"));
                    list.add(it);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findReservationItems: " + e.getMessage(), e);
        }

        return list;
    }*/
    public List<entities.CartItem> findReservationItems(int idReservation, int idClient) {
        List<entities.CartItem> list = new java.util.ArrayList<>();

        String sql =
                "SELECT lp.idReservation, lp.idOffre, lp.prixUnitaire, lp.agencyStatus, lp.refusalReason, lp.agencyDecisionAt, " +
                        "       o.titre, o.imageUrl, a.nomAgence " +
                        "FROM lignepanier lp " +
                        "JOIN offre o ON o.idOffre = lp.idOffre " +
                        "JOIN agence a ON a.idUser = o.idAgence " +
                        "JOIN reservation r ON r.idReservation = lp.idReservation " +
                        "WHERE lp.idReservation = ? AND r.idClient = ? " +
                        "ORDER BY lp.idOffre DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entities.CartItem it = new entities.CartItem();
                    it.setIdReservation(rs.getInt("idReservation"));
                    it.setIdOffre(rs.getInt("idOffre"));
                    it.setPrixUnitaire(rs.getBigDecimal("prixUnitaire"));
                    it.setTitre(rs.getString("titre"));
                    it.setImageUrl(rs.getString("imageUrl"));
                    it.setAgencyStatus(rs.getString("agencyStatus"));
                    it.setNomAgence(rs.getString("nomAgence"));
                    it.setRefusalReason(rs.getString("refusalReason"));
                    Timestamp ts = rs.getTimestamp("agencyDecisionAt");
                    if (ts != null) it.setAgencyDecisionAt(ts.toLocalDateTime());
                    list.add(it);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findReservationItems: " + e.getMessage(), e);
        }

        return list;
    }
    public boolean confirmReservation(int idReservation, String modePaiement) {
        String sql = "UPDATE reservation SET statut='CONFIRME', modePaiement=?, dateReservation=NOW() WHERE idReservation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, modePaiement == null ? "CASH" : modePaiement);
            ps.setInt(2, idReservation);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error confirmReservation: " + e.getMessage(), e);
        }
    }
    public boolean requestBooking(int idReservation, int idClient) {
        String sql = """
        UPDATE reservation
        SET statut='ENATTENTE', dateReservation=NOW()
        WHERE idReservation=? AND idClient=? AND statut='PANIER'
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idClient);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error requestBooking: " + e.getMessage(), e);
        }
    }
    public boolean isFullyApprovedByAgencies(int idReservation) {
        String sql = """
        SELECT COUNT(*) 
        FROM lignepanier
        WHERE idReservation=? AND agencyStatus <> 'APPROUVEE'
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error isFullyApprovedByAgencies: " + e.getMessage(), e);
        }
    }
    /*public boolean finalizeBooking(int idReservation, int idClient, String modePaiement) {
        // safety: must be request stage
        String ensureRequest = """
        SELECT description, statut FROM reservation
        WHERE idReservation=? AND idClient=?
    """;

        try (PreparedStatement ps = cnx.prepareStatement(ensureRequest)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                String desc = rs.getString("description");
                String st = rs.getString("statut");

                if (!"ENATTENTE".equals(st)) return false;
                if (!"REQUEST".equalsIgnoreCase(desc)) return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finalizeBooking check: " + e.getMessage(), e);
        }

        if (!isFullyApprovedByAgencies(idReservation)) return false;

        String sql = """
        UPDATE reservation
        SET statut='CONFIRME', modePaiement=?, dateReservation=NOW()
        WHERE idReservation=? AND idClient=? AND statut='ENATTENTE' AND description='REQUEST'
    """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, modePaiement == null ? "CASH" : modePaiement);
            ps.setInt(2, idReservation);
            ps.setInt(3, idClient);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error finalizeBooking: " + e.getMessage(), e);
        }
    }*/
    public boolean cancelReservation(int idReservation, int idClient) {
        String sql = """
        UPDATE reservation
        SET statut='ANNULE'
        WHERE idReservation=? AND idClient=? AND statut IN ('PANIER','ENATTENTE')
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idClient);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error cancelReservation: " + e.getMessage(), e);
        }
    }
    public boolean removeLine(int idReservation, int idClient, int idOffre) {
        // ensure ownership via join to reservation
        String sql = """
        DELETE lp
        FROM lignepanier lp
        JOIN reservation r ON r.idReservation = lp.idReservation
        WHERE lp.idReservation=? AND lp.idOffre=? AND r.idClient=? AND r.statut IN ('PANIER','ENATTENTE')
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idOffre);
            ps.setInt(3, idClient);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) recomputeTotal(idReservation);
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Error removeLine: " + e.getMessage(), e);
        }
    }
    public void resetAllLineStatusesToPending(int idReservation, int idClient) {
        String sql = """
        UPDATE lignepanier lp
        JOIN reservation r ON r.idReservation = lp.idReservation
        SET lp.agencyStatus='ENATTENTE'
        WHERE lp.idReservation=? AND r.idClient=? AND r.statut='ENATTENTE'
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idClient);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error resetAllLineStatusesToPending: " + e.getMessage(), e);
        }
    }
    public java.sql.Timestamp getEmailSentAt(int idReservation) {
        String sql = "SELECT emailSentAt FROM reservation WHERE idReservation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getTimestamp(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getEmailSentAt: " + e.getMessage(), e);
        }
    }

    public void markEmailSentNow(int idReservation) {
        String sql = "UPDATE reservation SET emailSentAt = NOW() WHERE idReservation=? AND emailSentAt IS NULL";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error markEmailSentNow: " + e.getMessage(), e);
        }
    }


}
