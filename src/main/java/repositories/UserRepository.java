package repositories;

import utils.MyDBConnexion;
import java.sql.*;

public class UserRepository {
    private final Connection cnx;

    public UserRepository() {
        this.cnx = MyDBConnexion.getInstance().getConnection();
    }

    public String findEmailByUserId(int idUser) {
        String sql = "SELECT email FROM user WHERE idUser=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("email") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findEmailByUserId: " + e.getMessage(), e);
        }
    }
}