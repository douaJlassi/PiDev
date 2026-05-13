package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.YearMonth;
import java.util.function.Consumer;

public class PaiementController {

    // ── Amount ──
    @FXML private Label amountValueLabel;

    // ── Card visual ──
    @FXML private VBox  cardFront;
    @FXML private VBox  cardBack;
    @FXML private Label cardNumberDisplay;
    @FXML private Label cardHolderDisplay;
    @FXML private Label cardExpiryDisplay;
    @FXML private Label cvvDisplay;
    @FXML private HBox  cardTypeBadge;

    // ── Fields ──
    @FXML private TextField    cardNumberField;
    @FXML private TextField    cardHolderField;
    @FXML private TextField    cardExpiryField;
    @FXML private PasswordField cardCVVField;

    // ── Validation groups ──
    @FXML private VBox  groupCardNumber;
    @FXML private VBox  groupCardHolder;
    @FXML private VBox  groupExpiry;
    @FXML private VBox  groupCVV;

    @FXML private Label errorCardNumber;
    @FXML private Label errorCardHolder;
    @FXML private Label errorExpiry;
    @FXML private Label errorCVV;

    @FXML private Button payButton;
    @FXML private Button cancelButton;

    private Stage stage;
    private double montant;
    private Consumer<Boolean> onResult;

    public void setStage(Stage stage)              { this.stage = stage; }
    public void setOnResult(Consumer<Boolean> cb)  { this.onResult = cb; }

    public void setMontant(double montant) {
        this.montant = montant;
        amountValueLabel.setText(String.format("%.0f", montant));
    }
    @FXML
    public void initialize() {
        bindCardNumberField();
        bindCardHolderField();
        bindCardExpiryField();
        bindCVVField();
    }

    private void bindCardNumberField() {
        cardNumberField.textProperty().addListener((obs, old, raw) -> {
            // Keep only digits, max 16
            String digits = raw.replaceAll("[^\\d]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append("  ");
                formatted.append(digits.charAt(i));
            }

            if (!cardNumberField.getText().equals(formatted.toString())) {
                cardNumberField.setText(formatted.toString());
                cardNumberField.positionCaret(formatted.length());
            }

            String padded = (digits + "••••••••••••••••").substring(0, 16);
            cardNumberDisplay.setText(
                    padded.substring(0,4) + " " +
                            padded.substring(4,8) + " " +
                            padded.substring(8,12) + " " +
                            padded.substring(12,16)
            );

            cardTypeBadge.setVisible(digits.length() > 0);
            cardTypeBadge.setManaged(digits.length() > 0);
            validateAll();
        });
    }

    private void bindCardHolderField() {
        cardHolderField.textProperty().addListener((obs, old, val) -> {
            String upper = val.toUpperCase();
            if (!cardHolderField.getText().equals(upper)) {
                cardHolderField.setText(upper);
                cardHolderField.positionCaret(upper.length());
            }
            cardHolderDisplay.setText(upper.isBlank() ? "VOTRE NOM" : upper);
            validateAll();
        });
    }

    private void bindCardExpiryField() {
        cardExpiryField.textProperty().addListener((obs, old, raw) -> {
            String digits = raw.replaceAll("[^\\d]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            String formatted = digits.length() >= 2
                    ? digits.substring(0, 2) + "/" + digits.substring(2)
                    : digits;
            if (!cardExpiryField.getText().equals(formatted)) {
                cardExpiryField.setText(formatted);
                cardExpiryField.positionCaret(formatted.length());
            }
            cardExpiryDisplay.setText(formatted.isEmpty() ? "MM/AA" : formatted);
            validateAll();
        });
    }

    private void bindCVVField() {
        // Show back of card on focus
        cardCVVField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            cardFront.setVisible(!isFocused);
            cardFront.setManaged(!isFocused);
            cardBack.setVisible(isFocused);
            cardBack.setManaged(isFocused);
        });

        cardCVVField.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("[^\\d]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            if (!cardCVVField.getText().equals(digits)) {
                cardCVVField.setText(digits);
            }
            cvvDisplay.setText(digits.isEmpty() ? "•••" : "•".repeat(digits.length()));
            validateAll();
        });
    }
    private void validateAll() {
        boolean numOk    = validateCardNumber();
        boolean holderOk = validateCardHolder();
        boolean expiryOk = validateExpiry();
        boolean cvvOk    = validateCVV();

        payButton.setDisable(!(numOk && holderOk && expiryOk && cvvOk));
    }

    private boolean validateCardNumber() {
        String digits = cardNumberField.getText().replaceAll("[^\\d]", "");
        // Only validate when fully entered (16 digits)
        if (digits.length() == 0) {
            setFieldState(groupCardNumber, errorCardNumber, true, false);
            return false;
        }
        if (digits.length() < 16) {
            // Still typing — no error shown yet
            setFieldState(groupCardNumber, errorCardNumber, true, false);
            return false;
        }
        boolean ok = luhnCheck(digits);
        setFieldState(groupCardNumber, errorCardNumber, ok, true);
        return ok;
    }

    private boolean validateCardHolder() {
        String v = cardHolderField.getText().trim();
        if (v.isEmpty()) {
            setFieldState(groupCardHolder, errorCardHolder, true, false);
            return false;
        }
        boolean ok = v.length() >= 2;
        setFieldState(groupCardHolder, errorCardHolder, ok, true);
        return ok;
    }

    private boolean validateExpiry() {
        String v = cardExpiryField.getText();
        if (v.isEmpty()) {
            setFieldState(groupExpiry, errorExpiry, true, false);
            return false;
        }
        // Only validate once MM/AA is fully typed (5 chars)
        if (v.length() < 5) {
            setFieldState(groupExpiry, errorExpiry, true, false);
            return false;
        }
        if (!v.matches("\\d{2}/\\d{2}")) {
            setFieldState(groupExpiry, errorExpiry, false, true);
            return false;
        }
        int mm = Integer.parseInt(v.substring(0, 2));
        int yy = Integer.parseInt(v.substring(3));
        if (mm < 1 || mm > 12) {
            setFieldState(groupExpiry, errorExpiry, false, true);
            return false;
        }
        YearMonth exp = YearMonth.of(2000 + yy, mm);
        boolean ok = !exp.isBefore(YearMonth.now());
        setFieldState(groupExpiry, errorExpiry, ok, true);
        return ok;
    }

    private boolean validateCVV() {
        String v = cardCVVField.getText();
        if (v.isEmpty()) {
            setFieldState(groupCVV, errorCVV, true, false);
            return false;
        }
        if (v.length() < 3) {
            // Still typing — no error yet
            setFieldState(groupCVV, errorCVV, true, false);
            return false;
        }
        setFieldState(groupCVV, errorCVV, true, true);
        return true;
    }

    private void setFieldState(VBox group, Label error, boolean ok, boolean dirty) {
        group.getStyleClass().removeAll("valid", "invalid");
        error.setVisible(false);
        error.setManaged(false);
        if (dirty) {
            group.getStyleClass().add(ok ? "valid" : "invalid");
            if (!ok) {
                error.setVisible(true);
                error.setManaged(true);
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Luhn algorithm
    // ─────────────────────────────────────────────
    private boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));
            if (alternate) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    @FXML
    private void handlePay() {
        payButton.setDisable(true);
        payButton.setText("⏳  Traitement...");
        new Thread(() -> simulatePayment()).start();

    }

    @FXML
    private void handleCancel() {
        if (onResult != null) onResult.accept(false);
        if (stage != null) stage.close();
    }

    private void simulatePayment() {
        try { Thread.sleep(1800); } catch (InterruptedException ignored) {}
        Platform.runLater(() -> onPaymentSuccess());
    }

    // ─────────────────────────────────────────────
    //  Option B — Real Stripe PaymentIntent API
    //  Requires: stripe-java library in pom.xml
    //  Add: <dependency>
    //         <groupId>com.stripe</groupId>
    //         <artifactId>stripe-java</artifactId>
    //         <version>25.3.0</version>
    //       </dependency>
    // ─────────────────────────────────────────────
    private void processWithStripe() {
        try {
            com.stripe.Stripe.apiKey = StripeConfig.SECRET_KEY;

            java.util.Map<String, Object> cardParams = new java.util.HashMap<>();
            cardParams.put("number",    cardNumberField.getText().replaceAll("\\s", ""));
            cardParams.put("exp_month", cardExpiryField.getText().substring(0, 2));
            cardParams.put("exp_year",  "20" + cardExpiryField.getText().substring(3));
            cardParams.put("cvc",       cardCVVField.getText());

            java.util.Map<String, Object> pmParams = new java.util.HashMap<>();
            pmParams.put("type", "card");
            pmParams.put("card", cardParams);

            com.stripe.model.PaymentMethod pm =
                    com.stripe.model.PaymentMethod.create(pmParams);

            java.util.Map<String, Object> piParams = new java.util.HashMap<>();
            piParams.put("amount",   (long)(montant * 100)); // centimes
            piParams.put("currency", "tnd");
            piParams.put("payment_method", pm.getId());
            piParams.put("confirm", true);
            piParams.put("return_url", "https://your-app.com/return");

            com.stripe.model.PaymentIntent intent =
                    com.stripe.model.PaymentIntent.create(piParams);

            if ("succeeded".equals(intent.getStatus())) {
                Platform.runLater(this::onPaymentSuccess);
            } else {
                Platform.runLater(() -> onPaymentError("Statut inattendu: " + intent.getStatus()));
            }

        } catch (com.stripe.exception.CardException e) {
            Platform.runLater(() -> onPaymentError("Carte refusée: " + e.getUserMessage()));
        } catch (Exception e) {
            Platform.runLater(() -> onPaymentError("Erreur: " + e.getMessage()));
        }
    }

    private void onPaymentSuccess() {
        payButton.setText("✓  Paiement accepté !");
        payButton.getStyleClass().add("pay-btn-success");

        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> {
            if (onResult != null) onResult.accept(true);
            if (stage != null) stage.close();
        });
        pause.play();
    }

    private void onPaymentError(String message) {
        payButton.setText("PAYER MAINTENANT");
        payButton.setDisable(false);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Paiement échoué");
        alert.showAndWait();
    }
}