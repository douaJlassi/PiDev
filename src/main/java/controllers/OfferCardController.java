package controllers;

import app.Session;
import entities.Actualite;
import entities.Offre;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import repositories.*;
import utils.BannerStorage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CountDownLatch;

public class OfferCardController {

    @FXML
    private Label titleLbl;

    @FXML
    private Label priceLbl;

    @FXML
    private Label datesLbl;

    @FXML
    private Label agencyLbl;

    @FXML
    private ImageView imageView;

    @FXML
    private Button editBtn;

    @FXML
    private Button addToCartBtn;

    @FXML
    private Button deleteBtn;

    @FXML
    private Label promoRibbonLbl;

    @FXML
    private Label originalPriceLbl;

    @FXML
    private Button genBannerBtn;

    @FXML
    private StackPane promoContainer;

    private Offre offer;

    private Runnable onChanged;

    private final IOffreRepository repo = new OffreRepository();

    private final ReservationRepository reservationRepo =
            new ReservationRepository();

    private final LignePanierRepository ligneRepo =
            new LignePanierRepository();

    private final IActualiteRepository actualiteRepo =
            new ActualiteRepository();

    public void setData(Offre offer, Runnable onChanged) {

        this.offer = offer;
        this.onChanged = onChanged;

        // ------------------------------------
        // ROLE UI
        // ------------------------------------

        if (Session.isAdmin()) {

            if (editBtn != null) {
                editBtn.setVisible(false);
                editBtn.setManaged(false);
            }

            if (addToCartBtn != null) {
                addToCartBtn.setVisible(false);
                addToCartBtn.setManaged(false);
            }
        }

        if (Session.isAgency()) {

            if (addToCartBtn != null) {
                addToCartBtn.setVisible(false);
                addToCartBtn.setManaged(false);
            }
        }

        if (Session.isClient()) {

            if (editBtn != null) {
                editBtn.setVisible(false);
                editBtn.setManaged(false);
            }

            if (deleteBtn != null) {
                deleteBtn.setVisible(false);
                deleteBtn.setManaged(false);
            }
        }

        if (Session.isAgency()) {
            if (deleteBtn != null) {
                deleteBtn.setText("Archive");
            }
        }

        // ------------------------------------
        // DATA
        // ------------------------------------

        titleLbl.setText(offer.getTitle());

        BigDecimal promo = offer.getPromoPrice();
        BigDecimal original = offer.getOriginalPrice();

        priceLbl.setText("Price: " + promo + " TND");

        boolean isPromo =
                original != null &&
                        promo != null &&
                        original.compareTo(promo) > 0;

        // ------------------------------------
        // PROMO RIBBON
        // ------------------------------------

        if (promoRibbonLbl != null) {

            promoRibbonLbl.setVisible(isPromo);
            promoRibbonLbl.setManaged(isPromo);

            if (isPromo) {

                BigDecimal pct =
                        BigDecimal.ONE
                                .subtract(
                                        promo.divide(
                                                original,
                                                4,
                                                RoundingMode.HALF_UP
                                        )
                                )
                                .multiply(new BigDecimal("100"));

                int pctInt =
                        pct.setScale(0, RoundingMode.HALF_UP)
                                .intValue();

                promoRibbonLbl.setText("-" + pctInt + "%");
            }
        }

        // ------------------------------------
        // ORIGINAL PRICE
        // ------------------------------------

        if (originalPriceLbl != null) {

            if (isPromo) {

                originalPriceLbl.setText(original + " TND");
                originalPriceLbl.setOpacity(1.0);

            } else {

                originalPriceLbl.setText("");
                originalPriceLbl.setOpacity(0.0);
            }
        }

        // ------------------------------------
        // DATES
        // ------------------------------------

        datesLbl.setText(
                "Dates: " +
                        offer.getStartDate() +
                        " → " +
                        offer.getEndDate()
        );

        // ------------------------------------
        // LOCATION
        // ------------------------------------

        if (agencyLbl != null) {

            String location =
                    offer.getLocation() != null
                            ? offer.getLocation()
                            : "Unknown";

            agencyLbl.setText("Location: " + location);
        }

        // ------------------------------------
        // IMAGE
        // ------------------------------------

        loadImage(offer.getImageUrl());

        // ------------------------------------
        // BANNER BTN
        // ------------------------------------

        if (genBannerBtn != null) {

            boolean canGenerate = Session.isAgency();

            genBannerBtn.setVisible(canGenerate);
            genBannerBtn.setManaged(canGenerate);
        }
    }

    @FXML
    private void onEdit() {

        if (Session.isAdmin()) {
            return;
        }

        OffreFormController.openDialog(
                offer,
                () -> {
                    if (onChanged != null) {
                        onChanged.run();
                    }
                }
        );
    }

    @FXML
    private void onDelete() {

        Alert confirm =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirm.setTitle("Delete Offer");

        confirm.setHeaderText(
                "Remove " + offer.getTitle() + " ?"
        );

        confirm.setContentText(
                "This will remove the offer from the marketplace."
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        boolean ok = repo.deleteSafe(offer.getId());

        if (ok) {

            if (onChanged != null) {
                onChanged.run();
            }

        } else {

            show(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Could not process the request."
            );
        }
    }

    private void show(
            Alert.AlertType type,
            String title,
            String content
    ) {

        Alert a = new Alert(type);

        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);

        a.showAndWait();
    }

    @FXML
    private void onDetails() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/OfferDetails.fxml"
                            )
                    );

            Parent root = loader.load();

            OfferDetailsController ctrl =
                    loader.getController();

            ctrl.setOffer(offer);

            Stage stage = new Stage();

            stage.setTitle("Offer Details");

            Scene scene = new Scene(root, 950, 650);

            scene.getStylesheets().add(
                    getClass()
                            .getResource("/css/app.css")
                            .toExternalForm()
            );

            stage.setScene(scene);

            stage.show();

        } catch (Exception e) {

            Alert a =
                    new Alert(Alert.AlertType.ERROR);

            a.setTitle("UI error");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());

            a.showAndWait();
        }
    }

    private void loadImage(String url) {

        Image img = null;

        if (url != null && !url.isBlank()) {

            try {

                img = new Image(url, true);

            } catch (Exception ignored) {
            }
        }

        if (img == null || img.isError()) {
            img = loadPlaceholder();
        }

        imageView.setImage(img);
    }

    private Image loadPlaceholder() {

        try {

            var stream =
                    getClass()
                            .getResourceAsStream(
                                    "/images/placeholder.png"
                            );

            if (stream == null) {
                return null;
            }

            return new Image(stream);

        } catch (Exception e) {

            return null;
        }
    }

    @FXML
    private void onAddToCart() {

        if (!Session.isClient()) {
            return;
        }

        if (offer == null) {
            show(Alert.AlertType.ERROR, "Reservation error", "Offer data is missing.");
            return;
        }

        try {
            int reservationId = reservationRepo.createReservationForOffer(
                    Session.getUserId(),
                    offer.getId(),
                    offer.getPromoPrice()
            );

            if (reservationId <= 0) {
                show(Alert.AlertType.ERROR, "Reservation error", "Could not create reservation.");
                return;
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Reservation");
            a.setHeaderText(null);
            a.setContentText("Reservation request sent ✅\nReservation #" + reservationId);
            a.showAndWait();

            if (onChanged != null) {
                onChanged.run();
            }

        } catch (Exception e) {
            show(Alert.AlertType.ERROR, "Reservation error", e.getMessage());
        }
    }

    @FXML
    private void onGenerateBanner() {

        if (!Session.isAgency()) {
            return;
        }

        Task<String> task = new Task<>() {

            @Override
            protected String call() throws Exception {

                final Image[] imgHolder =
                        new Image[1];

                CountDownLatch latch =
                        new CountDownLatch(1);

                Platform.runLater(() -> {

                    try {

                        imgHolder[0] =
                                services.MockAIService
                                        .generateBanner(offer);

                    } finally {

                        latch.countDown();
                    }
                });

                latch.await();

                if (imgHolder[0] == null) {
                    return null;
                }

                String bannerUrl =
                        BannerStorage.saveOfferBannerToFile(
                                imgHolder[0],
                                offer.getId()
                        );

                if (bannerUrl == null) {
                    return null;
                }

                Actualite a = new Actualite();

                a.setEndsAt(
                        java.time.LocalDateTime.now()
                                .plusDays(7)
                );

                a.setIdOffre(offer.getId());

                a.setIdAgence(Session.getUserId());

                a.setBannerUrl(bannerUrl);

                a.setTitre(
                        "New collection: "
                                + offer.getTitle()
                );

                a.setActive(true);

                boolean ok =
                        actualiteRepo.create(a);

                return ok ? bannerUrl : null;
            }
        };

        task.setOnSucceeded(ev -> {

            String bannerUrl = task.getValue();

            if (bannerUrl == null) {

                showSimple(
                        "Banner",
                        "Generation or DB save failed."
                );

                return;
            }

            showSimple(
                    "Banner",
                    "Banner published ✅"
            );

            if (onChanged != null) {
                onChanged.run();
            }
        });

        task.setOnFailed(ev -> {

            Throwable ex = task.getException();

            showSimple(
                    "Banner",
                    "Error: " +
                            (
                                    ex != null
                                            ? ex.getMessage()
                                            : "unknown"
                            )
            );

            if (ex != null) {
                ex.printStackTrace();
            }
        });

        new Thread(task).start();
    }

    private void showSimple(String title, String msg) {

        Alert a =
                new Alert(Alert.AlertType.INFORMATION);

        a.setTitle(title);

        a.setHeaderText(null);

        a.setContentText(msg);

        a.showAndWait();
    }

    public void updatePrice(
            String newPrice,
            String oldPrice
    ) {

        priceLbl.setText(newPrice + " TND");

        if (
                oldPrice == null ||
                        oldPrice.isEmpty() ||
                        oldPrice.equals("0.00")
        ) {

            promoContainer.setVisible(false);
            promoContainer.setManaged(false);

        } else {

            originalPriceLbl.setText(
                    oldPrice + " TND"
            );

            promoContainer.setVisible(true);
            promoContainer.setManaged(true);
        }
    }
}