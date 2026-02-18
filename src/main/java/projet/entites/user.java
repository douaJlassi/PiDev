package projet.entites;

import java.util.Objects;

public class user {
    private String nom;
    private String prenom;
    private String type;
    public  user(){}
    public user(String nom, String prenom, String type) {
        this.nom = nom;
        this.prenom = prenom;
        this.type = type;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof user user)) return false;
        return Objects.equals(nom, user.nom) && Objects.equals(prenom, user.prenom) && Objects.equals(type, user.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom, prenom, type);
    }
}
