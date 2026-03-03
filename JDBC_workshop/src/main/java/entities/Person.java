package entities;

import java.sql.Date;
import java.util.Objects;
import java.io.Serializable;

/**
 * Person — the canonical authenticated user entity.
 *
 * Since {@link Client} represents the same user in a social-feed context,
 * a {@code Person} can be converted to a {@code Client} via
 * {@code Client.fromPerson(person)} and back via {@link #fromClient(Client)}..
 *
 * <p><b>ID mapping:</b> {@code Person.id} ↔ {@code Client.clientID}</p>
 */
public class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String lastName;
    private String email;
    private String password;
    private Date date;
    private String role;
    private String username;
    private String status;
    private boolean twoFactorEnabled;
    private byte[] faceData;

    // Constructors
    public Person() {}

    public Person(int id, String name, String lastName, String email, String password,
                  Date date, String role, String username) {
        this.id                = id;
        this.name              = name;
        this.lastName          = lastName;
        this.email             = email;
        this.password          = password;
        this.date              = date;
        this.role              = role;
        this.username          = username;
        this.status            = "offline";
        this.twoFactorEnabled  = false;
    }

    // Getters and Setters
    public int    getId()       { return id; }
    public void   setId(int id) { this.id = id; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getLastName()                  { return lastName; }
    public void   setLastName(String lastName)   { this.lastName = lastName; }

    public String getEmail()               { return email; }
    public void   setEmail(String email)   { this.email = email; }

    public String getPassword()                   { return password; }
    public void   setPassword(String password)    { this.password = password; }

    public Date getDate()             { return date; }
    public void setDate(Date date)    { this.date = date; }

    public String getRole()             { return role; }
    public void   setRole(String role)  { this.role = role; }

    public String getUsername()                  { return username; }
    public void   setUsername(String username)   { this.username = username; }

    public String getStatus()               { return status; }
    public void   setStatus(String status)  { this.status = status; }

    public boolean isTwoFactorEnabled()                          { return twoFactorEnabled; }
    public void    setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public byte[] getFaceData()                { return faceData; }
    public void   setFaceData(byte[] faceData) { this.faceData = faceData; }

    // ── Client bridge ─────────────────────────────────────────────────────────

    /**
     * Converts a {@link Client} back into a partial {@code Person}.
     * Only {@code id} and {@code username} are populated — load the full
     * Person from the DB if you need the remaining fields.
     *
     * <pre>
     *   Person partial = Person.fromClient(client);
     *   Person full    = personDAO.getById(partial.getId());
     * </pre>
     */
    public static Person fromClient(Client client) {
        if (client == null) throw new IllegalArgumentException("Client cannot be null");
        Person p = new Person();
        p.setId(client.getClientID());
        p.setUsername(client.getUsername());
        return p;
    }

    /**
     * Convenience shortcut — converts this Person to a Client.
     * Equivalent to {@code Client.fromPerson(this)}.
     */
    public Client toClient() {
        return Client.fromPerson(this);
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", username='" + username + '\'' +
                ", status='" + status + '\'' +
                ", twoFactorEnabled=" + twoFactorEnabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Person person)) return false;
        return id == person.id &&
                Objects.equals(email, person.email) &&
                Objects.equals(username, person.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, username);
    }
}