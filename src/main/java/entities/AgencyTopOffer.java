package entities;

import java.math.BigDecimal;

public class AgencyTopOffer {

    private int idOffre;
    private String title;
    private BigDecimal revenue = BigDecimal.ZERO;

    public int getIdOffre() {
        return idOffre;
    }

    public void setIdOffre(int idOffre) {
        this.idOffre = idOffre;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}