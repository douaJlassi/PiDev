package tn.esprit.projet.entities;

import java.util.Date;

public class Achat {
    private int idAchat;
    private Date dateAchat;
    private int idClient;
    private double montantTotal;
    private String statut; // EN_ATTENTE, CONFIRMÉ, ANNULÉ, COMPLÉTÉ
    private String description;
    private Date dateCreation;

    // ===================== CONSTRUCTEURS =====================
    public Achat() {}

    public Achat(int idAchat, Date dateAchat, int idClient, double montantTotal,
                 String statut, String description) {
        this.idAchat = idAchat;
        this.dateAchat = dateAchat;
        this.idClient = idClient;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.description = description;
    }

    public Achat(Date dateAchat, int idClient, double montantTotal,
                 String statut, String description) {
        this.dateAchat = dateAchat;
        this.idClient = idClient;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.description = description;
    }

    // ===================== GETTERS & SETTERS =====================
    public int getIdAchat() {
        return idAchat;
    }

    public void setIdAchat(int idAchat) {
        this.idAchat = idAchat;
    }

    public Date getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(Date dateAchat) {
        this.dateAchat = dateAchat;
    }

    public int getIdClient() {
        return idClient;
    }

    public void setIdClient(int idClient) {
        this.idClient = idClient;
    }

    public double getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(double montantTotal) {
        this.montantTotal = montantTotal;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    // ===================== TOSTRING =====================
    @Override
    public String toString() {
        return "Achat{" +
                "idAchat=" + idAchat +
                ", dateAchat=" + dateAchat +
                ", idClient=" + idClient +
                ", montantTotal=" + montantTotal +
                ", statut='" + statut + '\'' +
                ", description='" + description + '\'' +
                ", dateCreation=" + dateCreation +
                '}';
    }
}