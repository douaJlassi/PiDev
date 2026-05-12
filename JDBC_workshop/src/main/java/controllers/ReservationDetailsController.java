package controllers;

import gestion_activite.ReservationDetail;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.AchatService;
import utils.SessionManager;
import entities.Person;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.List;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReservationDetailsController {

    @FXML
    private TableView<ReservationDetail> reservationsTable;
    @FXML
    private TableColumn<ReservationDetail, String> colNom;
    @FXML
    private TableColumn<ReservationDetail, String> colEmail;
    @FXML
    private TableColumn<ReservationDetail, String> colTelephone;
    @FXML
    private TableColumn<ReservationDetail, Integer> colNbPlaces;
    @FXML
    private TableColumn<ReservationDetail, Double> colMontant;
    @FXML
    private Label totalReservationsLabel;
    @FXML
    private Label activityTitleLabel;
    @FXML
    private Label welcomeUserLabel; // Add this to show welcome message
    @FXML
    private ProgressIndicator loadingIndicator;

    private ObservableList<ReservationDetail> reservations = FXCollections.observableArrayList();
    private AchatService achatService = new AchatService();

    // Current logged-in user
    private Person currentUser;

    // Data identifiers for refresh
    private Integer currentActivityId = null;
    private Integer currentClientId = null;
    private Integer currentGuideId = null;
    private String currentViewType = "client"; // Default to client view for users

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("clientNom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("clientEmail"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("clientTelephone"));
        colNbPlaces.setCellValueFactory(new PropertyValueFactory<>("nbPlaces"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantPaye"));

        // Format the montant column to show DT
        colMontant.setCellFactory(column -> new TableCell<ReservationDetail, Double>() {
            @Override
            protected void updateItem(Double montant, boolean empty) {
                super.updateItem(montant, empty);
                if (empty || montant == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DT", montant));
                }
            }
        });

        reservationsTable.setItems(reservations);

        // Load current user from session
        loadCurrentUser();
    }

    /**
     * Load the currently logged-in user from session
     */
    private void loadCurrentUser() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            System.out.println("ReservationDetailsController: Current user loaded - " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

            // Set client ID to current user's ID
            this.currentClientId = currentUser.getId();
            this.currentViewType = "client";

            // Update welcome label if it exists
            if (welcomeUserLabel != null) {
                String role = currentUser.getRole();
                if (role != null && role.toLowerCase().contains("admin")) {
                    welcomeUserLabel.setText("Bienvenue Administrateur " + currentUser.getUsername());
                } else if (role != null && role.toLowerCase().contains("guide")) {
                    welcomeUserLabel.setText("Bienvenue Guide " + currentUser.getUsername());
                } else {
                    welcomeUserLabel.setText("Bienvenue " + currentUser.getUsername());
                }
            }

            // Automatically load user's reservations
            loadUserReservations();
        } else {
            System.err.println("ReservationDetailsController: No user logged in!");
            if (welcomeUserLabel != null) {
                welcomeUserLabel.setText("Veuillez vous connecter");
            }
        }
    }

    /**
     * Load reservations for the current user
     */
    private void loadUserReservations() {
        if (currentUser == null) return;

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        try {
            // Check user role to determine what to load
            String role = currentUser.getRole();

            if (role != null && role.toLowerCase().contains("guide")) {
                // If user is a guide, load reservations for their activities
                List<ReservationDetail> details = achatService.getReservationDetailsByGuide(currentUser.getId());
                setReservationsForGuide(details, currentUser.getId());
            } else {
                // Regular user or admin - load their personal reservations
                List<ReservationDetail> details = achatService.getReservationDetailsByClient(currentUser.getId());
                setReservationsForClient(details, currentUser.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                showAlert("Erreur", "Impossible de charger vos réservations: " + e.getMessage());
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
            });
        }
    }

    /**
     * Set current user manually (if not using session)
     */
    public void setCurrentUser(Person user) {
        this.currentUser = user;
        if (user != null) {
            this.currentClientId = user.getId();
            System.out.println("ReservationDetailsController: User set manually - " + user.getUsername());
            loadUserReservations();
        }
    }

    /**
     * Set reservations for a specific activity (for guides)
     */
    public void setReservationsForActivity(List<ReservationDetail> details, int activityId, String activityTitle) {
        this.currentActivityId = activityId;
        this.currentViewType = "activity";

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        final String title = activityTitle;

        Platform.runLater(() -> {
            reservations.setAll(details);
            if (activityTitleLabel != null) {
                activityTitleLabel.setText("Réservations pour : " + title);
            }
            updateTotalLabel();
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
        });
    }

    /**
     * Set reservations for a specific client (for users)
     */
    public void setReservationsForClient(List<ReservationDetail> details, int clientId) {
        this.currentClientId = clientId;
        this.currentViewType = "client";

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        Platform.runLater(() -> {
            reservations.setAll(details);
            if (activityTitleLabel != null) {
                activityTitleLabel.setText("Mes réservations");
            }
            updateTotalLabel();
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
        });
    }

    /**
     * Set reservations for a specific guide (for admin/overview)
     */
    public void setReservationsForGuide(List<ReservationDetail> details, int guideId) {
        this.currentGuideId = guideId;
        this.currentViewType = "guide";

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        Platform.runLater(() -> {
            reservations.setAll(details);
            if (activityTitleLabel != null) {
                activityTitleLabel.setText("Toutes les réservations du guide");
            }
            updateTotalLabel();
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
        });
    }

    /**
     * Set guide ID (legacy method)
     */
    public void setGuideId(int guideId) {
        this.currentGuideId = guideId;
        this.currentViewType = "guide";
    }

    /**
     * Set activity title (legacy method)
     */
    public void setActivityTitle(String title) {
        if (activityTitleLabel != null) {
            activityTitleLabel.setText("Réservations pour : " + title);
        }
    }

    /**
     * Legacy method - kept for backward compatibility
     */
    public void setReservations(List<ReservationDetail> details) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        Platform.runLater(() -> {
            reservations.setAll(details);
            updateTotalLabel();
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
        });
    }

    /**
     * Legacy method - kept for backward compatibility
     */
    public void setReservationsWithActivity(List<ReservationDetail> details, String activityTitle) {
        reservations.setAll(details);
        if (activityTitleLabel != null) {
            activityTitleLabel.setText("Réservations pour : " + activityTitle);
        }
        updateTotalLabel();
    }

    private void updateTotalLabel() {
        int totalPlaces = reservations.stream().mapToInt(ReservationDetail::getNbPlaces).sum();
        double totalMontant = reservations.stream().mapToDouble(ReservationDetail::getMontantPaye).sum();

        totalReservationsLabel.setText(
                String.format("%d réservation(s) · %d places · Total: %.2f DT",
                        reservations.size(), totalPlaces, totalMontant)
        );
    }

    @FXML
    private void handleClose() {
        ((Stage) reservationsTable.getScene().getWindow()).close();
    }

    @FXML
    private void handleRefresh() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        // Create local copies for lambda
        final String viewType = this.currentViewType;
        final Integer activityId = this.currentActivityId;
        final Integer clientId = this.currentClientId;
        final Integer guideId = this.currentGuideId;

        try {
            if ("activity".equals(viewType) && activityId != null) {
                // Refresh for a specific activity
                List<ReservationDetail> refreshedDetails = achatService.getReservationDetailsByActivite(activityId);

                Platform.runLater(() -> {
                    reservations.setAll(refreshedDetails);
                    updateTotalLabel();
                    showInfo("Succès", "Réservations mises à jour pour l'activité");
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                });

            } else if ("client".equals(viewType) && clientId != null) {
                // Refresh for a specific client
                List<ReservationDetail> refreshedDetails = achatService.getReservationDetailsByClient(clientId);

                Platform.runLater(() -> {
                    reservations.setAll(refreshedDetails);
                    updateTotalLabel();
                    showInfo("Succès", "Vos réservations ont été mises à jour");
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                });

            } else if ("guide".equals(viewType) && guideId != null) {
                // Refresh for a specific guide
                List<ReservationDetail> refreshedDetails = achatService.getReservationDetailsByGuide(guideId);

                Platform.runLater(() -> {
                    reservations.setAll(refreshedDetails);
                    updateTotalLabel();
                    showInfo("Succès", "Réservations du guide mises à jour");
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                });
            } else {
                // No refresh possible, just update the label
                Platform.runLater(() -> {
                    updateTotalLabel();
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                showAlert("Erreur", "Impossible de rafraîchir les données: " + e.getMessage());
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
            });
        }
    }

    @FXML
    private void handleExportPDF() {
        if (reservations.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucune réservation à exporter.", ButtonType.OK).show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        // Suggest a filename with date
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String username = (currentUser != null) ? currentUser.getUsername() : "user";
        fileChooser.setInitialFileName("reservations_" + username + "_" + timestamp + ".pdf");

        File file = fileChooser.showSaveDialog(reservationsTable.getScene().getWindow());
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(64, 64, 64));
            Paragraph title = new Paragraph("Détails des Réservations", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // User info
            if (currentUser != null) {
                Font userFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(100, 100, 100));
                Paragraph userInfo = new Paragraph("Client: " + currentUser.getUsername() + " (" + currentUser.getEmail() + ")", userFont);
                userInfo.setAlignment(Element.ALIGN_CENTER);
                userInfo.setSpacingAfter(5);
                document.add(userInfo);
            }

            // Activity title if available
            if (activityTitleLabel != null && !activityTitleLabel.getText().isEmpty()) {
                Font activityFont = FontFactory.getFont(FontFactory.HELVETICA, 14, new Color(100, 100, 100));
                Paragraph activity = new Paragraph(activityTitleLabel.getText().replace("Réservations pour : ", ""), activityFont);
                activity.setAlignment(Element.ALIGN_CENTER);
                activity.setSpacingAfter(20);
                document.add(activity);
            }

            // Date d'export
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(128, 128, 128));
            String dateStr = "Exporté le " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            Paragraph datePar = new Paragraph(dateStr, dateFont);
            datePar.setAlignment(Element.ALIGN_RIGHT);
            datePar.setSpacingAfter(20);
            document.add(datePar);

            // Create table with 5 columns
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 3, 2, 1, 2});

            // En‑tête
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Color headerBg = new Color(15, 165, 162);

            String[] headers = {"Nom du client", "Email", "Téléphone", "Places", "Montant (DT)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                cell.setBorderColor(Color.WHITE);
                table.addCell(cell);
            }

            // Données
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Color evenBg = new Color(245, 245, 245);
            int row = 0;
            double totalMontant = 0;
            int totalPlaces = 0;

            for (ReservationDetail d : reservations) {
                Color bg = (row % 2 == 0) ? Color.WHITE : evenBg;

                // Nom
                PdfPCell cell = new PdfPCell(new Phrase(d.getClientNom(), cellFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(6);
                table.addCell(cell);

                // Email
                cell = new PdfPCell(new Phrase(d.getClientEmail(), cellFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(6);
                table.addCell(cell);

                // Téléphone
                String telephone = d.getClientTelephone();
                if (telephone == null || telephone.isEmpty() || "null".equals(telephone)) {
                    telephone = "Non renseigné";
                }
                cell = new PdfPCell(new Phrase(telephone, cellFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(6);
                table.addCell(cell);

                // Places
                cell = new PdfPCell(new Phrase(String.valueOf(d.getNbPlaces()), cellFont));
                cell.setBackgroundColor(bg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);

                // Montant
                cell = new PdfPCell(new Phrase(String.format("%.2f DT", d.getMontantPaye()), cellFont));
                cell.setBackgroundColor(bg);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell.setPadding(6);
                table.addCell(cell);

                totalMontant += d.getMontantPaye();
                totalPlaces += d.getNbPlaces();
                row++;
            }

            document.add(table);

            // Totaux
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 165, 162));
            Paragraph total = new Paragraph(
                    String.format("Récapitulatif : %d réservation(s) · %d place(s) · Montant total : %.2f DT",
                            reservations.size(), totalPlaces, totalMontant),
                    totalFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(20);
            document.add(total);

            document.close();

            showInfo("Succès", "PDF exporté avec succès : " + file.getName());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'export PDF: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}