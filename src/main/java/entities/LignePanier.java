package entities;

import java.math.BigDecimal;

public class LignePanier {
    private int idReservation;
    private int idOffre;
    private BigDecimal prixUnitaire;

    public LignePanier() {}

    public LignePanier(int idReservation, int idOffre, BigDecimal prixUnitaire) {
        this.idReservation = idReservation;
        this.idOffre = idOffre;
        this.prixUnitaire = prixUnitaire;
    }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }}
