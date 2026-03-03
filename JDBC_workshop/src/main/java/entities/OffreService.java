package entities;

import java.math.BigDecimal;

public class OffreService {
    private int idOffre;
    private int idService;
    private int quantite;
    private BigDecimal prixOverride; // nullable

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public int getIdService() { return idService; }
    public void setIdService(int idService) { this.idService = idService; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public BigDecimal getPrixOverride() { return prixOverride; }
    public void setPrixOverride(BigDecimal prixOverride) { this.prixOverride = prixOverride; }
}
