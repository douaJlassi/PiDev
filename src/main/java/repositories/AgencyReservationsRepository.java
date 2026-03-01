package repositories;

import entities.*;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgencyReservationsRepository implements IAgencyReservationsRepository {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();

    @Override
    public List<AgencyReservationLine> findLinesForAgency(int idAgence) {

        String sql = """
            SELECT 
              r.idReservation        AS idReservation,
              r.idClient             AS idClient,
              r.statut               AS reservationStatus,

              lp.idOffre             AS idOffre,
              o.titre                AS offerTitle,
              lp.prixUnitaire        AS prixFinal,

              lp.agencyStatus        AS agencyStatus,
              lp.refusalReason       AS refusalReason,
              lp.agencyDecisionAt    AS agencyDecisionAt
            FROM lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            WHERE o.idAgence = ?
            ORDER BY lp.idReservation DESC
        """;

        List<AgencyReservationLine> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AgencyReservationLine x = new AgencyReservationLine();

                    x.setIdReservation(rs.getInt("idReservation"));
                    x.setIdClient(rs.getInt("idClient"));

                    String resStatus = rs.getString("reservationStatus");
                    x.setReservationStatut(resStatus == null ? null : ReservationStatut.valueOf(resStatus));

                    x.setIdOffre(rs.getInt("idOffre"));
                    x.setOfferTitle(rs.getString("offerTitle"));
                    x.setPrixFinal(rs.getBigDecimal("prixFinal"));

                    String agStatus = rs.getString("agencyStatus");
                    x.setAgencyStatut(agStatus == null ? AgencyStatut.ENATTENTE : AgencyStatut.valueOf(agStatus));

                    x.setRefusalReason(rs.getString("refusalReason"));

                    Timestamp decisionTs = rs.getTimestamp("agencyDecisionAt");
                    if (decisionTs != null) x.setAgencyDecisionAt(decisionTs.toLocalDateTime());

                    list.add(x);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Approve ONLY the line (no email here).
     * Immutable after CONFIRME due to r.statut='ENATTENTE'.
     */
    @Override
    public boolean approveLine(int idAgence, int idReservation, int idOffre) {

        String sql = """
            UPDATE lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            SET lp.agencyStatus = 'APPROUVEE',
                lp.agencyDecisionAt = NOW(),
                lp.refusalReason = NULL
            WHERE lp.idReservation = ?
              AND lp.idOffre = ?
              AND o.idAgence = ?
              AND r.statut = 'ENATTENTE'
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idOffre);
            ps.setInt(3, idAgence);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reject line with a reason (stored).
     * Immutable after CONFIRME due to r.statut='ENATTENTE'.
     */
    @Override
    public boolean rejectLine(int idAgence, int idReservation, int idOffre, String reason) {

        String sql = """
            UPDATE lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            SET lp.agencyStatus = 'REFUSEE',
                lp.agencyDecisionAt = NOW(),
                lp.refusalReason = ?
            WHERE lp.idReservation = ?
              AND lp.idOffre = ?
              AND o.idAgence = ?
              AND r.statut = 'ENATTENTE'
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, (reason == null || reason.trim().isEmpty()) ? "Not available" : reason.trim());
            ps.setInt(2, idReservation);
            ps.setInt(3, idOffre);
            ps.setInt(4, idAgence);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * True if all lines are approved.
     * Handles NULL safely.
     */
    public boolean isReservationFullyApproved(int idReservation) {
        String sql = """
            SELECT COUNT(*) 
            FROM lignepanier 
            WHERE idReservation = ? AND (agencyStatus IS NULL OR agencyStatus <> 'APPROUVEE')
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Confirm reservation ONLY if it is fully approved.
     * Returns true ONLY if status actually changed ENATTENTE -> CONFIRME now.
     * This is what prevents duplicate emails (no extra column needed).
     */
    public boolean confirmReservationIfFullyApproved(int idReservation) {
        if (!isReservationFullyApproved(idReservation)) return false;

        String sql = """
            UPDATE reservation
            SET statut='CONFIRME', modePaiement='CASH', dateReservation=NOW()
            WHERE idReservation=? AND statut='ENATTENTE'
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Convenience: approve line then attempt confirmation.
     * Returns true ONLY if reservation became CONFIRME now.
     */
    public boolean approveLineAndConfirmIfReady(int idAgence, int idReservation, int idOffre) {
        boolean ok = approveLine(idAgence, idReservation, idOffre);
        if (!ok) return false;
        return confirmReservationIfFullyApproved(idReservation);
    }
}