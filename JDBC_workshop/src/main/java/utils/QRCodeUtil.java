package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QRCodeUtil {

    private static final int QR_CODE_SIZE = 300;
    private static final String QR_CODE_FORMAT = "png";

    /**
     * Generate QR code from user data
     */
    public static Image generateUserQRCode(String username, String email, String password) {
        try {
            // Create user data string
            String qrData = String.format(
                    "USER INFORMATION\n" +
                            "━━━━━━━━━━━━━━━\n" +
                            "Username: %s\n" +
                            "Email: %s\n" +
                            "Password: %s\n" +
                            "━━━━━━━━━━━━━━━\n" +
                            "Generated: %s",
                    username,
                    email,
                    password,
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
            );

            // Configure QR code
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            // Convert to JavaFX Image
            return bitMatrixToJavaFXImage(bitMatrix);

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate QR code as byte array (for email attachment)
     */
    public static byte[] generateUserQRCodeBytes(String username, String email, String password) {
        try {
            String qrData = String.format(
                    "USER INFORMATION\n" +
                            "━━━━━━━━━━━━━━━\n" +
                            "Username: %s\n" +
                            "Email: %s\n" +
                            "Password: %s\n" +
                            "━━━━━━━━━━━━━━━\n" +
                            "Generated: %s",
                    username,
                    email,
                    password,
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
            );

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, QR_CODE_FORMAT, baos);
            return baos.toByteArray();

        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * Convert BitMatrix to JavaFX Image
     */
    private static Image bitMatrixToJavaFXImage(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();

        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (bitMatrix.get(x, y)) {
                    pixelWriter.setColor(x, y, Color.BLACK);
                } else {
                    pixelWriter.setColor(x, y, Color.WHITE);
                }
            }
        }

        return writableImage;
    }
}