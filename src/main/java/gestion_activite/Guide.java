package gestion_activite;

public class Guide {
    private int idUser;
    private boolean disponibilite;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    public Guide() {}

    public Guide(int idUser, boolean disponibilite, String nom, String prenom, String email, String telephone) {
        this.idUser = idUser;
        this.disponibilite = disponibilite;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
    }

    public Guide(int idUser, boolean disponibilite) {
        this.idUser = idUser;
        this.disponibilite = disponibilite;
    }


    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public boolean isDisponibilite() { return disponibilite; }
    public void setDisponibilite(boolean disponibilite) { this.disponibilite = disponibilite; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    @Override
    public String toString() {
        return "Guide{" + "idUser=" + idUser + ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' + ", disponibilite=" + disponibilite + '}';
    }
}