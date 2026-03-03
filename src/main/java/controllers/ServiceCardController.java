package controllers;

import entities.ServiceEntityDetails;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ServiceCardController {

    @FXML private Label nameLbl;
    @FXML private Label kindLbl;
    @FXML private Label descLbl;
    @FXML private Label priceLbl;
    @FXML private Label qtyLbl;
    @FXML private Label subtotalLbl;
    @FXML private Label extraLbl;

    public void setData(ServiceEntityDetails s) {
        nameLbl.setText(s.getNom());
        kindLbl.setText(s.getKind());
        kindLbl.getStyleClass().add("badge-" + s.getKind().toLowerCase()); // vol / hotel / service

        descLbl.setText(s.getDescription() == null ? "" : s.getDescription());

        priceLbl.setText(s.getPrixApplique() + " TND");
        qtyLbl.setText("" + s.getQuantite());
        subtotalLbl.setText(s.getSousTotal() + " TND");

        // Extra info depending on kind
        String extra = "";
        if ("VOL".equals(s.getKind())) {
            extra = "Flight: " + s.getNumeroVol() +
                    " | " + s.getVilleDepart() + " → " + s.getVilleArrivee() +
                    " | " + s.getDateDepart() + " → " + s.getDateArrivee();
        } else if ("HOTEL".equals(s.getKind())) {
            extra = "Hotel: " + s.getNombreEtoiles() + "★" +
                    " | " + s.getLocalisation() +
                    " | Room: " + s.getTypeChambre();
        }
        extraLbl.setText(extra);
        extraLbl.setVisible(!extra.isBlank());
        extraLbl.setManaged(!extra.isBlank());
    }
}
