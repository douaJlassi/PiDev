package services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import entities.Conversation;
import entities.Message;
import entities.TypeMessage;
import entities.Utilisateur;
import utils.ElasticSearchClient;
import utils.MyDBConnexion;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceMessage implements CRUD<Message>{

    /*private Connection cnx;
    public ServiceMessage(){
        cnx = MyDBConnexion.getInstance().getConnection();
    }*/


    private ElasticsearchClient esClient= ElasticSearchClient.getInstance();

    private ServiceConversation serConv = new ServiceConversation();
    private ServiceUtilisateur serUtilisateur = new ServiceUtilisateur();

    @Override
    public void insertOne(Message message) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String query= "INSERT INTO `message`(`contenu`, `dateEnvoi`, `lu`, `idConversation`, `idExpediteur`, `typeMessage`, `urlFichier`) VALUES (?,?,?,?,?,?,?)";

        PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        pst.setString(1, message.getContenu());
        pst.setTimestamp(2, Timestamp.valueOf(message.getDateEnvoi()));
        pst.setBoolean(3, message.isLu());
        pst.setInt(4,message.getConversation().getIdConversation());
        pst.setInt(5,message.getExpediteur().getIdUtilisateur());
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
        pst.setInt(2, message.getExpediteur().getIdUtilisateur());
        pst.executeUpdate();
        try {
            Map<String, Object> esData = new HashMap<>();
            esData.put("idMessage", message.getIdMessage());
            esData.put("contenu",  message.getContenu());
            esData.put("dateEnvoi", message.getDateEnvoi().toString());
            esData.put("typeMessage", message.getTypeMessage().name());
            esData.put("urlFichier", message.getUrlFichier());
            esData.put("idConversation", message.getConversation().getIdConversation());
            esData.put("expediteurNom", message.getExpediteur().getPrenom()+" "+ message.getExpediteur().getNom());
            esData.put("lu", message.isLu());
            esClient.index(i -> i
                    .index("messages")
                    .id(String.valueOf(message.getIdMessage()))
                    .document(esData)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateOne(Message message) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String query= "UPDATE `message` SET contenu=? WHERE idMessage=?";

        PreparedStatement pst = cnx.prepareStatement(query);

        pst.setString(1, message.getContenu());
        pst.setInt(2, message.getIdMessage());

        pst.executeUpdate();
        System.out.println("Message modifié");
    }

    @Override
    public void deleteOne(Message m) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String query = "UPDATE `message` SET isDeleted = 1 WHERE idMessage = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, m.getIdMessage());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Message> selectALL() throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        List<Message> messages= new ArrayList<>();
        String query= "SELECT * FROM `message` ORDER BY dateEnvoi ASC";
        PreparedStatement pst = cnx.prepareStatement(query);
        ResultSet rs = pst.executeQuery();
        while(rs.next()){
            int idConv = rs.getInt("idConversation");
            Conversation conv = serConv.selectOne(idConv);
            int idExp = rs.getInt("idExpediteur");
            Utilisateur exp = serUtilisateur.selectOne(idExp);
            messages.add(new Message(
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


    public Message selectOne(int id) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String query= "SELECT * FROM `message` WHERE idMessage=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if(rs.next()){
            int idConv = rs.getInt("idConversation");
            Conversation conv = serConv.selectOne(idConv);
            int idExp = rs.getInt("idExpediteur");
            Utilisateur exp = serUtilisateur.selectOne(idExp);
            return new Message(
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

    public List<Message> selectByConversation(int idConversation) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM `message` WHERE idConversation=? ORDER BY dateEnvoi ASC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, idConversation);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            int idConv = rs.getInt("idConversation");
            Conversation conv = serConv.selectOne(idConv);
            int idExp = rs.getInt("idExpediteur");
            Utilisateur exp = serUtilisateur.selectOne(idExp);
            messages.add(new Message(rs.getInt(1),
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

    public Message selectLastMessage(int idConversation) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String query = "SELECT * FROM `message` WHERE idConversation=? ORDER BY dateEnvoi Desc LIMIT 1";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, idConversation);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int idConv = rs.getInt("idConversation");
            Conversation conv = serConv.selectOne(idConv);
            int idExp = rs.getInt("idExpediteur");
            Utilisateur exp = serUtilisateur.selectOne(idExp);
            return new Message(rs.getInt("idMessage"),
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
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String query = "UPDATE `message` SET lu = 1 WHERE idConversation = ? AND idExpediteur != ? AND lu = 0";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, idConv);
            ps.setInt(2, idUser);
            ps.executeUpdate();
        }
    }

    public int countUnreadMessages(int userId) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
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

    public List<Message> getMediaHistory(int idConv) throws SQLException {
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        List<Message> medias = new ArrayList<>();
        String query = "SELECT * FROM `message` WHERE idConversation = ? AND typeMessage != 'TEXTE' ORDER BY dateEnvoi DESC";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, idConv);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int idConversation = rs.getInt("idConversation");
                Conversation conv = serConv.selectOne(idConversation);
                int idExp = rs.getInt("idExpediteur");
                Utilisateur exp = serUtilisateur.selectOne(idExp);
                medias.add(new Message(
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
        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String query = "UPDATE `message` SET reaction = ? WHERE idMessage = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, emoji);
            ps.setInt(2, idMsg);
            ps.executeUpdate();
        }
    }
}
