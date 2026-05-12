package services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import entities.reservation;
import entities.service;
import entities.vol;
import entities.Hotel;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfExportService {

    public static void exportReservation(reservation r, service s,String type) throws Exception {
        // Save to user's desktop or home directory

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = System.getProperty("user.home") + File.separator + "Downloads" + File.separator
                + "reservation_" + r.getNom().replaceAll("\\s+", "_") + "_" + timestamp + ".pdf";
        PdfWriter writer = new PdfWriter(filename);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        DeviceRgb teal = new DeviceRgb(75, 163, 161);   // #4ba3a1
        DeviceRgb lightGray = new DeviceRgb(245, 245, 245);

        // ── Header ──────────────────────────────────────────────
        Paragraph header = new Paragraph("🌍 Rehletna")
                .setFontSize(26)
                .setBold()
                .setFontColor(teal)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(header);

        Paragraph subHeader = new Paragraph("Reservation Confirmation")
                .setFontSize(14)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subHeader);

        // Divider line
        document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()));
        document.add(new Paragraph(" "));

        // ── Status Badge ────────────────────────────────────────
        Paragraph status = new Paragraph("✅  Status: ACCEPTED")
                .setFontSize(13)
                .setBold()
                .setFontColor(new DeviceRgb(34, 139, 34))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(15);
        document.add(status);

        // ── Section: Reservation Details ────────────────────────
        document.add(sectionTitle("Reservation Details", teal));

        Table reservationTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .useAllAvailableWidth();

        addRow(reservationTable, "Client Name", r.getNom(), lightGray, true);
        addRow(reservationTable, "Reservation Date", r.getDateReservation().toString(), lightGray, false);
        addRow(reservationTable, "Payment Method", r.getModePaiement(), lightGray, true);
        addRow(reservationTable, "Seat number", String.valueOf(r.getSeatNb()), lightGray, false);
        document.add(reservationTable);
        document.add(new Paragraph(" "));

        // ── Section: Service Details ─────────────────────────────
        document.add(sectionTitle("Service Details", teal));

        Table serviceTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .useAllAvailableWidth();

        addRow(serviceTable, "Service Name", s.getNom(), lightGray, true);
        addRow(serviceTable, "Price", s.getPrix() + " TND", lightGray, true);

        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            addRow(serviceTable, "Description", s.getDescription(), lightGray, false);
        }

        // ── Vol-specific fields ──────────────────────────────────
        if (type.equals("vol")) {
            VolService volService = new VolService();
            vol v = volService.selectByNom(s.getNom());
            addRow(serviceTable, "Flight Number", v.getNumeroVol(), lightGray, true);
            addRow(serviceTable, "Departure City", v.getVilleDepart(), lightGray, false);
            addRow(serviceTable, "Arrival City", v.getVilleArrivee(), lightGray, true);
            addRow(serviceTable, "Departure Date", v.getDateDepart() != null ? v.getDateDepart().toString() : "N/A", lightGray, false);
            addRow(serviceTable, "Arrival Date", v.getDateArrivee() != null ? v.getDateArrivee().toString() : "N/A", lightGray, true);
        }

        // ── Hotel-specific fields ────────────────────────────────
        if (type.equals("hotel")) {
            HotelService hotelService = new HotelService();
            Hotel h = hotelService.selectOne(s.getNom());
            addRow(serviceTable, "Stars", String.valueOf(h.getNbEtoiles()) + " ⭐", lightGray, true);
            addRow(serviceTable, "Location", h.getLocalisation(), lightGray, false);
            addRow(serviceTable, "Room Type", h.getChambre(), lightGray, true);
        }

        document.add(serviceTable);

        // ── Footer ───────────────────────────────────────────────
        document.add(new Paragraph(" "));
        document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()));
        Paragraph footer = new Paragraph("Generated by Rehletna • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(footer);

        document.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static Paragraph sectionTitle(String text, DeviceRgb color) {
        return new Paragraph(text)
                .setFontSize(13)
                .setBold()
                .setFontColor(color)
                .setMarginTop(10)
                .setMarginBottom(5);
    }

    private static void addRow(Table table, String label, String value,
                               DeviceRgb bgColor, boolean shaded) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setBold().setFontSize(11))
                .setBackgroundColor(shaded ? bgColor : ColorConstants.WHITE)
                .setPadding(6);

        Cell valueCell = new Cell()
                .add(new Paragraph(value != null ? value : "N/A").setFontSize(11))
                .setBackgroundColor(shaded ? bgColor : ColorConstants.WHITE)
                .setPadding(6);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}