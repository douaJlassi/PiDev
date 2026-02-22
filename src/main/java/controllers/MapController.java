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
    private DashboardController dashboard;
    private BorderPane          rootPane;

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

    public void show(DashboardController dash) {
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

    @FXML private void onClose() {
        dashboard.getContentContainer().getChildren().remove(rootPane);
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
        // Use explicit Scale(pivot=0,0) instead of setScaleX/Y so zoom math is consistent
        mapGroup.getTransforms().add(mapScale);

        mapContainer.setStyle("-fx-background-color: " + toHexStr(OCEAN) + ";");
        mapContainer.getChildren().add(0, mapGroup);

        // Dismiss popup on click (handled in mapContainer.setOnMouseClicked below,
        // set up in wirePan — we just need the canvas click to bubble up)
        mapCanvas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) dismissPopup();
        });

        wirePan();
        wireZoom();

        // Centre on Mediterranean / Tunisia once container has its size
        mapContainer.widthProperty().addListener((o, ov, nv) -> { if (!centred) initialView(); });
        mapContainer.heightProperty().addListener((o, ov, nv) -> { if (!centred) initialView(); });

        // Load GeoJSON and draw map on background thread, then geocode posts
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
    private void addPin(Publication pub, double lat, double lon,
                        int likes, int comments) {
        double x = lonToX(lon);
        double y = latToY(lat);

        // Teardrop SVG path, tip at bottom-centre (0,24), head at (0,0)
        SVGPath pin = new SVGPath();
        pin.setContent(
                "M10,0 C4.477,0 0,4.477 0,10 C0,17.5 10,24 10,24 " +
                        "C10,24 20,17.5 20,10 C20,4.477 15.523,0 10,0 Z");
        pin.setFill(Color.web(pub.hasImage() ? TEAL_DK : TEAL));
        pin.setStroke(Color.web(WHITE));
        pin.setStrokeWidth(1.5);

        // Inner white dot
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(3.5, Color.WHITE);
        dot.setLayoutX(10);
        dot.setLayoutY(10);

        Pane pinGroup = new Pane(pin, dot);
        pinGroup.setPrefSize(20, 24);
        // Centre pin horizontally, tip at the coordinate point
        pinGroup.setLayoutX(x - 10);
        pinGroup.setLayoutY(y - 24);
        pinGroup.setCursor(Cursor.HAND);

        // Hover animation
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

        // Click: show popup positioned in screen space
        pinGroup.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                // Convert pin's map position to screen position for popup placement
                double screenX = x * scale + transX;
                double screenY = y * scale + transY;
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

        // ── Teal header (fixed 40px) ──
        HBox header = new HBox();
        header.setPrefHeight(40);
        header.setMaxHeight(40);
        header.setPadding(new Insets(0, 14, 0, 14));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: " + TEAL + ";" +
                        "-fx-background-radius: 12 12 0 0;"
        );
        Label placeLabel = new Label("📍  " + trunc(pub.getPlace(), 26));
        placeLabel.setStyle(
                "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 12px;");
        placeLabel.setMaxWidth(230);
        header.getChildren().add(placeLabel);

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
        mapContainer.getChildren().add(card);

        FadeTransition ft = new FadeTransition(Duration.millis(160), card);
        ft.setToValue(1.0);
        ft.play();

        activePopup = card;
    }

    private void dismissPopup() {
        if (activePopup != null) {
            mapContainer.getChildren().remove(activePopup);
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

            Platform.runLater(() -> updateStatus("Mapping " + withPlace.size() + " posts…"));

            AtomicInteger pending  = new AtomicInteger(withPlace.size());
            AtomicInteger pinsDone = new AtomicInteger(0);

            for (Publication pub : withPlace) {
                geocodingService.geocode(pub.getPlace())
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
                            }
                            if (pending.decrementAndGet() == 0)
                                Platform.runLater(() -> updateStatus(""));
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
        double vw     = mapContainer.getWidth();
        double vh     = mapContainer.getHeight();
        double margin = 50.0;
        double rendW  = CANVAS_W * scale;
        double rendH  = CANVAS_H * scale;

        // Clamp X
        double minTX = Math.min(margin, vw - rendW - margin); // allow small maps to float
        double maxTX = margin;
        transX = Math.max(minTX, Math.min(maxTX, transX));

        // Clamp Y
        double minTY = Math.min(margin, vh - rendH - margin);
        double maxTY = margin;
        transY = Math.max(minTY, Math.min(maxTY, transY));

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