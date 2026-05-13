package entities;

import java.time.LocalDateTime;

public class Actualite {

    private int idActualite;
    private int idOffre;
    private int idAgence;

    private String bannerUrl;
    private String titre;

    private LocalDateTime createdAt;
    private boolean active;

    private LocalDateTime endsAt;
    private int clickCount;

    public int getIdActualite() { return idActualite; }
    public void setIdActualite(int idActualite) { this.idActualite = idActualite; }

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public int getIdAgence() { return idAgence; }
    public void setIdAgence(int idAgence) { this.idAgence = idAgence; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }

    public int getClickCount() { return clickCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
}