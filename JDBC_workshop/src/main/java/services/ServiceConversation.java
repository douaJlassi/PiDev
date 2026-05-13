package services;

import entities.Conversation;
import entities.TypeConversation;
import utils.MyDBConnexion;

import java.io.IOException;
import java.sql.*;
import java.util.*;

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


    }

    public String getNomAffichage(Conversation cnv, int idUserConnected){
        if (cnv.getTypeConversation() == TypeConversation.GROUPE) {
            return (cnv.getTitre() != null && !cnv.getTitre().isEmpty()) ? cnv.getTitre() : "Groupe sans nom";
        }
        String query= "SELECT u.name, u.last_name FROM user u " +
                "JOIN participantConversation pc ON u.id = pc.idUtilisateur " +
                "WHERE pc.idConversation = ? AND u.id != ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setInt(1, cnv.getIdConversation());
            ps.setInt(2, idUserConnected);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getString("last_name")+" "+rs.getString("name");
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
        String query = "SELECT c.*, MAX(m.dateEnvoi)" +
                "FROM conversation c " +
                "JOIN participantConversation pc ON c.idConversation = pc.idConversation " +
                "LEFT JOIN message m ON c.idConversation = m.idConversation " +
                "WHERE pc.idUtilisateur = ? " +
                "GROUP BY c.idConversation " +
                "ORDER BY COALESCE(MAX(m.dateEnvoi), c.dateCreation) DESC";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, idUserConnected);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                conversations.add(new Conversation(
                        rs.getInt("idConversation"),
                        TypeConversation.valueOf(rs.getString("type")),
                        rs.getTimestamp("dateCreation").toLocalDateTime(),
                        rs.getString("titre")
                ));
            }
        }
        return conversations;
    }

    public Conversation findPrivateChat(int user1Id, int user2Id) throws SQLException {
        String query = "SELECT c.* FROM conversation c " +
                "JOIN participantConversation pc1 ON c.idConversation = pc1.idConversation " +
                "JOIN participantConversation pc2 ON c.idConversation = pc2.idConversation " +
                "WHERE c.type = 'PRIVEE' AND pc1.idUtilisateur = ? AND pc2.idUtilisateur = ?";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, user1Id);
            ps.setInt(2, user2Id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Conversation(
                        rs.getInt("idConversation"),
                        TypeConversation.valueOf(rs.getString("type")),
                        rs.getTimestamp("dateCreation").toLocalDateTime(),
                        rs.getString("titre")
                );
            }
        }
        return null;
    }

    // Pour le PieChart (PRIVEE vs GROUPE)
    public Map<String, Integer> getStatsByType() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String query = "SELECT type, COUNT(*) FROM conversation GROUP BY type";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString(1), rs.getInt(2));
            }
        }
        return stats;
    }

    // Pour le BarChart (Taille des groupes)
    public Map<String, Integer> getGroupSizeStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String query = "SELECT titre, COUNT(idUtilisateur) FROM conversation c " +
                "JOIN participantconversation pc ON c.idConversation = pc.idConversation " +
                "WHERE c.type = 'GROUPE' GROUP BY c.idConversation";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString(1), rs.getInt(2));
            }
        }
        return stats;
    }
}
