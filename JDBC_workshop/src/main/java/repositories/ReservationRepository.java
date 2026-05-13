package repositories;

import entities.CartItem;
import entities.ReservationStatut;
import entities.ReservationSummary;
import utils.MyDBConnexion;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {

    private final Connection cnx;

    public ReservationRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public int getOrCreateDraftCart(int userId) {
        String find = """
            SELECT id
            FROM reservation
            WHERE user_id = ?
              AND status = 'CART'
            ORDER BY reservation_date DESC
            LIMIT 1
        """;

        try (PreparedStatement ps = cnx.prepareStatement(find)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error find draft cart: " + e.getMessage(), e);
        }

        String insert = """
            INSERT INTO reservation
                (reservation_date, status, payment_status, total_amount, user_id)
            VALUES
                (NOW(), 'CART', 'CASH', 0, ?)
        """;

        try (PreparedStatement ps = cnx.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error create draft cart: " + e.getMessage(), e);
        }
    }

    public Integer findDraftCartId(int userId) {
        String sql = """
            SELECT id
            FROM reservation
            WHERE user_id = ?
              AND status = 'CART'
            ORDER BY reservation_date DESC
            LIMIT 1
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error findDraftCartId: " + e.getMessage(), e);
        }
    }

    public void recomputeTotal(int reservationId) {
        String sql = """
            UPDATE reservation r
            LEFT JOIN offer o ON o.id = r.offer_id
            SET r.total_amount = COALESCE(o.promo_price, r.total_amount, 0)
            WHERE r.id = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error recomputeTotal: " + e.getMessage(), e);
        }
    }

    public BigDecimal getTotal(int reservationId) {
        String sql = """
            SELECT total_amount
            FROM reservation
            WHERE id = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total_amount");
                    return total == null ? BigDecimal.ZERO : total;
                }

                return BigDecimal.ZERO;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error getTotal: " + e.getMessage(), e);
        }
    }

    public boolean checkout(int reservationId, int userId, String paymentStatus) {
        String sql = """
            UPDATE reservation
            SET status = 'PENDING',
                payment_status = ?,
                reservation_date = NOW(),
                updated_at = NOW()
            WHERE id = ?
              AND user_id = ?
              AND status = 'CART'
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, paymentStatus == null ? "CASH" : paymentStatus);
            ps.setInt(2, reservationId);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error checkout: " + e.getMessage(), e);
        }
    }

    public List<ReservationSummary> findByClient(int userId) {
        List<ReservationSummary> list = new ArrayList<>();

        String sql = """
            SELECT
                id,
                reservation_date,
                status,
                payment_status,
                total_amount
            FROM reservation
            WHERE user_id = ?
              AND status <> 'CART'
            ORDER BY reservation_date DESC
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationSummary r = new ReservationSummary();

                    r.setIdReservation(rs.getInt("id"));

                    Timestamp ts = rs.getTimestamp("reservation_date");
                    r.setDateReservation(ts != null ? ts.toLocalDateTime() : null);

                    r.setStatut(
                            ReservationStatut.fromDb(
                                    rs.getString("status")
                            )
                    );

                    r.setModePaiement(rs.getString("payment_status"));
                    r.setMontantTotal(rs.getBigDecimal("total_amount"));

                    list.add(r);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error findByClient reservations: " + e.getMessage(), e);
        }

        return list;
    }

    public List<CartItem> findReservationItems(int reservationId, int userId) {
        List<CartItem> list = new ArrayList<>();

        String sql = """
            SELECT
                r.id                   AS reservationId,
                r.offer_id             AS offerId,
                r.total_amount         AS price,
                r.status               AS reservationStatus,
                r.special_request      AS specialRequest,
                r.updated_at           AS decisionAt,

                o.title                AS offerTitle,
                o.image_url            AS imageUrl,
                o.user_id              AS agencyUserId
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            WHERE r.id = ?
              AND r.user_id = ?
            ORDER BY r.id DESC
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem it = new CartItem();

                    it.setIdReservation(rs.getInt("reservationId"));
                    it.setIdOffre(rs.getInt("offerId"));
                    it.setPrixUnitaire(rs.getBigDecimal("price"));
                    it.setTitre(rs.getString("offerTitle"));
                    it.setImageUrl(rs.getString("imageUrl"));

                    String status = rs.getString("reservationStatus");
                    it.setAgencyStatus(status);

                    it.setNomAgence("Agency #" + rs.getInt("agencyUserId"));
                    it.setRefusalReason(rs.getString("specialRequest"));

                    Timestamp ts = rs.getTimestamp("decisionAt");
                    if (ts != null) {
                        it.setAgencyDecisionAt(ts.toLocalDateTime());
                    }

                    list.add(it);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error findReservationItems: " + e.getMessage(), e);
        }

        return list;
    }

    public boolean confirmReservation(int reservationId, String paymentStatus) {
        String sql = """
            UPDATE reservation
            SET status = 'CONFIRMED',
                payment_status = ?,
                reservation_date = NOW(),
                updated_at = NOW()
            WHERE id = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, paymentStatus == null ? "CASH" : paymentStatus);
            ps.setInt(2, reservationId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error confirmReservation: " + e.getMessage(), e);
        }
    }

    public boolean requestBooking(int reservationId, int userId) {
        String sql = """
            UPDATE reservation
            SET status = 'PENDING',
                reservation_date = NOW(),
                updated_at = NOW()
            WHERE id = ?
              AND user_id = ?
              AND status = 'CART'
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error requestBooking: " + e.getMessage(), e);
        }
    }

    public boolean isFullyApprovedByAgencies(int reservationId) {
        String sql = """
            SELECT status
            FROM reservation
            WHERE id = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                String status = rs.getString("status");
                return status != null && status.equalsIgnoreCase("CONFIRMED");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error isFullyApprovedByAgencies: " + e.getMessage(), e);
        }
    }

    public boolean cancelReservation(int reservationId, int userId) {
        String sql = """
            UPDATE reservation
            SET status = 'CANCELLED',
                updated_at = NOW()
            WHERE id = ?
              AND user_id = ?
              AND status IN ('CART', 'PENDING')
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error cancelReservation: " + e.getMessage(), e);
        }
    }

    public boolean removeLine(int reservationId, int userId, int offerId) {
        String sql = """
            UPDATE reservation
            SET status = 'CANCELLED',
                updated_at = NOW()
            WHERE id = ?
              AND user_id = ?
              AND offer_id = ?
              AND status IN ('CART', 'PENDING', 'REJECTED')
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, userId);
            ps.setInt(3, offerId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error removeLine: " + e.getMessage(), e);
        }
    }

    public void resetAllLineStatusesToPending(int reservationId, int userId) {
        String sql = """
            UPDATE reservation
            SET status = 'PENDING',
                updated_at = NOW()
            WHERE id = ?
              AND user_id = ?
              AND status = 'REJECTED'
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error resetAllLineStatusesToPending: " + e.getMessage(), e);
        }
    }

    public Timestamp getEmailSentAt(int reservationId) {
        String sql = """
            SELECT email_sent_at
            FROM reservation
            WHERE id = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return rs.getTimestamp("email_sent_at");
            }

        } catch (SQLException e) {
            return null;
        }
    }

    public void markEmailSentNow(int reservationId) {
        String sql = """
            UPDATE reservation
            SET email_sent_at = NOW()
            WHERE id = ?
              AND email_sent_at IS NULL
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();

        } catch (SQLException ignored) {
        }
    }
    public int createReservationForOffer(int userId, int offerId, java.math.BigDecimal price) {

        String sql = """
        INSERT INTO reservation
            (
                reservation_date,
                number_of_persons,
                total_amount,
                status,
                payment_status,
                created_at,
                offer_id,
                user_id
            )
        VALUES
            (
                NOW(),
                1,
                ?,
                'PENDING',
                'CASH',
                NOW(),
                ?,
                ?
            )
    """;

        try (java.sql.PreparedStatement ps =
                     cnx.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setBigDecimal(1, price == null ? java.math.BigDecimal.ZERO : price);
            ps.setInt(2, offerId);
            ps.setInt(3, userId);

            ps.executeUpdate();

            try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error createReservationForOffer: " + e.getMessage(), e);
        }
    }
}
