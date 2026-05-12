package controllers;

import app.Session;
import entities.Offre;
import entities.ServiceEntity;
import entities.ServiceEntityDetails;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import repositories.IOffreRepository;
import repositories.OffreRepository;
import repositories.OffreServiceRepository;
import repositories.ServiceRepository;
import services.OffreAIService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class OffreFormController {

    @FXML
    private Label formTitleLbl;

    @FXML
    private TextField titleTf;

    @FXML
    private TextField priceTf;

    @FXML
    private DatePicker startDp;

    @FXML
    private DatePicker endDp;

    @FXML
    private TextArea descTa;

    @FXML
    private Label errorLbl;

    @FXML
    private TextField imageTf;

    @FXML
    private ListView<ServiceEntity> servicesLv;

    @FXML
    private TextField originalPriceTf;

    @FXML
    private TextField discountTf;

    @FXML
    private TextField locationTf;

    @FXML
    private TextField capacityTf;

    @FXML
    private TextField serviceSearchTf;

    @FXML
    private ComboBox<String> serviceCategoryCb;

    @FXML
    private Label selectedCountLbl;

    @FXML
    private VBox servicePreviewHost;

    private Parent serviceCardNode;

    private ServiceCardController serviceCardCtrl;

    private List<ServiceEntity> allServices;

    private Offre current;

    private Runnable onSaved;

    private final IOffreRepository repo =
            new OffreRepository();

    private final ServiceRepository serviceRepo =
            new ServiceRepository();

    private final OffreServiceRepository offreServiceRepo =
            new OffreServiceRepository();

    public static void openDialog(
            Offre offerToEdit,
            Runnable onSaved
    ) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            OffreFormController.class.getResource(
                                    "/fxml/OffreForm.fxml"
                            )
                    );

            Parent root = loader.load();

            OffreFormController ctrl =
                    loader.getController();

            ctrl.setOnSaved(onSaved);

            ctrl.setOffer(offerToEdit);

            Stage stage = new Stage();

            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setTitle(
                    offerToEdit == null
                            ? "Add Offer"
                            : "Edit Offer"
            );

            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    OffreFormController.class
                            .getResource("/css/app.css")
                            .toExternalForm()
            );

            stage.setScene(scene);

            stage.showAndWait();

        } catch (IOException e) {

            Alert a =
                    new Alert(Alert.AlertType.ERROR);

            a.setTitle("UI error");

            a.setHeaderText(null);

            a.setContentText(e.getMessage());

            a.showAndWait();
        }
    }

    @FXML
    public void initialize() {

        servicesLv
                .getSelectionModel()
                .setSelectionMode(
                        SelectionMode.MULTIPLE
                );

        allServices =
                serviceRepo.findAllByAgency(
                        Session.getUserId()
                );

        servicesLv.getItems().setAll(allServices);

        setupServicePreview();

        setupPromoCalc();

        setupServiceFilters();

        setupSelectedCount();
    }

    private void setupPromoCalc() {

        ChangeListener<String> listener =
                new ChangeListener<>() {

                    @Override
                    public void changed(
                            ObservableValue<? extends String> obs,
                            String oldV,
                            String newV
                    ) {
                        computePromo();
                    }
                };

        originalPriceTf
                .textProperty()
                .addListener(listener);

        discountTf
                .textProperty()
                .addListener(listener);
    }

    private void setupServiceFilters() {

        java.util.Set<String> kinds =
                new java.util.TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        for (ServiceEntity s : allServices) {

            if (
                    s.getKind() != null &&
                            !s.getKind().isBlank()
            ) {

                kinds.add(
                        s.getKind().toUpperCase()
                );
            }
        }

        java.util.List<String> list =
                new java.util.ArrayList<>();

        list.add("ALL");

        list.addAll(kinds);

        serviceCategoryCb.setItems(
                FXCollections.observableArrayList(list)
        );

        serviceCategoryCb
                .getSelectionModel()
                .select("ALL");

        serviceSearchTf
                .textProperty()
                .addListener(
                        (obs, o, n) ->
                                applyServiceFilters()
                );

        serviceCategoryCb
                .valueProperty()
                .addListener(
                        (obs, o, n) ->
                                applyServiceFilters()
                );
    }

    private void applyServiceFilters() {

        String q =
                serviceSearchTf.getText() == null
                        ? ""
                        : serviceSearchTf
                        .getText()
                        .trim()
                        .toLowerCase();

        String kind =
                serviceCategoryCb.getValue() == null
                        ? "ALL"
                        : serviceCategoryCb.getValue();

        java.util.List<Integer> selectedIds =
                servicesLv
                        .getSelectionModel()
                        .getSelectedItems()
                        .stream()
                        .map(ServiceEntity::getIdService)
                        .toList();

        java.util.List<ServiceEntity> filtered =
                new java.util.ArrayList<>();

        for (ServiceEntity s : allServices) {

            if (!"ALL".equalsIgnoreCase(kind)) {

                String sk =
                        s.getKind() == null
                                ? ""
                                : s.getKind();

                if (!sk.equalsIgnoreCase(kind)) {
                    continue;
                }
            }

            if (!q.isEmpty()) {

                String name =
                        s.getNom() == null
                                ? ""
                                : s.getNom()
                                .toLowerCase();

                String k =
                        s.getKind() == null
                                ? ""
                                : s.getKind()
                                .toLowerCase();

                if (
                        !(name.contains(q)
                                || k.contains(q))
                ) {
                    continue;
                }
            }

            filtered.add(s);
        }

        servicesLv.getItems().setAll(filtered);

        servicesLv
                .getSelectionModel()
                .clearSelection();

        for (int i = 0; i < filtered.size(); i++) {

            if (
                    selectedIds.contains(
                            filtered.get(i)
                                    .getIdService()
                    )
            ) {

                servicesLv
                        .getSelectionModel()
                        .select(i);
            }
        }

        updateSelectedCount();
    }

    private void setupServicePreview() {

        if (
                servicesLv == null ||
                        servicePreviewHost == null
        ) {
            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/OffreServiceCard.fxml"
                            )
                    );

            serviceCardNode = loader.load();

            serviceCardCtrl =
                    loader.getController();

        } catch (Exception e) {

            e.printStackTrace();

            return;
        }

        servicesLv
                .getSelectionModel()
                .getSelectedItems()
                .addListener(
                        (
                                javafx.collections
                                        .ListChangeListener<ServiceEntity>
                                ) c -> {

                            var selected =
                                    servicesLv
                                            .getSelectionModel()
                                            .getSelectedItems();

                            if (
                                    selected == null ||
                                            selected.isEmpty()
                            ) {

                                servicePreviewHost
                                        .getChildren()
                                        .clear();

                                return;
                            }

                            ServiceEntity sel =
                                    selected.get(0);

                            ServiceEntityDetails d =
                                    null;

                            try {

                                d =
                                        serviceRepo
                                                .findDetailsByIdService(
                                                        sel.getIdService()
                                                );

                            } catch (Exception ex) {

                                ex.printStackTrace();
                            }

                            if (d == null) {

                                d = new ServiceEntityDetails();

                                d.setIdService(
                                        sel.getIdService()
                                );

                                d.setNom(sel.getNom());

                                d.setKind(
                                        sel.getKind() == null
                                                ? "SERVICE"
                                                : sel.getKind()
                                                .toUpperCase()
                                );

                                d.setDescription("");

                                d.setPrixOverride(null);
                            }

                            d.setQuantite(
                                    selected.size()
                            );

                            serviceCardCtrl.setData(d);

                            servicePreviewHost
                                    .getChildren()
                                    .setAll(serviceCardNode);
                        }
                );

        servicePreviewHost
                .getChildren()
                .clear();
    }

    private void setupSelectedCount() {

        servicesLv
                .getSelectionModel()
                .getSelectedItems()
                .addListener(
                        (
                                javafx.collections
                                        .ListChangeListener<ServiceEntity>
                                ) c -> updateSelectedCount()
                );

        updateSelectedCount();
    }

    private void updateSelectedCount() {

        if (selectedCountLbl == null) {
            return;
        }

        selectedCountLbl.setText(
                String.valueOf(
                        servicesLv
                                .getSelectionModel()
                                .getSelectedItems()
                                .size()
                )
        );
    }

    private void computePromo() {

        if (priceTf == null) {
            return;
        }

        BigDecimal original =
                parseBigDecimalOrNull(
                        originalPriceTf.getText()
                );

        BigDecimal discount =
                parseBigDecimalOrNull(
                        discountTf.getText()
                );

        if (original == null) {

            priceTf.setText("");

            return;
        }

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }

        if (
                discount.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {
            discount = BigDecimal.ZERO;
        }

        if (
                discount.compareTo(
                        new BigDecimal("90")
                ) > 0
        ) {
            discount = new BigDecimal("90");
        }

        BigDecimal hundred =
                new BigDecimal("100");

        BigDecimal promo =
                original
                        .multiply(
                                hundred.subtract(discount)
                        )
                        .divide(
                                hundred,
                                0,
                                RoundingMode.HALF_UP
                        );

        priceTf.setText(
                promo.toPlainString()
        );
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setOffer(Offre offer) {

        this.current = offer;

        if (offer == null) {

            formTitleLbl.setText("Create Offer");

            startDp.setValue(LocalDate.now());

            endDp.setValue(
                    LocalDate.now().plusDays(1)
            );

            return;
        }

        formTitleLbl.setText(
                "Edit Offer #" + offer.getId()
        );

        titleTf.setText(offer.getTitle());

        priceTf.setText(
                offer.getPromoPrice() != null
                        ? offer.getPromoPrice()
                        .toPlainString()
                        : ""
        );

        startDp.setValue(
                offer.getStartDate()
        );

        endDp.setValue(
                offer.getEndDate()
        );

        descTa.setText(
                offer.getDescription()
        );

        imageTf.setText(
                offer.getImageUrl()
        );

        locationTf.setText(
                offer.getLocation()
        );

        if (offer.getCapacity() != null) {

            capacityTf.setText(
                    String.valueOf(
                            offer.getCapacity()
                    )
            );
        }

        originalPriceTf.setText(
                offer.getOriginalPrice() != null
                        ? offer.getOriginalPrice()
                        .toPlainString()
                        : ""
        );

        discountTf.setText("");

        if (offer != null) {

            List<Integer> selectedIds =
                    offreServiceRepo
                            .findServiceIdsByOffre(
                                    offer.getId()
                            );

            servicesLv
                    .getSelectionModel()
                    .clearSelection();

            for (ServiceEntity s : servicesLv.getItems()) {

                if (
                        selectedIds.contains(
                                s.getIdService()
                        )
                ) {

                    servicesLv
                            .getSelectionModel()
                            .select(s);
                }
            }
        }
    }

    private BigDecimal parseBigDecimalOrNull(String s) {

        if (s == null) {
            return null;
        }

        s = s.trim();

        if (s.isEmpty()) {
            return null;
        }

        try {

            return new BigDecimal(s);

        } catch (Exception e) {

            return null;
        }
    }

    @FXML
    private void onSave() {

        if (!Session.isAgency()) {

            errorLbl.setText(
                    "Access denied: only agencies can manage offers."
            );

            return;
        }

        errorLbl.setText("");

        String title =
                titleTf.getText() != null
                        ? titleTf.getText().trim()
                        : "";

        String desc =
                descTa.getText() != null
                        ? descTa.getText().trim()
                        : "";

        String location =
                locationTf.getText() != null
                        ? locationTf.getText().trim()
                        : "";

        LocalDate start =
                startDp.getValue();

        LocalDate end =
                endDp.getValue();

        BigDecimal promoPrice;

        BigDecimal originalPrice;

        Integer capacity = null;

        try {

            promoPrice =
                    new BigDecimal(
                            priceTf.getText().trim()
                    );

        } catch (Exception ex) {

            errorLbl.setText(
                    "Promo price must be a valid number."
            );

            return;
        }

        try {

            originalPrice =
                    new BigDecimal(
                            originalPriceTf
                                    .getText()
                                    .trim()
                    );

        } catch (Exception ex) {

            errorLbl.setText(
                    "Original price must be a valid number."
            );

            return;
        }

        try {

            if (
                    capacityTf.getText() != null &&
                            !capacityTf.getText().isBlank()
            ) {

                capacity =
                        Integer.parseInt(
                                capacityTf
                                        .getText()
                                        .trim()
                        );
            }

        } catch (Exception ex) {

            errorLbl.setText(
                    "Capacity must be a valid integer."
            );

            return;
        }

        if (title.isEmpty()) {
            errorLbl.setText("Title is required.");
            return;
        }

        if (desc.isEmpty()) {
            errorLbl.setText("Description is required.");
            return;
        }

        if (start == null || end == null) {
            errorLbl.setText("Dates are required.");
            return;
        }

        if (start.isAfter(end)) {
            errorLbl.setText(
                    "Start date must be <= end date."
            );
            return;
        }

        if (
                promoPrice.compareTo(BigDecimal.ZERO) < 0
        ) {
            errorLbl.setText(
                    "Promo price must be >= 0."
            );
            return;
        }

        if (
                promoPrice.compareTo(originalPrice) > 0
        ) {
            errorLbl.setText(
                    "Promo price must be <= original price."
            );
            return;
        }

        if (current == null) {
            current = new Offre();
        }

        current.setTitle(title);

        current.setDescription(desc);

        current.setPromoPrice(promoPrice);

        current.setOriginalPrice(originalPrice);

        current.setStartDate(start);

        current.setEndDate(end);

        current.setLocation(location);

        current.setCapacity(capacity);

        current.setUserId(Session.getUserId());

        current.setStatus("ACTIVE");

        String imgUrl =
                imageTf.getText() == null
                        ? null
                        : imageTf.getText().trim();

        current.setImageUrl(
                imgUrl == null || imgUrl.isBlank()
                        ? null
                        : imgUrl
        );

        try {

            if (current.getId() == 0) {

                int newId =
                        repo.insert(current);

                current.setId(newId);

            } else {

                boolean ok =
                        repo.updateForAgency(
                                current,
                                Session.getUserId()
                        );

                if (!ok) {

                    errorLbl.setText(
                            "Update refused."
                    );

                    return;
                }
            }

            var selected =
                    servicesLv
                            .getSelectionModel()
                            .getSelectedItems();

            offreServiceRepo.replaceServices(
                    current.getId(),
                    selected.stream()
                            .map(ServiceEntity::getIdService)
                            .toList()
            );

            if (onSaved != null) {
                onSaved.run();
            }

            Stage stage =
                    (Stage)
                            titleTf
                                    .getScene()
                                    .getWindow();

            stage.close();

        } catch (Exception e) {

            errorLbl.setText(
                    "Save failed: " + e.getMessage()
            );
        }
    }

    @FXML
    private void onCancel() {

        Stage stage =
                (Stage)
                        titleTf
                                .getScene()
                                .getWindow();

        stage.close();
    }


    @FXML
    private void onGenerateAIDescription() {

        String title =
                titleTf.getText();

        if (
                title == null ||
                        title.trim().isEmpty()
        ) {

            errorLbl.setText(
                    "Please enter a title first."
            );

            return;
        }

        descTa.setPromptText(
                "AI is crafting your description... ✨"
        );

        descTa.setDisable(true);

        descTa.getStyleClass().add("ai-glow");

        DropShadow glow =
                new DropShadow();

        glow.setColor(
                Color.web("#6366F1")
        );

        glow.setRadius(0);

        descTa.setEffect(glow);

        Timeline pulse =
                new Timeline(
                        new KeyFrame(
                                Duration.ZERO,
                                new KeyValue(
                                        glow.radiusProperty(),
                                        5
                                )
                        ),
                        new KeyFrame(
                                Duration.seconds(1),
                                new KeyValue(
                                        glow.radiusProperty(),
                                        25
                                )
                        )
                );

        pulse.setAutoReverse(true);

        pulse.setCycleCount(
                Animation.INDEFINITE
        );

        pulse.play();

        Task<String> aiTask =
                new Task<>() {

                    @Override
                    protected String call()
                            throws Exception {

                        return OffreAIService
                                .generateDescription(title);
                    }
                };

        aiTask.setOnSucceeded(e -> {

            descTa.setText(
                    aiTask.getValue()
            );

            stopAIEffects(pulse);
        });

        aiTask.setOnFailed(e -> {

            errorLbl.setText(
                    "AI Service currently unavailable."
            );

            stopAIEffects(pulse);
        });

        new Thread(aiTask).start();
    }

    private void stopAIEffects(Timeline pulse) {

        pulse.stop();

        descTa.setDisable(false);

        descTa.setEffect(null);

        descTa
                .getStyleClass()
                .remove("ai-glow");

        descTa.setPromptText("");

        errorLbl.setText("");
    }
}