package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LignePanier {
    private int idReservation;
    private int idOffre;
    private BigDecimal prixUnitaire;

    // NEW: Tracking agency status and timing
    private AgencyStatut agencyStatus;
    private LocalDateTime agencyDecisionAt;
    private String refusalReason;

    public LignePanier() {
        // Default to pending when created
        this.agencyStatus = AgencyStatut.ENATTENTE;
    }

    public LignePanier(int idReservation, int idOffre, BigDecimal prixUnitaire) {
        this();
        this.idReservation = idReservation;
        this.idOffre = idOffre;
        this.prixUnitaire = prixUnitaire;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    // --- Existing Getters/Setters ---
    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setUnitaryPrice(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    // --- NEW Getters/Setters ---
    public AgencyStatut getAgencyStatus() { return agencyStatus; }
    public void setAgencyStatus(AgencyStatut agencyStatus) { this.agencyStatus = agencyStatus; }

    public LocalDateTime getAgencyDecisionAt() { return agencyDecisionAt; }
    public void setAgencyDecisionAt(LocalDateTime agencyDecisionAt) { this.agencyDecisionAt = agencyDecisionAt; }
}