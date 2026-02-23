package controllers;

import app.Session;
import entities.Offre;
import entities.ServiceDetails;
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

import java.util.List;

public class OfferDetailsController {

    @FXML private Label titleLbl;
    @FXML private Label kindLbl;
    @FXML private Label agencyLbl;
    @FXML private Label datesLbl;
    @FXML private Label priceLbl;
    @FXML private Label descLbl;
    @FXML private ImageView offerImg;
    @FXML private VBox servicesBox;
    @FXML private Button addToCartBtn;


    private Offre offer;

    private final OffreDetailsRepository detailsRepo = new OffreDetailsRepository();
    private final services.ExchangeRateService fx = new services.ExchangeRateService();
    private final repositories.ReservationRepository reservationRepo = new repositories.ReservationRepository();
    private final repositories.LignePanierRepository ligneRepo = new repositories.LignePanierRepository();

    // Add these to your FXML fields at the top
    @FXML private Label euroPriceLbl;
    @FXML private Label usdPriceLbl;

    public void setOffer(Offre offer) {
        this.offer = offer;

        // Basic Info
        titleLbl.setText(offer.getTitre());
        kindLbl.setText("OFFRE");
        agencyLbl.setText(offer.getNomAgence() != null ? offer.getNomAgence() : ("Agency #" + offer.getIdAgence()));
        datesLbl.setText(offer.getDateDebut() + " → " + offer.getDateFin());

        // Set Base TND Price
        priceLbl.setText(String.format("%.3f TND", offer.getPrixPromo()));

        // Handle Exchange Rates
        try {
            var eur = fx.convert(offer.getPrixPromo(), "TND", "EUR").setScale(2, java.math.RoundingMode.HALF_UP);
            var usd = fx.convert(offer.getPrixPromo(), "TND", "USD").setScale(2, java.math.RoundingMode.HALF_UP);

            euroPriceLbl.setText("€" + eur);
            usdPriceLbl.setText("$" + usd);
            System.out.println("FX EUR=" + eur + " USD=" + usd);
        } catch (Exception e) {
            System.out.println("FX API failed: " + e.getMessage());
            // Fallback: Show a generic estimate or keep it hidden
            euroPriceLbl.setText("--- €");
            usdPriceLbl.setText("--- $");
        }

        descLbl.setText(offer.getDescription() == null ? "No description available." : offer.getDescription());

        // Only show AddToCart to CLIENT
        boolean client = Session.isClient();
        addToCartBtn.setVisible(client);
        addToCartBtn.setManaged(client);

        loadServices();
    }

    private void loadServices() {
        servicesBox.getChildren().clear();
        servicesBox.setFillWidth(true);

        List<ServiceDetails> services = detailsRepo.findServicesDetailsByOffre(offer.getIdOffre());
        if (services.isEmpty()) {
            Label empty = new Label("No services attached to this offer yet.");
            servicesBox.getChildren().add(empty);
            return;
        }

        for (ServiceDetails s : services) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ServiceCard.fxml"));
                Parent node = loader.load();

                ServiceCardController ctrl = loader.getController();
                ctrl.setData(s);

                servicesBox.getChildren().add(node);
            } catch (Exception e) {
                Label err = new Label("Failed to load a service card: " + e.getMessage());
                servicesBox.getChildren().add(err);
            }
        }
    }

    private void loadImage(String url) {
        Image img = null;

        if (url != null && !url.isBlank()) {
            try { img = new Image(url, true); } catch (Exception ignored) {}
        }

        if (img == null || img.isError()) {
            try {
                var stream = getClass().getResourceAsStream("/images/placeholder.png");
                if (stream != null) img = new Image(stream);
            } catch (Exception ignored) {}
        }

        //offerImg.setImage(img);
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) titleLbl.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onAddToCart() {
        if (!Session.isClient()) return;

        try {
            int cartId = reservationRepo.getOrCreateDraftCart(Session.getUserId());

            boolean added = ligneRepo.addOffer(
                    cartId,
                    offer.getIdOffre(),
                    offer.getPrixPromo() // final price
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
