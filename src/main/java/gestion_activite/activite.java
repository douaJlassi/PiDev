package gestion_activite;

import java.sql.Timestamp;

public class Activite {
    private int idActivite;
    private String titre;
    private String description;
    private String lieu;
    private Timestamp dateActivite;
    private int dureParJour;
    private double prix;
    private int idGuide;
    private String image;

    private String statut;
    private int placesDisponibles;

    private String categorie;


    public Activite() {}


    public Activite(int idActivite, String titre, String description, String lieu,
                    Timestamp dateActivite, int dureParJour, double prix,
                    int idGuide, String image, String statut, int placesDisponibles, String categorie) {
        this.idActivite = idActivite;
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.dateActivite = dateActivite;
        this.dureParJour = dureParJour;
        this.prix = prix;
        this.idGuide = idGuide;
        this.image = image;
        this.statut = statut;
        this.placesDisponibles = placesDisponibles;
        this.categorie = categorie;
    }

    public Activite(String titre, String description, String lieu,
                    Timestamp dateActivite, int dureParJour, double prix,
                    int idGuide, String image, String statut, int placesDisponibles, String categorie) {
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.dateActivite = dateActivite;
        this.dureParJour = dureParJour;
        this.prix = prix;
        this.idGuide = idGuide;
        this.image = image;
        this.statut = statut;
        this.placesDisponibles = placesDisponibles;
        this.categorie = categorie;
    }


    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public Timestamp getDateActivite() { return dateActivite; }
    public void setDateActivite(Timestamp dateActivite) { this.dateActivite = dateActivite; }

    public int getDureParJour() { return dureParJour; }
    public void setDureParJour(int dureParJour) { this.dureParJour = dureParJour; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getIdGuide() { return idGuide; }
    public void setIdGuide(int idGuide) { this.idGuide = idGuide; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(int placesDisponibles) { this.placesDisponibles = placesDisponibles; }


    public String getCategorie() { return categorie; }          // NEW
    public void setCategorie(String categorie) { this.categorie = categorie; }

    @Override
    public String toString() {
        return "Activite{" +
                "idActivite=" + idActivite +
                ", titre='" + titre + '\'' +
                ", prix=" + prix +
                ", statut='" + statut + '\'' +
                ", placesDisponibles=" + placesDisponibles +
                ", categorie='" + categorie + '\'' +
                '}';
    }
}
