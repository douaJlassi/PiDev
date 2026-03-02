package Services;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OffreEmailService {
private String fromEmail="";
private String apiKey = "";
    private static final String FROM_EMAIL = "oueslati.sahar.11@gmail.com";
    private static final String APP_PASSWORD = "zpqu estp hoqn glcp";
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    private final String fromName  = "Rehletna";

    public void send(String toEmail, String subject, String textBody) {
        if (apiKey == null || apiKey.isBlank())
            throw new RuntimeException("Missing env var: MAILERSEND_API_KEY");
        if (fromEmail == null || fromEmail.isBlank())
            throw new RuntimeException("Missing env var: MAILERSEND_FROM_EMAIL");

        try {
            String payload = buildJson(toEmail, subject, textBody);

            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.mailersend.com/v1/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + res.statusCode() + " -> " + res.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("MailerSend API failed: " + e.getMessage(), e);
        }
    }

    private String buildJson(String toEmail, String subject, String textBody) {
        String fn = (fromName == null || fromName.isBlank()) ? "Rehletna" : fromName;

        return "{"
                + "\"from\":{\"email\":\"" + esc(fromEmail) + "\",\"name\":\"" + esc(fn) + "\"},"
                + "\"to\":[{\"email\":\"" + esc(toEmail) + "\",\"name\":\"Client\"}],"
                + "\"subject\":\"" + esc(subject) + "\","
                + "\"text\":\"" + esc(textBody) + "\""
                + "}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

}