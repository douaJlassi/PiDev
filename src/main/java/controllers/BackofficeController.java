package controllers;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.io.File; // <--- AJOUTE CET IMPORT

public class BackofficeController {

    @FXML
    private WebView webViewKibana;

    @FXML
    public void initialize() {
        WebEngine engine = webViewKibana.getEngine();

        String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "kibana_cache_" + System.currentTimeMillis();
        File userDataDir = new File(tempDir);
        if (!userDataDir.exists()) userDataDir.mkdirs();

        engine.setUserDataDirectory(userDataDir);
        engine.setJavaScriptEnabled(true);

        String kibanaURL = "http://localhost:5601/app/dashboards#/view/d00f58a0-11dc-11f1-b0ed-15b435a85f1b?embed=true&_g=(filters%3A!()%2CrefreshInterval%3A(pause%3A!t%2Cvalue%3A0)%2Ctime%3A(from%3Anow-1y%2Cto%3Anow))";

        engine.load(kibanaURL);

        webViewKibana.setContextMenuEnabled(false);
    }
}