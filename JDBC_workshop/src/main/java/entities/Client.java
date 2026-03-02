package entities;

import java.io.Serializable;

/**
 * Client — a lightweight social-feed view of a {@link Person}.
 *
 * Since {@code Client} and {@code Person} represent the same user account,
 * this class can be constructed directly from a {@code Person} instance.
 * All field names and getters are kept identical to the original so that
 * existing DAOs, services, and controllers (Comment, Like, Publication, etc.)
 * compile without any changes.
 *
 * <pre>
 *   Person person = personDAO.getById(42);
 *   Client client  = new Client(person);          // bridge constructor
 *   Client client2 = new Client(person, avatarBytes); // with profile image
 * </pre>
 */
public class Client implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Fields (names unchanged from original) ────────────────────────────────
    private int    clientID;    // mirrors Person.id
    private String username;    // mirrors Person.username
    private String avatarPath;  // URL or local path (from Profile.image or a path string)

    // ── Constructors ──────────────────────────────────────────────────────────

    public Client() {}

    /** Original constructor — used by DAOs that build Client directly. */
    public Client(int clientID, String username, String avatarPath) {
        this.clientID   = clientID;
        this.username   = username;
        this.avatarPath = avatarPath;
    }

    /**
     * Bridge constructor — creates a Client from a full Person.
     * avatarPath is left null; set it separately if you have a Profile.
     */
    public Client(Person person) {
        if (person == null) throw new IllegalArgumentException("Person cannot be null");
        this.clientID  = person.getId();
        this.username  = person.getUsername();
        this.avatarPath = null;
    }

    /**
     * Bridge constructor — creates a Client from a Person + avatar path string.
     * Use this when you already have the avatar stored as a file path or URL.
     */
    public Client(Person person, String avatarPath) {
        this(person);
        this.avatarPath = avatarPath;
    }

    // ── Getters & Setters (unchanged) ─────────────────────────────────────────

    public int    getClientID()   { return clientID; }
    public void   setClientID(int clientID) { this.clientID = clientID; }

    public String getUsername()   { return username; }
    public void   setUsername(String username) { this.username = username; }

    public String getAvatarPath() { return avatarPath; }
    public void   setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Convenience factory — converts a Person to a Client.
     * Equivalent to {@code new Client(person)}.
     */
    public static Client fromPerson(Person person) {
        return new Client(person);
    }

    /**
     * Convenience factory — converts a Person + Profile to a Client.
     * The Profile image bytes are NOT stored here (Client keeps a path/URL);
     * pass the resolved path or URL string as {@code avatarPath}.
     */
    public static Client fromPerson(Person person, String avatarPath) {
        return new Client(person, avatarPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client)) return false;
        Client other = (Client) o;
        return clientID == other.clientID;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(clientID);
    }

    @Override
    public String toString() {
        return "Client{clientID=" + clientID + ", username='" + username + "'}";
    }
}