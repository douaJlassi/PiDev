package controllers;

import entities.Publication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.PublicationService;
import javafx.scene.layout.FlowPane;

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
    @FXML private FlowPane submissionList;  // was VBox
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
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setPrefHeight(280);
        card.getStyleClass().add("agency-submission-card");
        if (pub.isPending())       card.getStyleClass().add("card-pending");
        else if (pub.isApproved()) card.getStyleClass().add("card-approved");
        else                       card.getStyleClass().add("card-rejected");

        // ── Status badge top-right ────────────────────────────────────────────
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_RIGHT);
        topRow.getChildren().add(statusBadge(pub.getStatus()));

        // ── Author row — real avatar ──────────────────────────────────────────
        HBox authorRow = new HBox(10);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        // Avatar: try to load image, fall back to teal circle with initial
        StackPane avatarStack = new StackPane();
        avatarStack.setPrefSize(36, 36);
        avatarStack.setMinSize(36, 36);
        avatarStack.setMaxSize(36, 36);

        // Base circle (always shown as background)
        javafx.scene.shape.Circle avatarCircle = new javafx.scene.shape.Circle(18);
        avatarCircle.getStyleClass().add("avatar");

        String avatarPath = pub.getClient().getAvatarPath();
        boolean avatarLoaded = false;

        if (avatarPath != null && !avatarPath.isBlank()) {
            try {
                javafx.scene.image.Image img;
                if (avatarPath.startsWith("http")) {
                    img = new javafx.scene.image.Image(avatarPath, 36, 36, true, true, true);
                } else {
                    java.io.File f = new java.io.File(avatarPath);
                    if (f.exists()) {
                        img = new javafx.scene.image.Image(f.toURI().toString(), 36, 36, true, true);
                    } else {
                        img = null;
                    }
                }
                if (img != null && !img.isError()) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                    iv.setFitWidth(36); iv.setFitHeight(36);
                    iv.setPreserveRatio(true);
                    // Clip to circle
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                    iv.setClip(clip);
                    avatarStack.getChildren().addAll(avatarCircle, iv);
                    avatarLoaded = true;
                }
            } catch (Exception ignored) {}
        }

        if (!avatarLoaded) {
            // Fallback: circle + first letter of username
            String uname = pub.getClient().getUsername();
            String initial = (uname != null && !uname.isEmpty())
                    ? String.valueOf(uname.charAt(0)).toUpperCase() : "?";
            Label initLabel = new Label(initial);
            initLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
            avatarStack.getChildren().addAll(avatarCircle, initLabel);
        }

        // Author name + date
        VBox info = new VBox(2);
        String uname = pub.getClient().getUsername() != null
                ? pub.getClient().getUsername()
                : "Traveler #" + pub.getClient().getClientID();
        Label name = new Label(uname);
        name.getStyleClass().add("card-author-name");
        Label date = new Label(DATE_FMT.format(pub.getDatePublication()));
        date.getStyleClass().add("card-author-date");
        info.getChildren().addAll(name, date);

        authorRow.getChildren().addAll(avatarStack, info);

        // ── Content ───────────────────────────────────────────────────────────
        Label content = new Label(pub.getContent());
        content.setWrapText(true);
        content.setPrefHeight(110);
        content.setMaxHeight(110);
        content.getStyleClass().add("card-content-text");
        VBox.setVgrow(content, Priority.ALWAYS);

        card.getChildren().addAll(topRow, authorRow, content);

        // ── Place tag ─────────────────────────────────────────────────────────
        if (pub.hasPlace()) {
            Label place = new Label("📍 " + pub.getPlace());
            place.getStyleClass().add("post-place-tag");
            card.getChildren().add(place);
        }

        // ── Approve / Reject — pending only ───────────────────────────────────
        if (pub.isPending()) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));

            Button reject = new Button("Reject");
            reject.getStyleClass().add("danger-btn");
            reject.setOnAction(e -> moderate(pub, Publication.Status.REJECTED, card));

            Button approve = new Button("Approve");
            approve.getStyleClass().add("primary-btn");
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
        l.getStyleClass().add("agency-placeholder");
        return l;
    }

    private void showErr(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg);
        ThemeManager.get().applyToPane(a.getDialogPane());
        a.showAndWait();
    }
}