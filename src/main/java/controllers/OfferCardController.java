package controllers;

import app.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import entities.Offre;
import javafx.stage.Stage;
import repositories.IOffreRepository;
import repositories.OffreRepository;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import repositories.LignePanierRepository;
import repositories.ReservationRepository;
import javafx.scene.control.Button;




public class OfferCardController {

    @FXML private Label titleLbl;
    @FXML private Label priceLbl;
    @FXML private Label datesLbl;
    @FXML private Label agencyLbl;
    @FXML private ImageView imageView;
    @FXML private javafx.scene.control.Button editBtn;
    @FXML private Button addToCartBtn;
    @FXML private Button deleteBtn;
    @FXML private Label promoRibbonLbl;
    @FXML private Label originalPriceLbl;






    private Offre offer;
    private Runnable onChanged;

    private final IOffreRepository repo = new OffreRepository();
    private final ReservationRepository reservationRepo = new ReservationRepository();
    private final LignePanierRepository ligneRepo = new LignePanierRepository();


    public void setData(Offre offer, Runnable onChanged) {
        if (Session.isAdmin()) {
            if (editBtn != null) { editBtn.setVisible(false); editBtn.setManaged(false); }
            if (addToCartBtn != null) { addToCartBtn.setVisible(false); addToCartBtn.setManaged(false); }
        }
        if (Session.isAgency()) {
            if (addToCartBtn != null) { addToCartBtn.setVisible(false); addToCartBtn.setManaged(false); }
        }
        if (Session.isClient()) {
            if (editBtn != null) { editBtn.setVisible(false); editBtn.setManaged(false); }
            if (deleteBtn != null) { deleteBtn.setVisible(false); deleteBtn.setManaged(false); }
        }
        if (Session.isAgency()) {
            if (deleteBtn != null) deleteBtn.setText("Archive");
        }
        this.offer = offer;
        this.onChanged = onChanged;

        titleLbl.setText(offer.getTitre());
        var promo = offer.getPrixPromo();
        var original = offer.getPrixOriginal(); // new field you added

        priceLbl.setText("Price: " + promo + " TND");

// promo condition: original exists and is greater than promo
        //boolean isPromo = (original != null && promo != null && original.compareTo(promo) > 0);

        boolean isPromo = (original != null && promo != null && original.compareTo(promo) > 0);



// --- Ribbon toggle (THIS was missing) ---
        if (promoRibbonLbl != null) {
            promoRibbonLbl.setVisible(isPromo);
            promoRibbonLbl.setManaged(isPromo);

            if (isPromo) {
                java.math.BigDecimal pct = java.math.BigDecimal.ONE
                        .subtract(promo.divide(original, 4, java.math.RoundingMode.HALF_UP))
                        .multiply(new java.math.BigDecimal("100"));

                int pctInt = pct.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
                promoRibbonLbl.setText("-" + pctInt + "%");
            }
        }

        if (originalPriceLbl != null) {
            if (isPromo) {
                originalPriceLbl.setText(original + " TND");
                originalPriceLbl.setOpacity(1.0);

            } else {
                // keep same height/space so cards align
                originalPriceLbl.setText(" ");     // or "--"
                originalPriceLbl.setOpacity(0.0);  // invisible but keeps layout
            }

        }
        datesLbl.setText("Dates: " + offer.getDateDebut() + " → " + offer.getDateFin());
        agencyLbl.setText("Agency: " + (offer.getNomAgence() != null ? offer.getNomAgence() : ("#" + offer.getIdAgence())));
        loadImage(offer.getImageUrl());

    }

    @FXML
    private void onEdit() {
        if (Session.isAdmin()) return; // admin read-only (except delete)
        OffreFormController.openDialog(offer, () -> {
            if (onChanged != null) onChanged.run();
        });
    }

    @FXML
    private void onDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Offer");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this offer?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        boolean ok;
        if (Session.isAdmin()) {
            ok = repo.deleteSafe(offer.getIdOffre()); // admin deletes any offer (still safe vs lignepanier)
        } else {
            ok = repo.archiveForAgency(offer.getIdOffre(), Session.getUserId()); // agency ownership check
        }

        if (!ok) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Cannot delete");
            a.setHeaderText(null);
            a.setContentText("This offer is already used in reservations (lignepanier). Delete refused.");
            a.showAndWait();
            return;
        }

        if (onChanged != null) onChanged.run();
    }

    /*@FXML
    private void onDetails() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Offer Details");
        info.setHeaderText(offer.getTitre());
        info.setContentText(
                "Description:\n" + offer.getDescription() + "\n\n" +
                        "Price: " + offer.getPrixPromo() + " TND\n" +
                        "Dates: " + offer.getDateDebut() + " → " + offer.getDateFin() + "\n" +
                        "Agency: " + (offer.getNomAgence() != null ? offer.getNomAgence() : ("#" + offer.getIdAgence()))
        );
        info.showAndWait();
    }*/
    /*@FXML
    private void onDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OfferDetails.fxml"));
            Parent root = loader.load();

            OfferDetailsController ctrl = loader.getController();
            ctrl.setOffer(offer);


            Stage stage = new Stage();
            stage.setTitle("Offer Details");
            stage.setScene(new javafx.scene.Scene(root, 950, 650));

            stage.show();

        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("UI error");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }*/
    @FXML
    private void onDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OfferDetails.fxml"));
            Parent root = loader.load();

            OfferDetailsController ctrl = loader.getController();
            ctrl.setOffer(offer);

            Stage stage = new Stage();
            stage.setTitle("Offer Details");

            Scene scene = new Scene(root, 950, 650);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("UI error");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }


    private void loadImage(String url) {
        Image img = null;

        // 1) try real url if provided
        if (url != null && !url.isBlank()) {
            try {
                img = new Image(url, true); // supports https://... or file:/...
            } catch (Exception ignored) {}
        }

        // 2) fallback placeholder if url missing or failed
        if (img == null || img.isError()) {
            img = loadPlaceholder();
        }

        // 3) apply (can be null, that's fine)
        imageView.setImage(img);
    }

    private Image loadPlaceholder() {
        try {
            var stream = getClass().getResourceAsStream("/images/placeholder.png");
            if (stream == null) {
                System.out.println("placeholder.png not found at /images/placeholder.png");
                return null; // no crash
            }
            return new Image(stream);
        } catch (Exception e) {
            return null;
        }
    }
    @FXML
    private void onAddToCart() {
        if (!Session.isClient()) return; // safety

        try {
            int cartId = reservationRepo.getOrCreateDraftCart(Session.getUserId());

            boolean added = ligneRepo.addOffer(
                    cartId,
                    offer.getIdOffre(),
                    offer.getPrixPromo()
            );

            reservationRepo.recomputeTotal(cartId);

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Cart");
            a.setHeaderText(null);
            a.setContentText(added ? "Added to cart ✅" : "Already in cart.");
            a.showAndWait();

        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Cart error");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }


}