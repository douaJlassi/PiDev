package controllers;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * ThemeManager — singleton, swaps light/dark CSS on every registered scene.
 *
 * HOW IT WORKS
 *  - dashboard.css  is always loaded first (via FXML stylesheets attribute).
 *  - Switching to dark adds dashboard-dark.css on top (cascade overrides colours).
 *  - Switching back removes dashboard-dark.css.
 *  - Dialogs / detail views don't have scenes of their own, so they expose
 *    a DialogPane overload that patches the pane's stylesheet list directly.
 *
 * USAGE
 *  ThemeManager.get().register(scene);   // once, after scene is attached
 *  ThemeManager.get().toggle();          // button handler
 *  ThemeManager.get().isDark();          // read state
 *  ThemeManager.get().applyToPane(pane); // for dialogs
 */
public class ThemeManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final ThemeManager INSTANCE = new ThemeManager();
    public static ThemeManager get() { return INSTANCE; }
    private ThemeManager() {}

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean dark = false;
    private final Set<Scene> scenes = new HashSet<>();

    // ── CSS resource paths (relative to classpath root) ───────────────────────
    private static final String LIGHT = "/styles/dashboard.css";
    private static final String DARK  = "/styles/dashboard-dark.css";

    // ── Public API ────────────────────────────────────────────────────────────
    public void register(Scene scene) {
        if (scene == null) return;
        scenes.add(scene);
        applyToScene(scene);
    }

    public void unregister(Scene scene) { scenes.remove(scene); }

    public void toggle() {
        dark = !dark;
        scenes.forEach(this::applyToScene);
    }

    public boolean isDark() { return dark; }

    /** Apply current theme to a dialog pane (dialogs have no Scene of their own). */
    public void applyToPane(DialogPane pane) {
        if (pane == null) return;
        String light = toExternalForm(LIGHT);
        String darkE  = toExternalForm(DARK);
        if (light != null && !pane.getStylesheets().contains(light))
            pane.getStylesheets().add(light);
        if (dark) {
            if (darkE != null && !pane.getStylesheets().contains(darkE))
                pane.getStylesheets().add(darkE);
        } else {
            pane.getStylesheets().remove(darkE);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────
    private void applyToScene(Scene scene) {
        String darkUrl = toExternalForm(DARK);
        if (darkUrl == null) return; // CSS file not found on classpath — skip

        if (dark) {
            if (!scene.getStylesheets().contains(darkUrl))
                scene.getStylesheets().add(darkUrl);
        } else {
            scene.getStylesheets().remove(darkUrl);
        }
    }

    /** Resolves a classpath resource to an external URL string, or null if missing. */
    private static String toExternalForm(String path) {
        URL url = ThemeManager.class.getResource(path);
        return url != null ? url.toExternalForm() : null;
    }
}