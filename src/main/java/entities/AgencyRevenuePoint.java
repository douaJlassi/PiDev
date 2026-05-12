package entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AgencyRevenuePoint {

    private LocalDate day;
    private BigDecimal revenue = BigDecimal.ZERO;

    public AgencyRevenuePoint() {
    }

    public AgencyRevenuePoint(LocalDate day, BigDecimal revenue) {
        this.day = day;
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}