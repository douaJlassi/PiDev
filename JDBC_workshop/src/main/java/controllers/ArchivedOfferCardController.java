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

    @FXML private Label titleLbl;
    @FXML private Label priceLbl;
    @FXML private Label datesLbl;

    private Offre offer;
    private Runnable onChanged;

    private final IOffreRepository repo = new OffreRepository();

    public void setData(Offre offer, Runnable onChanged) {
        this.offer = offer;
        this.onChanged = onChanged;

        titleLbl.setText(offer.getTitre());
        priceLbl.setText("Price: " + offer.getPrixPromo() + " TND");
        datesLbl.setText("Dates: " + offer.getDateDebut() + " → " + offer.getDateFin());
    }

    @FXML
    private void onRestore() {
        boolean ok = repo.restoreForAgency(offer.getIdOffre(), Session.getUserId());
        if (!ok) {
            warn("Restore refused", "This offer is not owned by your agency or no longer exists.");
            return;
        }
        if (onChanged != null) onChanged.run();
    }

    @FXML
    private void onDeleteHard() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Permanent Deletion");
        confirm.setContentText("Permanently delete from database?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        // Use our new permanent delete logic
        boolean deleted = repo.confirmPermanentDelete(offer.getIdOffre());

        if (deleted) {
            if (onChanged != null) onChanged.run(); // Refresh list
        } else {
            // Show the user WHY it wasn't deleted
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Action Restricted");
            warn.setHeaderText("Cannot delete this offer");
            warn.setContentText("This offer is tied to customer history (Reservations/Carts). " +
                    "It will remain in your Archive to keep your records accurate.");
            warn.showAndWait();
        }
    }

    private void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}