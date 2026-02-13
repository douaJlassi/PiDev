package services;

import entities.Conversation;
import entities.Message;
import entities.Utilisateur;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMessage implements CRUD<Message>{

    private Connection cnx;
    public ServiceMessage(){
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    private ServiceConversation serConv = new ServiceConversation();
    private ServiceUtilisateur serUtilisateur = new ServiceUtilisateur();

    @Override
    public void insertOne(Message message) throws SQLException {
        String query= "INSERT INTO `message`(`contenu`, `dateEnvoi`, `lu`, `idConversation`, `idExpediteur`) VALUES (?,?,?,?,?)";

        PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        pst.setString(1, message.getContenu());
        pst.setTimestamp(2, Timestamp.valueOf(message.getDateEnvoi()));
        pst.setBoolean(3, message.isLu());
        pst.setInt(4,message.getConversation().getIdConversation());
        pst.setInt(5,message.getExpediteur().getIdUtilisateur());
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
        System.out.println("Message envoyé et anciens messages marqués comme lus !");
    }

    @Override
    public void updateOne(Message message) throws SQLException {
        String query= "UPDATE `message` SET contenu=? WHERE idMessage=?";

        PreparedStatement pst = cnx.prepareStatement(query);

        pst.setString(1, message.getContenu());
        pst.setInt(2, message.getIdMessage());

        pst.executeUpdate();
        System.out.println("Message modifié");
    }

    @Override
    public void deleteOne(Message message) throws SQLException {
        String query= "DELETE FROM `message` WHERE idMessage=?";

        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, message.getIdMessage());

        pst.executeUpdate();
        System.out.println("Message supprimé");

    }

    @Override
    public List<Message> selectALL() throws SQLException {
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
                    exp
            ));
        }
        return messages;
    }

    @Override
    public Message selectOne(int id) throws SQLException {
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
                    exp);
        }
        return null;
    }

    public List<Message> selectByConversation(int idConversation) throws SQLException {
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
                    exp));
        }
        return messages;
    }
}
