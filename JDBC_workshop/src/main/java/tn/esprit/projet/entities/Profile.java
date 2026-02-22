package tn.esprit.projet.entities;

import java.io.Serializable;
import java.util.Arrays;

public class Profile implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private byte[] image;
    private String memberPremium;
    private String language;
    private int idUser;
    private int coins;

    public Profile() {}

    public Profile(int id, byte[] image, String memberPremium,  String language, int idUser, int coins) {
        this.id = id;
        this.image = image;
        this.memberPremium = memberPremium;

        this.language = language;
        this.idUser = idUser;
        this.coins = coins;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public String getMemberPremium() { return memberPremium; }
    public void setMemberPremium(String memberPremium) { this.memberPremium = memberPremium; }



    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    @Override
    public String toString() {
        return "Profile{" +
                "id=" + id +
                ", memberPremium='" + memberPremium + '\'' +
                ", language='" + language + '\'' +
                ", idUser=" + idUser +
                ", coins=" + coins +
                '}';
    }
}