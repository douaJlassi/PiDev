package projet.services;

import projet.entites.reservation;
import projet.utils.MyDBConnexion;// Assurez-vous d'importer votre classe de connexion

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationService implements CRUD<String, reservation> {

    private Connection connection;

    public ReservationService() {
        connection = MyDBConnexion.getInstance().getConnection();
    }
    @Override
    public void insertOne(reservation r) throws SQLException {
        java.sql.Date sqlDate = java.sql.Date.valueOf(r.getDateReservation().toString());
        String req = "INSERT INTO reservations (dateReservation, statut, modePaiement, idService, nom,seatNb) VALUES (?, ?, ?, ?, ?,?)";
        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setDate(1, sqlDate);
            pst.setString(2, r.getStatut());
            pst.setString(3, r.getModePaiement());
            pst.setInt(4, r.getIdService());
            pst.setString(5, r.getNom());
            pst.setInt(6, r.getSeatNb());
            pst.executeUpdate();
            System.out.println("Réservation ajoutée avec succès !");
        }
    }
    @Override
    public void updateOne(String id, reservation r) throws SQLException {
        java.sql.Date sqlDate = java.sql.Date.valueOf(r.getDateReservation().toString());
        String req = "UPDATE reservations SET dateReservation = ?, statut = ?, modePaiement = ?, idService = ?, nom = ? WHERE idReservation = ?";
        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setDate(1, sqlDate);
            pst.setString(2, r.getStatut());
            pst.setString(3, r.getModePaiement());
            pst.setInt(4, r.getIdService());
            pst.setString(5, r.getNom());
            pst.setInt(6, Integer.parseInt(id)); // Convertit l'ID de String en int
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Réservation modifiée avec succès !");
            } else {
                System.out.println("Aucune réservation trouvée avec l'ID : " + id);
            }
        }
    }
    @Override
    public void deleteOne(reservation r) throws SQLException {
        String req = "DELETE FROM `reservations` WHERE `nom` = " + "'" + r.getNom() + "'";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.executeUpdate();
    }
    @Override
    public List<reservation> selectALL() throws SQLException {
        List<reservation> reservations = new ArrayList<>();
        String req = "SELECT * FROM reservations";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                reservations.add(new reservation(

                        rs.getString(3),
                        rs.getDate(2),
                        rs.getInt(5),
                        rs.getString(4),
                        rs.getString(6),
                        rs.getInt(7)
                ));
            }
        }
        return reservations;
    }
    public List<Integer> selectSeats(int ServiceId) throws SQLException {
        List<Integer> reservations = new ArrayList<>();
        String req = "SELECT seatNb FROM reservations WHERE `idService` = '"+ServiceId+"'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                reservations.add(
                        rs.getInt("seatNb")
                );
            }
        }
        return reservations;
    }
    public void validerReservation(reservation r, String typeService) throws SQLException, ReservationException {

        // 1. Le service existe et est disponible
        verifierDisponibiliteService(r.getIdService());

        // 2. Pas deux hôtels à la même date pour le même client
        if (typeService.equals("hotel")) {
            verifierDoublonHotel(r.getNom(), (Date) r.getDateReservation());
        }

        // 3. Pas deux vols à la même heure pour le même client
        if (typeService.equals("vol")) {
            verifierDoublonVol(r.getNom(), r.getIdService());
        }

        // 4. Cohérence vol ↔ hôtel (ville d'arrivée du vol = ville de l'hôtel)
        verifierCoherenceVolHotel(r.getNom(), r.getIdService(), typeService);
    }
    private void verifierDisponibiliteService(int idService) throws SQLException, ReservationException {
        String req = "SELECT disponibilite, capacite, nom FROM services WHERE idService = ?";
        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setInt(1, idService);
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) {
                throw new ReservationException("❌ Service introuvable (id=" + idService + ").");
            }
            String dispo   = rs.getString("disponibilite");
            int    capacite = rs.getInt("capacite");
            String nom      = rs.getString("nom");

            if ("false".equalsIgnoreCase(dispo) || capacite <= 0) {
                throw new ReservationException(
                        "❌ Le service « " + nom + " » n'est plus disponible (complet ou désactivé).");
            }
        }
    }
    private void verifierDoublonHotel(String nomClient, Date dateReservation) throws SQLException, ReservationException {
        String req =
                "SELECT r.idReservation, s.nom AS nomHotel " +
                        "FROM reservations r " +
                        "JOIN services s ON r.idService = s.idService " +
                        "WHERE r.nom = ? " +
                        "  AND s.type = 'hotel' " +
                        "  AND r.dateReservation = ?";

        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setString(1, nomClient);
            pst.setDate(2, dateReservation);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                throw new ReservationException(
                        "❌ Vous avez déjà réservé l'hôtel « " + rs.getString("nomHotel") +
                                " » à cette date. Un seul hôtel par jour est autorisé.");
            }
        }
    }
    private void verifierDoublonVol(String nomClient, int idVolNouveau) throws SQLException, ReservationException {
        // Récupérer les horaires du nouveau vol
        String reqVol = "SELECT dateDepart, dateArrive FROM services WHERE idService = ?";
        Date departNouv, arriveeNouv;
        try (PreparedStatement pst = connection.prepareStatement(reqVol)) {
            pst.setInt(1, idVolNouveau);
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) return; // déjà géré par règle 1
            departNouv  = rs.getDate("dateDepart");
            arriveeNouv = rs.getDate("dateArrive");
        }

        // Chercher un vol existant du même client dont les horaires se chevauchent
        String req =
                "SELECT s.nom AS nomVol, s.dateDepart, s.dateArrive " +
                        "FROM reservations r " +
                        "JOIN services s ON r.idService = s.idService " +
                        "WHERE r.nom = ? " +
                        "  AND s.type = 'vol' " +
                        "  AND s.idService <> ? " +
                        "  AND NOT (s.dateArrive < ? OR s.dateDepart > ?)";

        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setString(1, nomClient);
            pst.setInt(2, idVolNouveau);
            pst.setDate(3, departNouv);   // conflit si l'existant se termine avant que le nouveau commence
            pst.setDate(4, arriveeNouv);  // conflit si l'existant commence après que le nouveau finit
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                throw new ReservationException(
                        "❌ Conflit horaire : vous avez déjà le vol « " + rs.getString("nomVol") +
                                " » (du " + rs.getDate("dateDepart") + " au " + rs.getDate("dateArrive") +
                                "). Impossible de réserver deux vols en même temps.");
            }
        }
    }

    private String getVilleArriveeVol(int idVol) throws SQLException {
        String req = "SELECT villeArrivee FROM services WHERE idService = ?";
        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setInt(1, idVol);
            ResultSet rs = pst.executeQuery();
            return rs.next() ? rs.getString("villeArrivee") : null;
        }
    }
    private String getLocalisationHotel(int idHotel) throws SQLException {
        String req = "SELECT localisation FROM services WHERE idService = ?";
        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setInt(1, idHotel);
            ResultSet rs = pst.executeQuery();
            return rs.next() ? rs.getString("localisation") : null;
        }
    }
    private void verifierCoherenceVolHotel(String nomClient, int idServiceNouveau, String typeNouv) throws SQLException, ReservationException {

        if (typeNouv.equals("vol")) {
            // Vérifier que les hôtels déjà réservés sont dans la ville d'arrivée du vol
            String villeArrivee = getVilleArriveeVol(idServiceNouveau);
            if (villeArrivee == null) return;

            String req =
                    "SELECT s.nom AS nomHotel, s.localisation " +
                            "FROM reservations r " +
                            "JOIN services s ON r.idService = s.idService " +
                            "WHERE r.nom = ? AND s.type = 'hotel'";

            try (PreparedStatement pst = connection.prepareStatement(req)) {
                pst.setString(1, nomClient);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    String locHotel = rs.getString("localisation");
                    if (locHotel != null && !locHotel.equalsIgnoreCase(villeArrivee)) {
                        throw new ReservationException(
                                "❌ Incohérence géographique : le vol arrive à « " + villeArrivee +
                                        " » mais votre hôtel « " + rs.getString("nomHotel") +
                                        " » est à « " + locHotel + "».");
                    }
                }
            }

        } else if (typeNouv.equals("hotel")) {
            // Vérifier que les vols déjà réservés arrivent bien dans la ville de ce nouvel hôtel
            String locHotelNouv = getLocalisationHotel(idServiceNouveau);
            if (locHotelNouv == null) return;

            String req =
                    "SELECT s.nom AS nomVol, s.villeArrivee " +
                            "FROM reservations r " +
                            "JOIN services s ON r.idService = s.idService " +
                            "WHERE r.nom = ? AND s.type = 'vol'";

            try (PreparedStatement pst = connection.prepareStatement(req)) {
                pst.setString(1, nomClient);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    String villeArriveeVol = rs.getString("villeArrivee");
                    if (villeArriveeVol != null && !villeArriveeVol.equalsIgnoreCase(locHotelNouv)) {
                        throw new ReservationException(
                                "❌ Incohérence géographique : votre vol « " + rs.getString("nomVol") +
                                        " » arrive à « " + villeArriveeVol +
                                        " » mais l'hôtel sélectionné est à « " + locHotelNouv + "».");
                    }
                }
            }
        }
    }
    public static class ReservationException extends Exception {
        public ReservationException(String message) { super(message); }
    }
}