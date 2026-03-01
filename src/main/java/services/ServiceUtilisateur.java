package services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import entities.Role;
import entities.Utilisateur;
import utils.ElasticSearchClient;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceUtilisateur implements CRUD<Utilisateur> {
    private Connection connection;
    public ServiceUtilisateur() {
        connection= MyDBConnexion.getInstance().getConnection();
    }
    private ElasticsearchClient esClient= ElasticSearchClient.getInstance();

    @Override
    public void insertOne(Utilisateur utilisateur) throws SQLException {
        String query="INSERT INTO `user`(`nom`, `prenom`, `email`, `password`, `telephone`, `role`, `dateCreation`, `statut`) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps =  connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, utilisateur.getNom());
        ps.setString(2, utilisateur.getPrenom());
        ps.setString(3, utilisateur.getEmail());
        ps.setString(4, utilisateur.getPassword());
        ps.setString(5, utilisateur.getTelephone());
        ps.setString(6, utilisateur.getRole().name());
        ps.setTimestamp(7, Timestamp.valueOf(utilisateur.getDateCreation()));
        ps.setBoolean(8, utilisateur.isStatus());
        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                utilisateur.setIdUtilisateur(rs.getInt(1));
            }
        }
        try {
            Map<String, Object> esData = new HashMap<>();
            esData.put("idUser", utilisateur.getIdUtilisateur());
            esData.put("nomComplet", utilisateur.getPrenom() + " " + utilisateur.getNom());
            esData.put("email", utilisateur.getEmail());
            esData.put("role", utilisateur.getRole().name());
            esData.put("dateCreation", utilisateur.getDateCreation().toString());
            esData.put("status", utilisateur.isStatus());

            esClient.index(i -> i
                    .index("utilisateurs")
                    .id(String.valueOf(utilisateur.getIdUtilisateur()))
                    .document(esData)
            );
        } catch (Exception e) { System.err.println("Erreur ES User: " + e.getMessage()); }

    }

    @Override
    public void updateOne(Utilisateur utilisateur) throws SQLException {

    }

    @Override
    public void deleteOne(Utilisateur utilisateur) throws SQLException {

    }

    @Override
    public List<Utilisateur> selectALL() throws SQLException {
        String query= "SELECT * FROM `user`";
        PreparedStatement ps =  connection.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        List<Utilisateur> utilisateurs = new ArrayList<>();
        while (rs.next()) {
            utilisateurs.add(new Utilisateur(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    Role.valueOf(rs.getString("role")),
                    rs.getTimestamp("dateCreation").toLocalDateTime(),
                    rs.getBoolean("statut")
            ));
        }
        return utilisateurs;
    }


    public Utilisateur selectOne(int id) throws SQLException {
        String query = "SELECT * FROM `user` WHERE idUser = ?";
        PreparedStatement ps =  connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return  new Utilisateur(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    Role.valueOf(rs.getString("role")),
                    rs.getTimestamp("dateCreation").toLocalDateTime(),
                    rs.getBoolean("statut")
            );
        }
        return null;
    }
}
