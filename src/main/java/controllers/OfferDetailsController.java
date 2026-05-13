package controllers;

import app.Session;
import entities.Offre;
import entities.ServiceEntityDetails;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import repositories.OffreDetailsRepository;
import repositories.ReservationRepository;

import java.math.RoundingMode;
import java.util.List;

public class OfferDetailsController {

    @FXML
    private Label titleLbl;

    @FXML
    private Label kindLbl;

    @FXML
    private Label agencyLbl;

    @FXML
    private Label datesLbl;

    @FXML
    private Label priceLbl;

    @FXML
    private Label descLbl;

    @FXML
    private ImageView offerImg;

    @FXML
    private VBox servicesBox;

    @FXML
    private Button addToCartBtn;

    @FXML
    private Label euroPriceLbl;

    @FXML
    private Label usdPriceLbl;

    private Offre offer;

    private final OffreDetailsRepository detailsRepo =
            new OffreDetailsRepository();

    private final services.ExchangeRateService fx =
            new services.ExchangeRateService();

    private final ReservationRepository reservationRepo =
            new ReservationRepository();

    public void setOffer(Offre offer) {

        this.offer = offer;

        if (offer == null) {
            showError("Offer error", "Offer data is missing.");
            return;
        }

        titleLbl.setText(offer.getTitle());

        kindLbl.setText("OFFRE");

        String location =
                offer.getLocation() != null && !offer.getLocation().isBlank()
                        ? offer.getLocation()
                        : "Unknown";

        agencyLbl.setText("Location: " + location);

        datesLbl.setText(
                offer.getStartDate()
                        + " → "
                        + offer.getEndDate()
        );

        if (offer.getPromoPrice() != null) {

            priceLbl.setText(
                    String.format(
                            "%.3f TND",
                            offer.getPromoPrice()
                    )
            );

        } else {

            priceLbl.setText("---");
        }

        try {

            if (offer.getPromoPrice() != null) {

                var eur =
                        fx.convert(
                                        offer.getPromoPrice(),
                                        "TND",
                                        "EUR"
                                )
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                );

                var usd =
                        fx.convert(
                                        offer.getPromoPrice(),
                                        "TND",
                                        "USD"
                                )
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                );

                euroPriceLbl.setText("€" + eur);
                usdPriceLbl.setText("$" + usd);

            } else {

                euroPriceLbl.setText("--- €");
                usdPriceLbl.setText("--- $");
            }

        } catch (Exception e) {

            System.out.println("FX API failed: " + e.getMessage());

            euroPriceLbl.setText("--- €");
            usdPriceLbl.setText("--- $");
        }

        descLbl.setText(
                offer.getDescription() == null || offer.getDescription().isBlank()
                        ? "No description available."
                        : offer.getDescription()
        );

        loadImage(offer.getImageUrl());

        boolean client = Session.isClient();

        if (addToCartBtn != null) {
            addToCartBtn.setVisible(client);
            addToCartBtn.setManaged(client);
            addToCartBtn.setText("Reserve now");
        }

        loadServices();
    }

    private void loadServices() {

        if (servicesBox == null) {
            return;
        }

        servicesBox.getChildren().clear();
        servicesBox.setFillWidth(true);

        List<ServiceEntityDetails> services =
                detailsRepo.findServicesDetailsByOffre(
                        offer.getId()
                );

        if (services.isEmpty()) {

            Label empty =
                    new Label(
                            "No services attached to this offer yet."
                    );

            servicesBox.getChildren().add(empty);

            return;
        }

        for (ServiceEntityDetails s : services) {

            try {

                FXMLLoader loader =
                        new FXMLLoader(
                                getClass().getResource(
                                        "/fxml/OffreServiceCard.fxml"
                                )
                        );

                Parent node = loader.load();

                ServiceCardController ctrl =
                        loader.getController();

                ctrl.setData(s);

                servicesBox.getChildren().add(node);

            } catch (Exception e) {

                Label err =
                        new Label(
                                "Failed to load a service card: "
                                        + e.getMessage()
                        );

                servicesBox.getChildren().add(err);
            }
        }
    }

    private void loadImage(String url) {

        if (offerImg == null) {
            return;
        }

        Image img = null;

        if (url != null && !url.isBlank()) {

            try {

                img = new Image(url, true);

            } catch (Exception ignored) {
            }
        }

        if (img == null || img.isError()) {

            try {

                var stream =
                        getClass().getResourceAsStream(
                                "/images/placeholder.png"
                        );

                if (stream != null) {
                    img = new Image(stream);
                }

            } catch (Exception ignored) {
            }
        }

        offerImg.setImage(img);
    }

    @FXML
    private void onBack() {

        Stage stage =
                (Stage) titleLbl
                        .getScene()
                        .getWindow();

        stage.close();
    }

    @FXML
    private void onAddToCart() {

        if (!Session.isClient()) {
            return;
        }

        if (offer == null) {
            showError("Reservation error", "Offer data is missing.");
            return;
        }

        try {

            int reservationId =
                    reservationRepo.createReservationForOffer(
                            Session.getUserId(),
                            offer.getId(),
                            offer.getPromoPrice()
                    );

            if (reservationId <= 0) {
                showError(
                        "Reservation error",
                        "Could not create the reservation."
                );
                return;
            }

            Alert a =
                    new Alert(Alert.AlertType.INFORMATION);

            a.setTitle("Reservation");
            a.setHeaderText(null);
            a.setContentText(
                    "Reservation request sent ✅\nReservation #" + reservationId
            );

            a.showAndWait();

        } catch (Exception e) {

            showError(
                    "Reservation error",
                    e.getMessage()
            );
        }
    }

    private void showError(String title, String msg) {

        Alert a =
                new Alert(Alert.AlertType.ERROR);

        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}