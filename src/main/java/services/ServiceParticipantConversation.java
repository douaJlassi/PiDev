package services;

import entities.Conversation;
import entities.Message;
import entities.ParticipantConversation;
import entities.Utilisateur;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceParticipantConversation implements CRUD<ParticipantConversation> {

    private Connection connection;
    public ServiceParticipantConversation() {
        connection= MyDBConnexion.getInstance().getConnection();
    }

    private ServiceConversation serCnv = new ServiceConversation();
    private ServiceUtilisateur serUser = new ServiceUtilisateur();

    @Override
    public void insertOne(ParticipantConversation pc) throws SQLException {
        String query= "INSERT INTO `participantConversation` (idConversation, idUtilisateur, dateAjout) VALUES (?,?,?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, pc.getConversation().getIdConversation());
        ps.setInt(2, pc.getParticipant().getIdUtilisateur());
        ps.setTimestamp(3, Timestamp.valueOf(pc.getDateAjout()));
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            pc.setIdParticipant(rs.getInt(1));
        }
    }

    @Override
    public void updateOne(ParticipantConversation pc) throws SQLException {
        String query = "UPDATE `participantConversation` SET idConversation=?, idUtilisateur=?, dateAjout=? WHERE idParticipant=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, pc.getConversation().getIdConversation());
        ps.setInt(2, pc.getParticipant().getIdUtilisateur());
        ps.setTimestamp(3, Timestamp.valueOf(pc.getDateAjout()));
        ps.setInt(4, pc.getIdParticipant());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(ParticipantConversation pc) throws SQLException {
        String query = "DELETE FROM `participantConversation` WHERE idParticipant=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, pc.getIdParticipant());
        ps.executeUpdate();
    }

    @Override
    public List<ParticipantConversation> selectALL() throws SQLException {
        List<ParticipantConversation> list = new ArrayList<>();
        String query = "SELECT * FROM `participantConversation`";
        PreparedStatement ps = connection.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int idCnv = rs.getInt("idConversation");
            Conversation cnv = serCnv.selectOne(idCnv);
            int idUser = rs.getInt("idUtilisateur");
            Utilisateur user = serUser.selectOne(idUser);
            list.add(new ParticipantConversation(
                    rs.getInt(1),
                    user,
                    cnv,
                    rs.getTimestamp(4).toLocalDateTime()
            ));
        }
        return list;
    }

    @Override
    public ParticipantConversation selectOne(int id) throws SQLException {
        String query = "SELECT * FROM `participantConversation` WHERE idParticipant=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int idcnv = rs.getInt("idConversation");
            Conversation cnv = serCnv.selectOne(idcnv);
            int idUser = rs.getInt("idUtilisateur");
            Utilisateur user = serUser.selectOne(idUser);

            return new ParticipantConversation(
                    rs.getInt(1),
                    user,
                    cnv,
                    rs.getTimestamp(4).toLocalDateTime()
            );
        }
        return null;
    }

    public List<Utilisateur> getParticipantsByConversation(int idCnv) throws SQLException {
        List<Utilisateur> participants = new ArrayList<>();
        String query = "SELECT * FROM `participantConversation` WHERE idParticipant=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, idCnv);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            participants.add(serUser.selectOne(rs.getInt("idUtilisateur")));
        }
        return participants;
    }

    public void quitterConversation(int idUtilisateur, int idConversation) throws SQLException {

        String query = "DELETE FROM `participantConversation` WHERE idUtilisateur = ? AND idConversation = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idUtilisateur);
            ps.setInt(2, idConversation);

            int rowsDeleted = ps.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("L'utilisateur " + idUtilisateur + " a quitté la conversation " + idConversation);
            } else {
                System.out.println("Aucun lien trouvé pour cet utilisateur dans cette conversation.");
            }
        }
    }
}
