package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OffreReservation {
    private int idReservation;
    private LocalDateTime dateReservation;
    private ReservationStatut statut;
    private String modePaiement;
    private BigDecimal montantTotal;
    private int idClient;

    public OffreReservation() {}

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public ReservationStatut getStatut() { return statut; }
    public void setStatut(ReservationStatut statut) { this.statut = statut; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }
}