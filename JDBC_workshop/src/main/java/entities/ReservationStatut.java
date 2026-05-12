package entities;

public enum ReservationStatut {

    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED;

    public static ReservationStatut fromDb(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }

        return switch (value.trim().toUpperCase()) {

            // New database values
            case "PENDING" -> PENDING;
            case "CONFIRMED" -> CONFIRMED;
            case "REJECTED" -> REJECTED;
            case "CANCELLED" -> CANCELLED;

            // Old values, just in case some old code still sends them
            case "ENATTENTE" -> PENDING;
            case "CONFIRME" -> CONFIRMED;
            case "REFUSEE" -> REJECTED;
            case "ANNULE" -> CANCELLED;
            case "PANIER" -> PENDING;

            default -> PENDING;
        };
    }
}