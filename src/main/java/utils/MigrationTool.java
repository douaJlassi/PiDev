package utils;

import Services.ServiceConversation;
import Services.ServiceMessage;
import Services.ServiceParticipantConversation;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import entities.*;
import Services.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MigrationTool {

    public static void main(String[] args) {
        ElasticsearchClient esClient = ElasticSearchClient.getInstance();

        ServiceConversation serConv = new ServiceConversation();
        ServiceMessage serMsg = new ServiceMessage();
        ServiceParticipantConversation serPart = new ServiceParticipantConversation();
        Services.ServiceUtilisateur serUser = new Services.ServiceUtilisateur();

        System.out.println("🚀 DÉBUT DE LA MIGRATION VERS ELASTICSEARCH...");

        try {
            List<Utilisateur> users = serUser.selectALL();
            for (Utilisateur u : users) {
                Map<String, Object> data = new HashMap<>();
                data.put("idUser", u.getIdUtilisateur());
                data.put("nomComplet", u.getPrenom() + " " + u.getNom());
                data.put("email", u.getEmail());
                data.put("role", u.getRole().name());
                data.put("status", u.isStatus());
                data.put("dateCreation", u.getDateCreation().toString());

                esClient.index(i -> i.index("utilisateurs").id(String.valueOf(u.getIdUtilisateur())).document(data));
            }
            System.out.println("✅ " + users.size() + " utilisateurs migrés.");

            List<Conversation> convs = serConv.selectALL();
            for (Conversation c : convs) {
                Map<String, Object> data = new HashMap<>();
                data.put("idConversation", c.getIdConversation());
                data.put("type", c.getTypeConversation().name());
                data.put("titre", c.getTitre() == null ? "Privé" : c.getTitre());
                data.put("dateCreation", c.getDateCreation().toString());

                esClient.index(i -> i.index("conversations").id(String.valueOf(c.getIdConversation())).document(data));
            }
            System.out.println("✅ " + convs.size() + " conversations migrées.");

            List<Message> msgs = serMsg.selectALL();
            for (Message m : msgs) {
                Map<String, Object> data = new HashMap<>();
                data.put("idMessage", m.getIdMessage());
                data.put("contenu", m.getContenu());
                data.put("dateEnvoi", m.getDateEnvoi().toString());
                data.put("typeMessage", m.getTypeMessage() != null ? m.getTypeMessage().name() : "TEXTE");
                data.put("expediteurNom", m.getExpediteur().getPrenom() + " " + m.getExpediteur().getNom());
                data.put("lu", m.isLu());
                data.put("idConversation", m.getConversation().getIdConversation());

                esClient.index(i -> i.index("messages").id(String.valueOf(m.getIdMessage())).document(data));
            }
            System.out.println("✅ " + msgs.size() + " messages migrés.");

            List<ParticipantConversation> parts = serPart.selectALL();
            for (ParticipantConversation pc : parts) {
                Map<String, Object> data = new HashMap<>();
                data.put("idParticipant", pc.getIdParticipant());
                data.put("idUser", pc.getParticipant().getIdUtilisateur());
                data.put("idConversation", pc.getConversation().getIdConversation());
                data.put("titreConversation", pc.getConversation().getTitre() != null ? pc.getConversation().getTitre() : "Privé");
                data.put("nomUtilisateur", pc.getParticipant().getPrenom() + " " + pc.getParticipant().getNom());

                data.put("dateAjout", pc.getDateAjout().toString());

                esClient.index(i -> i.index("participants").id(String.valueOf(pc.getIdParticipant())).document(data));
            }
            System.out.println("✅ " + parts.size() + " participants migrés.");

            System.out.println("🌟 MIGRATION TERMINÉE AVEC SUCCÈS !");

        } catch (Exception e) {
            System.err.println("❌ ERREUR DURANT LA MIGRATION : " + e.getMessage());
            e.printStackTrace();
        }
    }
}