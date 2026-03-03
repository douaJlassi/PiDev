package entities;

public class ServiceEntity {
    private int idService;
    private String nom;
    private String kind; // "VOL" | "HOTEL" | "SERVICE"

    public ServiceEntity() {}

    public ServiceEntity(int idService, String nom, String kind) {
        this.idService = idService;
        this.nom = nom;
        this.kind = kind;
    }

    public int getIdService() { return idService; }
    public void setIdService(int idService) { this.idService = idService; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }


    @Override
    public String toString() {
        if (kind == null || kind.isBlank()) return nom;
        return nom + " (" + kind + ")";
    }
}
