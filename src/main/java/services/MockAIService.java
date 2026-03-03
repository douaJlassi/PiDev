package services;

import entities.Offre;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class MockAIService {

    // 16:9 banner
    private static final int W = 1024;
    private static final int H = 576;

    public static Image generateBanner(Offre offer) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Background: random soft gradient (looks "AI-ish")
        Random r = new Random();
        Color c1 = Color.hsb(r.nextInt(360), 0.35, 0.95);
        Color c2 = Color.hsb(r.nextInt(360), 0.35, 0.75);

        // simple gradient blocks (no extra libs)
        for (int y = 0; y < H; y++) {
            double t = (double) y / H;
            Color row = c1.interpolate(c2, t);
            g.setFill(row);
            g.fillRect(0, y, W, 1);
        }

        // Dark overlay for readability
        g.setFill(Color.rgb(0, 0, 0, 0.35));
        g.fillRect(0, 0, W, H);

        // Discount text
        String discountText = computeDiscountText(offer);

        // Big discount
        g.setFill(Color.web("#FF2D2D"));
        g.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 90));
        g.fillText(discountText.isBlank() ? "SPECIAL" : discountText, 60, 150);

        // Title
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        g.fillText(trim(offer.getTitre(), 28), 60, 240);

        // Dates + Price
        g.setFill(Color.rgb(255, 255, 255, 0.9));
        g.setFont(Font.font("Arial", FontWeight.NORMAL, 26));
        g.fillText("Period: " + offer.getDateDebut() + " → " + offer.getDateFin(), 60, 290);
        g.fillText("From " + offer.getPrixPromo() + " TND", 60, 330);

        // "AI Generated" watermark (for teacher)
        g.setFill(Color.rgb(255, 255, 255, 0.65));
        g.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        g.fillText("AI GENERATED (MOCK MODE)", 60, H - 40);

        // Snapshot to Image
        WritableImage wi = new WritableImage(W, H);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        canvas.snapshot(params, wi);

        return wi;
    }

    private static String computeDiscountText(Offre offer) {
        try {
            if (offer.getPrixOriginal() != null &&
                    offer.getPrixPromo() != null &&
                    offer.getPrixOriginal().compareTo(offer.getPrixPromo()) > 0) {

                BigDecimal pct = BigDecimal.ONE
                        .subtract(offer.getPrixPromo()
                                .divide(offer.getPrixOriginal(), 4, RoundingMode.HALF_UP))
                        .multiply(new BigDecimal("100"));

                int pctInt = pct.setScale(0, RoundingMode.HALF_UP).intValue();
                return "-" + pctInt + "%";
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}