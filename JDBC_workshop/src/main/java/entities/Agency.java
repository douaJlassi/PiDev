package entities;

import java.io.Serializable;

/**
 * Agency — a travel agency registered on the Rehletna platform.
 *
 * Agencies have their own login (email + password) and can moderate
 * posts that travellers tag them in.
 */
public class Agency implements Serializable {

    private static final long serialVersionUID = 1L;

    private int    agencyID;
    private String name;
    private String email;
    private String password;      // stored as bcrypt hash in DB
    private String description;
    private String logoPath;
    private String phone;
    private String address;

    public Agency() {}

    public Agency(int agencyID, String name, String email, String description,
                  String logoPath, String phone, String address) {
        this.agencyID    = agencyID;
        this.name        = name;
        this.email       = email;
        this.description = description;
        this.logoPath    = logoPath;
        this.phone       = phone;
        this.address     = address;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int    getAgencyID()    { return agencyID; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPassword()    { return password; }
    public String getDescription() { return description; }
    public String getLogoPath()    { return logoPath; }
    public String getPhone()       { return phone; }
    public String getAddress()     { return address; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setAgencyID(int agencyID)       { this.agencyID    = agencyID; }
    public void setName(String name)            { this.name        = name; }
    public void setEmail(String email)          { this.email       = email; }
    public void setPassword(String password)    { this.password    = password; }
    public void setDescription(String desc)     { this.description = desc; }
    public void setLogoPath(String logoPath)    { this.logoPath    = logoPath; }
    public void setPhone(String phone)          { this.phone       = phone; }
    public void setAddress(String address)      { this.address     = address; }

    // ── Utility ───────────────────────────────────────────────────────────────
    /** Display name — falls back to email if name not set. */
    public String getDisplayName() {
        return (name != null && !name.isBlank()) ? name : email;
    }

    @Override
    public String toString() {
        return "Agency{id=" + agencyID + ", name='" + name + "', email='" + email + "'}";
    }
}