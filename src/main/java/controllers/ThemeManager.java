package controllers;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ThemeManager — singleton that swaps light/dark CSS on every registered scene.
 *
 * WHY DARK MODE WASN'T WORKING — two separate root causes:
 *
 * 1. post_card.fxml had stylesheets="@../styles/dashboard.css" on its root node.
 *    This creates an isolated style scope — Scene-level dark CSS doesn't reach it.
 *    FIX: removed the stylesheets attribute from post_card.fxml and post_detail.fxml.
 *
 * 2. DashboardController.showPostFormPanel() sets hardcoded inline styles like
 *    setStyle("-fx-background-color: white"). Inline styles always beat CSS rules.
 *    FIX: addChangeListener() lets controllers update their inline-styled nodes
 *    whenever the theme toggles.
 */
public class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();
    public static ThemeManager get() { return INSTANCE; }
    private ThemeManager() {}

    private boolean dark = false;
    private final Set<Scene>     scenes    = new HashSet<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private static final String LIGHT = "/styles/dashboard.css";
    private static final String DARK  = "/styles/dashboard-dark.css";

    public void register(Scene scene) {
        if (scene == null) return;
        scenes.add(scene);
        String lightUrl = toExternalForm(LIGHT);
        if (lightUrl != null && !scene.getStylesheets().contains(lightUrl))
            scene.getStylesheets().add(0, lightUrl);
        applyToScene(scene);
    }

    public void unregister(Scene scene) { scenes.remove(scene); }

    public void toggle() {
        dark = !dark;
        scenes.forEach(this::applyToScene);
        listeners.forEach(Runnable::run);
    }

    public boolean isDark() { return dark; }

    /**
     * Subscribe to theme changes.
     * Use in any controller that has inline setStyle() calls so those nodes
     * can update their colours when the user toggles dark mode.
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void applyToPane(DialogPane pane) {
        if (pane == null) return;
        String lightUrl = toExternalForm(LIGHT);
        String darkUrl  = toExternalForm(DARK);
        if (lightUrl != null && !pane.getStylesheets().contains(lightUrl))
            pane.getStylesheets().add(0, lightUrl);
        pane.getStylesheets().remove(darkUrl);
        if (dark && darkUrl != null)
            pane.getStylesheets().add(darkUrl);
    }

    private void applyToScene(Scene scene) {
        String darkUrl = toExternalForm(DARK);
        if (darkUrl == null) return;
        scene.getStylesheets().remove(darkUrl);
        if (dark) scene.getStylesheets().add(darkUrl);
    }

    private static String toExternalForm(String path) {
        URL url = ThemeManager.class.getResource(path);
        return url != null ? url.toExternalForm() : null;
    }
}