package entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Client {
    private int clientID;
    private String adresse;
    private Date dateNaissance;
    private List<Like> likeList = new ArrayList<>();
    private List<Publication> publicationList = new ArrayList<>();
    private List<Comment> commentList = new ArrayList<>();


    public Client(){}
    public Client(int clientID,String adresse, Date dateNaissance) {
        super();
        this.adresse = adresse;
        this.dateNaissance = dateNaissance;
        this.clientID = clientID;
    }
    public int getClientID() {
        return clientID;
    }
    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    public Date getDateNaissance() {
        return dateNaissance;
    }
    public void setDateNaissance(Date dateNaissance) {
        this.dateNaissance = dateNaissance;
    }
    @Override
    public String toString() {
        return "Adresse: " + getAdresse() + "| date naissance: " + getDateNaissance();
    }
}
