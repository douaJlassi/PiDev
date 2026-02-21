package Controllers;

import Services.ActiviteService;                    // ADDED
import gestion_activite.Achat;                       // ADDED (if not already)
import gestion_activite.Activite;                    // ADDED
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;


import gestion_activite.Guide;
import Services.GuideService;

public class TicketController {

    @FXML private ImageView activityImageView;
    @FXML private Label titreLabel;
    @FXML private Label dateAchatLabel;
    @FXML private Label nbPlacesLabel;
    @FXML private Label prixUnitaireLabel;
    @FXML private Label totalLabel;
    @FXML private Label statutLabel;
    @FXML private Button downloadButton;
    @FXML private Label guideNomLabel;
    @FXML private Label guideContactLabel;
    private GuideService guideService = new GuideService();

    private DashboardController dashboardController;
    private Achat achat;
    private ActiviteService activiteService = new ActiviteService();

    public void setAchat(Achat achat) {
        this.achat = achat;
        afficherTicket();
    }

    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }


    private void afficherTicket() {
        try {
            Activite activite = activiteService.selectById(achat.getIdActivite());
            if (activite != null) {
                titreLabel.setText(activite.getTitre());
                // Load image
                String imageName = activite.getImage();
                if (imageName == null || imageName.isEmpty()) imageName = "default.jpg";
                if (!imageName.contains(".")) imageName += ".jpg";
                String path = "/images/" + imageName;
                java.net.URL resource = getClass().getResource(path);
                if (resource != null) {
                    activityImageView.setImage(new Image(resource.toExternalForm()));
                } else {
                    java.net.URL defaultUrl = getClass().getResource("/images/default.jpg");
                    if (defaultUrl != null) activityImageView.setImage(new Image(defaultUrl.toExternalForm()));
                }

                // Get guide info
                int guideId = activite.getIdGuide();
                Guide guide = guideService.selectById(guideId);
                if (guide != null) {
                    String fullName = guide.getPrenom() + " " + guide.getNom();
                    guideNomLabel.setText(fullName);
                    String contact = guide.getEmail() + " | " + guide.getTelephone();
                    guideContactLabel.setText(contact);
                } else {
                    guideNomLabel.setText("Non assigné");
                    guideContactLabel.setText("");
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            dateAchatLabel.setText(sdf.format(achat.getDateAchat()));
            nbPlacesLabel.setText(String.valueOf(achat.getNbPlaces()));
            prixUnitaireLabel.setText(String.format("%.0f DT", achat.getMontantTotal() / achat.getNbPlaces()));
            totalLabel.setText(String.format("%.0f DT", achat.getMontantTotal()));
            statutLabel.setText(achat.getStatut());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleBack() {
        if (dashboardController != null) {
            dashboardController.showDashboardView();
        }
    }

    @FXML
    private void handleDownload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le ticket");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("ticket_" + achat.getIdAchat() + ".pdf");
        File file = fileChooser.showSaveDialog(downloadButton.getScene().getWindow());

        if (file != null) {
            try {
                generatePDF(file);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de générer le PDF.");
            }
        }
    }

    private void generatePDF(File file) throws DocumentException, IOException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        document.add(new Paragraph("🎫 Confirmation de réservation", titleFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Activité : " + titreLabel.getText(), boldFont));
        document.add(new Paragraph("Date de réservation : " + dateAchatLabel.getText(), normalFont));
        document.add(new Paragraph("Nombre de places : " + nbPlacesLabel.getText(), normalFont));
        document.add(new Paragraph("Prix unitaire : " + prixUnitaireLabel.getText(), normalFont));
        document.add(new Paragraph("Total : " + totalLabel.getText(), normalFont));
        document.add(new Paragraph("Statut : " + statutLabel.getText(), normalFont));

        // Add guide info
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Informations du guide :", boldFont));
        document.add(new Paragraph("Nom : " + guideNomLabel.getText(), normalFont));
        document.add(new Paragraph("Contact : " + guideContactLabel.getText(), normalFont));

        document.close();

        showAlert("Succès", "PDF généré avec succès !");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.show();
    }
}