package Services;

import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteAchatService
{

    private Connection cnx;

    public ActiviteAchatService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    // ===================== AJOUTER ASSOCIATION =====================
    public void ajouterActiviteAchat(int idActivite, int idAchat) throws SQLException {
        String req = "INSERT INTO `activite_achat` (`idActivite`, `idAchat`) VALUES (?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idActivite);
            ps.setInt(2, idAchat);
            ps.executeUpdate();
        }
    }

    // ===================== SUPPRIMER ASSOCIATION =====================
    public void supprimerActiviteAchat(int idActivite, int idAchat) throws SQLException {
        String req = "DELETE FROM `activite_achat` WHERE `idActivite`=? AND `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idActivite);
            ps.setInt(2, idAchat);
            ps.executeUpdate();
        }
    }

    // ===================== LISTER ACTIVITES PAR ACHAT =====================
    public List<Integer> getActivitesByAchat(int idAchat) throws SQLException {
        List<Integer> activites = new ArrayList<>();

        String req = "SELECT `idActivite` FROM `activite_achat` WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idAchat);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    activites.add(rs.getInt("idActivite"));
                }
            }
        }

        return activites;
    }

    // ===================== LISTER ACHATS PAR ACTIVITE =====================
    public List<Integer> getAchatsByActivite(int idActivite) throws SQLException {
        List<Integer> achats = new ArrayList<>();

        String req = "SELECT `idAchat` FROM `activite_achat` WHERE `idActivite`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idActivite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    achats.add(rs.getInt("idAchat"));
                }
            }
        }

        return achats;
    }

    // ===================== COMPTER ACTIVITES PAR ACHAT =====================
    public int countActivitesByAchat(int idAchat) throws SQLException {
        String req = "SELECT COUNT(*) as count FROM `activite_achat` WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idAchat);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }

        return 0;
    }

    // ===================== SUPPRIMER TOUTES LES ASSOCIATIONS D'UN ACHAT =====================
    public void supprimerAllActivitesAchat(int idAchat) throws SQLException {
        String req = "DELETE FROM `activite_achat` WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idAchat);
            ps.executeUpdate();
        }
    }
}