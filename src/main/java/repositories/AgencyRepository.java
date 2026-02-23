package repositories;

import entities.Agency;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgencyRepository {

    private final Connection cnx;

    public AgencyRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public List<Agency> findAllValidated() {
        List<Agency> list = new ArrayList<>();
        String sql = "SELECT idUser, nomAgence FROM agence WHERE validationAdmin=1 ORDER BY nomAgence";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Agency a = new Agency();
                a.setIdUser(rs.getInt("idUser"));
                a.setNomAgence(rs.getString("nomAgence"));
                list.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error findAllValidated agencies: " + e.getMessage(), e);
        }

        return list;
    }
}

