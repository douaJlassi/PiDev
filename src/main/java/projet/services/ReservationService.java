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
}