package Controllers;

import entities.CartItem;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import repositories.LignePanierRepository;
import repositories.ReservationRepository;

public class CartItemCardController {

    @FXML private Label titleLbl;
    @FXML private Label priceLbl;
    @FXML private ImageView img;

    private CartItem item;
    private Runnable onChanged;

    private final LignePanierRepository ligneRepo = new LignePanierRepository();
    private final ReservationRepository reservationRepo = new ReservationRepository();

    public void setData(CartItem item, Runnable onChanged) {
        this.item = item;
        this.onChanged = onChanged;

        titleLbl.setText(item.getTitre());
        priceLbl.setText(item.getPrixUnitaire() + " TND");

        loadImage(item.getImageUrl());
    }

    @FXML
    private void onRemove() {
        try {
            boolean ok = ligneRepo.removeOffer(item.getIdReservation(), item.getIdOffre());
            reservationRepo.recomputeTotal(item.getIdReservation());

            if (ok && onChanged != null) onChanged.run();
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Remove error");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    private void loadImage(String url) {
        Image im = null;

        if (url != null && !url.isBlank()) {
            try { im = new Image(url, true); } catch (Exception ignored) {}
        }
        if (im == null || im.isError()) {
            try {
                var stream = getClass().getResourceAsStream("/images/placeholder.png");
                if (stream != null) im = new Image(stream);
            } catch (Exception ignored) {}
        }
        img.setImage(im);
    }
}
