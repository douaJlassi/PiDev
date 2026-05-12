package repositories;

import entities.Offre;
import entities.OfferFilter;
import utils.MyDBConnexion;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OffreRepository implements IOffreRepository {

    private final Connection cnx;

    private static final String TABLE_NAME = "offer";

    public OffreRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public List<Offre> findAllAdmin() {
        List<Offre> list = new ArrayList<>();

        String sql = "SELECT * FROM " + TABLE_NAME +
                " WHERE status = 'ACTIVE' ORDER BY id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error findAll offre: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public Offre findById(int idOffre) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";

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
        String sql = "INSERT INTO " + TABLE_NAME +
                " (title, description, promo_price, original_price, start_date, end_date, status, image_url, capacity, location, created_at, updated_at, user_id) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NULL, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, o.getTitle());
            ps.setString(2, o.getDescription());

            ps.setBigDecimal(3, o.getPromoPrice());

            BigDecimal original = o.getOriginalPrice() != null
                    ? o.getOriginalPrice()
                    : o.getPromoPrice();

            ps.setBigDecimal(4, original);

            ps.setDate(5, o.getStartDate() != null ? Date.valueOf(o.getStartDate()) : null);
            ps.setDate(6, o.getEndDate() != null ? Date.valueOf(o.getEndDate()) : null);

            ps.setString(7, o.getStatus() != null ? o.getStatus() : "ACTIVE");
            ps.setString(8, o.getImageUrl());

            if (o.getCapacity() != null) {
                ps.setInt(9, o.getCapacity());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.setString(10, o.getLocation());
            ps.setInt(11, o.getUserId());

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
        String sql = "UPDATE " + TABLE_NAME +
                " SET title=?, description=?, promo_price=?, original_price=?, start_date=?, end_date=?, " +
                " status=?, image_url=?, capacity=?, location=?, user_id=?, updated_at=NOW() " +
                " WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, o.getTitle());
            ps.setString(2, o.getDescription());
            ps.setBigDecimal(3, o.getPromoPrice());
            ps.setBigDecimal(4, o.getOriginalPrice());
            ps.setDate(5, o.getStartDate() != null ? Date.valueOf(o.getStartDate()) : null);
            ps.setDate(6, o.getEndDate() != null ? Date.valueOf(o.getEndDate()) : null);
            ps.setString(7, o.getStatus() != null ? o.getStatus() : "ACTIVE");
            ps.setString(8, o.getImageUrl());

            if (o.getCapacity() != null) {
                ps.setInt(9, o.getCapacity());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.setString(10, o.getLocation());
            ps.setInt(11, o.getUserId());
            ps.setInt(12, o.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error update offre: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteSafe(int idOffre) {
        return archiveOffre(idOffre);
    }

    public boolean archiveOffre(int idOffre) {
        String sql = "UPDATE " + TABLE_NAME +
                " SET status='ARCHIVED', updated_at=NOW() WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error archiving offer: " + e.getMessage(), e);
        }
    }

    public boolean confirmPermanentDelete(int idOffre) {
        if (isUsedInLignePanier(idOffre)) {
            System.out.println("Cannot delete: Offer is linked to existing reservations.");
            return false;
        }

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error during permanent deletion: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isUsedInLignePanier(int idOffre) {
        String sql = "SELECT COUNT(*) FROM reservation WHERE offer_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error checking offer usage: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Offre> findAllByAgency(int idAgence) {
        List<Offre> list = new ArrayList<>();

        String sql = "SELECT * FROM " + TABLE_NAME +
                " WHERE user_id = ? AND status = 'ACTIVE' ORDER BY id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error findAllByAgency offre: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public boolean isOwnedByAgency(int idOffre, int idAgence) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE id=? AND user_id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idAgence);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error isOwnedByAgency: " + e.getMessage(), e);
        }
    }

    public boolean deleteSafeForAgency(int idOffre, int idAgence) {
        if (!isOwnedByAgency(idOffre, idAgence)) {
            return false;
        }

        return deleteSafe(idOffre);
    }

    @Override
    public Offre findByIdForAgency(int idOffre, int idAgence) {
        String sql = "SELECT * FROM " + TABLE_NAME +
                " WHERE id=? AND user_id=?";

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
        String sql = "UPDATE " + TABLE_NAME +
                " SET title=?, description=?, promo_price=?, original_price=?, start_date=?, end_date=?, " +
                " status=?, image_url=?, capacity=?, location=?, updated_at=NOW() " +
                " WHERE id=? AND user_id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, o.getTitle());
            ps.setString(2, o.getDescription());
            ps.setBigDecimal(3, o.getPromoPrice());
            ps.setBigDecimal(4, o.getOriginalPrice());
            ps.setDate(5, o.getStartDate() != null ? Date.valueOf(o.getStartDate()) : null);
            ps.setDate(6, o.getEndDate() != null ? Date.valueOf(o.getEndDate()) : null);
            ps.setString(7, o.getStatus() != null ? o.getStatus() : "ACTIVE");
            ps.setString(8, o.getImageUrl());

            if (o.getCapacity() != null) {
                ps.setInt(9, o.getCapacity());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.setString(10, o.getLocation());
            ps.setInt(11, o.getId());
            ps.setInt(12, idAgence);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error updateForAgency: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Offre> findAllAdminByAgency(Integer agencyId) {
        List<Offre> list = new ArrayList<>();

        String sql = agencyId == null
                ? "SELECT * FROM " + TABLE_NAME + " WHERE status='ACTIVE' ORDER BY id DESC"
                : "SELECT * FROM " + TABLE_NAME + " WHERE status='ACTIVE' AND user_id=? ORDER BY id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            if (agencyId != null) {
                ps.setInt(1, agencyId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error findAllAdminByAgency: " + e.getMessage(), e);
        }

        return list;
    }

    public boolean archiveForAgency(int idOffre, int idAgence) {
        if (!isOwnedByAgency(idOffre, idAgence)) {
            return false;
        }

        String sql = "UPDATE " + TABLE_NAME +
                " SET status='ARCHIVED', updated_at=NOW() WHERE id=? AND user_id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idAgence);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error archive offer: " + e.getMessage(), e);
        }
    }

    public boolean restoreForAgency(int idOffre, int idAgence) {
        if (!isOwnedByAgency(idOffre, idAgence)) {
            return false;
        }

        String sql = "UPDATE " + TABLE_NAME +
                " SET status='ACTIVE', updated_at=NOW() WHERE id=? AND user_id=?";

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

        String sql = "SELECT * FROM " + TABLE_NAME +
                " WHERE user_id=? AND status='ARCHIVED' ORDER BY id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAgence);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error findArchivedByAgency: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public boolean deleteHardAdmin(int idOffre) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error deleteHardAdmin: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Offre> searchActiveOffers(OfferFilter f) {
        List<Offre> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM " + TABLE_NAME + " WHERE status='ACTIVE' "
        );

        List<Object> params = new ArrayList<>();

        if (f != null && f.getKeyword() != null && !f.getKeyword().trim().isEmpty()) {
            sql.append("AND (title LIKE ? OR description LIKE ? OR location LIKE ?) ");
            String like = "%" + f.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (f != null && f.getMinPrice() != null) {
            sql.append("AND promo_price >= ? ");
            params.add(f.getMinPrice());
        }

        if (f != null && f.getMaxPrice() != null) {
            sql.append("AND promo_price <= ? ");
            params.add(f.getMaxPrice());
        }

        if (f != null && f.getSelectedDate() != null) {
            sql.append("AND start_date <= ? AND end_date >= ? ");
            params.add(Date.valueOf(f.getSelectedDate()));
            params.add(Date.valueOf(f.getSelectedDate()));
        }

        if (f != null && f.getStatuses() != null && !f.getStatuses().isEmpty()) {
            sql.append("AND status IN (");
            sql.append(String.join(",", Collections.nCopies(f.getStatuses().size(), "?")));
            sql.append(") ");

            for (String status : f.getStatuses()) {
                params.add(status);
            }
        }

        if (f != null && f.getLocations() != null && !f.getLocations().isEmpty()) {
            sql.append("AND location IN (");
            sql.append(String.join(",", Collections.nCopies(f.getLocations().size(), "?")));
            sql.append(") ");

            for (String location : f.getLocations()) {
                params.add(location);
            }
        }

        sql.append("ORDER BY id DESC");

        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);

                if (p instanceof BigDecimal) {
                    ps.setBigDecimal(i + 1, (BigDecimal) p);
                } else if (p instanceof Integer) {
                    ps.setInt(i + 1, (Integer) p);
                } else if (p instanceof Date) {
                    ps.setDate(i + 1, (Date) p);
                } else {
                    ps.setString(i + 1, String.valueOf(p));
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error searchActiveOffers: " + e.getMessage(), e);
        }

        return list;
    }

    private Offre map(ResultSet rs) throws SQLException {
        Offre o = new Offre();

        o.setId(rs.getInt("id"));
        o.setTitle(rs.getString("title"));
        o.setDescription(rs.getString("description"));
        o.setPromoPrice(rs.getBigDecimal("promo_price"));
        o.setOriginalPrice(rs.getBigDecimal("original_price"));

        Date startDate = rs.getDate("start_date");
        Date endDate = rs.getDate("end_date");

        o.setStartDate(startDate != null ? startDate.toLocalDate() : null);
        o.setEndDate(endDate != null ? endDate.toLocalDate() : null);

        o.setStatus(rs.getString("status"));
        o.setImageUrl(rs.getString("image_url"));

        Object capacityObj = rs.getObject("capacity");
        o.setCapacity(capacityObj != null ? ((Number) capacityObj).intValue() : null);

        o.setLocation(rs.getString("location"));
        o.setUserId(rs.getInt("user_id"));

        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        o.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        o.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);

        return o;
    }
}