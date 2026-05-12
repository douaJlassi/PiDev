package repositories;

import entities.*;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgencyReservationsRepository
        implements IAgencyReservationsRepository {

    private final Connection cnx =
            MyDBConnexion
                    .getInstance()
                    .getConnection();

    @Override
    public List<AgencyReservationLine> findLinesForAgency(
            int agencyUserId
    ) {

        String sql = """
            SELECT
                r.id                    AS reservationId,
                r.user_id               AS clientId,
                r.status                AS reservationStatus,

                u.last_name             AS nom,
                u.name                  AS prenom,
                u.email                 AS email,

                o.id                    AS offerId,
                o.title                 AS offerTitle,

                r.total_amount          AS prixFinal,

                r.payment_status        AS paymentStatus,
                r.special_request       AS specialRequest,
                r.updated_at            AS updatedAt

            FROM reservation r

            JOIN offer o
                ON o.id = r.offer_id

            JOIN user u
                ON u.id = r.user_id

            WHERE o.user_id = ?

            ORDER BY r.id DESC
        """;

        List<AgencyReservationLine> list =
                new ArrayList<>();

        try (
                PreparedStatement ps =
                        cnx.prepareStatement(sql)
        ) {

            ps.setInt(1, agencyUserId);

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    AgencyReservationLine x =
                            new AgencyReservationLine();

                    x.setIdReservation(
                            rs.getInt("reservationId")
                    );

                    x.setIdClient(
                            rs.getInt("clientId")
                    );

                    String resStatus =
                            rs.getString(
                                    "reservationStatus"
                            );

                    if (resStatus != null) {

                        try {

                            x.setReservationStatut(
                                    ReservationStatut.valueOf(
                                            resStatus.toUpperCase()
                                    )
                            );

                        } catch (Exception ignored) {
                        }
                    }

                    x.setIdOffre(
                            rs.getInt("offerId")
                    );

                    x.setOfferTitle(
                            rs.getString("offerTitle")
                    );

                    x.setPrixFinal(
                            rs.getBigDecimal("prixFinal")
                    );

                    x.setRefusalReason(
                            rs.getString("specialRequest")
                    );

                    String nom =
                            rs.getString("nom");

                    String prenom =
                            rs.getString("prenom");

                    String email =
                            rs.getString("email");

                    String fullName =
                            (
                                    (prenom == null ? "" : prenom)
                                            + " "
                                            + (nom == null ? "" : nom)
                            ).trim();

                    if (fullName.isEmpty()) {
                        fullName =
                                email == null
                                        ? "Client"
                                        : email;
                    }

                    x.setClientName(fullName);

                    Timestamp updatedTs =
                            rs.getTimestamp(
                                    "updatedAt"
                            );

                    if (updatedTs != null) {

                        x.setAgencyDecisionAt(
                                updatedTs.toLocalDateTime()
                        );
                    }

                    list.add(x);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return list;
    }

    @Override
    public boolean approveLine(
            int agencyUserId,
            int reservationId,
            int offerId
    ) {

        String sql = """
            UPDATE reservation r
            JOIN offer o
                ON o.id = r.offer_id

            SET
                r.status = 'CONFIRMED',
                r.updated_at = NOW()

            WHERE r.id = ?
              AND r.offer_id = ?
              AND o.user_id = ?
              AND r.status = 'PENDING'
        """;

        try (
                PreparedStatement ps =
                        cnx.prepareStatement(sql)
        ) {

            ps.setInt(1, reservationId);
            ps.setInt(2, offerId);
            ps.setInt(3, agencyUserId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    @Override
    public boolean rejectLine(
            int agencyUserId,
            int reservationId,
            int offerId,
            String reason
    ) {

        String sql = """
            UPDATE reservation r
            JOIN offer o
                ON o.id = r.offer_id

            SET
                r.status = 'REJECTED',
                r.special_request = ?,
                r.updated_at = NOW()

            WHERE r.id = ?
              AND r.offer_id = ?
              AND o.user_id = ?
              AND r.status = 'PENDING'
        """;

        try (
                PreparedStatement ps =
                        cnx.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    (
                            reason == null ||
                                    reason.trim().isEmpty()
                    )
                            ? "Rejected"
                            : reason.trim()
            );

            ps.setInt(2, reservationId);
            ps.setInt(3, offerId);
            ps.setInt(4, agencyUserId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean isReservationFullyApproved(
            int reservationId
    ) {

        String sql = """
            SELECT status
            FROM reservation
            WHERE id = ?
        """;

        try (
                PreparedStatement ps =
                        cnx.prepareStatement(sql)
        ) {

            ps.setInt(1, reservationId);

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                if (rs.next()) {

                    String status =
                            rs.getString("status");

                    return
                            status != null &&
                                    status.equalsIgnoreCase(
                                            "CONFIRMED"
                                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }

    public boolean confirmReservationIfFullyApproved(
            int reservationId
    ) {

        String sql = """
            UPDATE reservation
            SET
                status = 'CONFIRMED',
                updated_at = NOW()

            WHERE id = ?
              AND status = 'PENDING'
        """;

        try (
                PreparedStatement ps =
                        cnx.prepareStatement(sql)
        ) {

            ps.setInt(1, reservationId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean approveLineAndConfirmIfReady(
            int agencyUserId,
            int reservationId,
            int offerId
    ) {

        return approveLine(
                agencyUserId,
                reservationId,
                offerId
        );
    }
}