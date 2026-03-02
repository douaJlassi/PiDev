package entities;

import java.io.Serializable;
import java.util.Arrays;

public class Shop implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private int priceCoins;
    private int quantity;
    private byte[] image;
    private String category;

    public Shop() {}

    public Shop(int id, String name, String description, int priceCoins, int quantity, byte[] image, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priceCoins = priceCoins;
        this.quantity = quantity;
        this.image = image;
        this.category = category;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPriceCoins() { return priceCoins; }
    public void setPriceCoins(int priceCoins) { this.priceCoins = priceCoins; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "Shop{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", priceCoins=" + priceCoins +
                ", quantity=" + quantity +
                ", category='" + category + '\'' +
                '}';
    }
}