package gestion_activite;

import java.sql.Timestamp;

public class Achat {
    private int idAchat;
    private int idClient;
    private int idActivite;
    private Timestamp dateAchat;
    private int nbPlaces;
    private double montantTotal;
    private String statut;

    // Default constructor
    public Achat() {}

    // Constructor without id (for new records)
    public Achat(int idClient, int idActivite, Timestamp dateAchat, int nbPlaces, double montantTotal, String statut) {
        this.idClient = idClient;
        this.idActivite = idActivite;
        this.dateAchat = dateAchat;
        this.nbPlaces = nbPlaces;
        this.montantTotal = montantTotal;
        this.statut = statut;
    }

    // Full constructor
    public Achat(int idAchat, int idClient, int idActivite, Timestamp dateAchat, int nbPlaces, double montantTotal, String statut) {
        this.idAchat = idAchat;
        this.idClient = idClient;
        this.idActivite = idActivite;
        this.dateAchat = dateAchat;
        this.nbPlaces = nbPlaces;
        this.montantTotal = montantTotal;
        this.statut = statut;
    }

    // Getters and Setters
    public int getIdAchat() { return idAchat; }
    public void setIdAchat(int idAchat) { this.idAchat = idAchat; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public Timestamp getDateAchat() { return dateAchat; }
    public void setDateAchat(Timestamp dateAchat) { this.dateAchat = dateAchat; }

    public int getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(int nbPlaces) { this.nbPlaces = nbPlaces; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Achat{" +
                "idAchat=" + idAchat +
                ", idClient=" + idClient +
                ", idActivite=" + idActivite +
                ", dateAchat=" + dateAchat +
                ", nbPlaces=" + nbPlaces +
                ", montantTotal=" + montantTotal +
                ", statut='" + statut + '\'' +
                '}';
    }
}