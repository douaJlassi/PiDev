package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceEntityDetails {
    // base service
    private int idService;
    private String nom;
    private String description;
    private BigDecimal prix;
    private boolean disponibilite;
    private int capacite;
    private int idAgence;

    // pivot
    private int quantite;
    private BigDecimal prixOverride;

    // inferred type
    private String kind; // "VOL" or "HOTEL" or "SERVICE"

    // VOL
    private String numeroVol;
    private String villeDepart;
    private String villeArrivee;
    private LocalDateTime dateDepart;
    private LocalDateTime dateArrivee;

    // HOTEL
    private Integer nombreEtoiles;
    private String localisation;
    private String typeChambre;

    public BigDecimal getPrixApplique() {
        return (prixOverride != null) ? prixOverride : prix;
    }

    public BigDecimal getSousTotal() {
        if (getPrixApplique() == null) return BigDecimal.ZERO;
        return getPrixApplique().multiply(BigDecimal.valueOf(quantite));
    }


    public int getIdService() {
        return idService;
    }

    public void setIdService(int idService) {
        this.idService = idService;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public boolean isDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(boolean disponibilite) {
        this.disponibilite = disponibilite;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public int getIdAgence() {
        return idAgence;
    }


    public void setIdAgence(int idAgence) {
        this.idAgence = idAgence;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setPrixOverride(BigDecimal prixOverride) {
        this.prixOverride = prixOverride;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getNumeroVol() {
        return numeroVol;
    }

    public void setNumeroVol(String numeroVol) {
        this.numeroVol = numeroVol;
    }

    public String getVilleDepart() {
        return villeDepart;
    }

    public void setVilleDepart(String villeDepart) {
        this.villeDepart = villeDepart;
    }

    public String getVilleArrivee() {
        return villeArrivee;
    }

    public void setVilleArrivee(String villeArrivee) {
        this.villeArrivee = villeArrivee;
    }

    public LocalDateTime getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(LocalDateTime dateDepart) {
        this.dateDepart = dateDepart;
    }

    public LocalDateTime getDateArrivee() {
        return dateArrivee;
    }

    public void setDateArrivee(LocalDateTime dateArrivee) {
        this.dateArrivee = dateArrivee;
    }

    public Integer getNombreEtoiles() {
        return nombreEtoiles;
    }

    public void setNombreEtoiles(Integer nombreEtoiles) {
        this.nombreEtoiles = nombreEtoiles;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getTypeChambre() {
        return typeChambre;
    }

    public void setTypeChambre(String typeChambre) {
        this.typeChambre = typeChambre;
    }
}
