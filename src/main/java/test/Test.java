package test;

import Services.GuideService;
import utils.MyDBConnexion;
import gestion_activite.Activite;
import gestion_activite.Guide;
import gestion_activite.Achat;

import Services.ActiviteService;
import Services.AchatService;
import Services.ActiviteAchatService;

import java.sql.Timestamp; // Use Timestamp instead of Date for activities
import java.sql.SQLException;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        MyDBConnexion c1 = MyDBConnexion.getInstance();

        testActiviteService();
        testGuideService();
        testAchatService();
        testActiviteAchatService();
    }

    // ==================== ACTIVITE SERVICE TESTS ====================
    public static void testActiviteService() {
        System.out.println("\n========== TEST ACTIVITE SERVICE ==========\n");
        ActiviteService service = new ActiviteService();

        // Updated with 9 parameters (added image and used Timestamp)
        Activite a = new Activite(
                10,
                "aaaaaaaaaaaaaa Safari",
                "Tour en quad dans le désert",
                "Douz",
                new Timestamp(System.currentTimeMillis()),
                1,
                200.0,
                3,
                "safari.jpg" // Added image parameter
        );

        try {
            System.out.println("--- INSERT ONE ---");
            // service.insertOne(a); // Uncomment this to test insertion
            System.out.println("Activité préparée !");

            System.out.println("--- SELECT ALL ---");
            List<Activite> activites = service.selectALL();
            activites.forEach(System.out::println);

        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ==================== GUIDE SERVICE TESTS ====================
    public static void testGuideService() {
        System.out.println("\n========== TEST GUIDE SERVICE ==========\n");
        GuideService service = new GuideService();

        // Since you added nom, prenom, etc. to the table,
        // ensure your Guide constructor matches: (id, dispo, nom, prenom, email, tel)
        Guide g = new Guide(1, true, "Sahar", "Test", "sahar@esprit.tn", "12345678");

        try {
            System.out.println("--- SELECT ALL ---");
            List<Guide> guides = service.selectALL();
            guides.forEach(System.out::println);

        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ==================== ACHAT SERVICE TESTS ====================
    public static void testAchatService() {
        System.out.println("\n========== TEST ACHAT SERVICE ==========\n");
        AchatService service = new AchatService();

        try {
            System.out.println("--- SELECT ALL ---");
            List<Achat> achats = service.selectALL();
            achats.forEach(System.out::println);
        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ==================== ACTIVITE ACHAT SERVICE TESTS ====================
    public static void testActiviteAchatService() {
        System.out.println("\n========== TEST ACTIVITE ACHAT SERVICE ==========\n");
        // Add logic here if needed
    }
}