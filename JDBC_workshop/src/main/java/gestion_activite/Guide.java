package gestion_activite;

public class Guide {
    private int id;
    private String name;
    private String lastName;
    private String email;
    private String telephone;
    private String role;
    private String status;
    private boolean disponible; // You can derive this from status or activity count

    public Guide() {}

    public Guide(int id, String name, String lastName, String email, String telephone, String role, String status) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.telephone = telephone;
        this.role = role;
        this.status = status;
        this.disponible = "active".equalsIgnoreCase(status); // Example logic
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    @Override
    public String toString() {
        return name + " " + lastName + " (" + email + ")";
    }
}