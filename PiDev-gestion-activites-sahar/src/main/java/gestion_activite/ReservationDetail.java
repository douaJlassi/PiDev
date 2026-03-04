package gestion_activite;

public class ReservationDetail {
    private String clientNom;
    private String clientEmail;
    private String clientTelephone;
    private int nbPlaces;
    private double montantPaye;

    public ReservationDetail(String clientNom, String clientEmail, String clientTelephone, int nbPlaces, double montantPaye) {
        this.clientNom = clientNom;
        this.clientEmail = clientEmail;
        this.clientTelephone = clientTelephone;
        this.nbPlaces = nbPlaces;
        this.montantPaye = montantPaye;
    }

    // Getters et setters
    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }
    public int getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(int nbPlaces) { this.nbPlaces = nbPlaces; }
    public double getMontantPaye() { return montantPaye; }
    public void setMontantPaye(double montantPaye) { this.montantPaye = montantPaye; }
}