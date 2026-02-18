package Services;

import gestion_activite.Guide;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuideService implements CRUD<Guide> {

    private Connection cnx;

    public GuideService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    // ===================== INSERT =====================
    // Note: Physically inserts only idUser and disponibilite into the 'guide' table
    @Override

    public void insertOne(Guide guide) throws SQLException {
        // We must include all the columns now that you added them physically to the table
        String req = "INSERT INTO `guide` (`idUser`, `disponibilite`, `nom`, `prenom`, `email`, `telephone`) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, guide.getIdUser());
            ps.setBoolean(2, guide.isDisponibilite());
            ps.setString(3, guide.getNom());      // Add this
            ps.setString(4, guide.getPrenom());   // Add this
            ps.setString(5, guide.getEmail());    // Add this
            ps.setString(6, guide.getTelephone());// Add this

            ps.executeUpdate();
        }
    }

    // ===================== UPDATE =====================
    @Override
    public void updateOne(Guide guide) throws SQLException {
        String req = "UPDATE `guide` SET `disponibilite`=? WHERE `idUser`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setBoolean(1, guide.isDisponibilite());
            ps.setInt(2, guide.getIdUser());
            ps.executeUpdate();
        }
    }

    // ===================== DELETE =====================
    @Override
    public void deleteOne(Guide guide) throws SQLException {
        String req = "DELETE FROM `guide` WHERE `idUser`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, guide.getIdUser());
            ps.executeUpdate();
        }
    }

    // ===================== SELECT ALL (WITH JOIN) =====================
    @Override
    public List<Guide> selectALL() throws SQLException {
        List<Guide> guideList = new ArrayList<>();

        // Join with the user table to get names, email, and phone
        String req = "SELECT g.idUser, g.disponibilite, u.nom, u.prenom, u.email, u.telephone " +
                "FROM `guide` g " +
                "INNER JOIN `user` u ON g.idUser = u.idUser";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Guide g = new Guide(
                        rs.getInt("idUser"),
                        rs.getBoolean("disponibilite"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("telephone")
                );
                guideList.add(g);
            }
        }
        return guideList;
    }

    // ===================== SELECT BY ID (WITH JOIN) =====================
    public Guide selectById(int idUser) throws SQLException {
        String req = "SELECT g.idUser, g.disponibilite, u.nom, u.prenom, u.email, u.telephone " +
                "FROM `guide` g " +
                "INNER JOIN `user` u ON g.idUser = u.idUser " +
                "WHERE g.idUser = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idUser);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Guide(
                            rs.getInt("idUser"),
                            rs.getBoolean("disponibilite"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("telephone")
                    );
                }
            }
        }
        return null;
    }

    // ===================== SELECT GUIDES DISPONIBLES (WITH JOIN) =====================
    public List<Guide> selectGuidesDisponibles() throws SQLException {
        List<Guide> guideList = new ArrayList<>();

        String req = "SELECT g.idUser, g.disponibilite, u.nom, u.prenom, u.email, u.telephone " +
                "FROM `guide` g " +
                "INNER JOIN `user` u ON g.idUser = u.idUser " +
                "WHERE g.disponibilite = TRUE";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Guide g = new Guide(
                        rs.getInt("idUser"),
                        rs.getBoolean("disponibilite"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("telephone")
                );
                guideList.add(g);
            }
        }
        return guideList;
    }

    // ===================== ACTIVER/DESACTIVER GUIDE =====================
    public void toggleDisponibilite(int idUser) throws SQLException {
        Guide guide = selectById(idUser);
        if (guide != null) {
            guide.setDisponibilite(!guide.isDisponibilite());
            updateOne(guide);
        } else {
            throw new SQLException("Guide avec l'ID " + idUser + " non trouvé");
        }
    }






}