package controllers;

import entities.CartItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CartItemCardController {

    @FXML
    private Label titleLbl;

    @FXML
    private Label priceLbl;

    @FXML
    private ImageView img;

    private CartItem item;
    private Runnable onChanged;

    public void setData(CartItem item, Runnable onChanged) {
        this.item = item;
        this.onChanged = onChanged;

        if (item == null) {
            titleLbl.setText("No item");
            priceLbl.setText("-");
            return;
        }

        titleLbl.setText(item.getTitre());

        priceLbl.setText(
                item.getPrixUnitaire() == null
                        ? "- TND"
                        : item.getPrixUnitaire() + " TND"
        );

        loadImage(item.getImageUrl());
    }

    @FXML
    private void onRemove() {
        // No cart anymore.
        // We keep this method only to avoid FXML errors.
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void loadImage(String url) {
        if (img == null) {
            return;
        }

        Image im = null;

        if (url != null && !url.isBlank()) {
            try {
                im = new Image(url, true);
            } catch (Exception ignored) {
            }
        }

        if (im == null || im.isError()) {
            try {
                var stream = getClass().getResourceAsStream("/images/placeholder.png");
                if (stream != null) {
                    im = new Image(stream);
                }
            } catch (Exception ignored) {
            }
        }

        img.setImage(im);
    }
}