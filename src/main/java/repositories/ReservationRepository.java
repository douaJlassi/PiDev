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


    public int getOrCreateDraftCart(int idClient) {
        // 1) find existing ENATTENTE reservation for client
        String find = "SELECT idReservation FROM reservation WHERE idClient=? AND statut='ENATTENTE' LIMIT 1";
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
                "INSERT INTO reservation(dateReservation, statut, modePaiement, montantTotal, description, idClient) " +
                        "VALUES (NOW(), 'ENATTENTE', 'N/A', 0, 'CART', ?)";

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
                "SELECT idReservation " +
                        "FROM reservation " +
                        "WHERE idClient = ? AND statut = 'ENATTENTE' " +
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
    public List<entities.ReservationSummary> findByClient(int idClient) {
        List<ReservationSummary> list = new java.util.ArrayList<>();

        String sql =
                "SELECT idReservation, dateReservation, statut, modePaiement, montantTotal " +
                        "FROM reservation " +
                        "WHERE idClient = ? AND NOT (statut='ENATTENTE' AND description='CART') " +
                        "ORDER BY dateReservation DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entities.ReservationSummary r = new entities.ReservationSummary();
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
    public List<entities.CartItem> findReservationItems(int idReservation, int idClient) {
        List<entities.CartItem> list = new java.util.ArrayList<>();

        String sql =
                "SELECT lp.idReservation, lp.idOffre, lp.prixUnitaire, o.titre, o.imageUrl " +
                        "FROM lignepanier lp " +
                        "JOIN offre o ON o.idOffre = lp.idOffre " +
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


}
