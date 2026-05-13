package controllers;

import entities.ServiceEntityDetails;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.math.BigDecimal;

public class ServiceCardController {

    @FXML
    private Label nameLbl;

    @FXML
    private Label kindLbl;

    @FXML
    private Label descLbl;

    @FXML
    private Label priceLbl;

    @FXML
    private Label qtyLbl;

    @FXML
    private Label subtotalLbl;

    @FXML
    private Label extraLbl;

    public void setData(ServiceEntityDetails s) {

        if (s == null) {
            clear();
            return;
        }

        String name = s.getName() != null
                ? s.getName()
                : s.getNom();

        String type = s.getType() != null
                ? s.getType()
                : s.getKind();

        if (type == null || type.isBlank()) {
            type = "SERVICE";
        }

        type = type.toUpperCase();

        nameLbl.setText(name == null ? "Service" : name);
        kindLbl.setText(type);

        kindLbl.getStyleClass().removeIf(c ->
                c.equals("badge-vol") ||
                        c.equals("badge-hotel") ||
                        c.equals("badge-service")
        );

        kindLbl.getStyleClass().add(
                "badge-" + type.toLowerCase()
        );

        descLbl.setText(
                s.getDescription() == null
                        ? ""
                        : s.getDescription()
        );

        BigDecimal price = s.getPrixApplique();

        priceLbl.setText(
                price == null
                        ? "- TND"
                        : price + " TND"
        );

        int quantity = s.getQuantity() > 0
                ? s.getQuantity()
                : s.getQuantite();

        if (quantity <= 0) {
            quantity = 1;
        }

        qtyLbl.setText(String.valueOf(quantity));

        BigDecimal subtotal =
                price == null
                        ? BigDecimal.ZERO
                        : price.multiply(BigDecimal.valueOf(quantity));

        subtotalLbl.setText(subtotal + " TND");

        String extra = buildExtraInfo(s, type);

        extraLbl.setText(extra);
        extraLbl.setVisible(!extra.isBlank());
        extraLbl.setManaged(!extra.isBlank());
    }

    private String buildExtraInfo(ServiceEntityDetails s, String type) {

        if ("VOL".equalsIgnoreCase(type)) {
            return "Flight: " +
                    safe(s.getNumeroVol()) +
                    " | " +
                    safe(s.getVilleDepart()) +
                    " → " +
                    safe(s.getVilleArrivee()) +
                    " | " +
                    safeObj(s.getDateDepart()) +
                    " → " +
                    safeObj(s.getDateArrivee());
        }

        if ("HOTEL".equalsIgnoreCase(type)) {
            return "Hotel: " +
                    safeObj(s.getNombreEtoiles()) +
                    "★" +
                    " | " +
                    safe(s.getLocalisation()) +
                    " | Room: " +
                    safe(s.getTypeChambre());
        }

        String capacity = s.getCapacity() == null
                ? ""
                : "Capacity: " + s.getCapacity();

        String available = s.isAvailable()
                ? "Available"
                : "Not available";

        if (capacity.isBlank()) {
            return available;
        }

        return available + " | " + capacity;
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "-"
                : value;
    }

    private String safeObj(Object value) {
        return value == null
                ? "-"
                : value.toString();
    }

    private void clear() {
        nameLbl.setText("");
        kindLbl.setText("");
        descLbl.setText("");
        priceLbl.setText("");
        qtyLbl.setText("");
        subtotalLbl.setText("");
        extraLbl.setText("");
        extraLbl.setVisible(false);
        extraLbl.setManaged(false);
    }
}