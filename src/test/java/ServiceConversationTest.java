import entities.Conversation;
import entities.TypeConversation;
import org.junit.jupiter.api.*;
import services.ServiceConversation;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceConversationTest {

    static ServiceConversation sc;
    int id;
    @BeforeAll
    static void setUp() {
        sc = new ServiceConversation();
    }

    @Test
    @Order(1)
    void createConversation() {
        Conversation c = new Conversation(TypeConversation.PRIVEE, LocalDateTime.now(),"");
        try {
            sc.insertOne(c);
            List<Conversation> list= sc.selectALL();
            assertTrue(list.size()>0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    void updateConversation() {
        Conversation c = new Conversation(2,TypeConversation.GROUPE, LocalDateTime.now(),"Groupe Voyageurs");
        try {
            sc.updateOne(c);
            List<Conversation> list= sc.selectALL();
            assertTrue(list.stream().anyMatch(c1-> c1.getTitre().equals("Groupe Voyageurs")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    @Order(3)
    void deleteConversation() {
        Conversation c = new Conversation(TypeConversation.PRIVEE, LocalDateTime.now(), "A supprimer");
        try {
            sc.insertOne(c);
            sc.deleteOne(c);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
