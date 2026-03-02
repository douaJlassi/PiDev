package entities;

import java.math.BigDecimal;

public class AgencyTopClient {
    private int idClient;
    private int bookings;
    private BigDecimal revenue = BigDecimal.ZERO;

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getBookings() { return bookings; }
    public void setBookings(int bookings) { this.bookings = bookings; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
}