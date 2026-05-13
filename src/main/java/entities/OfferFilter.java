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

    // NEW FILTERS
    private Set<String> statuses = new HashSet<>();
    private Set<String> locations = new HashSet<>();

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public Set<String> getStatuses() {
        return statuses;
    }

    public void setStatuses(Set<String> statuses) {
        this.statuses = (statuses == null)
                ? new HashSet<>()
                : statuses;
    }

    public Set<String> getLocations() {
        return locations;
    }

    public void setLocations(Set<String> locations) {
        this.locations = (locations == null)
                ? new HashSet<>()
                : locations;
    }
}