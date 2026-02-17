package controllers;

import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

/**
 * ThemeManager — singleton that tracks the current theme and applies it
 * to every registered scene (main window, dialogs, detail overlays).
 *
 * Usage:
 *   ThemeManager.get().register(scene);   // called once per scene
 *   ThemeManager.get().toggle();          // called by the toggle button
 *   ThemeManager.get().isDark();          // read current state
 */
public class ThemeManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final ThemeManager INSTANCE = new ThemeManager();
    public static ThemeManager get() { return INSTANCE; }
    private ThemeManager() {}

    // ── CSS paths ─────────────────────────────────────────────────────────────
    public static final String LIGHT_CSS = "/styles/dashboard.css";
    public static final String DARK_CSS  = "/styles/dashboard-dark.css";

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean dark = false;
    private final List<Scene> scenes = new ArrayList<>();

    // ── Register a scene so it gets theme updates ─────────────────────────────
    public void register(Scene scene) {
        if (scene != null && !scenes.contains(scene)) {
            scenes.add(scene);
            apply(scene);
        }
    }

    public void unregister(Scene scene) {
        scenes.remove(scene);
    }

    // ── Toggle between light and dark ─────────────────────────────────────────
    public void toggle() {
        dark = !dark;
        scenes.forEach(this::apply);
    }

    public boolean isDark() { return dark; }

    // ── Apply current theme to a single scene ─────────────────────────────────
    public void apply(Scene scene) {
        if (scene == null) return;
        String toAdd    = dark ? darkUrl()  : lightUrl();
        String toRemove = dark ? lightUrl() : darkUrl();
        scene.getStylesheets().remove(toRemove);
        if (!scene.getStylesheets().contains(toAdd)) {
            scene.getStylesheets().add(toAdd);
        }
    }

    // ── Apply to a DialogPane (dialogs have their own stylesheet list) ─────────
    public void apply(javafx.scene.control.DialogPane pane) {
        if (pane == null) return;
        String toAdd    = dark ? darkUrl()  : lightUrl();
        String toRemove = dark ? lightUrl() : darkUrl();
        pane.getStylesheets().remove(toRemove);
        if (!pane.getStylesheets().contains(toAdd)) {
            pane.getStylesheets().add(toAdd);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String lightUrl() {
        return ThemeManager.class.getResource(LIGHT_CSS).toExternalForm();
    }

    private String darkUrl() {
        return ThemeManager.class.getResource(DARK_CSS).toExternalForm();
    }
}