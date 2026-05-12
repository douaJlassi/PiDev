package entities;

public class ServiceEntity {

    private int id;
    private String name;
    private String type; // "VOL" | "HOTEL" | "SERVICE"

    public ServiceEntity() {
    }

    public ServiceEntity(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    // NEW GETTERS / SETTERS

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    // OLD COMPATIBILITY GETTERS / SETTERS

    public int getIdService() {
        return id;
    }

    public void setIdService(int idService) {
        this.id = idService;
    }

    public String getNom() {
        return name;
    }

    public void setNom(String nom) {
        this.name = nom;
    }

    public String getKind() {
        return type;
    }

    public void setKind(String kind) {
        this.type = kind;
    }

    @Override
    public String toString() {
        if (type == null || type.isBlank()) {
            return name;
        }

        return name + " (" + type + ")";
    }
}