package controllers;

import app.Session;
import entities.Service;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    @FXML private ListView<Service> servicesLv;
    @FXML private TextField originalPriceTf;
    @FXML private TextField discountTf;



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
        List<Service> all = serviceRepo.findAllByAgency(Session.getUserId());
        servicesLv.getItems().setAll(all);
        setupPromoCalc();


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

        //List<Service> all = serviceRepo.findAllByAgency(Session.getUserId());
        //servicesLv.getItems().setAll(all);

        if (offer != null) {
            List<Integer> selectedIds = offreServiceRepo.findServiceIdsByOffre(offer.getIdOffre());

            servicesLv.getSelectionModel().clearSelection();

            for (Service s : servicesLv.getItems()) {
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
            errorLbl.setText("Agency Id must be a valid integer.");
            return;
        }*/

        if (titre.isEmpty()) { errorLbl.setText("Title is required."); return; }
        if (desc.isEmpty())  { errorLbl.setText("Description is required."); return; }
        if (start == null || end == null) { errorLbl.setText("Dates are required."); return; }
        if (start.isAfter(end)) { errorLbl.setText("Start date must be <= end date."); return; }
        //if (idAgence <= 0) { errorLbl.setText("Agency Id must be > 0."); return; }
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
                selected.stream().map(Service::getIdService).toList()
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

        // Visual feedback
        descTa.setPromptText("AI is crafting your description... ✨");
        descTa.setDisable(true);

        javafx.concurrent.Task<String> aiTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                // Using the service we created
                return services.AIService.generateDescription(title);
            }
        };

        aiTask.setOnSucceeded(e -> {
            descTa.setText(aiTask.getValue());
            descTa.setDisable(false);
            errorLbl.setText("");
        });

        aiTask.setOnFailed(e -> {
            descTa.setDisable(false);
            errorLbl.setText("AI Service currently unavailable.");
        });

        new Thread(aiTask).start();
    }


}
