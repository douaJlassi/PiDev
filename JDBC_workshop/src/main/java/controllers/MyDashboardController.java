package controllers;

import app.Session;
import entities.SearchCriteria;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.AISearchService;

public class MyDashboardController {

    @FXML private StackPane contentHost;
    @FXML private Label roleLbl;
    @FXML private Label userLbl;
    @FXML private TextField searchTf;
    @FXML private ImageView logoImg;
    @FXML private Button offersBtn;
    @FXML private Button postsBtn;
    @FXML private Button cartBtn;
    @FXML private Button reservationsBtn;
    @FXML private Button archiveBtn;
    @FXML private DatePicker calendarDp;
    @FXML private TextField minPriceTf;
    @FXML private TextField maxPriceTf;
    @FXML private VBox rightPanel;
    @FXML private Button filtersBtn;
    @FXML private Button bannersBtn;
    @FXML private Button agencyResBtn;
    @FXML private VBox aiAgentBox;
    @FXML private TextArea aiSearchArea;
    @FXML private Button aiSearchBtn;
    @FXML private Button analyticsBtn;

    private final entities.OfferFilter offerFilter = new entities.OfferFilter();

    private Object currentController;
    private boolean rightPanelVisible = true;

    @FXML
    public void initialize() {
        initAISection();

        try {
            var stream = getClass().getResourceAsStream("/images/logo.png");
            if (stream != null) {
                logoImg.setImage(new Image(stream));
            }
        } catch (Exception ignored) {
        }

        roleLbl.setText("Role: " + Session.getRole());
        userLbl.setText("User #" + Session.getUserId());

        boolean isAgency = Session.isAgency();
        boolean isClient = Session.isClient();

        setNavVisible(offersBtn, true);
        setNavVisible(postsBtn, true);
        setNavVisible(cartBtn, isClient);
        setNavVisible(reservationsBtn, isClient);
        setNavVisible(archiveBtn, isAgency);
        setNavVisible(bannersBtn, isAgency);
        setNavVisible(agencyResBtn, isAgency);
        setNavVisible(analyticsBtn, isAgency);

        setActive(offersBtn);

        if (searchTf != null) {
            searchTf.textProperty().addListener((o, oldV, newV) -> {
                offerFilter.setKeyword(newV);
                pushFilterToCurrentView();
            });
        }

        if (calendarDp != null) {
            calendarDp.valueProperty().addListener((o, oldV, newV) -> {
                offerFilter.setSelectedDate(newV);
                pushFilterToCurrentView();
            });
        }

        if (minPriceTf != null) {
            minPriceTf.textProperty().addListener((o, oldV, newV) -> {
                offerFilter.setMinPrice(parseBigDecimalOrNull(newV));
                pushFilterToCurrentView();
            });
        }

        if (maxPriceTf != null) {
            maxPriceTf.textProperty().addListener((o, oldV, newV) -> {
                offerFilter.setMaxPrice(parseBigDecimalOrNull(newV));
                pushFilterToCurrentView();
            });
        }

        loadOffersView();
    }

    private void initAISection() {
        boolean isClient = Session.isClient();

        if (aiAgentBox != null) {
            aiAgentBox.setVisible(isClient);
            aiAgentBox.setManaged(isClient);
        }
    }

    @FXML
    private void onAISmartSearch() {
        String query = aiSearchArea.getText();

        if (query == null || query.isBlank()) {
            return;
        }

        aiSearchBtn.setDisable(true);
        aiSearchBtn.setText("Gemini is thinking...");

        Task<SearchCriteria> task = new Task<>() {
            @Override
            protected SearchCriteria call() throws Exception {
                return AISearchService.parseDeepQuery(query);
            }
        };

        task.setOnSucceeded(e -> {
            SearchCriteria result = task.getValue();
            applyAICriteria(result);

            aiSearchBtn.setDisable(false);
            aiSearchBtn.setText("Search with Gemini");
        });

        task.setOnFailed(e -> {
            aiSearchBtn.setDisable(false);
            aiSearchBtn.setText("Search with Gemini");

            if (task.getException() != null) {
                System.err.println("AI Search Failed: " + task.getException().getMessage());
            }
        });

        new Thread(task).start();
    }

    private void applyAICriteria(SearchCriteria sc) {
        if (sc == null) {
            return;
        }

        onClearFilters();

        if (sc.destination != null) {
            offerFilter.setKeyword(sc.destination);

            if (searchTf != null) {
                searchTf.setText(sc.destination);
            }
        }

        if (sc.maxPrice != null) {
            java.math.BigDecimal price =
                    java.math.BigDecimal.valueOf(sc.maxPrice);

            offerFilter.setMaxPrice(price);

            if (maxPriceTf != null) {
                maxPriceTf.setText(price.toString());
            }
        }

        pushFilterToCurrentView();
    }

    @FXML
    private void onGoAnalytics() {
        if (!Session.isAgency()) {
            return;
        }

        setActive(analyticsBtn);
        loadIntoContent("/fxml/AgencyAnalytics.fxml");
    }

    @FXML
    private void onGoAgencyReservations() {
        if (!Session.isAgency()) {
            return;
        }

        setActive(agencyResBtn);
        loadIntoContent("/fxml/AgencyReservations.fxml");
    }

    @FXML
    private void onGoBanners() {
        if (!Session.isAgency()) {
            return;
        }

        setActive(bannersBtn);
        loadIntoContent("/fxml/AgencyActualites.fxml");
    }

    @FXML
    private void onToggleRightPanel() {
        rightPanelVisible = !rightPanelVisible;

        if (rightPanel != null) {
            rightPanel.setVisible(rightPanelVisible);
            rightPanel.setManaged(rightPanelVisible);
        }

        if (filtersBtn != null) {
            filtersBtn.setText(rightPanelVisible ? "Filters ◂" : "Filters ▸");
        }
    }

    @FXML
    private void onGoArchive() {
        if (!Session.isAgency()) {
            return;
        }

        setActive(archiveBtn);
        loadIntoContent("/fxml/ArchivedOffersGrid.fxml");
    }

    @FXML
    private void onGoOffers() {
        setActive(offersBtn);
        loadOffersView();
    }

    @FXML
    private void onGoPosts() {
        setActive(postsBtn);
        loadIntoContent("/views/wajdi_dashboard.fxml");
    }

    @FXML
    private void onGoCart() {
        if (!Session.isClient()) {
            return;
        }

        setActive(cartBtn);
        loadIntoContent("/fxml/CartView.fxml");
    }

    @FXML
    private void onGoReservations() {
        if (!Session.isClient()) {
            return;
        }

        setActive(reservationsBtn);
        loadIntoContent("/fxml/MyReservations.fxml");
    }

    @FXML
    private void onClearFilters() {
        offerFilter.setKeyword(null);
        offerFilter.setMinPrice(null);
        offerFilter.setMaxPrice(null);
        offerFilter.setSelectedDate(null);

        offerFilter.getStatuses().clear();
        offerFilter.getLocations().clear();

        if (searchTf != null) {
            searchTf.clear();
        }

        if (minPriceTf != null) {
            minPriceTf.clear();
        }

        if (maxPriceTf != null) {
            maxPriceTf.clear();
        }

        if (calendarDp != null) {
            calendarDp.setValue(null);
        }

        pushFilterToCurrentView();
    }

    private void loadOffersView() {
        String fxml;

        if (Session.isAdmin()) {
            fxml = "/fxml/AdminOffersGrid.fxml";
        } else if (Session.isAgency()) {
            fxml = "/fxml/OffersGrid.fxml";
        } else {
            fxml = "/fxml/VoyageurOffersGrid.fxml";
        }

        loadIntoContent(fxml);
    }

    private void loadIntoContent(String fxmlPath) {
        try {
            boolean isOfferPage =
                    fxmlPath.contains("OffersGrid") ||
                            fxmlPath.contains("VoyageurOffersGrid") ||
                            fxmlPath.contains("AdminOffersGrid") ||
                            fxmlPath.contains("ArchivedOffersGrid");

            setRightPanelVisible(isOfferPage);

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(fxmlPath));

            Parent view = loader.load();

            contentHost.getChildren().setAll(view);

            currentController = loader.getController();

            pushFilterToCurrentView();

            if (currentController instanceof CartViewController c) {
                c.loadCart();
            }

            if (currentController instanceof MyReservationsController r) {
                r.setOnBack(this::loadOffersView);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setRightPanelVisible(boolean visible) {
        rightPanelVisible = visible;

        if (rightPanel != null) {
            rightPanel.setVisible(visible);
            rightPanel.setManaged(visible);
        }

        if (filtersBtn != null) {
            filtersBtn.setText(visible ? "Filters ◂" : "Filters ▸");
        }
    }

    private void pushFilterToCurrentView() {
        if (currentController instanceof OfferFilterAware aware) {
            aware.applyFilter(offerFilter);
        }
    }

    private void setActive(Button activeBtn) {
        if (offersBtn != null) offersBtn.getStyleClass().remove("active");
        if (postsBtn != null) postsBtn.getStyleClass().remove("active");
        if (cartBtn != null) cartBtn.getStyleClass().remove("active");
        if (reservationsBtn != null) reservationsBtn.getStyleClass().remove("active");
        if (archiveBtn != null) archiveBtn.getStyleClass().remove("active");
        if (bannersBtn != null) bannersBtn.getStyleClass().remove("active");
        if (agencyResBtn != null) agencyResBtn.getStyleClass().remove("active");
        if (analyticsBtn != null) analyticsBtn.getStyleClass().remove("active");

        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }
    }

    private void setNavVisible(Button btn, boolean visible) {
        if (btn == null) {
            return;
        }

        btn.setVisible(visible);
        btn.setManaged(visible);
    }

    private java.math.BigDecimal parseBigDecimalOrNull(String s) {
        if (s == null) {
            return null;
        }

        s = s.trim();

        if (s.isEmpty()) {
            return null;
        }

        try {
            return new java.math.BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }
}