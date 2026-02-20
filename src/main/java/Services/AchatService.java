package Services;

import gestion_activite.Achat;
import gestion_activite.AchatActivite;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;



public class AchatService
{

    private Connection cnx;

    public AchatService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }
    public void insertAchat(Achat achat, List<AchatActivite> lignes) throws SQLException {
        String insertAchat = "INSERT INTO achat (dateAchat, montantTotal, statut, idClient) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = cnx.prepareStatement(insertAchat, Statement.RETURN_GENERATED_KEYS);
        pstmt.setTimestamp(1, achat.getDateAchat());
        pstmt.setDouble(2, achat.getMontantTotal());
        pstmt.setString(3, achat.getStatut());
        pstmt.setInt(4, achat.getIdClient());
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys();
        int idAchat = 0;
        if (rs.next()) {
            idAchat = rs.getInt(1);
        }


        String insertLigne = "INSERT INTO achat_activite (idAchat, idActivite, quantite, prixUnitaire) VALUES (?, ?, ?, ?)";
        pstmt = cnx.prepareStatement(insertLigne);
        for (AchatActivite ligne : lignes) {
            pstmt.setInt(1, idAchat);
            pstmt.setInt(2, ligne.getIdActivite());
            pstmt.setInt(3, ligne.getQuantite());
            pstmt.setDouble(4, ligne.getPrixUnitaire());
            pstmt.addBatch();
        }
        pstmt.executeBatch();
    }


    public List<Achat> selectAll() throws SQLException {
        List<Achat> achats = new ArrayList<>();
        String req = "SELECT * FROM achat";
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery(req);
        while (rs.next()) {
            Achat a = new Achat(
                    rs.getInt("idAchat"),
                    rs.getTimestamp("dateAchat"),
                    rs.getDouble("montantTotal"),
                    rs.getString("statut"),
                    rs.getInt("idClient")
            );
            achats.add(a);
        }
        return achats;
    }


    public Achat selectById(int id) throws SQLException {
        String reqAchat = "SELECT * FROM achat WHERE idAchat = ?";
        PreparedStatement pstmt = cnx.prepareStatement(reqAchat);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();
        Achat achat = null;
        if (rs.next()) {
            achat = new Achat(
                    rs.getInt("idAchat"),
                    rs.getTimestamp("dateAchat"),
                    rs.getDouble("montantTotal"),
                    rs.getString("statut"),
                    rs.getInt("idClient")
            );


            String reqLignes = "SELECT * FROM achat_activite WHERE idAchat = ?";
            pstmt = cnx.prepareStatement(reqLignes);
            pstmt.setInt(1, id);
            ResultSet rsLignes = pstmt.executeQuery();
            List<AchatActivite> lignes = new ArrayList<>();
            while (rsLignes.next()) {
                AchatActivite ligne = new AchatActivite(
                        rsLignes.getInt("idAchat"),
                        rsLignes.getInt("idActivite"),
                        rsLignes.getInt("quantite"),
                        rsLignes.getDouble("prixUnitaire")
                );
                lignes.add(ligne);
            }

        }
        return achat;
    }


    public void updateAchat(Achat achat) throws SQLException {
        String req = "UPDATE achat SET dateAchat=?, montantTotal=?, statut=?, idClient=? WHERE idAchat=?";
        PreparedStatement pstmt = cnx.prepareStatement(req);
        pstmt.setTimestamp(1, achat.getDateAchat());
        pstmt.setDouble(2, achat.getMontantTotal());
        pstmt.setString(3, achat.getStatut());
        pstmt.setInt(4, achat.getIdClient());
        pstmt.setInt(5, achat.getIdAchat());
        pstmt.executeUpdate();
    }


    public void deleteAchat(int id) throws SQLException {
        String req = "DELETE FROM achat WHERE idAchat = ?";
        PreparedStatement pstmt = cnx.prepareStatement(req);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
    }


    public List<Achat> selectByClient(int idClient) throws SQLException {
        List<Achat> achats = new ArrayList<>();
        String req = "SELECT * FROM achat WHERE idClient = ?";
        PreparedStatement pstmt = cnx.prepareStatement(req);
        pstmt.setInt(1, idClient);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            Achat a = new Achat(
                    rs.getInt("idAchat"),
                    rs.getTimestamp("dateAchat"),
                    rs.getDouble("montantTotal"),
                    rs.getString("statut"),
                    rs.getInt("idClient")
            );
            achats.add(a);
        }
        return achats;
    }
}