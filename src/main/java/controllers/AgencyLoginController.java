package controllers;

import entities.Agency;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.AgencyService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * AgencyLoginController — handles agency authentication.
 *
 * Flow:
 *  1. Agency opens agency_login.fxml (launched from a separate entry point
 *     or a "Agency Login" button in the main app).
 *  2. Staff enters email + password → onLogin() called.
 *  3. AgencyService.login() queries DB.
 *  4. On success:  saves agency to AgencySession, loads agency_dashboard.fxml,
 *                  replaces the current stage scene.
 *  5. On failure:  shows inline error label — no popups, no page reload.
 */
public class AgencyLoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginBtn;

    private final AgencyService agencyService = new AgencyService();

    @FXML
    public void initialize() {
        // Register scene with ThemeManager once attached
        emailField.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) ThemeManager.get().register(n);
        });
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    @FXML
    private void onLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");
        hideError();

        // Run DB call off the UI thread
        new Thread(() -> {
            try {
                Agency agency = agencyService.login(email, password);
                javafx.application.Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Sign In");
                    if (agency != null) {
                        onLoginSuccess(agency);
                    } else {
                        showError("Incorrect email or password. Please try again.");
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });
            } catch (SQLException e) {
                javafx.application.Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Sign In");
                    showError("Database error: " + e.getMessage());
                });
            }
        }).start();
    }

    // ── Post-login scene transition ───────────────────────────────────────────
    private void onLoginSuccess(Agency agency) {
        // Persist agency in session singleton
        AgencySession.get().setAgency(agency);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/agency_dashboard.fxml"));
            Parent root = loader.load();

            // The dashboard reads from AgencySession — no need to pass agency manually
            AgencyDashboardController ctrl = loader.getController();
            ctrl.applySession();     // reads AgencySession internally

            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            ThemeManager.get().register(scene);
            stage.setScene(scene);
            stage.setTitle("Rehletna — Agency Dashboard · " + agency.getName());

        } catch (IOException e) {
            showError("Could not load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}