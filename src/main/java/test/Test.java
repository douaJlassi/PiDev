package test;


import utils.MyDBConnexion;
import tn.esprit.projet.entities.Activite;
import tn.esprit.projet.entities.Guide;
import tn.esprit.projet.entities.Achat;

import tn.esprit.projet.services.ActiviteService;
import tn.esprit.projet.services.GuideService;
import tn.esprit.projet.services.AchatService;
import tn.esprit.projet.services.ActiviteAchatService;

import utils.MyDBConnexion;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        MyDBConnexion c1 = MyDBConnexion.getInstance();

        // ===================== TEST ACTIVITE SERVICE =====================
        testActiviteService();

        // ===================== TEST GUIDE SERVICE =====================
        testGuideService();

        // ===================== TEST ACHAT SERVICE =====================
        testAchatService();

        // ===================== TEST ACTIVITE ACHAT SERVICE =====================
        testActiviteAchatService();
    }

    // ==================== ACTIVITE SERVICE TESTS ====================
    public static void testActiviteService() {
        System.out.println("\n========== TEST ACTIVITE SERVICE ==========\n");

        ActiviteService service = new ActiviteService();

        Activite a = new Activite(
                3,
                "Quad Safari",
                "Tour en quad dans le désert",
                "Douz",
                new Date(System.currentTimeMillis()),
                1,
                200.0,
                3
        );

        Activite a2 = new Activite(
                4,
                "Plongée Sous-marine",
                "Découvrir les fonds marins",
                "Djerba",
                new Date(System.currentTimeMillis()),
                2,
                150.0,
                2
        );

        try {
            // ===== INSERT ONE =====
            System.out.println("--- INSERT ONE ---");
            service.insertOne(a);
            System.out.println("Activité insérée avec succès !");
            System.out.println("Activité insérée");

            // ===== INSERT ANOTHER =====
            //System.out.println("\n--- INSERT ANOTHER ---");
            //service.insertOne(a2);
            //System.out.println("Deuxième activité insérée avec succès !");

            // ===== SELECT ALL =====
            System.out.println("--- SELECT ALL ---");
            List<Activite> activites = service.selectALL();
            activites.forEach(System.out::println);

            // ===== SELECT BY ID =====
            //System.out.println("\n--- SELECT BY ID ---");
            //Activite activite = service.selectById(1);
            //System.out.println(activite != null ? activite : "Activité non trouvée");

            // ===== SELECT BY LIEU =====
            //System.out.println("\n--- SELECT BY LIEU ---");
            //List<Activite> activitesLieu = service.selectByLieu("Douz");
            //activitesLieu.forEach(System.out::println);

            // ===== SELECT BY GUIDE =====
            //System.out.println("\n--- SELECT BY GUIDE ---");
            //List<Activite> activitesGuide = service.selectByGuide(3);
            //activitesGuide.forEach(System.out::println);

            // ===== SELECT BY PRIX RANGE =====
            //System.out.println("\n--- SELECT BY PRIX RANGE ---");
            //List<Activite> activitesPrix = service.selectByPrixRange(100, 250);
            //activitesPrix.forEach(System.out::println);

            // ===== UPDATE ONE =====
            //System.out.println("\n--- UPDATE ONE ---");
            //a.setTitre("Quad Safari Premium");
            //a.setPrix(250.0);
            //service.updateOne(a);
            //System.out.println("Activité mise à jour avec succès !");

            // ===== DELETE ONE =====
            //System.out.println("\n--- DELETE ONE ---");
            //service.deleteOne(a);
            //System.out.println("Activité supprimée avec succès !");

        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ==================== GUIDE SERVICE TESTS ====================
    public static void testGuideService() {
        System.out.println("\n========== TEST GUIDE SERVICE ==========\n");

        GuideService service = new GuideService();

        Guide g = new Guide(1, true);
        Guide g2 = new Guide(2, false);
        Guide g3 = new Guide(3, true);

        try {
            // ===== INSERT ONE =====
            //System.out.println("--- INSERT ONE ---");
            //service.insertOne(g);
            //System.out.println("Guide inséré avec succès !");

            // ===== INSERT MULTIPLE =====
            //System.out.println("\n--- INSERT MULTIPLE ---");
            //service.insertOne(g2);
            //service.insertOne(g3);
            //System.out.println("Guides insérés avec succès !");

            // ===== SELECT ALL =====
            System.out.println("--- SELECT ALL ---");
            List<Guide> guides = service.selectALL();
            guides.forEach(System.out::println);

            // ===== SELECT BY ID =====
            //System.out.println("\n--- SELECT BY ID ---");
            //Guide guide = service.selectById(1);
            //System.out.println(guide != null ? guide : "Guide non trouvé");

            // ===== SELECT GUIDES DISPONIBLES =====
            //System.out.println("\n--- SELECT GUIDES DISPONIBLES ---");
            //List<Guide> guidesDisponibles = service.selectGuidesDisponibles();
            //guidesDisponibles.forEach(System.out::println);

            // ===== UPDATE ONE =====
            //System.out.println("\n--- UPDATE ONE ---");
            //g.setDisponibilite(false);
            //service.updateOne(g);
            //System.out.println("Guide mis à jour avec succès !");

            // ===== TOGGLE DISPONIBILITE =====
            //System.out.println("\n--- TOGGLE DISPONIBILITE ---");
            //service.toggleDisponibilite(1);
            //System.out.println("Disponibilité basculée avec succès !");

            // ===== DELETE ONE =====
            //System.out.println("\n--- DELETE ONE ---");
            //service.deleteOne(g);
            //System.out.println("Guide supprimé avec succès !");

        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ==================== ACHAT SERVICE TESTS ====================
    public static void testAchatService() {
        System.out.println("\n========== TEST ACHAT SERVICE ==========\n");

        AchatService service = new AchatService();

        Achat ach1 = new Achat(
                new Date(System.currentTimeMillis()),
                1,
                500.0,
                "EN_ATTENTE",
                "Achat de vacances"
        );

        Achat ach2 = new Achat(
                new Date(System.currentTimeMillis()),
                2,
                750.0,
                "CONFIRMÉ",
                "Package famille"
        );

        try {
            // ===== INSERT ONE =====
            //System.out.println("--- INSERT ONE ---");
            //service.insertOne(ach1);
            //System.out.println("Achat inséré avec succès !");

            // ===== INSERT MULTIPLE =====
            //System.out.println("\n--- INSERT MULTIPLE ---");
            //service.insertOne(ach2);
            //System.out.println("Achats insérés avec succès !");

            // ===== SELECT ALL =====
            System.out.println("--- SELECT ALL ---");
            List<Achat> achats = service.selectALL();
            achats.forEach(System.out::println);

            // ===== SELECT BY ID =====
            //System.out.println("\n--- SELECT BY ID ---");
            //Achat achat = service.selectById(1);
            //System.out.println(achat != null ? achat : "Achat non trouvé");

            // ===== SELECT BY CLIENT =====
            //System.out.println("\n--- SELECT BY CLIENT ---");
            //List<Achat> achatsClient = service.selectByClient(1);
            //achatsClient.forEach(System.out::println);

            // ===== SELECT BY STATUT =====
            //System.out.println("\n--- SELECT BY STATUT ---");
            //List<Achat> achatsStatut = service.selectByStatut("CONFIRMÉ");
            //achatsStatut.forEach(System.out::println);

            // ===== SELECT BY MONTANT RANGE =====
            //System.out.println("\n--- SELECT BY MONTANT RANGE ---");
            //List<Achat> achatsMontant = service.selectByMontantRange(400, 800);
            //achatsMontant.forEach(System.out::println);

            // ===== UPDATE ONE =====
            //System.out.println("\n--- UPDATE ONE ---");
            //ach1.setMontantTotal(600.0);
            //ach1.setStatut("CONFIRMÉ");
            //service.updateOne(ach1);
            //System.out.println("Achat mis à jour avec succès !");

            // ===== CHANGE STATUT =====
            //System.out.println("\n--- CHANGE STATUT ---");
            //service.changeStatut(1, "COMPLÉTÉ");
            //System.out.println("Statut changé avec succès !");

            // ===== UPDATE MONTANT =====
            //System.out.println("\n--- UPDATE MONTANT ---");
            //service.updateMontant(1, 550.0);
            //System.out.println("Montant mis à jour avec succès !");

            // ===== COUNT ACHATS =====
            //System.out.println("\n--- COUNT ACHATS ---");
            //int count = service.countAchats();
            //System.out.println("Nombre total d'achats : " + count);

            // ===== DELETE ONE =====
            //System.out.println("\n--- DELETE ONE ---");
            //service.deleteOne(ach1);
            //System.out.println("Achat supprimé avec succès !");

        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // ==================== ACTIVITE ACHAT SERVICE TESTS ====================
    public static void testActiviteAchatService() {
        System.out.println("\n========== TEST ACTIVITE ACHAT SERVICE ==========\n");

        ActiviteAchatService service = new ActiviteAchatService();

        try {
            // ===== AJOUTER ASSOCIATION =====
            //System.out.println("--- AJOUTER ASSOCIATION ---");
            //service.ajouterActiviteAchat(1, 1);
            //service.ajouterActiviteAchat(2, 1);
            //service.ajouterActiviteAchat(3, 2);
            //System.out.println("Associations ajoutées avec succès !");

            // ===== GET ACTIVITES BY ACHAT =====
            //System.out.println("\n--- GET ACTIVITES BY ACHAT ---");
            //List<Integer> activites = service.getActivitesByAchat(1);
            //System.out.println("Activités pour achat 1 : " + activites);

            // ===== GET ACHATS BY ACTIVITE =====
            //System.out.println("\n--- GET ACHATS BY ACTIVITE ---");
            //List<Integer> achats = service.getAchatsByActivite(1);
            //System.out.println("Achats pour activité 1 : " + achats);

            // ===== COUNT ACTIVITES BY ACHAT =====
            //System.out.println("\n--- COUNT ACTIVITES BY ACHAT ---");
            //int count = service.countActivitesByAchat(1);
            //System.out.println("Nombre d'activités pour achat 1 : " + count);

            // ===== SUPPRIMER ASSOCIATION =====
            //System.out.println("\n--- SUPPRIMER ASSOCIATION ---");
            //service.supprimerActiviteAchat(1, 1);
            //System.out.println("Association supprimée avec succès !");

            // ===== SUPPRIMER TOUTES LES ASSOCIATIONS D'UN ACHAT =====
            //System.out.println("\n--- SUPPRIMER TOUTES LES ASSOCIATIONS ---");
            //service.supprimerAllActivitesAchat(1);
            //System.out.println("Toutes les associations supprimées avec succès !");

        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}