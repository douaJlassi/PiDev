package Controllers;

import entities.Publication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.PublicationService;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AgencyDashboardController — back-office moderation screen.
 *
 * Agency staff see every post submitted to their agency, grouped by status.
 * They can APPROVE or REJECT pending posts with one click.
 *
 * reads the logged-in agency from AgencySession after login.
 * — in a real project these come from the session after agency login.
 */
public class AgencyDashboardController {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label        agencyNameLabel;
    @FXML private Button       themeToggleBtn;
    @FXML private VBox         submissionList;
    @FXML private Label        pendingCount;
    @FXML private Label        approvedCount;
    @FXML private Label        rejectedCount;
    @FXML private ToggleButton pendingTab;
    @FXML private ToggleButton approvedTab;
    @FXML private ToggleButton rejectedTab;
    @FXML private ToggleButton allTab;
    @FXML private ToggleGroup  filterGroup;

    // ── State ─────────────────────────────────────────────────────────────────
    private entities.Agency    agency;   // set from AgencySession after login
    private PublicationService service;
    private List<Publication>  allPosts;

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        service = new PublicationService();

        // Register with ThemeManager
        submissionList.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) ThemeManager.get().register(n);
            if (o != null) ThemeManager.get().unregister(o);
        });
        // Data loaded by applySession() after login injects the agency
    }

    /**
     * Called by AgencyLoginController immediately after FXML load.
     * Reads the logged-in agency from AgencySession and starts loading data.
     */
    public void applySession() {
        this.agency = AgencySession.get().getAgency();
        if (agency == null) throw new IllegalStateException("No agency in session");
        agencyNameLabel.setText(agency.getName());
        loadSubmissions();
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    @FXML private void onRefresh() { loadSubmissions(); }

    private void loadSubmissions() {
        submissionList.getChildren().clear();
        Label loading = placeholder("Loading submissions...");
        submissionList.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<Publication> posts = service.selectByAgency(agency.getAgencyID());
                allPosts = posts;
                Platform.runLater(() -> {
                    updateStats(posts);
                    renderFiltered();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showErr("Failed to load: " + e.getMessage()));
            }
        }).start();
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    @FXML private void onFilterChanged() { renderFiltered(); }

    private void renderFiltered() {
        if (allPosts == null) return;
        ToggleButton sel = (ToggleButton) filterGroup.getSelectedToggle();
        List<Publication> list;
        if      (sel == pendingTab)  list = byStatus(Publication.Status.PENDING);
        else if (sel == approvedTab) list = byStatus(Publication.Status.APPROVED);
        else if (sel == rejectedTab) list = byStatus(Publication.Status.REJECTED);
        else                         list = allPosts;

        submissionList.getChildren().clear();
        if (list.isEmpty()) {
            submissionList.getChildren().add(placeholder("No submissions here."));
        } else {
            list.forEach(p -> submissionList.getChildren().add(buildCard(p)));
        }
    }

    private List<Publication> byStatus(Publication.Status s) {
        return allPosts.stream().filter(p -> p.getStatus() == s).collect(Collectors.toList());
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private void updateStats(List<Publication> posts) {
        pendingCount.setText(String.valueOf(posts.stream().filter(Publication::isPending).count()));
        approvedCount.setText(String.valueOf(posts.stream().filter(Publication::isApproved).count()));
        rejectedCount.setText(String.valueOf(posts.stream().filter(Publication::isRejected).count()));
    }

    // ── Submission card ───────────────────────────────────────────────────────
    private VBox buildCard(Publication pub) {
        VBox card = new VBox(12);
        card.getStyleClass().add("agency-submission-card");

        // Header: avatar + author info + status badge
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Region avatar = new Region();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(36, 36); avatar.setMinSize(36, 36); avatar.setMaxSize(36, 36);

        VBox info = new VBox(2);
        String uname = pub.getClient().getUsername() != null
                ? pub.getClient().getUsername()
                : "Traveler #" + pub.getClient().getClientID();
        Label name = new Label(uname);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #e8eaf0;");
        Label date = new Label(DATE_FMT.format(pub.getDatePublication()));
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b90a7;");
        info.getChildren().addAll(name, date);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(avatar, info, spacer, statusBadge(pub.getStatus()));

        // Content
        Label content = new Label(pub.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 14px; -fx-text-fill: #c8cad8; -fx-line-spacing: 2;");

        card.getChildren().addAll(header, content);

        // Place tag
        if (pub.hasPlace()) {
            Label place = new Label("📍 " + pub.getPlace());
            place.getStyleClass().add("post-place-tag");
            card.getChildren().add(place);
        }

        // Approve / Reject buttons — only for PENDING posts
        if (pub.isPending()) {
            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));

            Button reject = new Button("Reject");
            reject.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #e74c3c; " +
                            "-fx-border-color: rgba(231,76,60,0.5); -fx-border-radius: 6; -fx-border-width: 1; " +
                            "-fx-padding: 7 20; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: 600;");
            reject.setOnMouseEntered(e -> reject.setStyle(reject.getStyle() + "-fx-background-color: rgba(231,76,60,0.08);"));
            reject.setOnMouseExited(e -> reject.setStyle(reject.getStyle().replace("-fx-background-color: rgba(231,76,60,0.08);", "")));
            reject.setOnAction(e -> moderate(pub, Publication.Status.REJECTED, card));

            Button approve = new Button("Approve");
            approve.setStyle(
                    "-fx-background-color: #17B3A6; -fx-text-fill: white; " +
                            "-fx-padding: 7 20; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: 600;");
            approve.setOnMouseEntered(e -> approve.setStyle(approve.getStyle() + "-fx-background-color: #0D8F85;"));
            approve.setOnMouseExited(e -> approve.setStyle(approve.getStyle().replace("-fx-background-color: #0D8F85;", "")));
            approve.setOnAction(e -> moderate(pub, Publication.Status.APPROVED, card));

            actions.getChildren().addAll(reject, approve);
            card.getChildren().add(actions);
        }

        return card;
    }

    private Label statusBadge(Publication.Status status) {
        Label badge = new Label(status.name());
        String bg, fg;
        if      (status == Publication.Status.PENDING)  { bg = "rgba(255,193,7,0.15)";  fg = "#FFC107"; }
        else if (status == Publication.Status.APPROVED) { bg = "rgba(23,179,166,0.15)"; fg = "#17B3A6"; }
        else                                            { bg = "rgba(231,76,60,0.15)";  fg = "#e74c3c"; }

        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-padding: 4 10; -fx-background-radius: 20;");
        return badge;
    }

    // ── Moderate action ───────────────────────────────────────────────────────
    private void moderate(Publication pub, Publication.Status newStatus, VBox card) {
        try {
            service.updateStatus(pub.getPublicationID(), newStatus);
            pub.setStatus(newStatus);
            // Replace card in-place with updated version
            int idx = submissionList.getChildren().indexOf(card);
            if (idx >= 0) submissionList.getChildren().set(idx, buildCard(pub));
            updateStats(allPosts);
        } catch (SQLException e) {
            showErr("Failed to update: " + e.getMessage());
        }
    }

    // ── Theme toggle ──────────────────────────────────────────────────────────
    @FXML
    private void toggleTheme() {
        ThemeManager.get().toggle();
        if (themeToggleBtn != null)
            themeToggleBtn.setText(ThemeManager.get().isDark() ? "Light Mode" : "Dark Mode");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Label placeholder(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #8b90a7; -fx-font-size: 14px; -fx-padding: 32 0;");
        return l;
    }

    private void showErr(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg);
        ThemeManager.get().applyToPane(a.getDialogPane());
        a.showAndWait();
    }
}