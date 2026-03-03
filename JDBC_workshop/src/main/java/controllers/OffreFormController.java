package controllers;

import app.Session;
import entities.ServiceEntity;
import entities.ServiceEntityDetails;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import entities.Offre;
import repositories.IOffreRepository;
import repositories.OffreRepository;
import javafx.stage.FileChooser;
import repositories.OffreServiceRepository;
import repositories.ServiceRepository;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.Duration;
import services.OffreAIService;

import java.io.File;


import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class OffreFormController {

    @FXML private Label formTitleLbl;
    @FXML private TextField titleTf;
    @FXML private TextField priceTf;
    @FXML private DatePicker startDp;
    @FXML private DatePicker endDp;
    //@FXML private TextField agencyTf;
    @FXML private TextArea descTa;
    @FXML private Label errorLbl;
    @FXML private TextField imageTf;
    @FXML private ListView<ServiceEntity> servicesLv;
    @FXML private TextField originalPriceTf;
    @FXML private TextField discountTf;
    @FXML private TextField serviceSearchTf;
    @FXML private ComboBox<String> serviceCategoryCb;
    @FXML private Label selectedCountLbl;
    @FXML private VBox servicePreviewHost;

    private Parent serviceCardNode;
    private ServiceCardController serviceCardCtrl;

    private List<ServiceEntity> allServices;



    private Offre current; // null = create
    private Runnable onSaved;

    private final IOffreRepository repo = new OffreRepository();
    private final ServiceRepository serviceRepo = new ServiceRepository();
    private final OffreServiceRepository offreServiceRepo = new OffreServiceRepository();

    public static void openDialog(Offre offerToEdit, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(OffreFormController.class.getResource("/fxml/OffreForm.fxml"));
            Parent root = loader.load();

            OffreFormController ctrl = loader.getController();
            ctrl.setOnSaved(onSaved);
            ctrl.setOffer(offerToEdit);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(offerToEdit == null ? "Add Offer" : "Edit Offer");
            Scene scene = new Scene(root);

            scene.getStylesheets().add(OffreFormController.class.getResource("/css/app.css").toExternalForm());
            //scene.getStylesheets().add(OffreFormController.class.getResource("/css/forms.css").toExternalForm());


            stage.setScene(scene);
            stage.showAndWait();


        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("UI error");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }
    @FXML
    public void initialize() {
        servicesLv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        allServices = serviceRepo.findAllByAgency(Session.getUserId());
        servicesLv.getItems().setAll(allServices);
        setupServicePreview();

        setupPromoCalc();
        setupServiceFilters();
        setupSelectedCount();
    }
    private void setupPromoCalc() {
        ChangeListener<String> listener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldV, String newV) {
                computePromo();
            }
        };

        originalPriceTf.textProperty().addListener(listener);
        discountTf.textProperty().addListener(listener);
    }
    private void setupServiceFilters() {
        // kind values from your ServiceEntity: VOL / HOTEL / SERVICE
        java.util.Set<String> kinds = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ServiceEntity s : allServices) {
            if (s.getKind() != null && !s.getKind().isBlank()) kinds.add(s.getKind().toUpperCase());
        }

        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("ALL");
        list.addAll(kinds);

        serviceCategoryCb.setItems(FXCollections.observableArrayList(list));
        serviceCategoryCb.getSelectionModel().select("ALL");

        serviceSearchTf.textProperty().addListener((obs, o, n) -> applyServiceFilters());
        serviceCategoryCb.valueProperty().addListener((obs, o, n) -> applyServiceFilters());
    }

    private void applyServiceFilters() {
        String q = serviceSearchTf.getText() == null ? "" : serviceSearchTf.getText().trim().toLowerCase();
        String kind = serviceCategoryCb.getValue() == null ? "ALL" : serviceCategoryCb.getValue();

        // keep selection after filtering
        java.util.List<Integer> selectedIds = servicesLv.getSelectionModel()
                .getSelectedItems()
                .stream()
                .map(ServiceEntity::getIdService)
                .toList();

        java.util.List<ServiceEntity> filtered = new java.util.ArrayList<>();

        for (ServiceEntity s : allServices) {
            // kind filter
            if (!"ALL".equalsIgnoreCase(kind)) {
                String sk = s.getKind() == null ? "" : s.getKind();
                if (!sk.equalsIgnoreCase(kind)) continue;
            }

            // text filter by name or kind
            if (!q.isEmpty()) {
                String name = s.getNom() == null ? "" : s.getNom().toLowerCase();
                String k = s.getKind() == null ? "" : s.getKind().toLowerCase();
                if (!(name.contains(q) || k.contains(q))) continue;
            }

            filtered.add(s);
        }

        servicesLv.getItems().setAll(filtered);

        // restore selection
        servicesLv.getSelectionModel().clearSelection();
        for (int i = 0; i < filtered.size(); i++) {
            if (selectedIds.contains(filtered.get(i).getIdService())) {
                servicesLv.getSelectionModel().select(i);
            }
        }

        updateSelectedCount();
    }
    private void setupServicePreview() {
        if (servicesLv == null || servicePreviewHost == null) return;

        // preload card once
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OffreServiceCard.fxml"));
            serviceCardNode = loader.load();
            serviceCardCtrl = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // show/hide depending on selection list
        servicesLv.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<ServiceEntity>) c -> {
                    var selected = servicesLv.getSelectionModel().getSelectedItems();
                    if (selected == null || selected.isEmpty()) {
                        servicePreviewHost.getChildren().clear();
                        return;
                    }

                    ServiceEntity sel = selected.get(0); // show first selected

                    ServiceEntityDetails d = null;
                    try {
                        d = serviceRepo.findDetailsByIdService(sel.getIdService()); // ✅ real details
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

// fallback if not found / query failed
                    if (d == null) {
                        d = new ServiceEntityDetails();
                        d.setIdService(sel.getIdService());
                        d.setNom(sel.getNom());
                        d.setKind(sel.getKind() == null ? "SERVICE" : sel.getKind().toUpperCase());
                        d.setDescription("");
                        d.setPrixOverride(null);
                    }

// quantity = number selected (nice demo)
                    d.setQuantite(selected.size());

                    serviceCardCtrl.setData(d);
                    servicePreviewHost.getChildren().setAll(serviceCardNode);
                }
        );

        // initial state
        servicePreviewHost.getChildren().clear();
    }

    private void setupSelectedCount() {
        servicesLv.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<ServiceEntity>) c -> updateSelectedCount()
        );
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        if (selectedCountLbl == null) return;
        selectedCountLbl.setText(String.valueOf(servicesLv.getSelectionModel().getSelectedItems().size()));
    }

    private void computePromo() {
        if (priceTf == null) return;

        var original = parseBigDecimalOrNull(originalPriceTf.getText());
        var discount = parseBigDecimalOrNull(discountTf.getText());

        if (original == null) { priceTf.setText(""); return; }
        if (discount == null) discount = java.math.BigDecimal.ZERO;

        // clamp 0..90
        if (discount.compareTo(java.math.BigDecimal.ZERO) < 0) discount = java.math.BigDecimal.ZERO;
        if (discount.compareTo(new java.math.BigDecimal("90")) > 0) discount = new java.math.BigDecimal("90");

        var hundred = new java.math.BigDecimal("100");
        var promo = original.multiply(hundred.subtract(discount)).divide(hundred, 0, java.math.RoundingMode.HALF_UP);

        priceTf.setText(promo.toPlainString());
    }


    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setOffer(Offre offer) {
        this.current = offer;
        //servicesLv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);




        if (offer == null) {
            formTitleLbl.setText("Create Offer");
            startDp.setValue(LocalDate.now());
            endDp.setValue(LocalDate.now().plusDays(1));
            return;
        }

        formTitleLbl.setText("Edit Offer #" + offer.getIdOffre());
        titleTf.setText(offer.getTitre());
        priceTf.setText(offer.getPrixPromo() != null ? offer.getPrixPromo().toPlainString() : "");
        startDp.setValue(offer.getDateDebut());
        endDp.setValue(offer.getDateFin());
        //agencyTf.setText(String.valueOf(offer.getIdAgence()));
        descTa.setText(offer.getDescription());
        imageTf.setText(offer.getImageUrl());
        originalPriceTf.setText(offer.getPrixOriginal() != null ? offer.getPrixOriginal().toPlainString() : "");
        discountTf.setText(""); // optional
        priceTf.setText(offer.getPrixPromo() != null ? offer.getPrixPromo().toPlainString() : "");
        //servicesLv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //List<ServiceEntity> all = serviceRepo.findAllByAgency(Session.getUserId());
        //servicesLv.getItems().setAll(all);

        if (offer != null) {
            List<Integer> selectedIds = offreServiceRepo.findServiceIdsByOffre(offer.getIdOffre());

            servicesLv.getSelectionModel().clearSelection();

            for (ServiceEntity s : servicesLv.getItems()) {
                if (selectedIds.contains(s.getIdService())) {
                    servicesLv.getSelectionModel().select(s);
                }
            }

        }


    }
    private java.math.BigDecimal parseBigDecimalOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return new java.math.BigDecimal(s); }
        catch (Exception e) { return null; }
    }


    @FXML
    private void onSave() {
        if (!Session.isAgency()) {
            errorLbl.setText("Access denied: only agencies can manage offers.");
            return;
        }

        errorLbl.setText("");

        String titre = titleTf.getText() != null ? titleTf.getText().trim() : "";
        String desc  = descTa.getText() != null ? descTa.getText().trim() : "";
        LocalDate start = startDp.getValue();
        LocalDate end   = endDp.getValue();

        BigDecimal price;

        try {
            price = new BigDecimal(priceTf.getText().trim());
        } catch (Exception ex) {
            errorLbl.setText("Price must be a valid number.");
            return;
        }

        /*try {
            idAgence = Integer.parseInt(agencyTf.getText().trim());
        } catch (Exception ex) {
            errorLbl.setText("OffreAgency Id must be a valid integer.");
            return;
        }*/

        if (titre.isEmpty()) { errorLbl.setText("Title is required."); return; }
        if (desc.isEmpty())  { errorLbl.setText("Description is required."); return; }
        if (start == null || end == null) { errorLbl.setText("Dates are required."); return; }
        if (start.isAfter(end)) { errorLbl.setText("Start date must be <= end date."); return; }
        //if (idAgence <= 0) { errorLbl.setText("OffreAgency Id must be > 0."); return; }
        if (price.compareTo(BigDecimal.ZERO) < 0) { errorLbl.setText("Price must be >= 0."); return; }

        //  create object FIRST
        if (current == null) current = new Offre();

        current.setTitre(titre);
        current.setDescription(desc);
        current.setPrixPromo(price);
        current.setDateDebut(start);
        current.setDateFin(end);
        current.setIdAgence(Session.getUserId());


        //  way safe to set imageUrl
        String imgUrl = (imageTf.getText() == null) ? null : imageTf.getText().trim();
        current.setImageUrl(imgUrl == null || imgUrl.isBlank() ? null : imgUrl);

        try {
            if (current.getIdOffre() == 0) {
                int newId = repo.insert(current);
                current.setIdOffre(newId);
            } else {
                boolean ok = repo.updateForAgency(current, Session.getUserId());
                if (!ok) {
                    errorLbl.setText("Update refused: this offer is not yours (or it was deleted).");
                    return;
                }
            }


            if (onSaved != null) onSaved.run();

            Stage stage = (Stage) titleTf.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            errorLbl.setText("Save failed: " + e.getMessage());
        }
        var selected = servicesLv.getSelectionModel().getSelectedItems();
        offreServiceRepo.replaceServices(
                current.getIdOffre(),
                selected.stream().map(ServiceEntity::getIdService).toList()
        );
        BigDecimal original;
        BigDecimal promo;

        try { original = new BigDecimal(originalPriceTf.getText().trim()); }
        catch (Exception ex) { errorLbl.setText("Original price must be a valid number."); return; }

        try { promo = new BigDecimal(priceTf.getText().trim()); }
        catch (Exception ex) { errorLbl.setText("Promo price is missing (check discount)."); return; }

        if (promo.compareTo(original) > 0) {
            errorLbl.setText("Promo price must be <= original price.");
            return;
        }

        current.setPrixOriginal(original);
        current.setPrixPromo(promo);


    }


    @FXML
    private void onCancel() {
        Stage stage = (Stage) titleTf.getScene().getWindow();
        stage.close();
    }
    @FXML
    private void onBrowseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Offer Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        // start in user Pictures folder
        File file = fc.showOpenDialog(titleTf.getScene().getWindow());
        if (file == null) return;

        // Store as a URI that Image() can read directly
        String uri = file.toURI().toString();   // e.g. file:/C:/Users/.../pic.jpg
        imageTf.setText(uri);
    }
    @FXML
    private void onGenerateAIDescription() {
        String title = titleTf.getText();

        if (title == null || title.trim().isEmpty()) {
            errorLbl.setText("Please enter a title first so the AI knows what to write about!");
            return;
        }

        // 1. Visual Feedback & Disable
        descTa.setPromptText("AI is crafting your description... ✨");
        descTa.setDisable(true);
        descTa.getStyleClass().add("ai-glow"); // Apply CSS glow

        // 2. Setup the "Breathe" Animation
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setColor(javafx.scene.paint.Color.web("#6366F1"));
        glow.setRadius(0);
        descTa.setEffect(glow);

        javafx.animation.Timeline pulse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(glow.radiusProperty(), 5)),
                new javafx.animation.KeyFrame(Duration.seconds(1),
                        new javafx.animation.KeyValue(glow.radiusProperty(), 25))
        );
        pulse.setAutoReverse(true);
        pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse.play();

        // 3. The AI Task (Your Original Logic)
        javafx.concurrent.Task<String> aiTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return OffreAIService.generateDescription(title);
            }
        };

        aiTask.setOnSucceeded(e -> {
            descTa.setText(aiTask.getValue());
            stopAIEffects(pulse);
        });

        aiTask.setOnFailed(e -> {
            errorLbl.setText("AI ServiceEntity currently unavailable.");
            stopAIEffects(pulse);
        });

        new Thread(aiTask).start();
    }

    // Helper to clean up the UI after AI finishes
    private void stopAIEffects(javafx.animation.Timeline pulse) {
        pulse.stop();
        descTa.setDisable(false);
        descTa.setEffect(null);
        descTa.getStyleClass().remove("ai-glow");
        descTa.setPromptText("");
        errorLbl.setText("");
    }



}
