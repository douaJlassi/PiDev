package entites;

import java.util.Objects;

public class Hotel extends service{
    private int nbEtoiles;
    private String localisation;
    private String chambre;
    public Hotel(){}
    public Hotel(String nom,String description,double prix,boolean disponibilite,int capacite,String type,int nbEtoiles,String localisation,String chambre,String imgUrl){
        super(nom,description,prix,disponibilite,capacite,type,imgUrl);
     this.nbEtoiles = nbEtoiles;
     this.localisation = localisation;
     this.chambre = chambre;
    }

    public int getNbEtoiles() {
        return nbEtoiles;
    }

    public void setNbEtoiles(int nbEtoiles) {
        this.nbEtoiles = nbEtoiles;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getChambre() {
        return chambre;
    }

    public void setChambre(String chambre) {
        this.chambre = chambre;
    }

    @Override
    public String toString(){
        return "Hotel{"+super.toString()+" nombre etoiles:"+nbEtoiles+", localisation:"+localisation+", chambre:"+chambre+"}";
    }
    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if(!(o instanceof Hotel)) return false;
        return super.equals(o) && chambre.equals(((Hotel)o).chambre);
    }
    @Override
    public int hashCode(){
        int result=super.hashCode();
        result=31 * result + Integer.hashCode(nbEtoiles);
        result=31 * result + Objects.hashCode(localisation);
        result=31 * result + Objects.hashCode(chambre);
        return result;
    }
}
