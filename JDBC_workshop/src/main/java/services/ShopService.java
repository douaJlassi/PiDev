package services;

import entities.Shop;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShopService implements CRUD<Shop> {

    private Connection cnx;

    public ShopService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Shop shop) throws SQLException {
        String req = "INSERT INTO shop (name, description, price_coins, quantity, image, category) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, shop.getName());
        ps.setString(2, shop.getDescription());
        ps.setInt(3, shop.getPriceCoins());
        ps.setInt(4, shop.getQuantity());
        ps.setBytes(5, shop.getImage());
        ps.setString(6, shop.getCategory());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            shop.setId(rs.getInt(1));
        }
    }

    @Override
    public void updateOne(Shop shop) throws SQLException {
        String req = "UPDATE shop SET name=?, description=?, price_coins=?, quantity=?, image=?, category=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, shop.getName());
        ps.setString(2, shop.getDescription());
        ps.setInt(3, shop.getPriceCoins());
        ps.setInt(4, shop.getQuantity());
        ps.setBytes(5, shop.getImage());
        ps.setString(6, shop.getCategory());
        ps.setInt(7, shop.getId());

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Shop shop) throws SQLException {
        String req = "DELETE FROM shop WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, shop.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Shop> selectALL() throws SQLException {
        List<Shop> shopList = new ArrayList<>();
        String req = "SELECT * FROM shop ORDER BY category, name";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Shop shop = new Shop(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("price_coins"),
                    rs.getInt("quantity"),
                    rs.getBytes("image"),
                    rs.getString("category")
            );
            shopList.add(shop);
        }
        return shopList;
    }

    public List<Shop> getAvailableProducts() throws SQLException {
        List<Shop> shopList = new ArrayList<>();
        String req = "SELECT * FROM shop WHERE quantity > 0 ORDER BY category, name";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Shop shop = new Shop(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("price_coins"),
                    rs.getInt("quantity"),
                    rs.getBytes("image"),
                    rs.getString("category")
            );
            shopList.add(shop);
        }
        return shopList;
    }

    public Shop getById(int id) throws SQLException {
        String req = "SELECT * FROM shop WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Shop(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("price_coins"),
                    rs.getInt("quantity"),
                    rs.getBytes("image"),
                    rs.getString("category")
            );
        }
        return null;
    }

    public void decreaseQuantity(int shopId, int quantity) throws SQLException {
        String req = "UPDATE shop SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, quantity);
        ps.setInt(2, shopId);
        ps.setInt(3, quantity);
        ps.executeUpdate();
    }
}