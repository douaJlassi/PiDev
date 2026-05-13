package services;

import gestion_activite.Achat;
import gestion_activite.ReservationDetail;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AchatService {

    // Get a fresh connection each time
    private Connection getConnection() throws SQLException {
        return MyDBConnexion.getInstance().getCnx();
    }

    public void insert(Achat achat) throws SQLException {
        String req = "INSERT INTO achat (idClient, idActivite, dateAchat, nbPlaces, montantTotal, statut) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, achat.getIdClient());
            ps.setInt(2, achat.getIdActivite());
            ps.setTimestamp(3, achat.getDateAchat());
            ps.setInt(4, achat.getNbPlaces());
            ps.setDouble(5, achat.getMontantTotal());
            ps.setString(6, achat.getStatut() != null ? achat.getStatut() : "Confirmé");

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating achat failed, no rows affected.");
            }

            // Get the generated ID
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    achat.setIdAchat(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating achat failed, no ID obtained.");
                }
            }
        }
    }

    public void update(Achat achat) throws SQLException {
        String req = "UPDATE achat SET statut = ? WHERE idAchat = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setString(1, achat.getStatut());
            ps.setInt(2, achat.getIdAchat());
            ps.executeUpdate();
        }
    }

    public void delete(int idAchat) throws SQLException {
        String req = "DELETE FROM achat WHERE idAchat = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, idAchat);
            ps.executeUpdate();
        }
    }

    public List<Achat> selectAll() throws SQLException {
        List<Achat> list = new ArrayList<>();
        String req = "SELECT * FROM achat ORDER BY dateAchat DESC";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                list.add(mapResultSetToAchat(rs));
            }
        }
        return list;
    }

    public List<Achat> selectByClient(int clientId) throws SQLException {
        List<Achat> list = new ArrayList<>();
        String req = "SELECT * FROM achat WHERE idClient = ? ORDER BY dateAchat DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAchat(rs));
                }
            }
        }
        return list;
    }

    public List<Achat> selectByActivite(int activiteId) throws SQLException {
        List<Achat> list = new ArrayList<>();
        String req = "SELECT * FROM achat WHERE idActivite = ? AND statut != 'Annulé' ORDER BY dateAchat DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, activiteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAchat(rs));
                }
            }
        }
        return list;
    }

    /**
     * Get reservation details for a specific activity with user information
     * This is the method you need for your ReservationDetailsController
     */
    public List<ReservationDetail> getReservationDetailsByActivite(int activiteId) throws SQLException {
        List<ReservationDetail> details = new ArrayList<>();

        String req = "SELECT u.name, u.last_name, u.email, u.telephone, a.nbPlaces, a.montantTotal " +
                "FROM achat a " +
                "JOIN user u ON a.idClient = u.id " +
                "WHERE a.idActivite = ? AND a.statut != 'Annulé' " +
                "ORDER BY a.dateAchat DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, activiteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nomComplet = rs.getString("name") + " " + rs.getString("last_name");
                    String email = rs.getString("email");
                    String telephone = rs.getString("telephone");
                    int places = rs.getInt("nbPlaces");
                    double montant = rs.getDouble("montantTotal");

                    details.add(new ReservationDetail(nomComplet, email, telephone, places, montant));
                }
            }
        }
        return details;
    }

    /**
     * Get reservation details for a specific client
     */
    public List<ReservationDetail> getReservationDetailsByClient(int clientId) throws SQLException {
        List<ReservationDetail> details = new ArrayList<>();

        String req = "SELECT act.titre, u.name, u.last_name, u.email, u.telephone, a.nbPlaces, a.montantTotal, a.dateAchat " +
                "FROM achat a " +
                "JOIN user u ON a.idClient = u.id " +
                "JOIN activite act ON a.idActivite = act.idActivite " +
                "WHERE a.idClient = ? AND a.statut != 'Annulé' " +
                "ORDER BY a.dateAchat DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nomComplet = rs.getString("name") + " " + rs.getString("last_name");
                    String email = rs.getString("email");
                    String telephone = rs.getString("telephone");
                    int places = rs.getInt("nbPlaces");
                    double montant = rs.getDouble("montantTotal");

                    details.add(new ReservationDetail(nomComplet, email, telephone, places, montant));
                }
            }
        }
        return details;
    }

    /**
     * Get all reservations for a specific guide (across all their activities)
     */
    public List<ReservationDetail> getReservationDetailsByGuide(int guideId) throws SQLException {
        List<ReservationDetail> details = new ArrayList<>();

        String req = "SELECT u.name, u.last_name, u.email, u.telephone, a.nbPlaces, a.montantTotal, act.titre " +
                "FROM achat a " +
                "JOIN user u ON a.idClient = u.id " +
                "JOIN activite act ON a.idActivite = act.idActivite " +
                "WHERE act.idGuide = ? AND a.statut != 'Annulé' " +
                "ORDER BY a.dateAchat DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, guideId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nomComplet = rs.getString("name") + " " + rs.getString("last_name");
                    String email = rs.getString("email");
                    String telephone = rs.getString("telephone");
                    int places = rs.getInt("nbPlaces");
                    double montant = rs.getDouble("montantTotal");

                    details.add(new ReservationDetail(nomComplet, email, telephone, places, montant));
                }
            }
        }
        return details;
    }

    public Achat selectById(int idAchat) throws SQLException {
        String req = "SELECT * FROM achat WHERE idAchat = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, idAchat);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAchat(rs);
                }
            }
        }
        return null;
    }

    public int getTotalPlacesReservees(int idActivite) throws SQLException {
        String req = "SELECT COALESCE(SUM(nbPlaces), 0) FROM achat WHERE idActivite = ? AND statut != 'Annulé'";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, idActivite);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public double getTotalRevenusForGuide(int guideId) throws SQLException {
        String req = "SELECT COALESCE(SUM(a.montantTotal), 0) FROM achat a " +
                "JOIN activite act ON a.idActivite = act.idActivite " +
                "WHERE act.idGuide = ? AND a.statut != 'Annulé'";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, guideId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double getTotalRevenusForActivite(int idActivite) throws SQLException {
        String req = "SELECT COALESCE(SUM(montantTotal), 0) FROM achat WHERE idActivite = ? AND statut != 'Annulé'";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, idActivite);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public int getNombreReservationsForActivite(int idActivite) throws SQLException {
        String req = "SELECT COUNT(*) FROM achat WHERE idActivite = ? AND statut != 'Annulé'";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, idActivite);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void annulerAchat(int idAchat) throws SQLException {
        String req = "UPDATE achat SET statut = 'Annulé' WHERE idAchat = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, idAchat);
            ps.executeUpdate();
        }
    }

    public boolean checkDisponibilite(int idActivite, int placesDemandees) throws SQLException {
        String req = "SELECT placesDisponibles FROM activite WHERE idActivite = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, idActivite);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int placesDisponibles = rs.getInt("placesDisponibles");
                    return placesDisponibles >= placesDemandees;
                }
            }
        }
        return false;
    }

    /**
     * Get monthly reservation stats for a guide (for charts)
     */
    public java.util.Map<String, Integer> getMonthlyReservationCount(int guideId, int year) throws SQLException {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String req = "SELECT MONTH(a.dateAchat) as mois, COUNT(*) as nb " +
                "FROM achat a " +
                "JOIN activite act ON a.idActivite = act.idActivite " +
                "WHERE act.idGuide = ? AND YEAR(a.dateAchat) = ? AND a.statut != 'Annulé' " +
                "GROUP BY mois ORDER BY mois";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, guideId);
            ps.setInt(2, year);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(getMonthAbbreviation(rs.getInt("mois")), rs.getInt("nb"));
                }
            }
        }
        return map;
    }

    private String getMonthAbbreviation(int month) {
        String[] months = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
                "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};
        return months[month - 1];
    }

    private Achat mapResultSetToAchat(ResultSet rs) throws SQLException {
        Achat achat = new Achat();
        achat.setIdAchat(rs.getInt("idAchat"));
        achat.setIdClient(rs.getInt("idClient"));
        achat.setIdActivite(rs.getInt("idActivite"));
        achat.setDateAchat(rs.getTimestamp("dateAchat"));
        achat.setNbPlaces(rs.getInt("nbPlaces"));
        achat.setMontantTotal(rs.getDouble("montantTotal"));
        achat.setStatut(rs.getString("statut"));
        return achat;
    }
}