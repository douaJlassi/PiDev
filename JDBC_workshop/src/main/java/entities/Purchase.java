package entities;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Purchase — a shop order placed by a {@link Person} user.
 *
 * <p>{@code userId} is a foreign key to {@link Person#getId()}.
 * {@code shopId} is a foreign key to {@link Shop#getId()}.</p>
 */
public class Purchase implements Serializable {

    private static final long serialVersionUID = 1L;

    private int       id;
    /** FK → {@link Person#getId()} */
    private int       userId;
    /** FK → {@link Shop#getId()} */
    private int       shopId;
    private int       quantity;
    private int       totalCoins;
    private String    buyerName;
    private String    buyerEmail;
    private String    buyerAddress;
    private String    status;
    private Timestamp purchaseDate;

    // Additional fields for history display
    private String productName;
    private String buyerUsername;

    // Constructors
    public Purchase() {
        this.status = "pending";
    }

    public Purchase(int userId, int shopId, int quantity, int totalCoins,
                    String buyerName, String buyerEmail, String buyerAddress) {
        this.userId       = userId;
        this.shopId       = shopId;
        this.quantity     = quantity;
        this.totalCoins   = totalCoins;
        this.buyerName    = buyerName;
        this.buyerEmail   = buyerEmail;
        this.buyerAddress = buyerAddress;
        this.status       = "pending";
    }

    // Getters and Setters
    public int       getId()                { return id; }
    public void      setId(int id)          { this.id = id; }

    public int       getUserId()               { return userId; }
    public void      setUserId(int userId)     { this.userId = userId; }

    public int       getShopId()               { return shopId; }
    public void      setShopId(int shopId)     { this.shopId = shopId; }

    public int       getQuantity()                  { return quantity; }
    public void      setQuantity(int quantity)      { this.quantity = quantity; }

    public int       getTotalCoins()                    { return totalCoins; }
    public void      setTotalCoins(int totalCoins)      { this.totalCoins = totalCoins; }

    public String    getBuyerName()                      { return buyerName; }
    public void      setBuyerName(String buyerName)      { this.buyerName = buyerName; }

    public String    getBuyerEmail()                       { return buyerEmail; }
    public void      setBuyerEmail(String buyerEmail)      { this.buyerEmail = buyerEmail; }

    public String    getBuyerAddress()                         { return buyerAddress; }
    public void      setBuyerAddress(String buyerAddress)      { this.buyerAddress = buyerAddress; }

    public String    getStatus()                { return status; }
    public void      setStatus(String status)   { this.status = status; }

    public Timestamp getPurchaseDate()                       { return purchaseDate; }
    public void      setPurchaseDate(Timestamp purchaseDate) { this.purchaseDate = purchaseDate; }

    public String    getProductName()                        { return productName; }
    public void      setProductName(String productName)      { this.productName = productName; }

    public String    getBuyerUsername()                          { return buyerUsername; }
    public void      setBuyerUsername(String buyerUsername)      { this.buyerUsername = buyerUsername; }

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", userId=" + userId +
                ", shopId=" + shopId +
                ", quantity=" + quantity +
                ", totalCoins=" + totalCoins +
                ", status='" + status + '\'' +
                ", purchaseDate=" + purchaseDate +
                '}';
    }
}