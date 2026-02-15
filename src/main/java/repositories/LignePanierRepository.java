package repositories;

import entities.CartItem;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LignePanierRepository {

    private final Connection cnx;

    public LignePanierRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public boolean exists(int idReservation, int idOffre) {
        String sql = "SELECT COUNT(*) FROM lignepanier WHERE idReservation=? AND idOffre=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error exists lignepanier: " + e.getMessage(), e);
        }
    }

    public boolean addOffer(int idReservation, int idOffre, java.math.BigDecimal prixUnitaire) {
        if (exists(idReservation, idOffre)) return false; // already in cart

        String sql = "INSERT INTO lignepanier (idReservation, idOffre, prixUnitaire) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idOffre);
            ps.setBigDecimal(3, prixUnitaire);
            ps.executeUpdate();
            return true; // inserted
        } catch (SQLException e) {
            throw new RuntimeException("Error addOffer lignepanier: " + e.getMessage(), e);
        }
    }


    public boolean removeOffer(int idReservation, int idOffre) {
        String sql = "DELETE FROM lignepanier WHERE idReservation=? AND idOffre=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idOffre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error removeOffer lignepanier: " + e.getMessage(), e);
        }
    }

    public List<CartItem> findCartItems(int idReservation) {
        List<CartItem> list = new ArrayList<>();

        String sql =
                "SELECT lp.idReservation, lp.idOffre, lp.prixUnitaire, " +
                        "       o.titre, o.imageUrl " +
                        "FROM lignepanier lp " +
                        "JOIN offre o ON o.idOffre = lp.idOffre " +
                        "WHERE lp.idReservation = ? " +
                        "ORDER BY lp.idOffre DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem it = new CartItem();
                    it.setIdReservation(rs.getInt("idReservation"));
                    it.setIdOffre(rs.getInt("idOffre"));
                    it.setPrixUnitaire(rs.getBigDecimal("prixUnitaire"));
                    it.setTitre(rs.getString("titre"));
                    it.setImageUrl(rs.getString("imageUrl"));
                    list.add(it);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findCartItems: " + e.getMessage(), e);
        }

        return list;
    }



}
