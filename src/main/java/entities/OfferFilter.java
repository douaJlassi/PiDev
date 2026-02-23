package entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class OfferFilter {
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate selectedDate;
    private Set<Integer> agencyIds = new HashSet<>();

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public LocalDate getSelectedDate() { return selectedDate; }
    public void setSelectedDate(LocalDate selectedDate) { this.selectedDate = selectedDate; }

    public Set<Integer> getAgencyIds() { return agencyIds; }
    public void setAgencyIds(Set<Integer> agencyIds) {
        this.agencyIds = (agencyIds == null) ? new HashSet<>() : agencyIds;
    }
}