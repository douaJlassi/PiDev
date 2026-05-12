package entities;

import java.math.BigDecimal;

public class AgencyAnalyticsKpi {

    private BigDecimal revenue = BigDecimal.ZERO;
    private int confirmedBookings = 0;
    private int pendingLines = 0;
    private double approvalRate = 0.0;

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }

    public int getConfirmedBookings() {
        return confirmedBookings;
    }

    public void setConfirmedBookings(int confirmedBookings) {
        this.confirmedBookings = confirmedBookings;
    }

    public int getPendingLines() {
        return pendingLines;
    }

    public void setPendingLines(int pendingLines) {
        this.pendingLines = pendingLines;
    }

    public double getApprovalRate() {
        return approvalRate;
    }

    public void setApprovalRate(double approvalRate) {
        this.approvalRate = approvalRate;
    }
}