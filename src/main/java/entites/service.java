package entites;
import java.util.Objects;
public class service {

    private String nom, description;
    private  double prix;
    private  boolean disponibilite;
    private int capacite;
    private String image;


    private String type;
    public service(){}
    public service(String nom,String description,double prix,boolean disponibilite,int capacite,String type,String imgUrl){
        this.nom=nom;
        this.description=description;
        this.prix=prix;
        this.disponibilite=disponibilite;
        this.capacite=capacite;
        this.type=type;
        this.image=imgUrl;
}
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public boolean isDisponibilite() {
        return disponibilite;
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
    public boolean getDisponibilite() {
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
    public double getPrix() {
        return prix;
    }
    public void setPrix(double prix) {
        this.prix = prix;
    }
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
    return "service:{nom="+nom+" description:"+description+" disponibilité:"+disponibilite+" capacite:"+capacite+" prix:"+prix+"}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof service)) return false;
        return nom.equals(((service) o).nom) && description.equals(((service) o).description);
    }
    @Override
    public int hashCode() {
        int result = nom.hashCode();
        result = 31 * result + Objects.hashCode(nom);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Double.hashCode(prix);
        result = 31 * result + Integer.hashCode(capacite);
        return result;
    }


}
