package services;

import entities.Conversation;
import entities.TypeConversation;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceConversation implements CRUD<Conversation>{

    private Connection cnx;
    public ServiceConversation(){
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(Conversation conversation) throws SQLException {
        String query= "INSERT INTO `conversation`(`type`, `dateCreation`, `titre`) VALUES (?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1,conversation.getTypeConversation().name());
        ps.setTimestamp(2, Timestamp.valueOf(conversation.getDateCreation()));
        if(conversation.getTypeConversation() == TypeConversation.GROUPE){
            ps.setString(3,conversation.getTitre());
        }else {
            ps.setNull(3,Types.VARCHAR);
        }
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            conversation.setIdConversation(rs.getInt(1));
        }
        System.out.println("Conversation créée");
    }

    public String getNomAffichage(Conversation cnv, int idUserConnected){
        String query= "SELECT u.nom, u.prenom FROM utilisateur u " +
                "JOIN participant_conversation pc ON u.idUtilisateur = pc.idUtilisateur " +
                "WHERE pc.idConversation = ? AND u.idUtilisateur != ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setInt(1, cnv.getIdConversation());
            ps.setInt(2, idUserConnected);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getString("prenom")+" "+rs.getString("nom");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return "Utilisateur inconnu";
    }

    @Override
    public void updateOne(Conversation conversation) throws SQLException {
        String query= "UPDATE `conversation` SET type=?, titre=? WHERE idConversation=?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setString(1,conversation.getTypeConversation().name());
        if(conversation.getTypeConversation() == TypeConversation.GROUPE){
            ps.setString(2,conversation.getTitre());
        }else {
            ps.setNull(2,Types.VARCHAR);
        }
        ps.setInt(3,conversation.getIdConversation());
        ps.executeUpdate();
        System.out.println("Conversation modifiée");
    }

    @Override
    public void deleteOne(Conversation conversation) throws SQLException {
        String query= "DELETE FROM `conversation` WHERE idConversation=?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1,conversation.getIdConversation());
        ps.executeUpdate();
        System.out.println("Conversation supprimée");
    }

    @Override
    public List<Conversation> selectALL() throws SQLException {
        List<Conversation> conversations = new ArrayList<>();
        String query = "SELECT * FROM `conversation`";
        PreparedStatement ps = cnx.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            conversations.add(new Conversation(
                    rs.getInt(1),
                    TypeConversation.valueOf(rs.getString("type")),
                    rs.getTimestamp(3).toLocalDateTime(),
                    rs.getString(4)
            ));
        }
        return conversations;
    }

    @Override
    public Conversation selectOne(int id) throws SQLException {
        String query= "SELECT * FROM `conversation` WHERE idConversation=?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1,id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Conversation(
                    rs.getInt(1),
                    TypeConversation.valueOf(rs.getString("type")),
                    rs.getTimestamp(3).toLocalDateTime(),
                    rs.getString(4)
            );
        }
        return null;
    }

    public List<Conversation> selectByUser(int idUserConnected) throws SQLException {
        List<Conversation> conversations = new ArrayList<>();
        String query = "SELECT c.* FROM conversation c " +
                "JOIN participant_conversation pc ON c.idConversation = pc.idConversation " +
                "WHERE pc.idUtilisateur = ?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, idUserConnected);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            conversations.add(new Conversation(
                    rs.getInt(1),
                    TypeConversation.valueOf(rs.getString("type")),
                    rs.getTimestamp(3).toLocalDateTime(),
                    rs.getString(4)
            ));
        }
        return conversations;
    }
}
