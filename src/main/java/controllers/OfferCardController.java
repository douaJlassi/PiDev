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
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import repositories.IOffreRepository;
import repositories.OffreRepository;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import repositories.LignePanierRepository;
import repositories.ReservationRepository;
import javafx.scene.control.Button;
import repositories.ActualiteRepository;
import repositories.IActualiteRepository;
import entities.Actualite;
import javafx.concurrent.Task;
import utils.BannerStorage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;


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
    @FXML private Button genBannerBtn;
    @FXML private StackPane promoContainer;






    private Offre offer;
    private Runnable onChanged;

    private final IOffreRepository repo = new OffreRepository();
    private final ReservationRepository reservationRepo = new ReservationRepository();
    private final LignePanierRepository ligneRepo = new LignePanierRepository();
    private final IActualiteRepository actualiteRepo = new ActualiteRepository();


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
                originalPriceLbl.setText("");     // or "--"
                originalPriceLbl.setOpacity(0.0);  // invisible but keeps layout
            }

        }
        datesLbl.setText("Dates: " + offer.getDateDebut() + " → " + offer.getDateFin());
        agencyLbl.setText("Agency: " + (offer.getNomAgence() != null ? offer.getNomAgence() : ("#" + offer.getIdAgence())));
        loadImage(offer.getImageUrl());
        if (genBannerBtn != null) {
            boolean canGenerate = Session.isAgency();
            genBannerBtn.setVisible(canGenerate);
            genBannerBtn.setManaged(canGenerate);
        }

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
        confirm.setHeaderText("Remove " + offer.getTitre() + "?");
        confirm.setContentText("This will remove the offer from the marketplace.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        // Use a single method that handles the logic internally
        // It should DELETE if unused, or ARCHIVE if used.
        boolean ok = repo.deleteSafe(offer.getIdOffre());

        if (ok) {
            // Success! The offer is gone from the "Active" list.
            if (onChanged != null) onChanged.run();
        } else {
            // This should only happen if there's a database connection error now
            show(Alert.AlertType.ERROR, "Error", "Could not process the request. Please try again.");
        }
    }

    // Helper for quick alerts
    private void show(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
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
    @FXML
    private void onGenerateBanner() {
        if (!Session.isAgency()) return;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {

                // 1) Generate banner IMAGE on JavaFX thread (Canvas.snapshot needs FX thread)
                final javafx.scene.image.Image[] imgHolder = new javafx.scene.image.Image[1];
                CountDownLatch latch = new CountDownLatch(1);

                Platform.runLater(() -> {
                    try {
                        imgHolder[0] = services.MockAIService.generateBanner(offer);
                    } finally {
                        latch.countDown();
                    }
                });

                latch.await(); // wait until FX thread finishes generating image

                if (imgHolder[0] == null) return null;

                // 2) Save to file (can be background thread)
                String bannerUrl = utils.BannerStorage.saveOfferBannerToFile(imgHolder[0], offer.getIdOffre());
                if (bannerUrl == null) return null;

                // 3) Insert into DB (background thread)
                entities.Actualite a = new entities.Actualite();
                a.setEndsAt(java.time.LocalDateTime.now().plusDays(7));
                a.setIdOffre(offer.getIdOffre());
                a.setIdAgence(Session.getUserId());
                a.setBannerUrl(bannerUrl);
                a.setTitre("New collection: " + offer.getTitre());
                a.setActive(true);

                boolean ok = actualiteRepo.create(a);
                return ok ? bannerUrl : null;
            }
        };

        task.setOnSucceeded(ev -> {
            String bannerUrl = task.getValue();
            if (bannerUrl == null) {
                showSimple("Banner", "Generation or DB save failed.");
                return;
            }
            showSimple("Banner", "Banner published ✅");
            if (onChanged != null) onChanged.run();
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            showSimple("Banner", "Error: " + (ex != null ? ex.getMessage() : "unknown"));
            if (ex != null) ex.printStackTrace();
        });

        new Thread(task).start();
    }
    private void showSimple(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    public void updatePrice(String newPrice, String oldPrice) {
        priceLbl.setText(newPrice + " TND");

        if (oldPrice == null || oldPrice.isEmpty() || oldPrice.equals("0.00")) {
            // This makes the strike line and label disappear AND stop taking up space
            promoContainer.setVisible(false);
            promoContainer.setManaged(false);
        } else {
            originalPriceLbl.setText(oldPrice + " TND");
            promoContainer.setVisible(true);
            promoContainer.setManaged(true);
        }
    }

}