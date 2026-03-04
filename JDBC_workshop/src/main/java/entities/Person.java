package entities;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;
import java.io.Serializable;

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
    private String twoFactorCode;
    private Timestamp twoFactorExpiry;
    private byte[] faceData;
    private byte[] fingerprintData;
    private int fingerprintSlotId = -1;  // ADD THIS - default -1 means no slot assigned

    // Getters and Setters
    public byte[] getFingerprintData() {
        return fingerprintData;
    }

    public void setFingerprintData(byte[] fingerprintData) {
        this.fingerprintData = fingerprintData;
    }

    // ADD THIS Getter and Setter for fingerprintSlotId
    public int getFingerprintSlotId() {
        return fingerprintSlotId;
    }

    public void setFingerprintSlotId(int fingerprintSlotId) {
        this.fingerprintSlotId = fingerprintSlotId;
    }

    // Constructors
    public Person() {}

    public Person(int id, String name, String lastName, String email, String password,
                  Date date, String role, String username) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.date = date;
        this.role = role;
        this.username = username;
        this.status = "offline";
        this.twoFactorEnabled = false;
        this.twoFactorCode = null;
        this.twoFactorExpiry = null;
        this.fingerprintSlotId = -1;  // Initialize to -1
    }

    // Getters and Setters for existing fields
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getTwoFactorCode() { return twoFactorCode; }
    public void setTwoFactorCode(String twoFactorCode) { this.twoFactorCode = twoFactorCode; }

    public Timestamp getTwoFactorExpiry() { return twoFactorExpiry; }
    public void setTwoFactorExpiry(Timestamp twoFactorExpiry) { this.twoFactorExpiry = twoFactorExpiry; }

    public byte[] getFaceData() { return faceData; }
    public void setFaceData(byte[] faceData) { this.faceData = faceData; }

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
                ", fingerprintSlotId=" + fingerprintSlotId +
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