package controllers;

import app.Session;
import entities.Offre;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import repositories.IOffreRepository;
import repositories.OffreRepository;

public class ArchivedOfferCardController {

    @FXML
    private Label titleLbl;

    @FXML
    private Label priceLbl;

    @FXML
    private Label datesLbl;

    private Offre offer;

    private Runnable onChanged;

    private final IOffreRepository repo =
            new OffreRepository();

    public void setData(
            Offre offer,
            Runnable onChanged
    ) {

        this.offer = offer;

        this.onChanged = onChanged;

        // ------------------------------------
        // TITLE
        // ------------------------------------

        titleLbl.setText(
                offer.getTitle()
        );

        // ------------------------------------
        // PRICE
        // ------------------------------------

        if (offer.getPromoPrice() != null) {

            priceLbl.setText(
                    "Price: "
                            + offer.getPromoPrice()
                            + " TND"
            );

        } else {

            priceLbl.setText("Price: ---");
        }

        // ------------------------------------
        // DATES
        // ------------------------------------

        datesLbl.setText(
                "Dates: "
                        + offer.getStartDate()
                        + " → "
                        + offer.getEndDate()
        );
    }

    @FXML
    private void onRestore() {

        boolean ok =
                repo.restoreForAgency(
                        offer.getId(),
                        Session.getUserId()
                );

        if (!ok) {

            warn(
                    "Restore refused",
                    "This offer is not owned by your agency or no longer exists."
            );

            return;
        }

        if (onChanged != null) {
            onChanged.run();
        }
    }

    @FXML
    private void onDeleteHard() {

        Alert confirm =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirm.setTitle(
                "Permanent Deletion"
        );

        confirm.setHeaderText(null);

        confirm.setContentText(
                "Permanently delete this offer from database?"
        );

        if (
                confirm.showAndWait()
                        .orElse(ButtonType.CANCEL)
                        != ButtonType.OK
        ) {
            return;
        }

        boolean deleted =
                repo.confirmPermanentDelete(
                        offer.getId()
                );

        if (deleted) {

            if (onChanged != null) {
                onChanged.run();
            }

        } else {

            Alert warn =
                    new Alert(
                            Alert.AlertType.WARNING
                    );

            warn.setTitle(
                    "Action Restricted"
            );

            warn.setHeaderText(
                    "Cannot delete this offer"
            );

            warn.setContentText(
                    "This offer is linked to customer history " +
                            "(Reservations / Carts). " +
                            "It will remain archived to preserve records."
            );

            warn.showAndWait();
        }
    }

    private void warn(
            String title,
            String msg
    ) {

        Alert a =
                new Alert(
                        Alert.AlertType.WARNING
                );

        a.setTitle(title);

        a.setHeaderText(null);

        a.setContentText(msg);

        a.showAndWait();
    }
}