package Controllers;

import gestion_activite.ReservationDetail;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;


import com.lowagie.text.*;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import com.lowagie.text.*;
import com.lowagie.text.*;

public class ReservationDetailsController {


    @FXML
    private TableView<ReservationDetail> reservationsTable;
    @FXML
    private TableColumn<ReservationDetail, String> colNom;
    @FXML
    private TableColumn<ReservationDetail, String> colEmail;
    @FXML
    private TableColumn<ReservationDetail, String> colTelephone;
    @FXML
    private TableColumn<ReservationDetail, Integer> colNbPlaces;
    @FXML
    private TableColumn<ReservationDetail, Double> colMontant;
    @FXML
    private Label totalReservationsLabel;

    private ObservableList<ReservationDetail> reservations = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("clientNom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("clientEmail"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("clientTelephone"));
        colNbPlaces.setCellValueFactory(new PropertyValueFactory<>("nbPlaces"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantPaye"));
        reservationsTable.setItems(reservations);
    }

    public void setReservations(List<ReservationDetail> details) {
        reservations.setAll(details);
        totalReservationsLabel.setText(details.size() + " réservation(s)");
    }

    @FXML
    private void handleClose() {
        ((Stage) reservationsTable.getScene().getWindow()).close();
    }





    @FXML

    private void handleExportPDF() {
        if (reservations.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucune réservation à exporter.", ButtonType.OK).show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(reservationsTable.getScene().getWindow());
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Titre (couleur via java.awt.Color)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(64, 64, 64));
            Paragraph title = new Paragraph("Réservations", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Date (gris)
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(128, 128, 128));
            String dateStr = "Exporté le " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            Paragraph datePar = new Paragraph(dateStr, dateFont);
            datePar.setAlignment(Element.ALIGN_RIGHT);
            datePar.setSpacingAfter(20);
            document.add(datePar);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 3, 2, 1, 2});

            // En‑tête
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Color headerBg = new Color(59, 130, 246);
            String[] headers = {"Nom", "Email", "Téléphone", "Places", "Montant (DT)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                cell.setBorderColor(Color.WHITE);
                table.addCell(cell);
            }

            // Données
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Color evenBg = new Color(245, 245, 245);
            int row = 0;
            double totalMontant = 0;
            int totalPlaces = 0;

            for (ReservationDetail d : reservations) {
                Color bg = (row % 2 == 0) ? Color.WHITE : evenBg;

                PdfPCell cell = new PdfPCell(new Phrase(d.getClientNom(), cellFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(6);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(d.getClientEmail(), cellFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(6);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(d.getClientTelephone(), cellFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(6);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(String.valueOf(d.getNbPlaces()), cellFont));
                cell.setBackgroundColor(bg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(String.format("%.2f DT", d.getMontantPaye()), cellFont));
                cell.setBackgroundColor(bg);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell.setPadding(6);
                table.addCell(cell);

                totalMontant += d.getMontantPaye();
                totalPlaces += d.getNbPlaces();
                row++;
            }

            document.add(table);

            // Total
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(0, 0, 0));
            Paragraph total = new Paragraph(
                    String.format("Total : %d réservations · %d places · Montant total : %.2f DT",
                            reservations.size(), totalPlaces, totalMontant),
                    totalFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(20);
            document.add(total);

            document.close();

            new Alert(Alert.AlertType.INFORMATION, "PDF exporté avec succès.", ButtonType.OK).show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'export PDF : " + e.getMessage(), ButtonType.OK).show();
        }
    }
}