package controllers;

import entities.Actualite;
import entities.Offre;
import entities.OfferFilter;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import repositories.ActualiteRepository;
import repositories.IActualiteRepository;
import repositories.IOffreRepository;
import repositories.OffreRepository;

import java.io.IOException;
import java.util.List;

public class VoyageurOffersGridController
        implements OfferFilterAware {

    private OfferFilter currentFilter =
            new OfferFilter();

    @FXML
    private TilePane tilePane;

    @FXML
    private ScrollPane offersScroll;

    @FXML
    private VBox actualitesContainer;

    @FXML
    private HBox actualitiesBox;

    @FXML
    private StackPane filtersHost;

    @FXML
    private Button filtersBtn;

    private OfferFiltersPanelController filtersCtrl;

    private boolean filtersVisible = false;

    private final IOffreRepository repo =
            new OffreRepository();

    private final IActualiteRepository actualiteRepo =
            new ActualiteRepository();

    private boolean actualitesVisible = true;

    private boolean animating = false;

    @FXML
    public void initialize() {

        loadFiltersPanel();

        loadActualites();

        refresh();

        setupAutoHideActualitesOnScroll();
    }

    private void setupAutoHideActualitesOnScroll() {

        if (
                offersScroll == null ||
                        actualitesContainer == null
        ) {
            return;
        }

        final double SHOW_AT = 0.01;
        final double HIDE_AT = 0.05;

        offersScroll
                .vvalueProperty()
                .addListener((obs, oldV, newV) -> {

                    double v = newV.doubleValue();

                    if (!actualitesVisible && v <= SHOW_AT) {

                        showActualitesSmooth();

                    } else if (
                            actualitesVisible &&
                                    v >= HIDE_AT
                    ) {

                        hideActualitesSmooth();
                    }
                });
    }

    private void loadFiltersPanel() {

        if (filtersHost == null) {
            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/OfferFiltersPanel.fxml"
                            )
                    );

            Parent panelRoot =
                    loader.load();

            filtersCtrl =
                    loader.getController();

            filtersHost
                    .getChildren()
                    .setAll(panelRoot);

            filtersHost.setVisible(false);
            filtersHost.setManaged(false);

            filtersCtrl.setFilter(currentFilter);

            filtersCtrl.setOnChanged(f -> {

                currentFilter =
                        (f == null)
                                ? new OfferFilter()
                                : f;

                loadActualites();

                refresh();
            });

            filtersCtrl.setOnClose(
                    this::hideFilters
            );

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "UI error",
                    "Cannot load filters panel: "
                            + e.getMessage()
            );
        }
    }

    @FXML
    private void onToggleFilters() {

        filtersVisible = !filtersVisible;

        if (filtersHost != null) {

            filtersHost.setVisible(filtersVisible);

            filtersHost.setManaged(filtersVisible);
        }

        if (filtersBtn != null) {

            filtersBtn.setText(
                    filtersVisible
                            ? "Filters ◂"
                            : "Filters ▾"
            );
        }
    }

    private void hideFilters() {

        filtersVisible = false;

        if (filtersHost != null) {

            filtersHost.setVisible(false);

            filtersHost.setManaged(false);
        }

        if (filtersBtn != null) {

            filtersBtn.setText("Filters ▾");
        }
    }

    private void hideActualitesSmooth() {

        if (
                animating ||
                        !actualitesVisible ||
                        actualitesContainer == null
        ) {
            return;
        }

        animating = true;

        FadeTransition fade =
                new FadeTransition(
                        Duration.millis(100),
                        actualitesContainer
                );

        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        TranslateTransition slide =
                new TranslateTransition(
                        Duration.millis(100),
                        actualitesContainer
                );

        slide.setFromY(0);
        slide.setToY(-12);

        fade.setOnFinished(e -> {

            actualitesContainer.setVisible(false);

            actualitesContainer.setManaged(false);

            actualitesContainer.setOpacity(1.0);

            actualitesContainer.setTranslateY(0);

            actualitesVisible = false;

            animating = false;
        });

        slide.play();
        fade.play();
    }

    private void showActualitesSmooth() {

        if (
                animating ||
                        actualitesVisible ||
                        actualitesContainer == null
        ) {
            return;
        }

        animating = true;

        actualitesContainer.setManaged(true);

        actualitesContainer.setVisible(true);

        actualitesContainer.setOpacity(0.0);

        actualitesContainer.setTranslateY(-12);

        FadeTransition fade =
                new FadeTransition(
                        Duration.millis(180),
                        actualitesContainer
                );

        fade.setFromValue(0.0);

        fade.setToValue(1.0);

        TranslateTransition slide =
                new TranslateTransition(
                        Duration.millis(180),
                        actualitesContainer
                );

        slide.setFromY(-12);

        slide.setToY(0);

        fade.setOnFinished(e -> {

            actualitesVisible = true;

            animating = false;
        });

        slide.play();
        fade.play();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    public void refresh() {

        if (tilePane == null) {
            return;
        }

        tilePane.getChildren().clear();

        List<Offre> offers =
                repo.searchActiveOffers(
                        currentFilter
                );

        for (Offre offer : offers) {

            try {

                FXMLLoader loader =
                        new FXMLLoader(
                                getClass().getResource(
                                        "/fxml/OfferCard.fxml"
                                )
                        );

                Parent card =
                        loader.load();

                OfferCardController ctrl =
                        loader.getController();

                ctrl.setData(
                        offer,
                        this::refresh
                );

                tilePane
                        .getChildren()
                        .add(card);

            } catch (IOException e) {

                showError(
                        "UI error",
                        e.getMessage()
                );
            }
        }
    }

    @Override
    public void applyFilter(
            OfferFilter filter
    ) {

        this.currentFilter =
                (filter == null)
                        ? new OfferFilter()
                        : filter;

        loadActualites();

        refresh();
    }

    private void showError(
            String title,
            String msg
    ) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(msg);

        alert.showAndWait();
    }

    @FXML
    private void onMyCart() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/CartView.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            CartViewController ctrl =
                    loader.getController();

            ctrl.loadCart();

            Stage stage =
                    new Stage();

            stage.setTitle("My Cart");

            Scene scene =
                    new Scene(root, 950, 650);

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

    private void loadActualites() {

        if (actualitiesBox == null) {
            return;
        }

        actualitiesBox
                .getChildren()
                .clear();

        List<Actualite> list =
                actualiteRepo.findAllActive();

        for (Actualite a : list) {

            actualitiesBox
                    .getChildren()
                    .add(
                            createBannerCard(a)
                    );
        }
    }

    private StackPane createBannerCard(
            Actualite a
    ) {

        StackPane card =
                new StackPane();

        card.setPrefSize(420, 160);

        card.setStyle("""
                -fx-background-radius: 14;
                -fx-border-radius: 14;
                -fx-border-color: #E7ECF3;
                -fx-background-color: #F8FAFC;
                -fx-cursor: hand;
                """);

        ImageView iv =
                new ImageView();

        iv.setFitWidth(420);

        iv.setFitHeight(160);

        iv.setPreserveRatio(false);

        iv.setImage(
                new Image(
                        a.getBannerUrl(),
                        true
                )
        );

        card.getChildren().add(iv);

        card.setOnMouseClicked(e -> {

            actualiteRepo.incrementClick(
                    a.getIdActualite()
            );

            openOfferDetails(
                    a.getIdOffre()
            );
        });

        return card;
    }

    private void openOfferDetails(
            int offerId
    ) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/OfferDetails.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            OfferDetailsController ctrl =
                    loader.getController();

            Offre offer =
                    repo.findById(offerId);

            if (offer == null) {

                showError(
                        "Offer not found",
                        "This offer no longer exists."
                );

                return;
            }

            ctrl.setOffer(offer);

            Stage stage =
                    new Stage();

            stage.setTitle("Offer Details");

            Scene scene =
                    new Scene(root, 950, 650);

            scene.getStylesheets().add(
                    getClass()
                            .getResource("/css/app.css")
                            .toExternalForm()
            );

            stage.setScene(scene);

            stage.show();

        } catch (Exception ex) {

            showError(
                    "UI error",
                    ex.getMessage()
            );
        }
    }
}