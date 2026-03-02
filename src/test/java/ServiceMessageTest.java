import entities.*;
import org.junit.jupiter.api.*;
import services.ServiceConversation;
import services.ServiceMessage;
import services.ServiceParticipantConversation;
import services.ServiceUtilisateur;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceMessageTest {
    static ServiceMessage sm;
    static ServiceConversation sc;
    static ServiceUtilisateur su;
    static ServiceParticipantConversation spc;

    static Utilisateur ali;
    static Utilisateur sarra;
    static Conversation chat;

    @BeforeAll
    static void setUp(){
        sm= new ServiceMessage();
        sc=new ServiceConversation();
        su=new ServiceUtilisateur();
        spc = new ServiceParticipantConversation();

        ali =new Utilisateur();
        ali.setNom("sellini");
        ali.setPrenom("ali");
        ali.setEmail("ali@test.com");
        ali.setRole(Role.CLIENT);
        ali.setDateCreation(LocalDateTime.now());

        sarra = new Utilisateur();
        sarra.setNom("Jlassi");
        sarra.setPrenom("sarra");
        sarra.setEmail("sarra@test.com");
        sarra.setRole(Role.CLIENT);
        sarra.setDateCreation(LocalDateTime.now());

        try {
            su.insertOne(ali);
            su.insertOne(sarra);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        chat = new Conversation(TypeConversation.PRIVEE, LocalDateTime.now(), null);
        try {
            sc.insertOne(chat);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
        spc.insertOne(new ParticipantConversation(ali, chat, LocalDateTime.now()));
        spc.insertOne(new ParticipantConversation(sarra, chat, LocalDateTime.now()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    void simulationEchangeMessages() throws SQLException {
        // ali envoie le premier message
        Messages m1 = new Messages();
        m1.setContenu("Salut Douaa, tu as fini le projet ?");
        m1.setExpediteur(ali);
        m1.setConversation(chat);
        m1.setDateEnvoi(LocalDateTime.now());
        m1.setLu(false);
        sm.insertOne(m1);

        // Douaa répond
        Messages m2 = new Messages();
        m2.setContenu("Oui ali, je viens de terminer les tests !");
        m2.setExpediteur(sarra);
        m2.setConversation(chat);
        m2.setDateEnvoi(LocalDateTime.now().plusSeconds(30)); // 30 sec après
        m2.setLu(false);
        sm.insertOne(m2);

        // Vérification
        List<Messages> discussion = sm.selectByConversation(chat.getIdConversation());
        assertEquals(2, discussion.size(), "Il devrait y avoir 2 messages dans la conversation");

        // On vérifie que le premier message vient bien d'ali
        assertEquals("ali", discussion.get(0).getExpediteur().getPrenom());
        // On vérifie que le deuxième vient de sarra
        assertEquals("sarra", discussion.get(1).getExpediteur().getPrenom());

        System.out.println("--- Historique de la discussion ---");
        for(Messages m : discussion) {
            System.out.println(m.getExpediteur().getPrenom() + " : " + m.getContenu());
        }
    }
}
