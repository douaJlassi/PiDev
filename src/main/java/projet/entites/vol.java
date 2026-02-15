package projet.entites;
import java.util.Date;
import java.util.Objects;

public class vol extends service{
    private String numeroVol,villeDepart,villeArrivee;
    private Date dateDepart,dateArrivee;
    public vol(String nom,String description,double prix,boolean disponibilite,int capacite,String numeroVol,String villeDepart,String villeArrivee,Date dateDepart,Date dateArrivee){
        super(nom,description,prix,disponibilite,capacite);
        this.numeroVol=numeroVol;
        this.villeDepart=villeDepart;
        this.villeArrivee=villeArrivee;
        this.dateDepart=dateDepart;
        this.dateArrivee=dateArrivee;
    }

    public Date getDateArrivee() {
        return dateArrivee;
    }
    public void setDateArrivee(Date dateArrivee) {
        this.dateArrivee = dateArrivee;
    }
    public Date getDateDepart() {
        return dateDepart;
    }
    public void setDateDepart(Date dateDepart) {
        this.dateDepart = dateDepart;
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

    @Override
    public String toString() {
        return "vol:{"+super.toString()+"numero vol:"+numeroVol+" ville depart:"+villeDepart+" ville arrivee:"+villeArrivee+" date arrivee:"+dateArrivee+" date depart:"+dateDepart+"}";
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if(!(obj instanceof vol)) return false;
        return super.equals(obj) && numeroVol.equals(((vol) obj).numeroVol);
    }
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(numeroVol);
        result = 31 * result + Objects.hashCode(villeArrivee);
        result = 31 * result + Objects.hashCode(villeDepart);
        return result;
    }

}
