package utils;

import entities.Utilisateur;
import Services.ServiceUtilisateur;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserMigration {
    public static void main(String[] args) {
        ElasticsearchClient esClient = ElasticSearchClient.getInstance();
        ServiceUtilisateur serUser = new ServiceUtilisateur();

        System.out.println("Migration des utilisateurs en cours...");

        try {
            List<Utilisateur> users = serUser.selectALL();

            for (Utilisateur u : users) {
                Map<String, Object> esData = new HashMap<>();
                esData.put("idUser", u.getIdUtilisateur());
                esData.put("nomComplet", u.getPrenom() + " " + u.getNom());
                esData.put("email", u.getEmail());
                esData.put("role", u.getRole().name());
                esData.put("dateCreation", u.getDateCreation().toString());
                esData.put("status", u.isStatus());

                esClient.index(i -> i
                        .index("utilisateurs")
                        .id(String.valueOf(u.getIdUtilisateur()))
                        .document(esData)
                );
            }
            System.out.println("Succès ! " + users.size() + " utilisateurs envoyés à Elasticsearch.");

        } catch (Exception e) {
            System.err.println("Erreur migration : " + e.getMessage());
            e.printStackTrace();
        }
    }
}