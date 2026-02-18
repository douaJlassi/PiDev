package test;

import Services.GuideService;
import utils.MyDBConnexion;
import gestion_activite.Activite;
import gestion_activite.Guide;
import gestion_activite.Achat;

import Services.ActiviteService;
import Services.AchatService;
import Services.ActiviteAchatService;

import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        // Initialisation de la connexion
        MyDBConnexion.getInstance();

        testActiviteService();
        testGuideService();
        testAchatService();
        testActiviteAchatService();
    }

    // ==================== ACTIVITE SERVICE TESTS ====================
    public static void testActiviteService() {
        System.out.println("\n========== TEST ACTIVITE SERVICE ==========\n");
        ActiviteService service = new ActiviteService();

        // Mis à jour avec 11 paramètres pour correspondre au nouveau constructeur
        Activite a = new Activite(
                10,                        // idActivite
                "aaaaaaaaaaaaaa Safari",   // titre
                "Tour en quad dans le désert", // description
                "Douz",                    // lieu
                new Timestamp(System.currentTimeMillis()), // date
                1,                         // duree
                200.0,                     // prix
                3,                         // idGuide
                "safari.jpg",              // image
                "Actif",                   // statut (NOUVEAU)
                20                         // placesDisponibles (NOUVEAU)
        );

        try {
            System.out.println("--- SELECT ALL ---");
            List<Activite> activites = service.selectALL();
            if (activites.isEmpty()) {
                System.out.println("Aucune activité trouvée en base.");
            } else {
                activites.forEach(System.out::println);
            }

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    // ==================== GUIDE SERVICE TESTS ====================
    public static void testGuideService() {
        System.out.println("\n========== TEST GUIDE SERVICE ==========\n");
        GuideService service = new GuideService();

        // Le constructeur doit avoir : (id, dispo, nom, prenom, email, tel)
        Guide g = new Guide(1, true, "Sahar", "Test", "sahar@esprit.tn", "12345678");

        try {
            System.out.println("--- SELECT ALL ---");
            List<Guide> guides = service.selectALL();
            if (guides.isEmpty()) {
                System.out.println("Aucun guide trouvé.");
            } else {
                guides.forEach(System.out::println);
            }

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    // ==================== ACHAT SERVICE TESTS ====================
    public static void testAchatService() {
        System.out.println("\n========== TEST ACHAT SERVICE ==========\n");
        AchatService service = new AchatService();

        try {
            System.out.println("--- SELECT ALL ---");
            List<Achat> achats = service.selectALL();
            if (achats.isEmpty()) {
                System.out.println("Aucun achat trouvé.");
            } else {
                achats.forEach(System.out::println);
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    // ==================== ACTIVITE ACHAT SERVICE TESTS ====================
    public static void testActiviteAchatService() {
        System.out.println("\n========== TEST ACTIVITE ACHAT SERVICE ==========\n");
        // Logique de test ici si nécessaire
    }
}