package entities;

public class SearchCriteria {
    // Basic Filters
    public Double maxPrice;
    public String destination;

    // Deep Hotel Filters
    public Integer minStars;
    public String roomType; // e.g., "SGL", "DBL"

    // Deep Flight (Vol) Filters
    public String departureCity;
    public String arrivalCity;

    // Logic Flags
    public boolean requiresHotel;
    public boolean requiresFlight;
}