package entities;

public class Agency {
    private int idUser;
    private String nomAgence;

    public Agency() {}

    public Agency(int idUser, String nomAgence) {
        this.idUser = idUser;
        this.nomAgence = nomAgence;
    }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public String getNomAgence() { return nomAgence; }
    public void setNomAgence(String nomAgence) { this.nomAgence = nomAgence; }

    @Override
    public String toString() {
        return nomAgence == null ? ("#" + idUser) : nomAgence; // ComboBox display
    }
}
