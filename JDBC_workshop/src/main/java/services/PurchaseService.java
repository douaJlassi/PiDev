package services;

import entities.Purchase;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseService {

    private Connection cnx;

    public PurchaseService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    public void insertPurchase(Purchase purchase) throws SQLException {
        String req = "INSERT INTO purchases (user_id, shop_id, quantity, total_coins, buyer_name, buyer_email, buyer_address, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, purchase.getUserId());
        ps.setInt(2, purchase.getShopId());
        ps.setInt(3, purchase.getQuantity());
        ps.setInt(4, purchase.getTotalCoins());
        ps.setString(5, purchase.getBuyerName());
        ps.setString(6, purchase.getBuyerEmail());
        ps.setString(7, purchase.getBuyerAddress());
        ps.setString(8, "confirmed");

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            purchase.setId(rs.getInt(1));
        }
    }

    public List<Purchase> getPurchasesByUser(int userId) throws SQLException {
        List<Purchase> purchaseList = new ArrayList<>();
        String req = "SELECT p.*, s.name as shop_name FROM purchases p " +
                "JOIN shop s ON p.shop_id = s.id " +
                "WHERE p.user_id = ? ORDER BY p.purchase_date DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Purchase purchase = new Purchase();
            purchase.setId(rs.getInt("id"));
            purchase.setUserId(rs.getInt("user_id"));
            purchase.setShopId(rs.getInt("shop_id"));
            purchase.setQuantity(rs.getInt("quantity"));
            purchase.setTotalCoins(rs.getInt("total_coins"));
            purchase.setBuyerName(rs.getString("buyer_name"));
            purchase.setBuyerEmail(rs.getString("buyer_email"));
            purchase.setBuyerAddress(rs.getString("buyer_address"));
            purchase.setStatus(rs.getString("status"));
            purchase.setPurchaseDate(rs.getTimestamp("purchase_date"));
            purchase.setProductName(rs.getString("product_name"));
            purchaseList.add(purchase);
        }
        return purchaseList;
    }

    public List<Purchase> getAllPurchases() throws SQLException {
        List<Purchase> purchaseList = new ArrayList<>();
        String req = "SELECT p.*, s.name as shop_name, u.username as user_name FROM purchases p " +
                "JOIN shop s ON p.shop_id = s.id " +
                "JOIN user u ON p.user_id = u.id " +
                "ORDER BY p.purchase_date DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Purchase purchase = new Purchase();
            purchase.setId(rs.getInt("id"));
            purchase.setUserId(rs.getInt("user_id"));
            purchase.setShopId(rs.getInt("shop_id"));
            purchase.setQuantity(rs.getInt("quantity"));
            purchase.setTotalCoins(rs.getInt("total_coins"));
            purchase.setBuyerName(rs.getString("buyer_name"));
            purchase.setBuyerEmail(rs.getString("buyer_email"));
            purchase.setBuyerAddress(rs.getString("buyer_address"));
            purchase.setStatus(rs.getString("status"));
            purchase.setPurchaseDate(rs.getTimestamp("purchase_date"));
            purchase.setProductName(rs.getString("product_name"));
            purchase.setBuyerUsername(rs.getString("buyer_username")); // Changed from setUserName

            purchaseList.add(purchase);
        }
        return purchaseList;
    }
}