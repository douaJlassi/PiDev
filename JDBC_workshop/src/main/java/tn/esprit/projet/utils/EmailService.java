package tn.esprit.projet.utils;

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
import java.util.Random;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "becharguiaw@gmail.com"; // Your email
    private static final String EMAIL_PASSWORD = "jlphxtjobtinubpj"; // Your app password

    /**
     * Generate a random 6-digit code
     */
    public static String generate2FACode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Send 2FA code via email
     */
    public static boolean send2FACode(String toEmail, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Your 2FA Verification Code - rehletna.tn");

            String emailContent = """
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #1D4D7C; margin: 0; text-align: center; }
                        h2 { color: #0FA5A2; text-align: center; font-size: 48px; letter-spacing: 5px; margin: 30px 0; }
                        .info { background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #999; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>✨ rehletna.tn ✨</h1>
                        <p style="text-align: center; color: #666;">Two-Factor Authentication</p>
                        
                        <div class="info">
                            <p style="color: #666;">Your verification code is:</p>
                            <h2>%s</h2>
                            <p style="color: #666; font-size: 14px;">Enter this code to complete your login</p>
                        </div>
                        
                        <div style="background-color: #fff3cd; border-radius: 8px; padding: 15px; margin: 20px 0;">
                            <p style="color: #856404; margin: 0; font-size: 13px;">
                                ⏰ This code will expire in 5 minutes. If you didn't request this code, please ignore this email.
                            </p>
                        </div>
                        
                        <div class="footer">
                            <p>© 2025 rehletna.tn - All rights reserved</p>
                            <p style="font-size: 10px;">This is an automated message, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(code);

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ 2FA code sent to: " + toEmail);
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send 2FA email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send 2FA activation confirmation email
     */
    public static boolean send2FAActivationEmail(String toEmail, String username) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("✅ 2FA Activated - rehletna.tn");

            String emailContent = """
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #1D4D7C; text-align: center; }
                        .success-icon { font-size: 60px; color: #2ecc71; text-align: center; }
                        .info { background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #999; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>✨ rehletna.tn ✨</h1>
                        <div class="success-icon">✅</div>
                        <h2 style="color: #1D4D7C; text-align: center;">Hello %s!</h2>
                        <p style="text-align: center; color: #666; font-size: 16px;">
                            Two-Factor Authentication has been successfully enabled on your account.
                        </p>
                        
                        <div class="info">
                            <p style="color: #666; margin: 5px 0;"><strong>🔐 What's next?</strong></p>
                            <ul style="color: #666; margin: 10px 0; padding-left: 20px;">
                                <li>You'll need to enter a verification code sent to this email every time you log in</li>
                                <li>Keep your email account secure</li>
                                <li>If you didn't enable this, please contact support immediately</li>
                            </ul>
                        </div>
                        
                        <div class="footer">
                            <p>© 2025 rehletna.tn - All rights reserved</p>
                            <p style="font-size: 10px;">This is an automated message, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ 2FA activation email sent to: " + toEmail);
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send activation email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Custom DataSource for byte array
     */
    private static class ByteArrayDataSource implements DataSource {
        private final byte[] data;
        private final String contentType;
        private final String name;

        public ByteArrayDataSource(byte[] data, String contentType, String name) {
            this.data = data;
            this.contentType = contentType;
            this.name = name;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Send QR Code as email with embedded image
     */
    public static boolean sendQRCodeEmail(String toEmail, String username, byte[] qrCodeBytes) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("📱 Your Account QR Code - Rehletna.tn");

            // Create multipart message
            Multipart multipart = new MimeMultipart("related");

            // HTML part with embedded image
            MimeBodyPart htmlPart = new MimeBodyPart();
            String htmlContent = """
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 15px; padding: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
                        h1 { color: #0FA5A2; text-align: center; font-size: 28px; margin: 0 0 10px 0; }
                        h2 { color: #1D4D7C; text-align: center; font-size: 22px; margin: 0 0 20px 0; }
                        .qr-container { text-align: center; margin: 30px 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 15px; }
                        .qr-code { display: inline-block; background-color: white; padding: 15px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.2); }
                        .info { background-color: #f8f9fa; border-radius: 10px; padding: 20px; margin: 20px 0; border-left: 4px solid #0FA5A2; }
                        .username { color: #0FA5A2; font-size: 18px; font-weight: bold; }
                        .note { color: #FEC74C; font-size: 14px; font-style: italic; text-align: center; margin: 20px 0; }
                        hr { border: 1px solid #eee; margin: 20px 0; }
                        .footer { color: #999; font-size: 12px; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>✨ Rehletna.tn ✨</h1>
                        <h2>Your Account QR Code</h2>
                        
                        <div class="info">
                            <p style="font-size: 16px; color: #333;">Hello <span class="username">%s</span>,</p>
                            <p style="font-size: 14px; color: #666;">Here is your personal QR code. Scan it with your phone to access your account information.</p>
                        </div>
                        
                        <div class="qr-container">
                            <div class="qr-code">
                                <img src="cid:qrcode" alt="QR Code" style="max-width: 250px; height: auto; display: block;">
                            </div>
                        </div>
                        
                        <p class="note">🔒 Keep this QR code secure - it contains your account credentials</p>
                        
                        <div style="background-color: #f8f9fa; border-radius: 10px; padding: 15px; margin: 20px 0;">
                            <p style="color: #666; margin: 5px 0;"><strong>📧 Email:</strong> %s</p>
                            <p style="color: #666; margin: 5px 0;"><strong>👤 Username:</strong> %s</p>
                            <p style="color: #666; margin: 5px 0;"><strong>🔑 Password:</strong> ********</p>
                        </div>
                        
                        <hr>
                        <div class="footer">
                            <p>© 2025 Rehletna.tn - All rights reserved</p>
                            <p style="font-size: 10px;">This is an automated message, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, toEmail, username);

            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // Image part - embed the QR code
            if (qrCodeBytes != null && qrCodeBytes.length > 0) {
                MimeBodyPart imagePart = new MimeBodyPart();

                // Create a DataSource from the byte array
                DataSource dataSource = new ByteArrayDataSource(qrCodeBytes, "image/png", "qrcode.png");

                // Set the DataHandler with the DataSource
                imagePart.setDataHandler(new DataHandler(dataSource));
                imagePart.setHeader("Content-ID", "<qrcode>");
                imagePart.setDisposition(MimeBodyPart.INLINE);
                imagePart.setFileName("qrcode.png");

                multipart.addBodyPart(imagePart);

                System.out.println("✅ QR Code image embedded, size: " + qrCodeBytes.length + " bytes");
            }

            message.setContent(multipart);

            Transport.send(message);
            System.out.println("✅ QR Code email sent successfully to " + toEmail);
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send QR Code email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}