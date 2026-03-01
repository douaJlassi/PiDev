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
import entities.Agency;
import javafx.scene.layout.VBox;
import services.AISearchService;

public class MyDashboardController {

    @FXML private StackPane contentHost;
    @FXML private Label roleLbl;
    @FXML private Label userLbl;
    @FXML private TextField searchTf;
    @FXML private ImageView logoImg;
    @FXML private Button offersBtn;
    @FXML private Button cartBtn;
    @FXML private Button reservationsBtn;
    @FXML private Button archiveBtn;
    @FXML private javafx.scene.control.DatePicker calendarDp;
    @FXML private javafx.scene.control.TextField minPriceTf;
    @FXML private javafx.scene.control.TextField maxPriceTf;
    @FXML private ListView<Agency> agenciesLv;
    @FXML private javafx.scene.layout.VBox rightPanel;
    @FXML private Button filtersBtn;
    @FXML private Button bannersBtn;
    @FXML private Button agencyResBtn;
    @FXML private VBox aiAgentBox;
    @FXML private TextArea aiSearchArea;
    @FXML private Button aiSearchBtn;

    private final repositories.AgencyRepository agencyRepo = new repositories.AgencyRepository();
    private final entities.OfferFilter offerFilter = new entities.OfferFilter();
    private Object currentController;
    private boolean rightPanelVisible = true;

    @FXML
    public void initialize() {
        initAISection();
        if (Session.isAgency()) {
            offerFilter.getAgencyIds().clear();
            offerFilter.getAgencyIds().add(Session.getUserId()); // agency idUser = idAgence
            if (aiAgentBox != null) {
                boolean isClient = Session.isClient();
                aiAgentBox.setVisible(isClient);
                aiAgentBox.setManaged(isClient);
            }
        }
        // logo
        try {
            var stream = getClass().getResourceAsStream("/images/logo.png"); // put your logo here
            if (stream != null) logoImg.setImage(new Image(stream));
        } catch (Exception ignored) {}
        setNavVisible(agencyResBtn, Session.isAgency());

        roleLbl.setText("Role: " + Session.getRole());
        userLbl.setText("User #" + Session.getUserId());
        boolean isAdmin = Session.isAdmin();
        boolean isAgency = Session.isAgency();
        boolean isClient = Session.isClient();
        boolean showAgencyFilters = isClient || isAdmin;

// agencies list visible only for client/admin
        if (agenciesLv != null) {
            agenciesLv.setVisible(showAgencyFilters);
            agenciesLv.setManaged(showAgencyFilters);

            if (showAgencyFilters) {
                var agencies = agencyRepo.findAllValidated();
                agenciesLv.getItems().setAll(agencies);

                // Checkbox cells
                agenciesLv.setCellFactory(list -> new javafx.scene.control.cell.CheckBoxListCell<entities.Agency>(
                        (entities.Agency agency) -> {
                            javafx.beans.property.BooleanProperty prop =
                                    new javafx.beans.property.SimpleBooleanProperty(
                                            offerFilter.getAgencyIds().contains(agency.getIdUser())
                                    );

                            prop.addListener((obs, was, now) -> {
                                if (now) offerFilter.getAgencyIds().add(agency.getIdUser());
                                else offerFilter.getAgencyIds().remove(agency.getIdUser());
                                pushFilterToCurrentView();
                            });

                            return prop;
                        },
                        new javafx.util.StringConverter<entities.Agency>() {
                            @Override public String toString(entities.Agency a) {
                                return (a == null) ? "" : a.toString();
                            }
                            @Override public entities.Agency fromString(String s) {
                                return null;
                            }
                        }
                ));
            }
        }

        //boolean isClient = Session.isClient();

        setNavVisible(offersBtn, true);
        setNavVisible(cartBtn, isClient);
        setNavVisible(reservationsBtn, isClient);
        setActive(offersBtn);
        setNavVisible(archiveBtn, Session.isAgency());
        //boolean showAgencyFilters = isClient || isAdmin;
        // Search
        if (searchTf != null) {
            searchTf.textProperty().addListener((o, oldV, newV) -> {
                offerFilter.setKeyword(newV);
                pushFilterToCurrentView();
            });
        }

// Calendar date filter
        if (calendarDp != null) {
            calendarDp.valueProperty().addListener((o, oldV, newV) -> {
                offerFilter.setSelectedDate(newV);
                pushFilterToCurrentView();
            });
        }

// Price min/max
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
        setNavVisible(bannersBtn, Session.isAgency());






        // default page

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
        if (query == null || query.isBlank()) return;

        // Loading State
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
            System.err.println("AI Search Failed: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void applyAICriteria(SearchCriteria sc) {
        if (sc == null) return;

        // 1. Clear current filters first for a "fresh" AI search
        onClearFilters();

        // 2. Map AI results to your OfferFilter object
        if (sc.destination != null) {
            offerFilter.setKeyword(sc.destination);
            if (searchTf != null) searchTf.setText(sc.destination);
        }

        if (sc.maxPrice != null) {
            java.math.BigDecimal price = java.math.BigDecimal.valueOf(sc.maxPrice);
            offerFilter.setMaxPrice(price);
            if (maxPriceTf != null) maxPriceTf.setText(price.toString());
        }

        // 3. Push to the Grid View
        pushFilterToCurrentView();
    }
    @FXML
    private void onGoAgencyReservations() {
        if (!Session.isAgency()) return;
        setActive(agencyResBtn);
        loadIntoContent("/fxml/AgencyReservations.fxml");
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
    private void onGoBanners() {
        if (!Session.isAgency()) return;
        setActive(bannersBtn);
        loadIntoContent("/fxml/AgencyActualites.fxml");
    }

    private java.math.BigDecimal parseBigDecimalOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return new java.math.BigDecimal(s); }
        catch (Exception e) { return null; }
    }
    private void setNavVisible(Button btn, boolean visible) {
        if (btn == null) return;
        btn.setVisible(visible);
        btn.setManaged(visible);
    }
    @FXML
    private void onGoArchive() {
        if (!Session.isAgency()) return;
        setActive(archiveBtn); // if you added active highlight
        loadIntoContent("/fxml/ArchivedOffersGrid.fxml");
    }


    @FXML private void onGoOffers() {
        setActive(offersBtn);
        loadOffersView();
    }

    @FXML private void onGoCart() {
        if (!Session.isClient()) return;
        setActive(cartBtn);
        loadIntoContent("/fxml/CartView.fxml");
    }

    @FXML private void onGoReservations() {
        if (!Session.isClient()) return;
        setActive(reservationsBtn);
        loadIntoContent("/fxml/MyReservations.fxml");
    }


    private void loadOffersView() {
        String fxml;
        if (Session.isAdmin()) fxml = "/fxml/AdminOffersGrid.fxml";
        else if (Session.isAgency()) fxml = "/fxml/OffersGrid.fxml";
        else fxml = "/fxml/VoyageurOffersGrid.fxml";

        loadIntoContent(fxml);
    }

    /*private void loadIntoContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
    /*private void loadIntoContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);

            Object ctrl = loader.getController();

            // ✅ trigger load for pages that require it
            if (ctrl instanceof CartViewController c) {
                c.loadCart();
            }
            if (ctrl instanceof MyReservationsController r) {
                // r.refresh();  // only if you make refresh() public
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
    private void loadIntoContent(String fxmlPath) {
        try {
            boolean isOfferPage =
                    fxmlPath.contains("OffersGrid") ||
                            fxmlPath.contains("VoyageurOffersGrid") ||
                            fxmlPath.contains("AdminOffersGrid") ||
                            fxmlPath.contains("ArchivedOffersGrid");

            setRightPanelVisible(isOfferPage);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);

            currentController = loader.getController();
            pushFilterToCurrentView();

            // cart needs manual load
            if (currentController instanceof controllers.CartViewController c) {
                c.loadCart();
            }
            if (currentController instanceof MyReservationsController r) {
                r.setOnBack(() -> loadOffersView());
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
        if (currentController instanceof controllers.OfferFilterAware aware) {
            aware.applyFilter(offerFilter);
        }
    }
    private void setActive(Button activeBtn) {
        if (offersBtn != null) offersBtn.getStyleClass().remove("active");
        if (cartBtn != null) cartBtn.getStyleClass().remove("active");
        if (reservationsBtn != null) reservationsBtn.getStyleClass().remove("active");
        if (archiveBtn != null) archiveBtn.getStyleClass().remove("active"); // ✅ add this
        if (bannersBtn != null) bannersBtn.getStyleClass().remove("active");
        if (agencyResBtn != null) agencyResBtn.getStyleClass().remove("active");

        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }
    }
    @FXML
    private void onClearFilters() {
        offerFilter.setKeyword(null);
        offerFilter.setMinPrice(null);
        offerFilter.setMaxPrice(null);
        offerFilter.setSelectedDate(null);
        offerFilter.getAgencyIds().clear();
        if (Session.isAgency()) {
            offerFilter.getAgencyIds().add(Session.getUserId());
        }
        offerFilter.getAgencyIds().clear();

        if (searchTf != null) searchTf.clear();
        if (minPriceTf != null) minPriceTf.clear();
        if (maxPriceTf != null) maxPriceTf.clear();
        if (calendarDp != null) calendarDp.setValue(null);

        // reset checkboxes by reloading list (easy)
        if (agenciesLv != null) agenciesLv.refresh();

        pushFilterToCurrentView();
    }
}
