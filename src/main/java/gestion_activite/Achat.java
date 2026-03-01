package gestion_activite;

import java.sql.Timestamp;

public class Achat {
    private int idAchat;
    private Timestamp dateAchat;
    private double montantTotal;
    private String statut;
    private int idClient;
    private int nbPlaces;
    private int idActivite;

    public Achat() {}

    // Constructor without id (for insertion)
    public Achat(Timestamp dateAchat, double montantTotal, String statut,
                 int idClient, int nbPlaces, int idActivite) {
        this.dateAchat = dateAchat;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.idClient = idClient;
        this.nbPlaces = nbPlaces;
        this.idActivite = idActivite;
    }

    // Constructor with id (for retrieval)
    public Achat(int idAchat, Timestamp dateAchat, double montantTotal, String statut,
                 int idClient, int nbPlaces, int idActivite) {
        this.idAchat = idAchat;
        this.dateAchat = dateAchat;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.idClient = idClient;
        this.nbPlaces = nbPlaces;
        this.idActivite = idActivite;
    }

    // Getters and setters for ALL fields
    public int getIdAchat() { return idAchat; }
    public void setIdAchat(int idAchat) { this.idAchat = idAchat; }

    public Timestamp getDateAchat() { return dateAchat; }
    public void setDateAchat(Timestamp dateAchat) { this.dateAchat = dateAchat; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(int nbPlaces) { this.nbPlaces = nbPlaces; }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
}