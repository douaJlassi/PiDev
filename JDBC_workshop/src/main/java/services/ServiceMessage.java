package services;

import entities.*;
import utils.MyDBConnexion;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class ServiceMessage implements CRUD<Messages>{

    private Connection cnx;
    public ServiceMessage(){
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    private ServiceConversation serConv = new ServiceConversation();
    private ServiceUtilisateur serUtilisateur = new ServiceUtilisateur();

    @Override
    public void insertOne(Messages message) throws SQLException {
        String query= "INSERT INTO `message`(`contenu`, `dateEnvoi`, `lu`, `idConversation`, `idExpediteur`, `typeMessage`, `urlFichier`) VALUES (?,?,?,?,?,?,?)";

        PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        pst.setString(1, message.getContenu());
        pst.setTimestamp(2, Timestamp.valueOf(message.getDateEnvoi()));
        pst.setBoolean(3, message.isLu());
        pst.setInt(4,message.getConversation().getIdConversation());
        pst.setInt(5,message.getExpediteur().getId());
        pst.setString(6, message.getTypeMessage().name());
        pst.setString(7, message.getUrlFichier());
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if(rs.next()){
            message.setIdMessage(rs.getInt(1));
        }
        System.out.println("Message envoyé");

        String updateQuery = "UPDATE `message` SET lu = 1 " +
                "WHERE idConversation = ? AND idExpediteur != ? AND lu = 0";
        pst = cnx.prepareStatement(updateQuery);
        pst.setInt(1, message.getConversation().getIdConversation());
        pst.setInt(2, message.getExpediteur().getId());
        pst.executeUpdate();

    }

    @Override
    public void updateOne(Messages message) throws SQLException {
        String query= "UPDATE `message` SET contenu=? WHERE idMessage=?";

        PreparedStatement pst = cnx.prepareStatement(query);

        pst.setString(1, message.getContenu());
        pst.setInt(2, message.getIdMessage());

        pst.executeUpdate();
        System.out.println("Message modifié");
    }

    @Override
    public void deleteOne(Messages m) throws SQLException {
        String query = "UPDATE `message` SET isDeleted = 1 WHERE idMessage = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, m.getIdMessage());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Messages> selectALL() throws SQLException {
        List<Messages> messages= new ArrayList<>();
        String query= "SELECT * FROM `message` ORDER BY dateEnvoi ASC";
        PreparedStatement pst = cnx.prepareStatement(query);
        ResultSet rs = pst.executeQuery();
        while(rs.next()){
            int idConv = rs.getInt("idConversation");
            Conversation conv = serConv.selectOne(idConv);
            int idExp = rs.getInt("idExpediteur");
            Person exp = serUtilisateur.selectOne(idExp);
            messages.add(new Messages(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getTimestamp(3).toLocalDateTime(),
                    rs.getBoolean(4),
                    conv,
                    exp,
                    TypeMessage.valueOf(rs.getString("typeMessage")),
                    rs.getString("urlFichier"),
                    rs.getString("reaction"),
                    rs.getBoolean("isDeleted")
            ));
        }
        return messages;
    }

    public Messages selectOne(int id) throws SQLException {
        String query= "SELECT * FROM `message` WHERE idMessage=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if(rs.next()){
            int idConv = rs.getInt("idConversation");
            Conversation conv = serConv.selectOne(idConv);
            int idExp = rs.getInt("idExpediteur");
            Person exp = serUtilisateur.selectOne(idExp);
            return new Messages(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getTimestamp(3).toLocalDateTime(),
                    rs.getBoolean(4),
                    conv,
                    exp,
                    TypeMessage.valueOf(rs.getString("typeMessage")),
                    rs.getString("urlFichier"),
                    rs.getString("reaction"),
                    rs.getBoolean("isDeleted")
            );
        }
        return null;
    }

    public List<Messages> selectByConversation(int idConversation, int idUserConnecte) throws SQLException {
        List<Messages> messages = new ArrayList<>();

        String query = "SELECT m.* FROM message m " +
                "JOIN participantConversation pc ON m.idConversation = pc.idConversation " +
                "WHERE m.idConversation = ? AND pc.idUtilisateur = ? " +
                "AND (pc.estActif = 1 OR m.dateEnvoi <= pc.dateSortie) " +
                "ORDER BY m.dateEnvoi ASC";

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, idConversation);
            pst.setInt(2, idUserConnecte);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Conversation conv = serConv.selectOne(rs.getInt("idConversation"));
                Person exp = serUtilisateur.selectOne(rs.getInt("idExpediteur"));

                messages.add(new Messages(
                        rs.getInt("idMessage"),
                        rs.getString("contenu"),
                        rs.getTimestamp("dateEnvoi").toLocalDateTime(),
                        rs.getBoolean("lu"),
                        conv,
                        exp,
                        TypeMessage.valueOf(rs.getString("typeMessage")),
                        rs.getString("urlFichier"),
                        rs.getString("reaction"),
                        rs.getBoolean("isDeleted")
                ));
            }
        }
        return messages;
    }
    public Messages selectLastMessage(int idConversation) throws SQLException {

        String query = "SELECT * FROM `message` WHERE idConversation=? ORDER BY dateEnvoi Desc LIMIT 1";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, idConversation);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int idConv = rs.getInt("idConversation");
            Conversation conv = serConv.selectOne(idConv);
            int idExp = rs.getInt("idExpediteur");
            Person exp = serUtilisateur.selectOne(idExp);
            return new Messages(rs.getInt("idMessage"),
                    rs.getString("contenu"),
                    rs.getTimestamp("dateEnvoi").toLocalDateTime(),
                    rs.getBoolean("lu"),
                    conv,
                    exp,
                    TypeMessage.valueOf(rs.getString("typeMessage")),
                    rs.getString("urlFichier"),
                    rs.getString("reaction"),
                    rs.getBoolean("isDeleted")

            );
        }
        return null;
    }

    public void marquerCommeLu(int idConv, int idUser) throws SQLException {
        String query = "UPDATE `message` SET lu = 1 WHERE idConversation = ? AND idExpediteur != ? AND lu = 0";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, idConv);
            ps.setInt(2, idUser);
            ps.executeUpdate();
        }
    }

    public int countUnreadMessages(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM message m " +
                "JOIN participantConversation pc ON m.idConversation = pc.idConversation " +
                "WHERE pc.idUtilisateur = ? AND m.lu = 0 AND m.idExpediteur != ?";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<Messages> getMediaHistory(int idConv) throws SQLException {
        List<Messages> medias = new ArrayList<>();
        String query = "SELECT * FROM `message` WHERE idConversation = ? AND typeMessage != 'TEXTE' AND typeMessage != 'LOCATION' AND typeMessage !='AUDIO' ORDER BY dateEnvoi DESC";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, idConv);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int idConversation = rs.getInt("idConversation");
                Conversation conv = serConv.selectOne(idConversation);
                int idExp = rs.getInt("idExpediteur");
                Person exp = serUtilisateur.selectOne(idExp);
                medias.add(new Messages(
                        rs.getInt("idMessage"),
                        rs.getString("contenu"),
                        rs.getTimestamp("dateEnvoi").toLocalDateTime(),
                        rs.getBoolean("lu"),
                        conv,
                        exp,
                        TypeMessage.valueOf(rs.getString("typeMessage")),
                        rs.getString("urlFichier"),
                        rs.getString("reaction"),
                        rs.getBoolean("isDeleted")
                ));
            }
        }
        return medias;
    }

    public void updateReaction(int idMsg, String emoji) throws SQLException {
        String query = "UPDATE `message` SET reaction = ? WHERE idMessage = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, emoji);
            ps.setInt(2, idMsg);
            ps.executeUpdate();
        }
    }

    // Pour le PieChart (TEXTE, IMAGE, AUDIO...)
    public Map<String, Integer> getMediaTypeStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String query = "SELECT typeMessage, COUNT(*) FROM message GROUP BY typeMessage";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString(1), rs.getInt(2));
            }
        }
        return stats;
    }

    // Pour la courbe d'activité (Messages par jour)
    public Map<String, Integer> getDailyActivityStats() throws SQLException {
        Map<String, Integer> stats = new TreeMap<>(); // TreeMap pour garder les dates triées
        String query = "SELECT DATE(dateEnvoi), COUNT(*) FROM message GROUP BY DATE(dateEnvoi) ORDER BY DATE(dateEnvoi) ASC LIMIT 10";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString(1), rs.getInt(2));
            }
        }
        return stats;
    }
}
