package tn.esprit.projet.services;

import tn.esprit.projet.entities.Guide;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuideService implements CRUD<Guide> {

    private Connection cnx;

    public GuideService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    // ===================== INSERT =====================
    @Override
    public void insertOne(Guide guide) throws SQLException {
        if (guide.getIdUser() <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur doit être valide");
        }

        String req = "INSERT INTO `guide` (`idUser`, `disponibilite`) VALUES (?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, guide.getIdUser());
            ps.setBoolean(2, guide.isDisponibilite());
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

    // ===================== SELECT ALL =====================
    @Override
    public List<Guide> selectALL() throws SQLException {
        List<Guide> guideList = new ArrayList<>();

        String req = "SELECT * FROM `guide`";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Guide g = new Guide(
                        rs.getInt("idUser"),
                        rs.getBoolean("disponibilite")
                );
                guideList.add(g);
            }
        }

        return guideList;
    }

    // ===================== SELECT BY ID =====================
    public Guide selectById(int idUser) throws SQLException {
        String req = "SELECT * FROM `guide` WHERE `idUser`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idUser);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Guide(
                            rs.getInt("idUser"),
                            rs.getBoolean("disponibilite")
                    );
                }
            }
        }

        return null;
    }

    // ===================== SELECT GUIDES DISPONIBLES =====================
    public List<Guide> selectGuidesDisponibles() throws SQLException {
        List<Guide> guideList = new ArrayList<>();

        String req = "SELECT * FROM `guide` WHERE `disponibilite` = TRUE";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Guide g = new Guide(
                        rs.getInt("idUser"),
                        rs.getBoolean("disponibilite")
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
