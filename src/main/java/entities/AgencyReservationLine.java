package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AgencyReservationLine {

    private int idReservation;
    private int idClient;
    private ReservationStatut reservationStatut;
    private LocalDateTime createdAt;

    private int idOffre;
    private String offerTitle;
    private BigDecimal prixFinal;

    private AgencyStatut agencyStatut;
    private LocalDateTime agencyDecisionAt;
    private String refusalReason;

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public ReservationStatut getReservationStatut() { return reservationStatut; }
    public void setReservationStatut(ReservationStatut reservationStatut) { this.reservationStatut = reservationStatut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public String getOfferTitle() { return offerTitle; }
    public void setOfferTitle(String offerTitle) { this.offerTitle = offerTitle; }

    public BigDecimal getPrixFinal() { return prixFinal; }
    public void setPrixFinal(BigDecimal prixFinal) { this.prixFinal = prixFinal; }

    public AgencyStatut getAgencyStatut() { return agencyStatut; }
    public void setAgencyStatut(AgencyStatut agencyStatut) { this.agencyStatut = agencyStatut; }

    public LocalDateTime getAgencyDecisionAt() { return agencyDecisionAt; }
    public void setAgencyDecisionAt(LocalDateTime agencyDecisionAt) { this.agencyDecisionAt = agencyDecisionAt; }
}