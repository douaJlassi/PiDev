package entities;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Offre {
    private int idOffre;
    private String titre;
    private String description;
    private BigDecimal prixPromo;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int idAgence;
    private String imageUrl;
    private String nomAgence;

    // Optional: if you applied the "status" column
    // private OffreStatus status = OffreStatus.ACTIVE;

    public Offre() {}

    public Offre(int idOffre, String titre, String description, BigDecimal prixPromo,
                 LocalDate dateDebut, LocalDate dateFin, int idAgence, String imageUrl) {
        this.idOffre = idOffre;
        this.titre = titre;
        this.description = description;
        this.prixPromo = prixPromo;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.idAgence = idAgence;
        this.imageUrl = imageUrl;
    }

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrixPromo() { return prixPromo; }
    public void setPrixPromo(BigDecimal prixPromo) { this.prixPromo = prixPromo; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getIdAgence() { return idAgence; }
    public void setIdAgence(int idAgence) { this.idAgence = idAgence; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getNomAgence() { return nomAgence; }
    public void setNomAgence(String nomAgence) { this.nomAgence = nomAgence; }

    @Override
    public String toString() {
        return titre + " (" + prixPromo + " TND) " + dateDebut + " → " + dateFin;
    }
}