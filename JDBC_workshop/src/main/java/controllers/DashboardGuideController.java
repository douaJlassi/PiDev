package controllers;

import services.WeatherService;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.animation.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.io.File;
import java.util.stream.Collectors;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

import gestion_activite.Activite;
import gestion_activite.ReservationDetail;
import services.ActiviteService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.WeatherServiceActi;
import utils.EventBus;
import utils.MyDBConnexion;
import utils.SessionManager;
import entities.Person;

public class DashboardGuideController {

    // ===== Cartes de statistiques (ajout des fx:id) =====
    @FXML private VBox carteTotalActivites;
    @FXML private VBox carteActives;
    @FXML private VBox carteReservations;
    @FXML private VBox carteRevenus;

    @FXML
    private Text userNameText;
    // ===== Composants existants =====
    @FXML private ListView<Activite> activitiesListView;
    @FXML private Label pageInfoLabel;
    @FXML private Label totalActivitesStat;
    @FXML private Label activesStat;
    @FXML private Label reservationsStat;
    @FXML private Label revenusStat;
    @FXML private BorderPane borderPane;
    @FXML private DatePicker dateFilter;
    @FXML private StackPane notificationPane;
    @FXML private Button clearDateFilterButton;


    private ActiviteService activiteService;
    private List<Activite> toutesActivites;
    private List<Activite> filteredActivites;
    private int currentPage = 1;
    private int itemsPerPage = 10000;
    private int totalPages = 1;
    private int guideId = -1; // Will be set from session
    private Person currentUser; // Store the current user
    private WeatherServiceActi weatherServiceActi = new WeatherServiceActi();

    public DashboardGuideController() {
        activiteService = new ActiviteService();
    }


    public void setCurrentUser(Person user) {
        this.currentUser = user;
        if (user != null) {
            this.guideId = user.getId();
            System.out.println("DashboardGuideController: User set - " + user.getUsername() + " (ID: " + guideId + ")");

            // Reload data with the new guide ID
            refreshActivites();
            loadGuideProfile();
        }
    }

    @FXML
    public void initialize() {
        // Get current user from session first
        currentUser = SessionManager.getCurrentUser();

        if (currentUser != null) {
            guideId = currentUser.getId();
            System.out.println("DashboardGuideController initialized with user: " + currentUser.getUsername() + " (ID: " + guideId + ")");
        } else {
            System.err.println("⚠️ No user logged in! Please login first.");
            guideId = -1; // Invalid ID
        }

        // Initialisation des graphiques
        chargerActivites();
        setupListView();
        loadGuideProfile();

        filteredActivites = toutesActivites;

        clearDateFilterButton.setOnAction(e -> clearDateFilter());
        dateFilter.setOnAction(e -> filterByDate());

        // Rendre les cartes de statistiques cliquables
        if (carteTotalActivites != null) {
            carteTotalActivites.setCursor(Cursor.HAND);
            carteTotalActivites.setOnMouseClicked(e -> showActivitesParMois());
        }

        if (carteActives != null) {
            carteActives.setCursor(Cursor.HAND);
            carteActives.setOnMouseClicked(e -> showActivitesParMois());
        }

        if (carteReservations != null) {
            carteReservations.setCursor(Cursor.HAND);
            carteReservations.setOnMouseClicked(e -> showReservationsParMois());
        }

        if (carteRevenus != null) {
            carteRevenus.setCursor(Cursor.HAND);
            carteRevenus.setOnMouseClicked(e -> showRevenusParMois());
        }

        // Abonnement à l'EventBus pour rafraîchissement automatique
        EventBus.getInstance().addListener(() -> {
            javafx.application.Platform.runLater(() -> {
                refreshActivites();
            });
        });
    }
    private void showActivitesParMois() {
        CategoryAxis xAxis = createStyledCategoryAxis("Mois");
        NumberAxis   yAxis = createStyledNumberAxis("Activités");

        AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);

        int year = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        List<String> monthLabels = buildMonthLabels(currentMonth);

        Map<String, Integer> totalMap  = fetchMonthlyCount(
                "SELECT MONTH(dateActivite) as m, COUNT(*) as nb " +
                        "FROM activite WHERE idGuide=? AND YEAR(dateActivite)=? GROUP BY m", year);

        Map<String, Integer> activesMap = fetchMonthlyCount(
                "SELECT MONTH(dateActivite) as m, COUNT(*) as nb " +
                        "FROM activite WHERE idGuide=? AND YEAR(dateActivite)=? AND statut='Actif' GROUP BY m", year);

        XYChart.Series<String, Number> seriesTotal   = new XYChart.Series<>();
        seriesTotal.setName("Total activités");
        XYChart.Series<String, Number> seriesActives = new XYChart.Series<>();
        seriesActives.setName("Actives");

        chart.getData().addAll(seriesTotal, seriesActives);

        String css = """
        .chart-plot-background { -fx-background-color: transparent; }
        .chart-vertical-grid-lines { -fx-stroke: rgba(255,255,255,0.04); }
        .chart-horizontal-grid-lines { -fx-stroke: rgba(255,255,255,0.07); }
        .chart-series-area-line:nth-child(1n+1) { -fx-stroke:#00d4aa; -fx-stroke-width:2.5; }
        .chart-series-area-fill:nth-child(1n+1) { -fx-fill:linear-gradient(to bottom,rgba(0,212,170,0.25),transparent); }
        .chart-series-area-line:nth-child(1n+2) { -fx-stroke:#3b82f6; -fx-stroke-width:2; -fx-stroke-dash-array:7 4; }
        .chart-series-area-fill:nth-child(1n+2) { -fx-fill:linear-gradient(to bottom,rgba(59,130,246,0.15),transparent); }
        .default-color0.chart-symbol { -fx-background-color:#00d4aa,#0e1425; -fx-background-radius:5; -fx-padding:4; }
        .default-color1.chart-symbol { -fx-background-color:#3b82f6,#0e1425; -fx-background-radius:5; -fx-padding:4; }
        """;

        showModernChartWindow(chart,
                "Activités par mois",
                "ANNÉE " + year + "  ·  Jan → " + monthLabels.get(monthLabels.size() - 1).toUpperCase(),
                css, 920, 520,
                () -> revealTwoSeriesMonthByMonth(monthLabels, totalMap, activesMap,
                        seriesTotal, seriesActives, currentMonth));
    }

    private void showReservationsParMois() {
        CategoryAxis xAxis = createStyledCategoryAxis("Mois");
        NumberAxis   yAxis = createStyledNumberAxis("Réservations");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setBarGap(4);
        chart.setCategoryGap(20);

        int year = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        List<String> monthLabels = buildMonthLabels(currentMonth);

        Map<String, Integer> resaMap = fetchMonthlyCount(
                "SELECT MONTH(a.dateAchat) as m, COUNT(a.idAchat) as nb " +
                        "FROM achat a JOIN activite act ON a.idActivite=act.idActivite " +
                        "WHERE act.idGuide=? AND YEAR(a.dateAchat)=? AND a.statut!='Annulé' GROUP BY m", year);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Réservations confirmées");
        chart.getData().add(series);

        String css = """
        .chart-plot-background { -fx-background-color: transparent; }
        .chart-vertical-grid-lines { -fx-stroke: transparent; }
        .chart-horizontal-grid-lines { -fx-stroke: rgba(255,255,255,0.06); }
        .default-color0.chart-bar {
            -fx-background-color: linear-gradient(to top, #4f46e5, #8b5cf6);
            -fx-background-radius: 8 8 3 3;
            -fx-effect: dropshadow(gaussian,rgba(139,92,246,0.45),12,0,0,2);
        }
        .default-color0.chart-bar:hover {
            -fx-background-color: linear-gradient(to top, #6366f1, #a78bfa);
        }
        """;

        showModernChartWindow(chart,
                "Réservations par mois",
                "ANNÉE " + year + "  ·  CONFIRMÉES · HORS ANNULÉES",
                css, 920, 520,
                () -> revealIntSeriesMonthByMonth(monthLabels, resaMap, series, currentMonth));
    }

    private void showRevenusParMois() {
        CategoryAxis xAxis = createStyledCategoryAxis("Mois");
        NumberAxis   yAxis = createStyledNumberAxis("DT");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);

        int year = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        List<String> monthLabels = buildMonthLabels(currentMonth);

        Map<String, Double> revenusMap = fetchMonthlyDouble(
                "SELECT MONTH(a.dateAchat) as m, SUM(a.montantTotal) as val " +
                        "FROM achat a JOIN activite act ON a.idActivite=act.idActivite " +
                        "WHERE act.idGuide=? AND YEAR(a.dateAchat)=? AND a.statut!='Annulé' GROUP BY m", year);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenus (DT)");
        chart.getData().add(series);

        String css = """
        .chart-plot-background { -fx-background-color: transparent; }
        .chart-vertical-grid-lines { -fx-stroke: rgba(255,255,255,0.04); }
        .chart-horizontal-grid-lines { -fx-stroke: rgba(255,255,255,0.07); }
        .default-color0.chart-series-line {
            -fx-stroke: #f59e0b;
            -fx-stroke-width: 3;
            -fx-effect: dropshadow(gaussian,rgba(245,158,11,0.65),12,0,0,0);
        }
        .default-color0.chart-symbol {
            -fx-background-color: #f59e0b, #0e1425;
            -fx-background-radius: 6;
            -fx-padding: 5;
        }
        """;

        showModernChartWindow(chart,
                "Revenus par mois",
                "ANNÉE " + year + "  ·  DINARS TUNISIENS",
                css, 920, 520,
                () -> revealDoubleSeriesMonthByMonth(monthLabels, revenusMap, series, currentMonth));
    }

    private void revealTwoSeriesMonthByMonth(
            List<String> labels,
            Map<String, Integer> map1,
            Map<String, Integer> map2,
            XYChart.Series<String, Number> s1,
            XYChart.Series<String, Number> s2,
            int totalMonths) {

        int delayMs = calcDelay(totalMonths);
        for (int i = 0; i < labels.size(); i++) {
            final String lbl = labels.get(i);
            final int v1 = map1.getOrDefault(lbl, 0);
            final int v2 = map2.getOrDefault(lbl, 0);
            PauseTransition pt = new PauseTransition(Duration.millis((long) delayMs * (i + 1)));
            pt.setOnFinished(e -> {
                s1.getData().add(new XYChart.Data<>(lbl, v1));
                s2.getData().add(new XYChart.Data<>(lbl, v2));
            });
            pt.play();
        }
    }

    private void revealIntSeriesMonthByMonth(
            List<String> labels,
            Map<String, Integer> map,
            XYChart.Series<String, Number> series,
            int totalMonths) {

        int delayMs = calcDelay(totalMonths);
        for (int i = 0; i < labels.size(); i++) {
            final String lbl = labels.get(i);
            final int val = map.getOrDefault(lbl, 0);
            PauseTransition pt = new PauseTransition(Duration.millis((long) delayMs * (i + 1)));
            pt.setOnFinished(e -> series.getData().add(new XYChart.Data<>(lbl, val)));
            pt.play();
        }
    }

    private void revealDoubleSeriesMonthByMonth(
            List<String> labels,
            Map<String, Double> map,
            XYChart.Series<String, Number> series,
            int totalMonths) {

        int delayMs = calcDelay(totalMonths);
        for (int i = 0; i < labels.size(); i++) {
            final String lbl = labels.get(i);
            final double val = map.getOrDefault(lbl, 0.0);
            PauseTransition pt = new PauseTransition(Duration.millis((long) delayMs * (i + 1)));
            pt.setOnFinished(e -> series.getData().add(new XYChart.Data<>(lbl, val)));
            pt.play();
        }
    }

    /** Spread 2.4 s evenly across N months; clamp between 100–450 ms */
    private int calcDelay(int months) {
        if (months <= 0) return 300;
        return Math.max(100, Math.min(450, 2400 / months));
    }

// ═════════════════════════════════════════════════════════════════════════════
//  DB HELPERS  — both bind (guideId, year)
// ═════════════════════════════════════════════════════════════════════════════

    private Map<String, Integer> fetchMonthlyCount(String sql, int year) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection c = MyDBConnexion.getInstance().getCnx();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(monthAbbrev(rs.getInt(1)), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    private Map<String, Double> fetchMonthlyDouble(String sql, int year) {
        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection c = MyDBConnexion.getInstance().getCnx();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(monthAbbrev(rs.getInt(1)), rs.getDouble(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

// ═════════════════════════════════════════════════════════════════════════════
//  LABEL UTILITIES
// ═════════════════════════════════════════════════════════════════════════════

    /** "Jan", "Fév", "Mar", … in French */
    private String monthAbbrev(int monthNum) {
        return Month.of(monthNum).getDisplayName(TextStyle.SHORT, Locale.FRENCH);
    }

    /** List ["Jan","Fév",…] from 1 up to upToMonth */
    private List<String> buildMonthLabels(int upToMonth) {
        List<String> labels = new ArrayList<>();
        for (int m = 1; m <= upToMonth; m++) labels.add(monthAbbrev(m));
        return labels;
    }

    // ═════════════════════════════════════════════════════════════════════════════
//  WINDOW BUILDER
// ═════════════════════════════════════════════════════════════════════════════
    private void showModernChartWindow(Chart chart, String title, String subtitle,
                                       String extraCss, double w, double h,
                                       Runnable onShown) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #070b14;");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 28, 14, 28));
        header.setStyle("-fx-background-color: #070b14;");

        VBox titleBox = new VBox(4);
        Text titleText = new Text(title);
        titleText.setStyle("-fx-font-family:'Georgia'; -fx-font-size:20px; -fx-font-weight:bold; -fx-fill:white;");
        Text subText = new Text(subtitle);
        subText.setStyle("-fx-font-family:'Courier New'; -fx-font-size:10px; -fx-fill:#475569;");
        titleBox.getChildren().addAll(titleText, subText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label yearBadge = new Label(String.valueOf(LocalDate.now().getYear()));
        yearBadge.setStyle("""
        -fx-background-color: rgba(99,102,241,0.15);
        -fx-text-fill: #818cf8;
        -fx-font-family: 'Courier New';
        -fx-font-size: 11px;
        -fx-background-radius: 20;
        -fx-padding: 4 12;
        -fx-border-color: rgba(99,102,241,0.25);
        -fx-border-radius: 20;
        """);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("""
        -fx-background-color: rgba(255,255,255,0.07);
        -fx-text-fill: #64748b;
        -fx-font-size: 13px;
        -fx-background-radius: 8;
        -fx-padding: 6 10;
        -fx-cursor: hand;
        -fx-border-color: rgba(255,255,255,0.08);
        -fx-border-radius: 8;
        """);
        HBox.setMargin(closeBtn, new Insets(0, 0, 0, 12));

        header.getChildren().addAll(titleBox, spacer, yearBadge, closeBtn);

        Pane sep = new Pane();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.06);");

        chart.setPrefSize(w, h - 100);
        chart.setStyle("-fx-background-color: transparent; -fx-padding: 10 20 10 10;");

        StackPane chartWrap = new StackPane(chart);
        chartWrap.setStyle("-fx-background-color: #0e1425; -fx-background-radius: 0 0 16 16;");
        chartWrap.setPadding(new Insets(16));
        VBox.setVgrow(chartWrap, Priority.ALWAYS);

        root.getChildren().addAll(header, sep, chartWrap);

        Scene scene = new Scene(root, w, h);
        String baseCss = """
        .chart { -fx-background-color: transparent; }
        .chart-content { -fx-background-color: transparent; }
        .chart-legend {
            -fx-background-color: rgba(255,255,255,0.04);
            -fx-background-radius: 8;
            -fx-padding: 6 14;
        }
        .chart-legend-item { -fx-text-fill: #94a3b8; -fx-font-family:'Courier New'; -fx-font-size:11px; }
        .axis { -fx-tick-label-fill: #475569; -fx-font-family:'Courier New'; -fx-font-size:10px; }
        .axis-label { -fx-text-fill: #334155; -fx-font-family:'Courier New'; -fx-font-size:11px; }
        .chart-title { -fx-text-fill: transparent; }
        """ + extraCss;

        try {
            java.io.File tmpCss = java.io.File.createTempFile("chart_style_", ".css");
            tmpCss.deleteOnExit();
            java.nio.file.Files.writeString(tmpCss.toPath(), baseCss);
            scene.getStylesheets().add(tmpCss.toURI().toString());
        } catch (Exception ex) { ex.printStackTrace(); }

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);

        closeBtn.setOnAction(e -> stage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                closeBtn.getStyle().replace("-fx-text-fill: #64748b;", "-fx-text-fill: #ef4444;")));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                closeBtn.getStyle().replace("-fx-text-fill: #ef4444;", "-fx-text-fill: #64748b;")));

        root.setOpacity(0);
        stage.show();
        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setOnFinished(e -> { if (onShown != null) onShown.run(); });
        ft.play();
    }

    // ═════════════════════════════════════════════════════════════════════════════
//  AXIS FACTORIES
// ═════════════════════════════════════════════════════════════════════════════
    private CategoryAxis createStyledCategoryAxis(String label) {
        CategoryAxis a = new CategoryAxis();
        a.setLabel(label);
        a.setTickLabelGap(6);
        return a;
    }

    private NumberAxis createStyledNumberAxis(String label) {
        NumberAxis a = new NumberAxis();
        a.setLabel(label);
        a.setMinorTickVisible(false);
        return a;
    }










    private void showModernChartWindow(Chart chart, String title, String subtitle, String extraCss, double w, double h) {
        // Outer container
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #070b14;");

        // Header bar
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(0);
        header.setPadding(new Insets(22, 28, 14, 28));
        header.setStyle("-fx-background-color: #070b14;");

        VBox titleBox = new VBox(4);
        Text titleText = new Text(title);
        titleText.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: white;");
        Text subText = new Text(subtitle);
        subText.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-fill: #475569; -fx-letter-spacing: 2;");
        titleBox.getChildren().addAll(titleText, subText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("""
        -fx-background-color: rgba(255,255,255,0.07);
        -fx-text-fill: #64748b;
        -fx-font-size: 13px;
        -fx-background-radius: 8;
        -fx-padding: 6 10;
        -fx-cursor: hand;
        -fx-border-color: rgba(255,255,255,0.08);
        -fx-border-radius: 8;
        """);

        header.getChildren().addAll(titleBox, spacer, closeBtn);

        // Separator
        Pane sep = new Pane();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.06);");

        // Chart area
        chart.setPrefSize(w, h - 100);
        chart.setStyle("-fx-background-color: transparent; -fx-padding: 10 20 10 10;");

        StackPane chartWrap = new StackPane(chart);
        chartWrap.setStyle("-fx-background-color: #0e1425; -fx-background-radius: 0 0 16 16;");
        chartWrap.setPadding(new Insets(16));
        VBox.setVgrow(chartWrap, Priority.ALWAYS);

        root.getChildren().addAll(header, sep, chartWrap);

        Scene scene = new Scene(root, w, h);
        // Base dark stylesheet inline
        String baseCss = """
        .chart { -fx-background-color: transparent; }
        .chart-content { -fx-background-color: transparent; }
        .chart-legend {
            -fx-background-color: rgba(255,255,255,0.04);
            -fx-background-radius: 8;
            -fx-padding: 6 12;
        }
        .chart-legend-item-symbol { -fx-background-radius: 4; }
        .chart-legend-item { -fx-text-fill: #94a3b8; -fx-font-family: 'Courier New'; -fx-font-size: 11px; }
        .axis { -fx-tick-label-fill: #475569; -fx-font-family: 'Courier New'; -fx-font-size: 10px; }
        .axis-label { -fx-text-fill: #334155; -fx-font-family: 'Courier New'; -fx-font-size: 11px; }
        .chart-title { -fx-text-fill: transparent; }
        """ + extraCss;

        // Write CSS to temp file
        try {
            java.io.File tmpCss = java.io.File.createTempFile("chart_style_", ".css");
            tmpCss.deleteOnExit();
            java.nio.file.Files.writeString(tmpCss.toPath(), baseCss);
            scene.getStylesheets().add(tmpCss.toURI().toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);

        closeBtn.setOnAction(e -> stage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeBtn.getStyle().replace("#64748b", "#ef4444")));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(closeBtn.getStyle().replace("#ef4444", "#64748b")));

        // Fade-in animation
        root.setOpacity(0);
        stage.show();
        FadeTransition ft = new FadeTransition(Duration.millis(250), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ══════════════════════════════════════════════════════════════


    private void fillSeriesFromQuery(Connection c, String sql, XYChart.Series<String, Number> series) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) series.getData().add(
                    new XYChart.Data<>(rs.getString(1), rs.getInt(2)));
        }
    }
    private void afficherFenetreGraphique(BarChart<?, ?> chart, String titre) {
        Stage stage = new Stage();
        stage.setTitle(titre);
        VBox root = new VBox(10, chart);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }

    // ===== Filtres =====
    private void filterByDate() {
        LocalDate selectedDate = dateFilter.getValue();
        if (selectedDate == null) {
            filteredActivites = toutesActivites;
        } else {
            filteredActivites = toutesActivites.stream()
                    .filter(a -> {
                        LocalDate actDate = a.getDateActivite().toLocalDateTime().toLocalDate();
                        return actDate.equals(selectedDate);
                    })
                    .collect(Collectors.toList());
        }
        currentPage = 1;
        totalPages = (int) Math.ceil((double) filteredActivites.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        mettreAJourPage();
    }

    private void clearDateFilter() {
        dateFilter.setValue(null);
        filterByDate();
    }

    private void loadGuideProfile() {
        if (guideId <= 0) {
            userNameText.setText("Non connecté");
            return;
        }

        String guideName = getGuideNameFromDatabase(guideId);
        if (guideName != null && !guideName.isEmpty()) {
            userNameText.setText(guideName);
        } else {
            userNameText.setText("Guide inconnu");
        }
    }

    // ===== Statistiques globales (cards) =====
    private int getTotalReservationsForGuide(int guideId) {
        String sql = "SELECT COALESCE(SUM(a.nbPlaces), 0) FROM achat a " +
                "JOIN activite act ON a.idActivite = act.idActivite " +
                "WHERE act.idGuide = ? AND a.statut != 'Annulé'";

        try (Connection conn = MyDBConnexion.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getTotalRevenusForGuide(int guideId) {
        String sql = "SELECT COALESCE(SUM(a.montantTotal), 0) FROM achat a " +
                "JOIN activite act ON a.idActivite = act.idActivite " +
                "WHERE act.idGuide = ? AND a.statut != 'Annulé'";

        try (Connection conn = MyDBConnexion.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void mettreAJourStatistiques() {
        int total = toutesActivites.size();
        totalActivitesStat.setText(String.valueOf(total));

        int actives = (int) toutesActivites.stream().filter(a -> "Actif".equals(a.getStatut())).count();
        activesStat.setText(String.valueOf(actives));

        int reservations = getTotalReservationsForGuide(guideId);
        reservationsStat.setText(String.valueOf(reservations));

        double revenus = getTotalRevenusForGuide(guideId);
        revenusStat.setText(String.format("%.0f DT", revenus));
    }

    // ===== Chargement des activités =====
    private void chargerActivites() {
        if (guideId <= 0) {
            System.err.println("Cannot load activities: Invalid guide ID");
            toutesActivites = new ArrayList<>();
            filteredActivites = new ArrayList<>();
            mettreAJourPage();
            mettreAJourStatistiques();
            return;
        }

        try {
            toutesActivites = activiteService.selectByGuide(guideId);
            toutesActivites.sort((a1, a2) -> a1.getDateActivite().compareTo(a2.getDateActivite()));

            filteredActivites = toutesActivites;
            totalPages = (int) Math.ceil((double) toutesActivites.size() / itemsPerPage);
            if (totalPages == 0) totalPages = 1;
            currentPage = 1;

            mettreAJourPage();
            mettreAJourStatistiques();

            System.out.println("Loaded " + toutesActivites.size() + " activities for guide ID: " + guideId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void mettreAJourPage() {
        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredActivites.size());
        List<Activite> activitesPage = filteredActivites.subList(fromIndex, toIndex);
        activitiesListView.getItems().setAll(activitesPage);
        activitiesListView.refresh();
        pageInfoLabel.setText("Page " + currentPage + " sur " + totalPages);
    }

    // ===== Configuration de la ListView =====
    private void setupListView() {
        activitiesListView.setCellFactory(param -> new ListCell<Activite>() {
            @Override
            protected void updateItem(Activite activite, boolean empty) {
                super.updateItem(activite, empty);
                if (empty || activite == null) {
                    setGraphic(null);
                } else {
                    activite.setParticipantsActuels(getNbParticipantsActuels(activite.getIdActivite()));

                    setGraphic(creerCarteActivite(activite, DashboardGuideController.this));
                }
            }
        });
    }

    public int getNbParticipantsActuels(int idActivite) {
        String sql = "SELECT COALESCE(SUM(nbPlaces), 0) FROM achat WHERE idActivite = ? AND statut != 'Annulé'";

        try (Connection conn = MyDBConnexion.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idActivite);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ===== Création d'une carte d'activité =====
    private VBox creerCarteActivite(Activite a, DashboardGuideController controller)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");

        HBox card = new HBox(16);
        card.getStyleClass().add("card-modern");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> controller.voirReservations(a));

        // Partie gauche : image + badge
        VBox leftBox = new VBox();
        leftBox.setAlignment(Pos.TOP_LEFT);
        leftBox.setPrefWidth(200);
        leftBox.setMaxWidth(200);

        StackPane imageStack = new StackPane();
        imageStack.setPrefHeight(150);
        imageStack.setPrefWidth(200);
        imageStack.getStyleClass().add("card-image-modern");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        Image img = controller.loadActivityImage(a);
        if (img != null) imageView.setImage(img);

        String status = a.getStatut();
        Label badge = new Label(status != null ? status : "Activité");
        badge.getStyleClass().add("badge-category-modern");
        if (status != null && status.equalsIgnoreCase("Privé")) {
            badge.setStyle("-fx-background-color: #95a5a6;");
        }

        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(8, 0, 0, 8));
        imageStack.getChildren().addAll(imageView, badge);
        leftBox.getChildren().add(imageStack);

        // Partie centrale : détails
        VBox detailsBox = new VBox(8);
        detailsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);
        detailsBox.setPadding(new Insets(4, 8, 4, 8));

        Text titre = new Text(a.getTitre());
        titre.getStyleClass().add("card-title-modern");

        Label lieu = new Label("📍 " + a.getLieu());
        lieu.getStyleClass().add("card-meta-modern");

        Label date = new Label("📅 " + dateFormat.format(a.getDateActivite()));
        date.getStyleClass().add("card-meta-modern");

        // Ligne prix
        HBox priceRow = new HBox(12);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        Text prix = new Text(String.format("%.0f DT", a.getPrix()));
        prix.getStyleClass().add("card-price-modern");

        Label note = new Label("★ 4.9");
        note.getStyleClass().add("card-rating-modern");

        Label duree = new Label(a.getDureParJour() + (a.getDureParJour() > 1 ? " jours" : " jour"));
        duree.getStyleClass().add("card-duration-modern");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        priceRow.getChildren().addAll(prix, note, spacer1, duree);

        // Ligne capacité
        HBox capacityRow = new HBox(10);
        capacityRow.setAlignment(Pos.CENTER_LEFT);
        capacityRow.getStyleClass().add("capacity-row");

        Label iconLabel = new Label("👥");
        iconLabel.getStyleClass().add("capacity-icon");

        int current = a.getParticipantsActuels();
        int max = a.getPlacesDisponibles();
        Label capacityValue = new Label(current + " / " + max + " places");
        capacityValue.getStyleClass().add("capacity-badge");

        Label suffixLabel = new Label("participants");
        suffixLabel.getStyleClass().add("capacity-sub");

        capacityRow.getChildren().addAll(iconLabel, capacityValue, suffixLabel);

        // Ligne météo
        HBox weatherBox = new HBox(5);
        weatherBox.setAlignment(Pos.CENTER_LEFT);
        ImageView weatherIcon = new ImageView();
        weatherIcon.setFitWidth(30);
        weatherIcon.setFitHeight(30);
        weatherIcon.setVisible(false);
        Label weatherLabel = new Label();
        weatherLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");
        weatherBox.getChildren().addAll(weatherIcon, weatherLabel);

        detailsBox.getChildren().addAll(titre, lieu, date, priceRow, capacityRow, weatherBox);

        // Charger la météo
        controller.loadWeatherForActivity(a, weatherLabel, weatherIcon);

        // Partie droite : boutons d'action
        VBox actionsBox = new VBox(12);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.setPrefWidth(160);
        actionsBox.setPadding(new Insets(0, 0, 0, 0));

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("btn-modifier-modern");
        btnModifier.setMaxWidth(Double.MAX_VALUE);
        btnModifier.setOnAction(e -> {
            e.consume();
            controller.modifierActivite(a);
        });

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.getStyleClass().add("btn-supprimer-modern");
        btnSupprimer.setMaxWidth(Double.MAX_VALUE);
        btnSupprimer.setOnAction(e -> {
            e.consume();
            controller.supprimerActivite(a);
        });

        actionsBox.getChildren().addAll(btnModifier, btnSupprimer);

        card.getChildren().addAll(leftBox, detailsBox, actionsBox);
        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(Insets.EMPTY);
        return wrapper;
    }

    // ===== Chargement des images =====
    private Image loadActivityImage(Activite activite) {
        String imageName = activite.getImage();
        if (imageName == null || imageName.trim().isEmpty()) {
            imageName = "default.jpg";
        }
        if (!imageName.contains(".")) {
            imageName += ".jpg";
        }
        File externalFile = new File("$images/" + imageName);
        if (externalFile.exists()) {
            try {
                return new Image(externalFile.toURI().toURL().toExternalForm(), true);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        String resourcePath = "/images/" + imageName;
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl != null) {
            return new Image(resourceUrl.toExternalForm(), true);
        }
        URL defaultUrl = getClass().getResource("/images/default.jpg");
        if (defaultUrl != null) {
            return new Image(defaultUrl.toExternalForm());
        } else {
            System.err.println("❌ ERREUR : default.jpg est manquant !");
            return null;
        }
    }

    // ===== Récupération du nom du guide =====
    private String getGuideNameFromDatabase(int guideId) {
        String query = "SELECT name, last_name FROM user WHERE id = ? AND (role = 'guide' OR role = 'admin')";

        try (Connection conn = MyDBConnexion.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, guideId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String lastName = rs.getString("last_name");
                    return name + " " + lastName;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===== Actions sur les activités =====
    private void voirReservations(Activite activite) {
        try {
            List<ReservationDetail> details = getReservationsForActivity(activite.getIdActivite());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationDetailsView.fxml"));
            Parent root = loader.load();
            ReservationDetailsController controller = loader.getController();
            controller.setReservations(details);

            Stage stage = new Stage();
            stage.setTitle("Réservations - " + activite.getTitre());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<ReservationDetail> getReservationsForActivity(int idActivite) {
        List<ReservationDetail> list = new ArrayList<>();
        String sql = "SELECT c.name, c.last_name, c.email, c.telephone, a.nbPlaces, a.montantTotal " +
                "FROM achat a " +
                "JOIN user c ON a.idClient = c.id " +
                "WHERE a.idActivite = ? AND a.statut != 'Annulé'";

        try (Connection conn = MyDBConnexion.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idActivite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nomComplet = rs.getString("name") + " " + rs.getString("last_name");
                    String email = rs.getString("email");
                    String telephone = rs.getString("telephone");
                    int places = rs.getInt("nbPlaces");
                    double montant = rs.getDouble("montantTotal");
                    list.add(new ReservationDetail(nomComplet, email, telephone, places, montant));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void modifierActivite(Activite a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddActivite.fxml"));
            Node addActiviteView = loader.load();
            AddActiviteController controller = loader.getController();
            controller.setDashboardController(this);

            Node currentCenter = borderPane.getCenter();
            controller.setPreviousView(currentCenter);
            controller.setMainBorderPane(borderPane);
            controller.setActivite(a);

            borderPane.setCenter(addActiviteView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void supprimerActivite(Activite a) {
        try {
            int reservationsCount = activiteService.countReservationsForActivity(a.getIdActivite());
            if (reservationsCount > 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Suppression impossible");
                alert.setHeaderText("Cette activité a déjà des réservations");
                alert.setContentText("Impossible de supprimer une activité qui a déjà des clients inscrits. (" + reservationsCount + " participant(s))");
                alert.showAndWait();
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la vérification des réservations.", ButtonType.OK);
            alert.show();
            return;
        }

        // Si aucune réservation, demander confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer activité");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette activité ?");
        alert.setContentText(a.getTitre());
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    activiteService.deleteOne(a);
                    chargerActivites();
                    EventBus.getInstance().publish();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.", ButtonType.OK);
                    errorAlert.show();
                }
            }
        });
    }

    // ===== Pagination =====
    @FXML
    private void pagePrecedente() {
        if (currentPage > 1) {
            currentPage--;
            mettreAJourPage();
        }
    }

    @FXML
    private void pageSuivante() {
        if (currentPage < totalPages) {
            currentPage++;
            mettreAJourPage();
        }
    }

    // ===== Rafraîchissement global =====
    public void refreshActivites() {
        chargerActivites();
        mettreAJourStatistiques();
    }

    // ===== Nouvelle activité =====
    @FXML
    private void handleNouvelleActivite() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddActivite.fxml"));
            Node addActiviteView = loader.load();

            AddActiviteController controller = loader.getController();
            controller.setDashboardController(this);

            // Pass the current user to the AddActiviteController
            if (currentUser != null) {
                controller.setCurrentUser(currentUser);
            } else {
                // Try to get from session again
                currentUser = SessionManager.getCurrentUser();
                if (currentUser != null) {
                    controller.setCurrentUser(currentUser);
                }
            }

            Node currentCenter = borderPane.getCenter();
            controller.setPreviousView(currentCenter);
            controller.setMainBorderPane(borderPane);
            borderPane.setCenter(addActiviteView);

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire.", ButtonType.OK);
            alert.show();
        }
    }



    @FXML
    private void handleCalendar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CalendarView.fxml"));
            Parent root = loader.load();
            CalendarController controller = loader.getController();
            controller.setGuideId(guideId);

            Stage stage = new Stage();
            stage.setTitle("Calendrier des activités");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le calendrier.", ButtonType.OK);
            alert.show();
        }
    }




    private void loadWeatherForActivity(Activite a, Label label, ImageView icon) {
        LocalDateTime activityDateTime = a.getDateActivite().toLocalDateTime();
        if (activityDateTime.isAfter(LocalDateTime.now().plusDays(5))) {
            label.setText("Météo >5j");
            return;
        }
        weatherServiceActi.getWeatherForCityAndDateTime(a.getLieu(), activityDateTime)
                .thenAccept(weatherInfo -> {
                    javafx.application.Platform.runLater(() -> {
                        if (weatherInfo != null) {
                            label.setText(String.format("%.1f°C, %s", weatherInfo.getTemperature(), weatherInfo.getDescription()));
                            icon.setImage(new Image(weatherInfo.getIconUrl(), true));
                            icon.setVisible(true);
                        } else {
                            label.setText("Météo N/A");
                        }
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> label.setText("Erreur"));
                    return null;
                });
    }



}