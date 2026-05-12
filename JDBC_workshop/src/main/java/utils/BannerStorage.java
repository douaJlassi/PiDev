package utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BannerStorage {

    // saves under /banners directory next to your app execution folder
    public static String saveOfferBannerToFile(Image img, int offerId) throws Exception {
        if (img == null) return null;

        File dir = new File("banners");
        if (!dir.exists()) dir.mkdirs();

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File out = new File(dir, "offer_" + offerId + "_" + ts + ".png");

        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", out);

        // store this in DB; later: new Image(url, true)
        return out.toURI().toString(); // file:/C:/.../banners/offer_12_20260223_210501.png
    }
}