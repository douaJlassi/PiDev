package services;

import entities.Purchase;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseHistoryService {
    private Connection connection;

    public PurchaseHistoryService() {
        connection = MyDBConnexion.getInstance().getCnx();
    }

    // Get purchase history for a specific user
    public List<Purchase> getUserPurchaseHistory(int userId) throws SQLException {
        List<Purchase> purchases = new ArrayList<>();
        String query = "SELECT p.*, s.name as product_name " +
                "FROM purchases p " +  // Changed from 'purchase' to 'purchases'
                "JOIN shop s ON p.shop_id = s.id " +
                "WHERE p.user_id = ? " +
                "ORDER BY p.purchase_date DESC";

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Purchase purchase = mapPurchase(rs);
            purchase.setProductName(rs.getString("product_name"));
            purchases.add(purchase);
        }
        return purchases;
    }

    // Get purchase history for a specific product
    public List<Purchase> getProductPurchaseHistory(int shopId) throws SQLException {
        List<Purchase> purchases = new ArrayList<>();
        String query = "SELECT * FROM purchases WHERE shop_id = ? ORDER BY purchase_date DESC"; // Changed from 'purchase'

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, shopId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            purchases.add(mapPurchase(rs));
        }
        return purchases;
    }

    // Get all purchases with filters
    public List<Purchase> getAllPurchases(String statusFilter, Date fromDate, Date toDate) throws SQLException {
        List<Purchase> purchases = new ArrayList<>();
        StringBuilder query = new StringBuilder(
                "SELECT p.*, s.name as product_name, u.username as buyer_username " +
                        "FROM purchases p " +  // Changed from 'purchase'
                        "JOIN shop s ON p.shop_id = s.id " +
                        "JOIN user u ON p.user_id = u.id " +
                        "WHERE 1=1 "
        );

        if (statusFilter != null && !statusFilter.isEmpty()) {
            query.append(" AND p.status = ?");
        }
        if (fromDate != null) {
            query.append(" AND p.purchase_date >= ?");
        }
        if (toDate != null) {
            query.append(" AND p.purchase_date <= ?");
        }
        query.append(" ORDER BY p.purchase_date DESC");

        PreparedStatement ps = connection.prepareStatement(query.toString());

        int paramIndex = 1;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            ps.setString(paramIndex++, statusFilter);
        }
        if (fromDate != null) {
            ps.setDate(paramIndex++, fromDate);
        }
        if (toDate != null) {
            ps.setDate(paramIndex++, toDate);
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Purchase purchase = mapPurchase(rs);
            purchase.setProductName(rs.getString("product_name"));
            purchase.setBuyerUsername(rs.getString("buyer_username"));
            purchases.add(purchase);
        }
        return purchases;
    }

    // Get purchase statistics for a product
    public ProductStats getProductStats(int shopId) throws SQLException {
        String query = "SELECT " +
                "COUNT(*) as total_purchases, " +
                "SUM(quantity) as total_quantity_sold, " +
                "SUM(total_coins) as total_revenue, " +
                "AVG(total_coins) as average_purchase_value, " +
                "MAX(purchase_date) as last_purchase_date " +
                "FROM purchases WHERE shop_id = ?";  // Changed from 'purchase'

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, shopId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            ProductStats stats = new ProductStats();
            stats.setTotalPurchases(rs.getInt("total_purchases"));
            stats.setTotalQuantitySold(rs.getInt("total_quantity_sold"));
            stats.setTotalRevenue(rs.getInt("total_revenue"));
            stats.setAveragePurchaseValue(rs.getDouble("average_purchase_value"));
            stats.setLastPurchaseDate(rs.getDate("last_purchase_date"));
            return stats;
        }
        return null;
    }

    // Get user's purchase summary
    public UserPurchaseSummary getUserPurchaseSummary(int userId) throws SQLException {
        String query = "SELECT " +
                "COUNT(*) as total_purchases, " +
                "SUM(total_coins) as total_spent, " +
                "COUNT(DISTINCT shop_id) as unique_products_bought " +
                "FROM purchases WHERE user_id = ?";  // Changed from 'purchase'

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            UserPurchaseSummary summary = new UserPurchaseSummary();
            summary.setTotalPurchases(rs.getInt("total_purchases"));
            summary.setTotalSpent(rs.getInt("total_spent"));
            summary.setUniqueProductsBought(rs.getInt("unique_products_bought"));
            return summary;
        }
        return null;
    }

    private Purchase mapPurchase(ResultSet rs) throws SQLException {
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
        return purchase;
    }

    // Inner classes for statistics
    public static class ProductStats {
        private int totalPurchases;
        private int totalQuantitySold;
        private int totalRevenue;
        private double averagePurchaseValue;
        private Date lastPurchaseDate;

        public int getTotalPurchases() { return totalPurchases; }
        public void setTotalPurchases(int totalPurchases) { this.totalPurchases = totalPurchases; }
        public int getTotalQuantitySold() { return totalQuantitySold; }
        public void setTotalQuantitySold(int totalQuantitySold) { this.totalQuantitySold = totalQuantitySold; }
        public int getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(int totalRevenue) { this.totalRevenue = totalRevenue; }
        public double getAveragePurchaseValue() { return averagePurchaseValue; }
        public void setAveragePurchaseValue(double averagePurchaseValue) { this.averagePurchaseValue = averagePurchaseValue; }
        public Date getLastPurchaseDate() { return lastPurchaseDate; }
        public void setLastPurchaseDate(Date lastPurchaseDate) { this.lastPurchaseDate = lastPurchaseDate; }
    }

    public static class UserPurchaseSummary {
        private int totalPurchases;
        private int totalSpent;
        private int uniqueProductsBought;

        public int getTotalPurchases() { return totalPurchases; }
        public void setTotalPurchases(int totalPurchases) { this.totalPurchases = totalPurchases; }
        public int getTotalSpent() { return totalSpent; }
        public void setTotalSpent(int totalSpent) { this.totalSpent = totalSpent; }
        public int getUniqueProductsBought() { return uniqueProductsBought; }
        public void setUniqueProductsBought(int uniqueProductsBought) { this.uniqueProductsBought = uniqueProductsBought; }
    }
}