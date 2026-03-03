import entities.*;
import org.junit.jupiter.api.*;
import services.ServiceConversation;
import services.ServiceParticipantConversation;
import services.ServiceUtilisateur;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceParticipantConversationTest {
    static ServiceParticipantConversation spc ;
    static ServiceConversation servc ;
    static ServiceUtilisateur servu ;

    static Utilisateur testUser;
    static Conversation testConv;

    @BeforeAll
    static void setUp(){
        spc = new ServiceParticipantConversation();
        servc = new ServiceConversation();
        servu = new ServiceUtilisateur();

        testUser = new Utilisateur();
        testUser.setNom("Test");
        testUser.setPrenom("User");
        testUser.setEmail("test@pidev.com");
        testUser.setPassword("1234");
        testUser.setRole(Role.CLIENT);
        testUser.setDateCreation(LocalDateTime.now());

        testConv = new Conversation(TypeConversation.GROUPE, LocalDateTime.now(), "Groupe Test");
        try {
            servu.insertOne(testUser);
            servc.insertOne(testConv);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    void addParticipant() {
        ParticipantConversation pc= new ParticipantConversation(testUser, testConv, LocalDateTime.now());
        try {
            spc.insertOne(pc);
            List<ParticipantConversation> list= spc.selectALL();
            assertTrue( list.size()>0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    void testQuitterConversation() throws SQLException {
        List<ParticipantConversation> avant = spc.selectALL();
        int sizeAvant = avant.size();

        spc.quitterConversation(testUser.getIdUtilisateur(), testConv.getIdConversation());

        List<ParticipantConversation> apres = spc.selectALL();
        assertEquals(sizeAvant - 1, apres.size(), "Le nombre de participants devrait avoir diminué de 1");
    }}
