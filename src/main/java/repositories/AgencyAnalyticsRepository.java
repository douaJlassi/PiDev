package repositories;

import entities.*;
import utils.MyDBConnexion;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AgencyAnalyticsRepository {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();

    public AgencyAnalyticsKpi loadKpis(int agencyUserId) {
        AgencyAnalyticsKpi k = new AgencyAnalyticsKpi();

        String sqlRevenue = """
            SELECT
                COALESCE(SUM(r.total_amount), 0) AS revenue,
                COUNT(r.id) AS bookings
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            WHERE o.user_id = ?
              AND r.status = 'CONFIRMED'
        """;

        String sqlPending = """
            SELECT COUNT(*) AS pendingLines
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            WHERE o.user_id = ?
              AND r.status = 'PENDING'
        """;

        String sqlApproval = """
            SELECT
                SUM(CASE WHEN r.status = 'CONFIRMED' THEN 1 ELSE 0 END) AS approved,
                SUM(CASE WHEN r.status = 'REJECTED' THEN 1 ELSE 0 END) AS refused
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            WHERE o.user_id = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sqlRevenue)) {
            ps.setInt(1, agencyUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    k.setRevenue(rs.getBigDecimal("revenue"));
                    k.setConfirmedBookings(rs.getInt("bookings"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error KPI revenue: " + e.getMessage(), e);
        }

        try (PreparedStatement ps = cnx.prepareStatement(sqlPending)) {
            ps.setInt(1, agencyUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    k.setPendingLines(rs.getInt("pendingLines"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error KPI pending: " + e.getMessage(), e);
        }

        try (PreparedStatement ps = cnx.prepareStatement(sqlApproval)) {
            ps.setInt(1, agencyUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int approved = rs.getInt("approved");
                    int refused = rs.getInt("refused");

                    int denom = approved + refused;
                    double rate = denom == 0 ? 0.0 : approved * 100.0 / denom;

                    k.setApprovalRate(rate);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error KPI approval: " + e.getMessage(), e);
        }

        return k;
    }

    public List<AgencyRevenuePoint> revenueLast30Days(int agencyUserId) {
        String sql = """
            SELECT
                DATE(r.reservation_date) AS day,
                COALESCE(SUM(r.total_amount), 0) AS revenue
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            WHERE o.user_id = ?
              AND r.status = 'CONFIRMED'
              AND r.reservation_date >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)
            GROUP BY DATE(r.reservation_date)
            ORDER BY day ASC
        """;

        List<AgencyRevenuePoint> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, agencyUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("day");
                    BigDecimal rev = rs.getBigDecimal("revenue");

                    if (d != null) {
                        list.add(new AgencyRevenuePoint(d.toLocalDate(), rev));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error revenueLast30Days: " + e.getMessage(), e);
        }

        return fillMissingDays(list, 30);
    }

    private List<AgencyRevenuePoint> fillMissingDays(List<AgencyRevenuePoint> points, int days) {
        List<AgencyRevenuePoint> out = new ArrayList<>();

        LocalDate start = LocalDate.now().minusDays(days - 1);

        java.util.Map<LocalDate, BigDecimal> map = new java.util.HashMap<>();

        for (AgencyRevenuePoint p : points) {
            map.put(p.getDay(), p.getRevenue());
        }

        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);

            out.add(
                    new AgencyRevenuePoint(
                            day,
                            map.getOrDefault(day, BigDecimal.ZERO)
                    )
            );
        }

        return out;
    }

    public List<AgencyTopOffer> topOffersByRevenue(int agencyUserId, int limit) {
        String sql = """
            SELECT
                o.id AS offerId,
                o.title AS offerTitle,
                COALESCE(SUM(r.total_amount), 0) AS revenue
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            WHERE o.user_id = ?
              AND r.status = 'CONFIRMED'
            GROUP BY o.id, o.title
            ORDER BY revenue DESC
            LIMIT ?
        """;

        List<AgencyTopOffer> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, agencyUserId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AgencyTopOffer t = new AgencyTopOffer();

                    t.setIdOffre(rs.getInt("offerId"));
                    t.setTitle(rs.getString("offerTitle"));
                    t.setRevenue(rs.getBigDecimal("revenue"));

                    list.add(t);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error topOffersByRevenue: " + e.getMessage(), e);
        }

        return list;
    }

    public List<javafx.scene.chart.PieChart.Data> statusDistribution(int agencyUserId) {
        String sql = """
            SELECT
                r.status AS status,
                COUNT(*) AS countStatus
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            WHERE o.user_id = ?
            GROUP BY r.status
            ORDER BY countStatus DESC
        """;

        List<javafx.scene.chart.PieChart.Data> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, agencyUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("countStatus");

                    if (status == null || status.isBlank()) {
                        status = "UNKNOWN";
                    }

                    list.add(new javafx.scene.chart.PieChart.Data(status, count));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error statusDistribution: " + e.getMessage(), e);
        }

        return list;
    }

    public List<AgencyTopClient> topClients(int agencyUserId, int limit) {
        String sql = """
            SELECT
                r.user_id AS clientId,
                u.last_name AS nom,
                u.name AS prenom,
                NULL AS telephone,
                COUNT(r.id) AS bookings,
                COALESCE(SUM(r.total_amount), 0) AS revenue
            FROM reservation r
            JOIN offer o ON o.id = r.offer_id
            JOIN user u ON u.id = r.user_id
            WHERE o.user_id = ?
              AND r.status = 'CONFIRMED'
            GROUP BY r.user_id, u.last_name, u.name
            ORDER BY revenue DESC
            LIMIT ?
        """;

        List<AgencyTopClient> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, agencyUserId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AgencyTopClient c = new AgencyTopClient();

                    c.setIdClient(rs.getInt("clientId"));
                    c.setBookings(rs.getInt("bookings"));
                    c.setRevenue(rs.getBigDecimal("revenue"));

                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");

                    String fullName =
                            ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();

                    c.setFullName(fullName.isEmpty() ? "Client" : fullName);
                    c.setTelephone(rs.getString("telephone"));

                    list.add(c);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error topClients: " + e.getMessage(), e);
        }

        return list;
    }
}