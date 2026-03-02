package entities;

import java.io.Serializable;

/**
 * Profile — extended user settings and avatar for a {@link Person}.
 *
 * <p>{@code idUser} is a foreign key to {@code Person.id}.</p>
 */
public class Profile implements Serializable {

    private static final long serialVersionUID = 1L;

    private int    id;
    private byte[] image;
    private String memberPremium;
    private String language;
    /** Foreign key → {@link Person#getId()} */
    private int    idUser;
    private int    coins;

    // Default constructor
    public Profile() {}

    // Full constructor
    public Profile(int id, byte[] image, String memberPremium,
                   String language, int idUser, int coins) {
        this.id           = id;
        this.image        = image;
        this.memberPremium = memberPremium;
        this.language     = language;
        this.idUser       = idUser;
        this.coins        = coins;
    }

    // Constructor without id (for new profiles)
    public Profile(byte[] image, String memberPremium,
                   String language, int idUser, int coins) {
        this.image         = image;
        this.memberPremium = memberPremium;
        this.language      = language;
        this.idUser        = idUser;
        this.coins         = coins;
    }

    // Getters and Setters
    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }

    public byte[] getImage()               { return image; }
    public void   setImage(byte[] image)   { this.image = image; }

    public String getMemberPremium()                     { return memberPremium; }
    public void   setMemberPremium(String memberPremium) { this.memberPremium = memberPremium; }

    public String getLanguage()                  { return language; }
    public void   setLanguage(String language)   { this.language = language; }

    /** @return Person.id of the owning user */
    public int  getIdUser()             { return idUser; }
    public void setIdUser(int idUser)   { this.idUser = idUser; }

    public int  getCoins()              { return coins; }
    public void setCoins(int coins)     { this.coins = coins; }

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