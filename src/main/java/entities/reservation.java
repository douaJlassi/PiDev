package entities;

import java.util.Date;
import java.util.Objects;

public class reservation {
    private final int seatNb;
    private Date dateReservation;
    private String statut;
    private int idService;
    private  String modePaiement;
private  String nom;
    public reservation(String statut, Date dateReservation, int idService, String modePaiement,String nom,int seatNb) {
        this.statut = statut;
        this.dateReservation = dateReservation;
        this.idService = idService;
        this.modePaiement = modePaiement;
        this.nom = nom;
        this.seatNb = seatNb;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Date getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(Date dateReservation) {
        this.dateReservation = dateReservation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdService() {
        return idService;
    }

    public void setIdService(int idService) {
        this.idService = idService;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public int getSeatNb() {
        return seatNb;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof reservation that)) return false;
        return idService == that.idService && Objects.equals(dateReservation, that.dateReservation) && Objects.equals(statut, that.statut) && Objects.equals(modePaiement, that.modePaiement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateReservation, statut, idService, modePaiement);
    }

}
