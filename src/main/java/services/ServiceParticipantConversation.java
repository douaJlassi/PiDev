package services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import entities.Conversation;
import entities.Messages;
import entities.ParticipantConversation;
import entities.Utilisateur;
import utils.ElasticSearchClient;
import utils.MyDBConnexion;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceParticipantConversation implements CRUD<ParticipantConversation> {

    private Connection connection;
    public ServiceParticipantConversation() {
        connection= MyDBConnexion.getInstance().getConnection();
    }
    private ElasticsearchClient esClient= ElasticSearchClient.getInstance();

    private ServiceConversation serCnv = new ServiceConversation();
    private ServiceUtilisateur serUser = new ServiceUtilisateur();

    @Override
    public void insertOne(ParticipantConversation pc) throws SQLException {
        String query= "INSERT INTO `participantConversation` (idConversation, idUtilisateur, dateAjout, estActif) VALUES (?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, pc.getConversation().getIdConversation());
        ps.setInt(2, pc.getParticipant().getIdUtilisateur());
        ps.setTimestamp(3, Timestamp.valueOf(pc.getDateAjout()));
        ps.setBoolean(4, pc.isEstActif());
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            pc.setIdParticipant(rs.getInt(1));
        }
        Map<String, Object> esData = new HashMap<>();
        esData.put("idParticipant", pc.getIdParticipant());
        esData.put("idConversation", pc.getConversation().getIdConversation());
        esData.put("titreConversation", pc.getConversation().getTitre() != null ? pc.getConversation().getTitre() : "Privé");
        esData.put("nomUtilisateur", pc.getParticipant().getPrenom() + " " + pc.getParticipant().getNom());
        esData.put("dateAjout", pc.getDateAjout().toString());

        try {
            esClient.index(i -> i
                    .index("participants")
                    .id(String.valueOf(pc.getIdParticipant()))
                    .document(esData)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                    rs.getTimestamp(4).toLocalDateTime(),
                    rs.getBoolean("estActif")
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
                    rs.getTimestamp(4).toLocalDateTime(),
                    rs.getBoolean("estActif")
            );
        }
        return null;
    }
    public List<Utilisateur> getParticipantsByConversation(int idCnv) throws SQLException {
        List<Utilisateur> participants = new ArrayList<>();
        String query = "SELECT idUtilisateur FROM `participantConversation` WHERE idConversation = ? AND estActif = 1";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idCnv);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idUser = rs.getInt("idUtilisateur");
                    Utilisateur u = serUser.selectOne(idUser);

                    if (u != null) {
                        participants.add(u);
                    }
                }
            }
        }
        return participants;
    }

    public void quitterConversation(int idUtilisateur, int idConversation) throws SQLException {
        String query = "UPDATE `participantConversation` SET estActif = 0 " +
                "WHERE idUtilisateur = ? AND idConversation = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idUtilisateur);
            ps.setInt(2, idConversation);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("L'utilisateur " + idUtilisateur + " est maintenant inactif dans la conversation " + idConversation);
            }
            Map<String, Object> esData = new HashMap<>();
            esData.put("idUtilisateur", idUtilisateur);
            esData.put("idConversation", idConversation);
            esData.put("estActif", 0);
            String esId = idUtilisateur + "_" + idConversation;

            esClient.index(i -> i
                    .index("participants")
                    .id(esId)
                    .document(esData)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean isUserActiveInConversation(int idUser, int idConv) throws SQLException {
        String query = "SELECT estActif FROM `participantConversation` WHERE idUtilisateur = ? AND idConversation = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idConv);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("estActif");
            }
        }
        return false;
    }

    public Integer findExistingGroupWithMembers(Set<Integer> memberIds) throws SQLException {
        String idsFormatted = memberIds.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));

        String query = "SELECT pc.idConversation " +
                "FROM participantConversation pc " +
                "JOIN conversation c ON pc.idConversation = c.idConversation " +
                "WHERE c.type = 'GROUPE' " +
                "GROUP BY pc.idConversation " +
                "HAVING COUNT(pc.idUtilisateur) = ? " +
                "AND COUNT(CASE WHEN pc.idUtilisateur IN (" + idsFormatted + ") THEN 1 END) = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, memberIds.size());
            ps.setInt(2, memberIds.size());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1); // Retourne l'ID de la conversation trouvée
            }
        }
        return null;
    }
}
