package tn.esprit.projet.services;

import tn.esprit.projet.entities.Achat;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AchatService implements CRUD<Achat> {

    private Connection cnx;

    public AchatService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    // ===================== INSERT =====================
    @Override
    public void insertOne(Achat achat) throws SQLException {
        // Validations
        if (achat.getIdClient() <= 0) {
            throw new IllegalArgumentException("L'ID client est obligatoire");
        }
        if (achat.getMontantTotal() < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif");
        }
        if (achat.getStatut() == null || achat.getStatut().isEmpty()) {
            throw new IllegalArgumentException("Le statut est obligatoire");
        }

        String req = "INSERT INTO `achat`(`dateAchat`, `idClient`, `montantTotal`, `statut`, `description`) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setTimestamp(1, new Timestamp(achat.getDateAchat().getTime()));
            ps.setInt(2, achat.getIdClient());
            ps.setDouble(3, achat.getMontantTotal());
            ps.setString(4, achat.getStatut());
            ps.setString(5, achat.getDescription());

            ps.executeUpdate();
        }
    }

    // ===================== UPDATE =====================
    @Override
    public void updateOne(Achat achat) throws SQLException {
        String req = "UPDATE `achat` SET `dateAchat`=?, `idClient`=?, `montantTotal`=?, `statut`=?, `description`=? " +
                "WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setTimestamp(1, new Timestamp(achat.getDateAchat().getTime()));
            ps.setInt(2, achat.getIdClient());
            ps.setDouble(3, achat.getMontantTotal());
            ps.setString(4, achat.getStatut());
            ps.setString(5, achat.getDescription());
            ps.setInt(6, achat.getIdAchat());

            ps.executeUpdate();
        }
    }

    // ===================== DELETE =====================
    @Override
    public void deleteOne(Achat achat) throws SQLException {
        String req = "DELETE FROM `achat` WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, achat.getIdAchat());
            ps.executeUpdate();
        }
    }

    // ===================== SELECT ALL =====================
    @Override
    public List<Achat> selectALL() throws SQLException {
        List<Achat> achatList = new ArrayList<>();

        String req = "SELECT * FROM `achat`";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Achat a = mapResultSetToAchat(rs);
                achatList.add(a);
            }
        }

        return achatList;
    }

    // ===================== SELECT BY ID =====================
    public Achat selectById(int idAchat) throws SQLException {
        String req = "SELECT * FROM `achat` WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idAchat);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAchat(rs);
                }
            }
        }

        return null;
    }

    // ===================== SELECT BY CLIENT =====================
    public List<Achat> selectByClient(int idClient) throws SQLException {
        List<Achat> achatList = new ArrayList<>();

        String req = "SELECT * FROM `achat` WHERE `idClient`=? ORDER BY `dateAchat` DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idClient);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Achat a = mapResultSetToAchat(rs);
                    achatList.add(a);
                }
            }
        }

        return achatList;
    }

    // ===================== SELECT BY STATUT =====================
    public List<Achat> selectByStatut(String statut) throws SQLException {
        List<Achat> achatList = new ArrayList<>();

        String req = "SELECT * FROM `achat` WHERE `statut`=? ORDER BY `dateAchat` DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, statut);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Achat a = mapResultSetToAchat(rs);
                    achatList.add(a);
                }
            }
        }

        return achatList;
    }

    // ===================== SELECT BY DATE RANGE =====================
    public List<Achat> selectByDateRange(Date dateDebut, Date dateFin) throws SQLException {
        List<Achat> achatList = new ArrayList<>();

        String req = "SELECT * FROM `achat` WHERE `dateAchat` BETWEEN ? AND ? ORDER BY `dateAchat` DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setTimestamp(1, new Timestamp(dateDebut.getTime()));
            ps.setTimestamp(2, new Timestamp(dateFin.getTime()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Achat a = mapResultSetToAchat(rs);
                    achatList.add(a);
                }
            }
        }

        return achatList;
    }

    // ===================== SELECT BY MONTANT RANGE =====================
    public List<Achat> selectByMontantRange(double minMontant, double maxMontant) throws SQLException {
        List<Achat> achatList = new ArrayList<>();

        String req = "SELECT * FROM `achat` WHERE `montantTotal` BETWEEN ? AND ? ORDER BY `montantTotal` DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setDouble(1, minMontant);
            ps.setDouble(2, maxMontant);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Achat a = mapResultSetToAchat(rs);
                    achatList.add(a);
                }
            }
        }

        return achatList;
    }

    // ===================== CHANGE STATUT =====================
    public void changeStatut(int idAchat, String newStatut) throws SQLException {
        String req = "UPDATE `achat` SET `statut`=? WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, newStatut);
            ps.setInt(2, idAchat);
            ps.executeUpdate();
        }
    }

    // ===================== UPDATE MONTANT =====================
    public void updateMontant(int idAchat, double newMontant) throws SQLException {
        String req = "UPDATE `achat` SET `montantTotal`=? WHERE `idAchat`=?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setDouble(1, newMontant);
            ps.setInt(2, idAchat);
            ps.executeUpdate();
        }
    }

    // ===================== COUNT ACHATS =====================
    public int countAchats() throws SQLException {
        String req = "SELECT COUNT(*) as count FROM `achat`";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        }

        return 0;
    }

    // ===================== HELPER MAPPER =====================
    private Achat mapResultSetToAchat(ResultSet rs) throws SQLException {
        return new Achat(
                rs.getInt("idAchat"),
                rs.getTimestamp("dateAchat"),
                rs.getInt("idClient"),
                rs.getDouble("montantTotal"),
                rs.getString("statut"),
                rs.getString("description")
        );
    }
}