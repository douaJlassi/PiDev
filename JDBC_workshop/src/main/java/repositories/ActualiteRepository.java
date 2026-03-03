package repositories;

import entities.Actualite;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActualiteRepository implements IActualiteRepository {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();


    @Override
    public boolean create(Actualite a) {
        String sql = """
        INSERT INTO actualites (idOffre, idAgence, bannerUrl, titre, isActive, endsAt, clickCount)
        VALUES (?, ?, ?, ?, 1, ?, 0)
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, a.getIdOffre());
            ps.setInt(2, a.getIdAgence());
            ps.setString(3, a.getBannerUrl());
            ps.setString(4, a.getTitre());

            java.time.LocalDateTime ends = a.getEndsAt();
            if (ends == null) ends = java.time.LocalDateTime.now().plusDays(7);

            ps.setTimestamp(5, Timestamp.valueOf(ends));
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Actualite> findAllActive() {
        String sql = """
            SELECT idActualite, idOffre, idAgence, bannerUrl, titre, createdAt, isActive, endsAt, clickCount
                                   FROM actualites
                                   WHERE isActive = 1
                                     AND (endsAt IS NULL OR endsAt >= NOW())
                                   ORDER BY createdAt DESC;
        """;
        List<Actualite> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Actualite a = new Actualite();
                a.setIdActualite(rs.getInt("idActualite"));
                a.setIdOffre(rs.getInt("idOffre"));
                a.setIdAgence(rs.getInt("idAgence"));
                a.setBannerUrl(rs.getString("bannerUrl"));
                a.setTitre(rs.getString("titre"));
                a.setActive(rs.getBoolean("isActive"));
                Timestamp createdTs = rs.getTimestamp("createdAt");
                if (createdTs != null) a.setCreatedAt(createdTs.toLocalDateTime());

                Timestamp endsTs = rs.getTimestamp("endsAt");
                if (endsTs != null) a.setEndsAt(endsTs.toLocalDateTime());

                a.setClickCount(rs.getInt("clickCount"));
                // createdAt optional mapping
                list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Actualite> findByAgency(int idAgence) {
        String sql = """
        SELECT idActualite, idOffre, idAgence, bannerUrl, titre, createdAt, isActive, endsAt, clickCount
        FROM actualites
        WHERE idAgence = ?
        ORDER BY createdAt DESC
    """;
        List<Actualite> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Actualite a = new Actualite();
                    a.setIdActualite(rs.getInt("idActualite"));
                    a.setIdOffre(rs.getInt("idOffre"));
                    a.setIdAgence(rs.getInt("idAgence"));
                    a.setBannerUrl(rs.getString("bannerUrl"));
                    a.setTitre(rs.getString("titre"));
                    a.setActive(rs.getBoolean("isActive"));

                    Timestamp createdTs = rs.getTimestamp("createdAt");
                    if (createdTs != null) a.setCreatedAt(createdTs.toLocalDateTime());

                    Timestamp endsTs = rs.getTimestamp("endsAt");
                    if (endsTs != null) a.setEndsAt(endsTs.toLocalDateTime());

                    a.setClickCount(rs.getInt("clickCount"));

                    list.add(a);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean archive(int idActualite, int idAgence) {
        String sql = """
            UPDATE actualites
            SET isActive = 0
            WHERE idActualite = ? AND idAgence = ?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idActualite);
            ps.setInt(2, idAgence);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public boolean incrementClick(int idActualite) {
        String sql = "UPDATE actualites SET clickCount = clickCount + 1 WHERE idActualite = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idActualite);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public boolean extendEndsAtPlus7Days(int idActualite, int idAgence) {
        String sql = """
        UPDATE actualites
        SET endsAt = CASE
            WHEN endsAt IS NULL OR endsAt < NOW() THEN DATE_ADD(NOW(), INTERVAL 7 DAY)
            ELSE DATE_ADD(endsAt, INTERVAL 7 DAY)
        END
        WHERE idActualite = ? AND idAgence = ?
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idActualite);
            ps.setInt(2, idAgence);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public boolean delete(int idActualite, int idAgence) {
        String sql = "DELETE FROM actualites WHERE idActualite = ? AND idAgence = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idActualite);
            ps.setInt(2, idAgence);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}