package Services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
// Random is not used here; you can add a verification code if needed

public class EmailService {

    private static final String FROM_EMAIL = "oueslati.sahar.11@gmail.com";
    private static final String APP_PASSWORD = "zpqu estp hoqn glcp";

    public static void envoyerConfirmation(
            String toEmail,
            String clientNom,
            String activiteTitre,
            int nbPlaces,
            double montantTotal,
            byte[] qrCodeBytes
    ) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("✅ Confirmation de votre réservation – " + activiteTitre);
            MimeMultipart multipart = new MimeMultipart("related");


            String htmlBody = buildHtmlEmail(clientNom, activiteTitre, nbPlaces, montantTotal);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // QR code
            MimeBodyPart qrPart = new MimeBodyPart();
            DataSource ds = new ByteArrayDataSource(qrCodeBytes, "image/png");
            qrPart.setDataHandler(new DataHandler(ds));
            qrPart.setHeader("Content-ID", "<qrcode>");
            qrPart.setDisposition(MimeBodyPart.INLINE);
            qrPart.setFileName("qrcode.png");
            multipart.addBodyPart(qrPart);

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("✅ Email envoyé à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String buildHtmlEmail(String clientNom, String activiteTitre,
                                         int nbPlaces, double montantTotal) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,sans-serif;background:#f8f7ff;padding:30px;'>"
                + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:16px;"
                + "box-shadow:0 4px 24px rgba(108,99,255,0.10);padding:36px;'>"
                + "<h2 style='color:#6c63ff;margin-top:0;'>✅ Réservation confirmée !</h2>"
                + "<p>Bonjour <strong>" + clientNom + "</strong>,</p>"
                + "<p>Votre réservation pour l'activité <strong>" + activiteTitre + "</strong> a bien été enregistrée.</p>"
                + "<table style='width:100%;border-collapse:collapse;margin:20px 0;'>"
                + "<tr style='background:#f0eeff;'>"
                + "<td style='padding:10px 14px;color:#555;'>Activité</td>"
                + "<td style='padding:10px 14px;font-weight:bold;color:#1a1a2e;'>" + activiteTitre + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style='padding:10px 14px;color:#555;'>Nombre de places</td>"
                + "<td style='padding:10px 14px;font-weight:bold;color:#1a1a2e;'>" + nbPlaces + "</td>"
                + "</tr>"
                + "<tr style='background:#f0eeff;'>"
                + "<td style='padding:10px 14px;color:#555;'>Montant total</td>"
                + "<td style='padding:10px 14px;font-weight:bold;color:#6c63ff;'>"
                + String.format("%.0f DT", montantTotal) + "</td>"
                + "</tr>"
                + "</table>"
                + "<p style='color:#555;'>Présentez ce QR code à l'entrée de l'activité :</p>"
                + "<div style='text-align:center;margin:24px 0;'>"
                + "<img src='cid:qrcode' width='200' height='200' style='border:4px solid #e4e4f0;border-radius:12px;'/>"
                + "</div>"
                + "<p style='color:#9090b0;font-size:12px;text-align:center;'>Merci pour votre confiance. À bientôt !</p>"
                + "</div></body></html>";
    }


    private static class ByteArrayDataSource implements DataSource {
        private final byte[] data;
        private final String contentType;

        public ByteArrayDataSource(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "qrcode.png";
        }
    }
}