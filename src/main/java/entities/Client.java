package entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Client {

    private String adresse;
    private Date dateNaissance;
    private List<Like> likeList = new ArrayList<>();
    private List<Publication> publicationList = new ArrayList<>();
    private List<Comment> commentList = new ArrayList<>();


    public Client(){}
    public Client(String adresse, Date dateNaissance) {
        super();
        this.adresse = adresse;
        this.dateNaissance = dateNaissance;
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
