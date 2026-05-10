package controllers;

import entities.Publication;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import services.CommentService;
import services.GeocodingService;
import services.LikeService;
import services.PublicationService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MapController — GeoJSON-powered JavaFX Canvas world map.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * APPROACH
 * ═══════════════════════════════════════════════════════════════════════
 * Reads src/main/resources/web/countries.geo.json (Natural Earth 110m),
 * parses every country polygon, and draws them onto a JavaFX Canvas using
 * GraphicsContext path commands. No WebView. No tile server. No PNG decoding.
 *
 * The GeoJSON file to bundle (public domain, ~500 KB):
 *   https://raw.githubusercontent.com/johan/world.geo.json/master/countries.geo.json
 *   Place at: src/main/resources/web/countries.geo.json
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROJECTION
 * ═══════════════════════════════════════════════════════════════════════
 * We use the Mercator projection — the same one used by Google Maps,
 * OpenStreetMap, and every web map. This means our pin coordinates
 * (from Nominatim, which uses WGS84 lat/lon) align perfectly with the
 * rendered country shapes.
 *
 *   mercatorX(lon) = (lon + 180) / 360 * CANVAS_W
 *   mercatorY(lat) = (π - ln(tan(π/4 + lat*π/360))) / (2π) * CANVAS_H
 *
 * GeoJSON coordinates are [longitude, latitude] pairs (note: lon first).
 *
 * ═══════════════════════════════════════════════════════════════════════
 * INTERACTION
 * ═══════════════════════════════════════════════════════════════════════
 * Pan:  drag the canvas with the mouse
 * Zoom: scroll wheel or +/− buttons (zooms towards cursor position)
 * Pins: SVGPath teardrops added to a Pane overlay on top of the canvas
 * Popup: pure JavaFX VBox added to mapContainer (screen space) so it
 *        never shifts when panning/zooming
 *
 * ═══════════════════════════════════════════════════════════════════════
 * CANVAS VIRTUAL SIZE vs DISPLAY SIZE
 * ═══════════════════════════════════════════════════════════════════════
 * The canvas is drawn at CANVAS_W × CANVAS_H pixels (4000 × 2000).
 * The mapGroup Pane is scaled and translated by the zoom/pan state.
 * Pins are placed in the same mapGroup coordinate space, so they move
 * naturally with the map when panning. Only the popup card is in
 * screen space (added to mapContainer directly).
 */
public class MapController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label     statusLabel;
    @FXML private Label     pinCountLabel;
    @FXML private StackPane mapContainer;

    // ── Injected ──────────────────────────────────────────────────────────────
    private WajdiDashboardController dashboard;
    private BorderPane          rootPane;

    // ── Close callback (set by WajdiDashboardController for overlay mode) ─────────
    private Runnable onCloseCallback = null;

    /** Called by WajdiDashboardController so the map can trigger its own fade-out. */
    public void setOnClose(Runnable callback) { this.onCloseCallback = callback; }

    // ── Services ──────────────────────────────────────────────────────────────
    private final PublicationService publicationService = new PublicationService();
    private final GeocodingService   geocodingService   = new GeocodingService();
    private final LikeService        likeService        = new LikeService();
    private final CommentService     commentService     = new CommentService();

    // ── Map layout ────────────────────────────────────────────────────────────
    // mapGroup contains: canvas (background) + pinsPane (pin shapes on top)
    // mapGroup is transformed (scale + translate) for zoom/pan
    private Pane   mapGroup;
    private Canvas mapCanvas;
    private Pane   pinsPane;    // pin SVGPath nodes — same coord space as canvas

    // Canvas virtual dimensions — high resolution for quality rendering
    private static final double CANVAS_W = 4000.0;
    private static final double CANVAS_H = 2000.0;

    // ── Zoom / Pan state ──────────────────────────────────────────────────────
    private double scale  = 1.0;
    private double transX = 0.0;
    private double transY = 0.0;
    private double dragStartX, dragStartY;
    private boolean centred = false;
    // Scale transform with explicit pivot at (0,0) — keeps transX/transY math consistent.
    // Using setScaleX/Y pivots at the node CENTER, which breaks clamping when zoomed.
    private final Scale mapScale = new Scale(1, 1, 0, 0);

    // ── Popup ─────────────────────────────────────────────────────────────────
    private VBox activePopup;
    private Pane popupLayer;   // full-size transparent Pane overlay for popup cards

    // ── Publication cache (for "Open" button) ─────────────────────────────────
    private List<Publication> publications = new ArrayList<>();

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color OCEAN       = Color.web("#a8d0e6");
    private static final Color LAND        = Color.web("#f5f0e8");
    private static final Color LAND_STROKE = Color.web("#c8bfa8");
    private static final Color LAND_HOVER  = Color.web("#eae0cc");
    private static final String TEAL       = "#17B3A6";
    private static final String TEAL_DK    = "#0d9e92";
    private static final String WHITE      = "#ffffff";

    // ── Entry point ───────────────────────────────────────────────────────────

    public void show(WajdiDashboardController dash) {
        this.dashboard = dash;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/map_view.fxml"));
            loader.setController(this);
            rootPane = loader.load();
            dashboard.getContentContainer().getChildren().add(rootPane);
            buildMap();
        } catch (IOException e) {
            e.printStackTrace();
            dashboard.showError("Could not open map: " + e.getMessage());
        }
    }

    /**
     * Overlay entry point — used by WajdiDashboardController.showMapView().
     * Loads map_view.fxml with this as controller, stores the dashboard
     * reference so the "Open →" popup button can open PostDetailController,
     * kicks off map building, and returns the root node for the caller
     * to place in whatever container it wants.
     */
    public javafx.scene.Parent loadForOverlay(WajdiDashboardController dash) throws java.io.IOException {
        this.dashboard = dash;  // FIX: store dashboard so "Open →" popup button works
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/map_view.fxml"));
        loader.setController(this);
        rootPane = loader.load();
        buildMap();
        return rootPane;
    }

    @FXML private void onClose() {
        if (onCloseCallback != null) {
            // Overlay mode — delegate fade-out to WajdiDashboardController
            onCloseCallback.run();
        } else if (dashboard != null) {
            // Legacy full-panel mode
            dashboard.getContentContainer().getChildren().remove(rootPane);
        }
    }

    @FXML private void onZoomIn() {
        applyZoom(1.35, mapContainer.getWidth() / 2, mapContainer.getHeight() / 2);
    }

    @FXML private void onZoomOut() {
        applyZoom(1.0 / 1.35, mapContainer.getWidth() / 2, mapContainer.getHeight() / 2);
    }

    // ── Map construction ──────────────────────────────────────────────────────

    private void buildMap() {
        mapCanvas = new Canvas(CANVAS_W, CANVAS_H);
        pinsPane  = new Pane();
        pinsPane.setPrefSize(CANVAS_W, CANVAS_H);
        pinsPane.setMouseTransparent(false);

        mapGroup = new Pane(mapCanvas, pinsPane);
        mapGroup.setPrefSize(CANVAS_W, CANVAS_H);
        mapGroup.getTransforms().add(mapScale);

        /*
         * CLIPPING — The single most important fix.
         *
         * mapContainer is a StackPane. Its children can visually overflow
         * outside it (StackPane doesn't clip by default). When transX > 0
         * or the map is large, it renders over the sidebar/navbar.
         *
         * We bind a Rectangle clip to mapContainer's width/height.
         * Anything outside [0,0,w,h] is invisible — including any part of
         * the giant 4000px canvas that would bleed into the sidebar.
         *
         * The clip is updated automatically when the container resizes.
         */
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(mapContainer.widthProperty());
        clip.heightProperty().bind(mapContainer.heightProperty());
        mapContainer.setClip(clip);

        mapContainer.setStyle("-fx-background-color: " + toHexStr(OCEAN) + ";");
        /*
         * POPUP LAYER — a full-size Pane that sits on top of the map in mapContainer.
         *
         * WHY NOT add popup directly to mapContainer (StackPane)?
         *   StackPane ignores layoutX/layoutY on its children — it positions
         *   them by alignment (default CENTER or TOP_LEFT). Our computed cx/cy
         *   would be silently discarded and the popup would always appear in
         *   the same place regardless of which pin was clicked.
         *
         * WHY NOT add popup to pinsPane (map space)?
         *   pinsPane is scaled and translated with the map. The popup would
         *   move when panning, and would be tiny/huge depending on zoom level.
         *
         * SOLUTION: A dedicated Pane (popupLayer) that:
         *   - Is sized to fill mapContainer via binding (so it covers the whole map area)
         *   - Is mouseTransparent EXCEPT where the popup card is
         *   - Respects layoutX/layoutY on its children (regular Pane does this)
         *   - Sits above the map but below nothing (last in z-order)
         */
        popupLayer = new Pane();
        popupLayer.setMouseTransparent(true); // transparent except popup children
        popupLayer.prefWidthProperty().bind(mapContainer.widthProperty());
        popupLayer.prefHeightProperty().bind(mapContainer.heightProperty());

        mapContainer.getChildren().addAll(mapGroup, popupLayer);

        mapCanvas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) dismissPopup();
        });

        wirePan();
        wireZoom();

        mapContainer.widthProperty().addListener((o, ov, nv) -> { if (!centred) initialView(); });
        mapContainer.heightProperty().addListener((o, ov, nv) -> { if (!centred) initialView(); });

        new Thread(() -> {
            updateStatus("Loading map…");
            drawCountries();
            startGeocoding();
        }).start();
    }

    // ── GeoJSON parsing + Canvas drawing ─────────────────────────────────────

    /**
     * Reads countries.geo.json from classpath, parses each country's polygon
     * coordinates, and draws them onto the canvas using GraphicsContext.
     *
     * GeoJSON geometry types handled:
     *   - Polygon:        array of rings, first ring is outer boundary
     *   - MultiPolygon:   array of Polygons
     *
     * Each coordinate pair is [longitude, latitude].
     * We convert to canvas pixel with mercatorX() / mercatorY().
     *
     * Drawing order:
     *   1. Fill canvas with ocean colour
     *   2. For each country: fill polygon(s) with land colour
     *   3. Stroke polygon(s) with border colour
     */
    private void drawCountries() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();

        // Ocean background
        Platform.runLater(() -> {
            gc.setFill(OCEAN);
            gc.fillRect(0, 0, CANVAS_W, CANVAS_H);
        });

        // Load GeoJSON
        String json = loadGeoJson();
        if (json == null) {
            Platform.runLater(() -> updateStatus("countries.geo.json not found in /web/"));
            return;
        }

        try {
            JSONObject root     = new JSONObject(json);
            JSONArray  features = root.getJSONArray("features");

            // Collect all draw calls then execute on FX thread in one batch
            // for performance (Canvas drawing must happen on FX thread)
            List<Runnable> drawCalls = new ArrayList<>();

            for (int f = 0; f < features.length(); f++) {
                JSONObject feature  = features.getJSONObject(f);
                JSONObject geometry = feature.getJSONObject("geometry");
                String     type     = geometry.getString("type");
                JSONArray  coords   = geometry.getJSONArray("coordinates");

                if (type.equals("Polygon")) {
                    double[][] outer = ringToPixels(coords.getJSONArray(0));
                    drawCalls.add(() -> drawPolygon(gc, outer));

                } else if (type.equals("MultiPolygon")) {
                    for (int p = 0; p < coords.length(); p++) {
                        double[][] outer = ringToPixels(
                                coords.getJSONArray(p).getJSONArray(0));
                        drawCalls.add(() -> drawPolygon(gc, outer));
                    }
                }
            }

            Platform.runLater(() -> {
                for (Runnable r : drawCalls) r.run();
                drawCountryLabels(gc);
                updateStatus("");
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> updateStatus("Map parse error: " + e.getMessage()));
        }
    }

    /**
     * Converts a GeoJSON ring (array of [lon, lat] pairs) to canvas pixel coords.
     * Returns double[n][2] where [i][0]=x, [i][1]=y.
     */
    private double[][] ringToPixels(JSONArray ring) {
        double[][] pts = new double[ring.length()][2];
        for (int i = 0; i < ring.length(); i++) {
            JSONArray coord = ring.getJSONArray(i);
            double lon = coord.getDouble(0);
            double lat = coord.getDouble(1);
            pts[i][0] = mercatorX(lon);
            pts[i][1] = mercatorY(lat);
        }
        return pts;
    }

    /**
     * Draws a single filled + stroked polygon on the canvas.
     * Uses GraphicsContext.beginPath() / lineTo() / closePath() for efficiency.
     */
    private void drawPolygon(GraphicsContext gc, double[][] pts) {
        if (pts.length < 3) return;
        gc.beginPath();
        gc.moveTo(pts[0][0], pts[0][1]);
        for (int i = 1; i < pts.length; i++) {
            gc.lineTo(pts[i][0], pts[i][1]);
        }
        gc.closePath();
        gc.setFill(LAND);
        gc.fill();
        gc.setStroke(LAND_STROKE);
        gc.setLineWidth(0.6);
        gc.stroke();
    }

    // ── Country name labels ──────────────────────────────────────────────────

    /**
     * Draws country name labels at approximate country centres.
     *
     * Technique: two-pass rendering
     *   Pass 1: draw text as a thick white stroke (halo effect)
     *   Pass 2: draw text as dark fill on top
     * This makes labels readable over both land (beige) and ocean (blue).
     *
     * Labels are only drawn for countries large enough to be visible at
     * the default zoom level (Mediterranean view). Micro-states are skipped.
     *
     * Coordinates are approximate geographic centres [lon, lat].
     */
    private void drawCountryLabels(GraphicsContext gc) {
        // [name, lon, lat] — approximate visual centres
        Object[][] countries = {
                {"Tunisia",        9.0,  34.0},
                {"Algeria",        3.0,  28.0},
                {"Morocco",       -6.0,  32.0},
                {"Libya",         17.0,  27.0},
                {"Egypt",         30.0,  27.0},
                {"France",         2.5,  46.5},
                {"Spain",         -3.5,  40.0},
                {"Germany",       10.5,  51.0},
                {"Italy",         12.5,  42.5},
                {"Turkey",        35.0,  39.0},
                {"Ukraine",       32.0,  49.0},
                {"Poland",        20.0,  52.0},
                {"Norway",         8.0,  65.0},
                {"Sweden",        18.0,  62.0},
                {"Finland",       27.0,  64.0},
                {"UK",            -2.0,  54.0},
                {"Russia",        95.0,  60.0},
                {"Kazakhstan",    68.0,  48.0},
                {"China",        105.0,  35.0},
                {"India",         80.0,  22.0},
                {"Saudi Arabia",  45.0,  24.0},
                {"Iran",          53.0,  32.0},
                {"Iraq",          44.0,  33.0},
                {"Pakistan",      69.0,  30.0},
                {"Sudan",         30.0,  16.0},
                {"Ethiopia",      40.0,   9.0},
                {"Nigeria",        8.0,  10.0},
                {"DR Congo",      24.0,  -3.0},
                {"South Africa",  25.0, -29.0},
                {"Kenya",         38.0,   0.0},
                {"Tanzania",      35.0,  -6.0},
                {"Canada",       -96.0,  60.0},
                {"USA",          -98.0,  39.0},
                {"Mexico",       -102.0, 23.0},
                {"Brazil",       -52.0, -10.0},
                {"Argentina",    -65.0, -35.0},
                {"Colombia",     -74.0,   4.0},
                {"Peru",         -76.0, -10.0},
                {"Australia",    134.0, -26.0},
                {"Indonesia",    118.0,  -2.0},
                {"Japan",        138.0,  37.0},
                {"Malaysia",     110.0,   3.0},
                {"Thailand",     101.0,  15.0},
                {"Myanmar",       96.0,  20.0},
                {"Angola",        18.0, -12.0},
                {"Mozambique",    35.0, -18.0},
                {"Madagascar",    47.0, -20.0},
                {"Mali",          -2.0,  18.0},
                {"Niger",          9.0,  17.0},
                {"Chad",          18.0,  15.0},
                {"Mauritania",   -11.0,  20.0},
        };

        gc.save();

        for (Object[] c : countries) {
            String name = (String) c[0];
            double lon  = (Double) c[1];
            double lat  = (Double) c[2];

            double px = mercatorX(lon);
            double py = mercatorY(lat);

            // Pass 1: white halo for legibility
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 22));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            gc.setStroke(javafx.scene.paint.Color.WHITE);
            gc.setLineWidth(4.0);
            gc.strokeText(name, px, py);

            // Pass 2: dark label on top
            gc.setFill(javafx.scene.paint.Color.web("#4a4a5a"));
            gc.fillText(name, px, py);
        }

        gc.restore();
    }

    // ── Mercator projection ────────────────────────────────────────────────────

    /**
     * Mercator X: maps longitude [-180, 180] to [0, CANVAS_W] linearly.
     */
    private double mercatorX(double lon) {
        return (lon + 180.0) / 360.0 * CANVAS_W;
    }

    /**
     * Mercator Y: maps latitude using the Web Mercator formula.
     * Clamps latitude to [-85.05, 85.05] (standard Web Mercator limit).
     *
     * Formula:
     *   y = (π - ln(tan(π/4 + lat_rad/2))) / (2π) * CANVAS_H
     *
     * This matches the projection used by Nominatim's coordinate output,
     * so pins from geocoding land exactly on the right country.
     */
    private double mercatorY(double lat) {
        lat = Math.max(-85.05, Math.min(85.05, lat));
        double latRad = Math.toRadians(lat);
        double y = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        // Normalise: at lat=+85 y≈π, at lat=-85 y≈-π → map to [0, CANVAS_H]
        double maxY = Math.log(Math.tan(Math.PI / 4.0 + Math.toRadians(85.05) / 2.0));
        return (1.0 - y / maxY) / 2.0 * CANVAS_H;
    }

    // ── Coordinate helpers used for pins and initial view ────────────────────

    private double lonToX(double lon) { return mercatorX(lon); }
    private double latToY(double lat) { return mercatorY(lat); }

    // ── GeoJSON loader ────────────────────────────────────────────────────────

    private String loadGeoJson() {
        try (InputStream is = getClass().getResourceAsStream("/web/countries.geo.json")) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[MapController] GeoJSON load error: " + e.getMessage());
            return null;
        }
    }

    // ── Initial view — centre on Mediterranean / Tunisia ─────────────────────

    /**
     * Sets the initial zoom and pan so the Mediterranean and North Africa
     * fill the viewport — where most Rehletna posts will be.
     *
     * Tunisia centre: lat=34, lon=9
     * We want ~60° of longitude to fill the screen width.
     */
    private void initialView() {
        double vw = mapContainer.getWidth();
        double vh = mapContainer.getHeight();
        if (vw == 0 || vh == 0) return;
        centred = true;

        // How many canvas pixels is 60° longitude wide?
        double pxPer60deg = 60.0 / 360.0 * CANVAS_W; // = 666px at scale=1
        // We want that to fill ~80% of viewport width
        scale  = vw * 0.8 / pxPer60deg;
        scale  = Math.max(0.3, Math.min(8.0, scale));

        // Centre of Tunisia in canvas pixels
        double cx = lonToX(9.0);
        double cy = latToY(34.0);

        // Set translation so Tunisia centre is in the middle of the viewport
        transX = vw / 2.0 - cx * scale;
        transY = vh / 2.0 - cy * scale;

        applyTransform();
    }

    // ── Pins ──────────────────────────────────────────────────────────────────

    /**
     * Adds a teal teardrop pin at the given lat/lon position on the map.
     *
     * Pin is an SVGPath (pure vector) added to pinsPane, which shares the
     * same coordinate space as the canvas. This means panning/zooming moves
     * pins naturally with the map without any extra calculation.
     *
     * The popup card is NOT in pinsPane — see showPopup() for why.
     */
    // Track all placed pin positions to detect overlaps
    private final java.util.List<double[]> placedPins = new java.util.ArrayList<>();

    private void addPin(Publication pub, double lat, double lon,
                        int likes, int comments) {
        double x = lonToX(lon);
        double y = latToY(lat);

        /*
         * CLUSTER OFFSET — prevent pins from stacking on top of each other.
         *
         * When multiple posts share the same location (e.g. "Tunis, Tunisia"),
         * Nominatim returns the exact same lat/lon for all of them. The pins
         * stack invisibly and only the top one is clickable.
         *
         * Fix: check if any existing pin is within CLUSTER_RADIUS canvas pixels.
         * If so, spread pins out in a small spiral around the original point:
         *   ring 1 (2-6 pins):  radius=18px, evenly spaced angles
         *   ring 2 (7-12 pins): radius=36px, evenly spaced angles
         *
         * The offset is small enough that the pin still appears "in" the city
         * but large enough to be individually clickable.
         */
        final double CLUSTER_RADIUS = 14.0; // canvas pixels — pins closer than this get offset

        // Count how many existing pins are within cluster radius of this position
        // Use effectively-final copies of x/y for the stream lambda
        final double baseX = x;
        final double baseY = y;
        long nearby = placedPins.stream()
                .filter(p -> Math.hypot(p[0] - baseX, p[1] - baseY) < CLUSTER_RADIUS)
                .count();

        // Compute final offset position — stored in pinX/pinY (effectively final)
        final double pinX;
        final double pinY;
        if (nearby > 0) {
            int ring   = (int)(nearby / 6) + 1;
            int pos    = (int)(nearby % 6);
            double r   = ring * 18.0;
            double ang = pos * (2 * Math.PI / 6.0);
            pinX = baseX + r * Math.cos(ang);
            pinY = baseY + r * Math.sin(ang);
        } else {
            pinX = baseX;
            pinY = baseY;
        }
        placedPins.add(new double[]{pinX, pinY});

        // Teardrop SVG path, tip at bottom-centre (0,24), head at (0,0)
        SVGPath pin = new SVGPath();
        pin.setContent(
                "M10,0 C4.477,0 0,4.477 0,10 C0,17.5 10,24 10,24 " +
                        "C10,24 20,17.5 20,10 C20,4.477 15.523,0 10,0 Z");
        pin.setFill(Color.web(pub.hasImage() ? TEAL_DK : TEAL));
        pin.setStroke(Color.web(WHITE));
        pin.setStrokeWidth(1.5);

        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(3.5, Color.WHITE);
        dot.setLayoutX(10);
        dot.setLayoutY(10);

        Pane pinGroup = new Pane(pin, dot);
        pinGroup.setPrefSize(20, 24);
        pinGroup.setLayoutX(pinX - 10);
        pinGroup.setLayoutY(pinY - 24);
        pinGroup.setCursor(Cursor.HAND);

        pinGroup.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), pinGroup);
            st.setToX(1.3); st.setToY(1.3);
            st.play();
        });
        pinGroup.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), pinGroup);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        // pinX/pinY are effectively final — safe to use in lambda
        pinGroup.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                double screenX = pinX * scale + transX;
                double screenY = pinY * scale + transY;
                showPopup(pub, screenX, screenY, likes, comments);
            }
        });

        pinsPane.getChildren().add(pinGroup);
    }

    // ── Popup ─────────────────────────────────────────────────────────────────

    /**
     * Shows a styled popup card above the clicked pin.
     *
     * CRITICAL: The popup is added to mapContainer (the StackPane, screen space),
     * NOT to pinsPane (map space). This means:
     *   - Popup position is in screen pixels → never shifts during zoom/pan
     *   - screenX/Y are computed at click time from pin's map coords + current
     *     scale and transX/Y
     *   - Popup is clamped to viewport bounds so it never overflows
     */
    private void showPopup(Publication pub, double screenX, double screenY,
                           int likes, int comments) {
        dismissPopup();

        /*
         * POPUP CARD LAYOUT
         * ─────────────────────────────────────────────────────────────────────
         * Fixed width (260px), fixed max-height enforced by clamping content.
         * Structure:
         *   ┌─ header (teal, 40px) ──────────────────────────────────────────┐
         *   │  📍 Place name                                                  │
         *   ├─ body (max 80px) ───────────────────────────────────────────────┤
         *   │  AUTHOR                                                         │
         *   │  Post content (2 lines max, truncated at 80 chars)              │
         *   ├─ footer (38px) ─────────────────────────────────────────────────┤
         *   │  ♥ N  💬 N                              [Open →]               │
         *   └────────────────────────────────────────────────────────────────┘
         * Total fixed height: ~158px — never grows, never overflows.
         */
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.22),16,0,0,4);"
        );
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setPrefHeight(158);
        card.setMaxHeight(158);

        // ── Teal header (fixed 40px) — with ✕ close button ──
        HBox header = new HBox();
        header.setPrefHeight(40);
        header.setMaxHeight(40);
        header.setPadding(new Insets(0, 10, 0, 14));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: " + TEAL + ";" +
                        "-fx-background-radius: 12 12 0 0;"
        );
        Label placeLabel = new Label("📍  " + trunc(pub.getPlace(), 22));
        placeLabel.setStyle(
                "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 12px;");
        placeLabel.setMaxWidth(180);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        // ✕ close button — lets user dismiss popup without dragging the map
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.85);" +
                        "-fx-font-size: 13px; -fx-font-weight: 700;" +
                        "-fx-padding: 2 6; -fx-cursor: hand;" +
                        "-fx-background-radius: 4;"
        );
        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.15);" +
                                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700;" +
                                "-fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 4;"));
        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13px; -fx-font-weight: 700;" +
                                "-fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 4;"));
        closeBtn.setOnAction(e -> dismissPopup());

        header.getChildren().addAll(placeLabel, headerSpacer, closeBtn);

        // ── Body (fixed 80px) ──
        VBox body = new VBox(3);
        body.setPrefHeight(80);
        body.setMaxHeight(80);
        body.setPadding(new Insets(8, 14, 6, 14));

        String uname = pub.getClient().getUsername() != null
                ? pub.getClient().getUsername() : "Traveler";
        Label authorLabel = new Label(uname.toUpperCase());
        authorLabel.setStyle(
                "-fx-text-fill: #8b90a7; -fx-font-size: 10px; -fx-font-weight: 700;");

        // Hard-limit content to 2 lines (80 chars) — no wrapping, ellipsis only
        Label contentLabel = new Label(trunc(pub.getContent(), 80));
        contentLabel.setStyle("-fx-text-fill: #1a1d27; -fx-font-size: 12px;");
        contentLabel.setWrapText(true);
        contentLabel.setMaxHeight(48);  // 2 lines × 24px line-height
        contentLabel.setPrefWidth(232);
        contentLabel.setMaxWidth(232);

        body.getChildren().addAll(authorLabel, contentLabel);

        // ── Footer (fixed 38px) ──
        HBox footer = new HBox(8);
        footer.setPrefHeight(38);
        footer.setMaxHeight(38);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(0, 14, 0, 14));
        footer.setStyle("-fx-border-color: #f0f2f5; -fx-border-width: 1 0 0 0;");

        Label likesLabel    = new Label("♥ " + likes);
        Label commentsLabel = new Label("💬 " + comments);
        likesLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 11px;");
        commentsLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openBtn = new Button("Open →");
        openBtn.setStyle(
                "-fx-background-color: " + TEAL + "; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;"
        );
        openBtn.setOnAction(e -> new PostDetailController(pub, dashboard).show());

        footer.getChildren().addAll(likesLabel, commentsLabel, spacer, openBtn);
        card.getChildren().addAll(header, body, footer);

        // ── Position in screen space ──
        double cardW   = 260;
        double cardH   = 158;
        double vw      = mapContainer.getWidth();
        double vh      = mapContainer.getHeight();

        double cx = screenX - cardW / 2.0;
        double cy = screenY - 24 - cardH - 8;

        cx = Math.max(8, Math.min(vw - cardW - 8, cx));
        cy = Math.max(8, Math.min(vh - cardH - 8, cy));

        card.setLayoutX(cx);
        card.setLayoutY(cy);

        // Prevent click-through to map (would dismiss the popup)
        card.setOnMouseClicked(javafx.event.Event::consume);

        card.setOpacity(0);
        // Add to popupLayer (a Pane) — it respects layoutX/layoutY unlike StackPane
        popupLayer.setMouseTransparent(false);
        popupLayer.getChildren().add(card);

        FadeTransition ft = new FadeTransition(Duration.millis(160), card);
        ft.setToValue(1.0);
        ft.play();

        activePopup = card;
    }

    private void dismissPopup() {
        if (activePopup != null) {
            popupLayer.getChildren().remove(activePopup);
            popupLayer.setMouseTransparent(true);
            activePopup = null;
        }
    }

    // ── Geocoding ─────────────────────────────────────────────────────────────

    private void startGeocoding() {
        try {
            List<Publication> all = publicationService.selectALL();
            publications = all;

            List<Publication> withPlace = new ArrayList<>();
            for (Publication p : all)
                if (p.getPlace() != null && !p.getPlace().trim().isEmpty())
                    withPlace.add(p);

            if (withPlace.isEmpty()) {
                Platform.runLater(() -> updateStatus("No posts with locations yet."));
                return;
            }

            Platform.runLater(() -> updateStatus("Geocoding " + withPlace.size() + " locations…"));

            AtomicInteger pending  = new AtomicInteger(withPlace.size());
            AtomicInteger pinsDone = new AtomicInteger(0);

            for (Publication pub : withPlace) {
                /*
                 * Two-attempt geocoding strategy:
                 *
                 * Many users type place names without a country (e.g. "Medina", "Sfax").
                 * Nominatim needs enough context to resolve ambiguous names.
                 * We try the raw place string first. If it returns null, we retry
                 * with ", Tunisia" appended — since Rehletna is a Tunisian travel app,
                 * most posts will be in Tunisia and this dramatically improves hit rate.
                 *
                 * If both attempts fail, the post is simply not mapped (no pin added)
                 * but it still counts toward the "posts with locations" total in status.
                 */
                geocodingService.geocode(pub.getPlace())
                        .thenCompose(coords -> {
                            if (coords != null) return java.util.concurrent.CompletableFuture.completedFuture(coords);
                            // Retry with Tunisia suffix
                            String fallback = pub.getPlace().trim() + ", Tunisia";
                            System.out.println("[Map] Retrying geocode with: " + fallback);
                            return geocodingService.geocode(fallback);
                        })
                        .thenAccept(coords -> {
                            if (coords != null) {
                                try {
                                    int lk = likeService.getLikeCount(pub.getPublicationID());
                                    int cm = commentService.getCommentCount(pub.getPublicationID());
                                    Platform.runLater(() -> {
                                        addPin(pub, coords[0], coords[1], lk, cm);
                                        updatePinCount(pinsDone.incrementAndGet());
                                    });
                                } catch (Exception ex) {
                                    System.err.println("[Map] count error: " + ex.getMessage());
                                }
                            } else {
                                System.err.println("[Map] Could not geocode: " + pub.getPlace());
                            }
                            if (pending.decrementAndGet() == 0) {
                                int total = pinsDone.get();
                                Platform.runLater(() -> {
                                    updateStatus("");
                                    // Final count: show how many were successfully mapped
                                    updatePinCount(total);
                                });
                            }
                        })
                        .exceptionally(err -> {
                            System.err.println("[Map] geocode error: " + err.getMessage());
                            if (pending.decrementAndGet() == 0)
                                Platform.runLater(() -> updateStatus(""));
                            return null;
                        });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> dashboard.showError("DB error: " + e.getMessage()));
        }
    }

    // ── Pan ───────────────────────────────────────────────────────────────────

    private void wirePan() {
        /*
         * Pan handler is on mapContainer (the StackPane), not mapGroup.
         *
         * WHY:
         *   e.getSceneX() = coords relative to window origin (includes sidebar offset).
         *   e.getX() on mapGroup = coords in mapGroup's own scaled space.
         *   Both are wrong for our transX/transY which are in mapContainer-local pixels.
         *
         *   mapContainer-local coords (e.getX(), e.getY() on mapContainer) are always
         *   in screen pixels relative to the top-left of the map area — exactly what
         *   transX/transY represent. This stays correct at any zoom level.
         *
         *   We consume the event so it doesn't also fire the popup-dismiss handler.
         */
        mapContainer.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getX() - transX;
                dragStartY = e.getY() - transY;
                mapContainer.setCursor(Cursor.CLOSED_HAND);
            }
        });
        mapContainer.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                transX = e.getX() - dragStartX;
                transY = e.getY() - dragStartY;
                applyTransform();
                dismissPopup(); // dismiss on drag — position would be stale
            }
        });
        mapContainer.setOnMouseReleased(e -> mapContainer.setCursor(Cursor.DEFAULT));
        mapContainer.setCursor(Cursor.DEFAULT);
    }

    // ── Zoom ──────────────────────────────────────────────────────────────────

    private void wireZoom() {
        // Scroll on mapContainer — e.getX()/getY() are mapContainer-local (screen pixels)
        // which is exactly the pivot space we need for applyZoom().
        mapContainer.setOnScroll((ScrollEvent e) -> {
            applyZoom(e.getDeltaY() > 0 ? 1.15 : 0.87, e.getX(), e.getY());
            e.consume();
        });
    }

    /**
     * Zooms by `factor` keeping the point (pivotX, pivotY) in screen space fixed.
     *
     * Math:
     *   After zoom, the map-space point under the pivot must remain at the
     *   same screen position. If the old transform is (scale, transX, transY):
     *     mapX = (pivotX - transX) / scale
     *   After new scale ns:
     *     newTransX = pivotX - mapX * ns = pivotX - (pivotX - transX) / scale * ns
     *               = pivotX - (pivotX - transX) * (ns / scale)
     *   Which simplifies to:
     *     newTransX = pivotX + (transX - pivotX) * realFactor
     */
    private void applyZoom(double factor, double pivotX, double pivotY) {
        double newScale    = Math.max(0.3, Math.min(12.0, scale * factor));
        double realFactor  = newScale / scale;
        transX = pivotX + (transX - pivotX) * realFactor;
        transY = pivotY + (transY - pivotY) * realFactor;
        scale  = newScale;
        applyTransform();
        // Dismiss popup on zoom (it's in screen space but the pin moved)
        dismissPopup();
    }

    private void applyTransform() {
        /*
         * TRANSFORM MODEL
         * ─────────────────────────────────────────────────────────────────────
         * We apply TWO transforms to mapGroup (in order):
         *   1. Scale(scale, scale, pivotX=0, pivotY=0)  — zoom about top-left
         *   2. Translate(transX, transY)                — pan
         *
         * With pivot at (0,0), a canvas point (px, py) maps to screen as:
         *   screenX = px * scale + transX
         *   screenY = py * scale + transY
         *
         * This is a purely linear model — transX/transY are in screen pixels,
         * independent of scale. Clamping is therefore straightforward:
         *
         * CLAMPING BOUNDS:
         *   The rendered map occupies screen rect:
         *     left   = transX
         *     right  = transX + CANVAS_W * scale
         *     top    = transY
         *     bottom = transY + CANVAS_H * scale
         *
         *   We require:
         *     left  <= margin           → transX <= margin
         *     right >= vw - margin      → transX >= vw - CANVAS_W*scale - margin
         *     top   <= margin           → transY <= margin
         *     bottom>= vh - margin      → transY >= vh - CANVAS_H*scale - margin
         *
         *   margin = 50px gives a soft overscroll feel.
         *
         * WHY NOT setScaleX/Y?
         *   JavaFX's setScaleX/Y pivots at the node's CENTER (CANVAS_W/2, CANVAS_H/2).
         *   That shifts the visual origin by (scale-1)*CANVAS_W/2, which the translate
         *   doesn't know about → wrong clamping when zoomed in.
         *   Using a Scale transform with pivot (0,0) keeps everything consistent.
         * ─────────────────────────────────────────────────────────────────────
         */
        double vw    = mapContainer.getWidth();
        double vh    = mapContainer.getHeight();
        double rendW = CANVAS_W * scale;
        double rendH = CANVAS_H * scale;

        /*
         * CLAMPING — hard bounds, no positive overscroll.
         *
         * transX=0 means map left edge is at container left edge.
         * transX>0 would push map right, revealing blank space on the left
         * through which the sidebar could bleed in. We never allow this.
         *
         * transX = -(rendW - vw) means map right edge is at container right edge.
         * Going further left would show blank space on the right.
         *
         * If the map is narrower than the viewport (low zoom), we centre it.
         */
        if (rendW >= vw) {
            // Map wider than viewport: constrain left/right edges
            transX = Math.max(-(rendW - vw), Math.min(0, transX));
        } else {
            // Map narrower than viewport: centre horizontally
            transX = (vw - rendW) / 2.0;
        }

        if (rendH >= vh) {
            transY = Math.max(-(rendH - vh), Math.min(0, transY));
        } else {
            transY = (vh - rendH) / 2.0;
        }

        // Apply: Scale(pivot=0,0) first, then translate
        mapScale.setX(scale);
        mapScale.setY(scale);
        mapGroup.setTranslateX(transX);
        mapGroup.setTranslateY(transY);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateStatus(String t) {
        if (statusLabel == null) return;
        Platform.runLater(() -> {
            statusLabel.setText(t);
            statusLabel.setVisible(!t.isEmpty());
            statusLabel.setManaged(!t.isEmpty());
        });
    }

    private void updatePinCount(int n) {
        if (pinCountLabel == null) return;
        Platform.runLater(() -> {
            pinCountLabel.setText(n + " place" + (n == 1 ? "" : "s") + " on the map");
            pinCountLabel.setVisible(true);
            pinCountLabel.setManaged(true);
        });
    }

    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private String toHexStr(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}