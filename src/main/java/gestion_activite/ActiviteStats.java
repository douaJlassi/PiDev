package gestion_activite;


public class ActiviteStats {
    private String titre;
    private int nbReservations;
    private int totalParticipants;
    private double revenuTotal;

    public ActiviteStats(String titre, int nbReservations, int totalParticipants, double revenuTotal) {
        this.titre = titre;
        this.nbReservations = nbReservations;
        this.totalParticipants = totalParticipants;
        this.revenuTotal = revenuTotal;
    }

    public String getTitre() { return titre; }
    public int getNbReservations() { return nbReservations; }
    public int getTotalParticipants() { return totalParticipants; }
    public double getRevenuTotal() { return revenuTotal; }
}