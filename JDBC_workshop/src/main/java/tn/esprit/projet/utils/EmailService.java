package tn.esprit.projet.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "becharguiaw@gmail.com"; // Replace with your email
    private static final String EMAIL_PASSWORD = "jlphxtjobtinubpj"; // Replace with your app password

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
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <h1 style="color: #1D4D7C; margin: 0;">rehletna.tn</h1>
                            <p style="color: #666; margin: 5px 0;">Two-Factor Authentication</p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <h2 style="color: #0FA5A2; font-size: 48px; letter-spacing: 5px; margin: 0;">%s</h2>
                            <p style="color: #666; margin-top: 10px;">Enter this code to complete your login</p>
                        </div>
                        
                        <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;">
                            <p style="color: #666; margin: 0; font-size: 14px;">
                                This code will expire in 5 minutes. If you didn't request this code, please ignore this email or contact support.
                            </p>
                        </div>
                        
                        <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                            <p style="color: #999; font-size: 12px;">
                                &copy; 2024 rehletna.tn. All rights reserved.<br>
                                This is an automated message, please do not reply.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(code);

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("2FA code sent to: " + toEmail);
            return true;

        } catch (MessagingException e) {
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
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <h1 style="color: #1D4D7C; margin: 0;">rehletna.tn</h1>
                            <p style="color: #666; margin: 5px 0;">Two-Factor Authentication Activated</p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <div style="font-size: 60px; color: #2ecc71;">✅</div>
                            <h2 style="color: #1D4D7C;">Hello %s!</h2>
                            <p style="color: #666; font-size: 16px;">
                                Two-Factor Authentication has been successfully enabled on your account.
                            </p>
                        </div>
                        
                        <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;">
                            <p style="color: #666; margin: 0; font-size: 14px;">
                                <strong>What's next?</strong><br>
                                • From now on, you'll need to enter a verification code sent to this email every time you log in.<br>
                                • Keep your email account secure.<br>
                                • If you didn't enable this, please contact support immediately.
                            </p>
                        </div>
                        
                        <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                            <p style="color: #999; font-size: 12px;">
                                &copy; 2024 rehletna.tn. All rights reserved.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("2FA activation email sent to: " + toEmail);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}