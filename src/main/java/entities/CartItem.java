package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CartItem {
    private int idReservation;
    private int idOffre;

    private String titre;
    private String imageUrl;
    private BigDecimal prixUnitaire;

    public int getIdReservation() {
        return idReservation;
    }

    public void setIdReservation(int idReservation) {
        this.idReservation = idReservation;
    }

    public int getIdOffre() {
        return idOffre;
    }

    public void setIdOffre(int idOffre) {
        this.idOffre = idOffre;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
    private String agencyStatus;

    public void setAgencyStatus(String agencyStatus) {
        this.agencyStatus = agencyStatus;
    }

    public String getAgencyStatus() {
        return agencyStatus;
    }
    private String nomAgence;
    private String refusalReason;
    private LocalDateTime agencyDecisionAt;

    // getter + setter
    public LocalDateTime getAgencyDecisionAt() {
        return agencyDecisionAt;
    }

    public void setAgencyDecisionAt(LocalDateTime agencyDecisionAt) {
        this.agencyDecisionAt = agencyDecisionAt;
    }


    public String getNomAgence() { return nomAgence; }
    public void setNomAgence(String nomAgence) { this.nomAgence = nomAgence; }

    public String getRefusalReason() { return refusalReason; }
    public void setRefusalReason(String refusalReason) { this.refusalReason = refusalReason; }

    // getters/setters
}
