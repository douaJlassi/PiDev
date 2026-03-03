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

    // KPI: revenue, confirmed bookings, pending lines, approval rate
    public AgencyAnalyticsKpi loadKpis(int idAgence) {
        AgencyAnalyticsKpi k = new AgencyAnalyticsKpi();

        // Revenue + confirmed bookings (CONFIRME only)
        String sqlRevenue = """
            SELECT
              COALESCE(SUM(lp.prixUnitaire),0) AS revenue,
              COUNT(DISTINCT r.idReservation)  AS bookings
            FROM lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            WHERE o.idAgence = ?
              AND r.statut = 'CONFIRME'
        """;

        // Pending lines (reservation pending + line pending)
        String sqlPending = """
            SELECT COUNT(*) AS pendingLines
            FROM lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            WHERE o.idAgence = ?
              AND r.statut = 'ENATTENTE'
              AND lp.agencyStatus = 'ENATTENTE'
        """;

        // Approval rate: APPROUVEE / (APPROUVEE + REFUSEE) * 100
        String sqlApproval = """
    SELECT
      SUM(CASE WHEN lp.agencyStatus='APPROUVEE' THEN 1 ELSE 0 END) AS approved,
      SUM(CASE WHEN lp.agencyStatus='REFUSEE'   THEN 1 ELSE 0 END) AS refused
    FROM lignepanier lp
    JOIN offre o ON o.idOffre = lp.idOffre
    JOIN reservation r ON r.idReservation = lp.idReservation
    WHERE o.idAgence = ?
""";

        try (PreparedStatement ps = cnx.prepareStatement(sqlRevenue)) {
            ps.setInt(1, idAgence);
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
            ps.setInt(1, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) k.setPendingLines(rs.getInt("pendingLines"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error KPI pending: " + e.getMessage(), e);
        }

        try (PreparedStatement ps = cnx.prepareStatement(sqlApproval)) {
            ps.setInt(1, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int approved = rs.getInt("approved");
                    int refused = rs.getInt("refused");
                    int denom = approved + refused;
                    double rate = (denom == 0) ? 0.0 : (approved * 100.0 / denom);
                    k.setApprovalRate(rate);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error KPI approval: " + e.getMessage(), e);
        }

        return k;
    }

    // Revenue per day (last 30 days) - CONFIRME only
    public List<AgencyRevenuePoint> revenueLast30Days(int idAgence) {
        String sql = """
            SELECT DATE(r.dateReservation) AS day, COALESCE(SUM(lp.prixUnitaire),0) AS revenue
            FROM lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            WHERE o.idAgence = ?
              AND r.statut='CONFIRME'
              AND r.dateReservation >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)
            GROUP BY DATE(r.dateReservation)
            ORDER BY day ASC
        """;

        List<AgencyRevenuePoint> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("day");
                    BigDecimal rev = rs.getBigDecimal("revenue");
                    list.add(new AgencyRevenuePoint(d.toLocalDate(), rev));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error revenueLast30Days: " + e.getMessage(), e);
        }

        // Fill missing days with 0 so the chart looks continuous
        return fillMissingDays(list, 30);
    }

    private List<AgencyRevenuePoint> fillMissingDays(List<AgencyRevenuePoint> points, int days) {
        List<AgencyRevenuePoint> out = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(days - 1);

        java.util.Map<LocalDate, BigDecimal> map = new java.util.HashMap<>();
        for (AgencyRevenuePoint p : points) map.put(p.getDay(), p.getRevenue());

        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            out.add(new AgencyRevenuePoint(day, map.getOrDefault(day, BigDecimal.ZERO)));
        }
        return out;
    }

    // Top offers by revenue (CONFIRME only)
    public List<AgencyTopOffer> topOffersByRevenue(int idAgence, int limit) {
        String sql = """
            SELECT o.idOffre, o.titre, COALESCE(SUM(lp.prixUnitaire),0) AS revenue
            FROM lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            WHERE o.idAgence = ?
              AND r.statut='CONFIRME'
            GROUP BY o.idOffre, o.titre
            ORDER BY revenue DESC
            LIMIT ?
        """;

        List<AgencyTopOffer> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AgencyTopOffer t = new AgencyTopOffer();
                    t.setIdOffre(rs.getInt("idOffre"));
                    t.setTitle(rs.getString("titre"));
                    t.setRevenue(rs.getBigDecimal("revenue"));
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error topOffersByRevenue: " + e.getMessage(), e);
        }
        return list;
    }

    // Status distribution for current pending requests (ENATTENTE reservations)
    public List<javafx.scene.chart.PieChart.Data> statusDistribution(int idAgence) {
        String sql = """
            SELECT lp.agencyStatus AS st, COUNT(*) AS cnt
            FROM lignepanier lp
            JOIN offre o ON o.idOffre = lp.idOffre
            JOIN reservation r ON r.idReservation = lp.idReservation
            WHERE o.idAgence = ?
              AND r.statut='ENATTENTE'
            GROUP BY lp.agencyStatus
        """;

        List<javafx.scene.chart.PieChart.Data> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString("st");
                    int cnt = rs.getInt("cnt");
                    if (st == null) st = "ENATTENTE";
                    list.add(new javafx.scene.chart.PieChart.Data(st, cnt));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error statusDistribution: " + e.getMessage(), e);
        }
        return list;
    }

    // Top clients by revenue (CONFIRME only)
    public List<AgencyTopClient> topClients(int idAgence, int limit) {
        String sql = """
            SELECT\s
                     r.idClient AS idClient,
                     u.nom AS nom,
                     u.prenom AS prenom,
                     u.telephone AS telephone,
                     COUNT(DISTINCT r.idReservation) AS bookings,
                     COALESCE(SUM(lp.prixUnitaire),0) AS revenue
                   FROM lignepanier lp
                   JOIN offre o ON o.idOffre = lp.idOffre
                   JOIN reservation r ON r.idReservation = lp.idReservation
                   JOIN user u ON u.idUser = r.idClient
                   WHERE o.idAgence = ?
                     AND r.statut='CONFIRME'
                   GROUP BY r.idClient, u.nom, u.prenom, u.telephone
                   ORDER BY revenue DESC
                   LIMIT ?
        """;

        List<AgencyTopClient> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AgencyTopClient c = new AgencyTopClient();
                    c.setIdClient(rs.getInt("idClient"));
                    c.setBookings(rs.getInt("bookings"));
                    c.setRevenue(rs.getBigDecimal("revenue"));
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    String fullName = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
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