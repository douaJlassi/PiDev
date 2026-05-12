// COPY / PASTE VERSION

package repositories;

import entities.OffreAgency;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgencyRepository {

    private final Connection cnx;

    public AgencyRepository() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    public List<OffreAgency> findAllValidated() {
        List<OffreAgency> list = new ArrayList<>();

        String sql = """
            SELECT id AS idUser, name AS nomAgence
            FROM user
            WHERE role = 'AGENCE'
            ORDER BY name
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OffreAgency a = new OffreAgency();
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