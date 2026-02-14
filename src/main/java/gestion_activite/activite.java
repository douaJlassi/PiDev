package gestion_activite;

import java.util.Date;

public class Activite {

    private int idActivite;
    private String titre;
    private String description;
    private String lieu;
    private Date dateActivite;
    private int dureParJour;
    private double prix;
    private int idGuide;

    public Activite() {
    }

    public Activite(String titre, String description, String lieu,
                    Date dateActivite, int dureParJour,
                    double prix, int idGuide) {

        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.dateActivite = dateActivite;
        this.dureParJour = dureParJour;
        this.prix = prix;
        this.idGuide = idGuide;
    }

    public Activite(int idActivite, String titre, String description,
                    String lieu, Date dateActivite,
                    int dureParJour, double prix, int idGuide) {

        this.idActivite = idActivite;
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.dateActivite = dateActivite;
        this.dureParJour = dureParJour;
        this.prix = prix;
        this.idGuide = idGuide;
    }

    // Getters & Setters
    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public Date getDateActivite() { return dateActivite; }
    public void setDateActivite(Date dateActivite) { this.dateActivite = dateActivite; }

    public int getDureParJour() { return dureParJour; }
    public void setDureParJour(int dureParJour) { this.dureParJour = dureParJour; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getIdGuide() { return idGuide; }
    public void setIdGuide(int idGuide) { this.idGuide = idGuide; }
}
