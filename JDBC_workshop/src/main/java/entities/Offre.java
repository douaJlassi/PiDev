package entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Offre {

    private int id;
    private String title;
    private String description;
    private BigDecimal promoPrice;
    private BigDecimal originalPrice;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String imageUrl;
    private Integer capacity;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int userId;

    public Offre() {
    }

    public Offre(int id, String title, String description, BigDecimal promoPrice,
                 BigDecimal originalPrice, LocalDate startDate, LocalDate endDate,
                 String status, String imageUrl, Integer capacity,
                 String location, LocalDateTime createdAt,
                 LocalDateTime updatedAt, int userId) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.promoPrice = promoPrice;
        this.originalPrice = originalPrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.imageUrl = imageUrl;
        this.capacity = capacity;
        this.location = location;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPromoPrice() {
        return promoPrice;
    }

    public void setPromoPrice(BigDecimal promoPrice) {
        this.promoPrice = promoPrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return title + " (" + promoPrice + " TND) " + startDate + " → " + endDate;
    }
}