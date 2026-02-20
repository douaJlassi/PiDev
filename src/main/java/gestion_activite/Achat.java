package gestion_activite;

import java.sql.Timestamp;

public class Achat
{
    private int idAchat;
    private Timestamp dateAchat;
    private double montantTotal;
    private String statut;
    private int idClient;


    public Achat() {}

    public Achat(Timestamp dateAchat, double montantTotal, String statut, int idClient) {
        this.dateAchat = dateAchat;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.idClient = idClient;
    }

    public Achat(int idAchat, Timestamp dateAchat, double montantTotal, String statut, int idClient) {
        this.idAchat = idAchat;
        this.dateAchat = dateAchat;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.idClient = idClient;
    }


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

    @Override
    public String toString() {
        return "Achat{" +
                "idAchat=" + idAchat +
                ", dateAchat=" + dateAchat +
                ", montantTotal=" + montantTotal +
                ", statut='" + statut + '\'' +
                ", idClient=" + idClient +
                '}';
    }
}