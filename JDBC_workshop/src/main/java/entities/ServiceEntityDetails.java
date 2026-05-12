package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceEntityDetails {

    // New service fields
    private int id;
    private String name;
    private String type;
    private String description;
    private BigDecimal basePrice;
    private boolean available;
    private Integer capacity;
    private Integer agencyId;
    private String imageUrl;
    private LocalDateTime createdAt;

    // Default compatibility values
    private int quantity = 1;
    private BigDecimal overridePrice;

    // Optional VOL fields, keep them if your vol table still exists
    private String numeroVol;
    private String villeDepart;
    private String villeArrivee;
    private LocalDateTime dateDepart;
    private LocalDateTime dateArrivee;

    // Optional HOTEL fields, keep them if your hotel table still exists
    private Integer nombreEtoiles;
    private String localisation;
    private String typeChambre;

    public ServiceEntityDetails() {
    }

    // -----------------------------
    // PRICE HELPERS
    // -----------------------------

    public BigDecimal getPrixApplique() {
        return overridePrice != null ? overridePrice : basePrice;
    }

    public BigDecimal getSousTotal() {
        if (getPrixApplique() == null) {
            return BigDecimal.ZERO;
        }

        return getPrixApplique().multiply(BigDecimal.valueOf(quantity));
    }

    // -----------------------------
    // NEW GETTERS / SETTERS
    // -----------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }


    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }


    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }


    public Integer getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(Integer agencyId) {
        this.agencyId = agencyId;
    }


    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }


    public BigDecimal getOverridePrice() {
        return overridePrice;
    }

    public void setOverridePrice(BigDecimal overridePrice) {
        this.overridePrice = overridePrice;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // -----------------------------
    // OLD COMPATIBILITY GETTERS / SETTERS
    // -----------------------------

    public int getIdService() {
        return id;
    }

    public void setIdService(int idService) {
        this.id = idService;
    }

    public String getNom() {
        return name;
    }

    public void setNom(String nom) {
        this.name = nom;
    }

    public BigDecimal getPrix() {
        return basePrice;
    }

    public void setPrix(BigDecimal prix) {
        this.basePrice = prix;
    }

    public boolean isDisponibilite() {
        return available;
    }

    public void setDisponibilite(boolean disponibilite) {
        this.available = disponibilite;
    }

    public int getCapacite() {
        return capacity == null ? 0 : capacity;
    }

    public void setCapacite(int capacite) {
        this.capacity = capacite;
    }

    public int getIdAgence() {
        return agencyId == null ? 0 : agencyId;
    }

    public void setIdAgence(int idAgence) {
        this.agencyId = idAgence;
    }

    public int getQuantite() {
        return quantity;
    }

    public void setQuantite(int quantite) {
        this.quantity = quantite;
    }

    public BigDecimal getPrixOverride() {
        return overridePrice;
    }

    public void setPrixOverride(BigDecimal prixOverride) {
        this.overridePrice = prixOverride;
    }

    public String getKind() {
        return type;
    }

    public void setKind(String kind) {
        this.type = kind;
    }

    // -----------------------------
    // VOL
    // -----------------------------

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

    // -----------------------------
    // HOTEL
    // -----------------------------

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