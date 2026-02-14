package tn.esprit.projet.entities;

public class Guide {
    private int idUser;
    private boolean disponibilite;

    // ===================== CONSTRUCTEURS =====================
    public Guide() {}

    public Guide(int idUser, boolean disponibilite) {
        this.idUser = idUser;
        this.disponibilite = disponibilite;
    }

    // ===================== GETTERS & SETTERS =====================
    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public boolean isDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(boolean disponibilite) {
        this.disponibilite = disponibilite;
    }

    // ===================== TOSTRING =====================
    @Override
    public String toString() {
        return "Guide{" +
                "idUser=" + idUser +
                ", disponibilite=" + disponibilite +
                '}';
    }
}