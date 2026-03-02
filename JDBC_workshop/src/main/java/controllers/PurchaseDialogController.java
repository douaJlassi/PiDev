package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.ResourceBundle;

public class PurchaseDialogController implements Initializable {

    @FXML
    private Label productNameLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextArea addressArea;
    @FXML
    private TextArea instructionsArea;
    @FXML
    private WebView mapView;
    @FXML
    private TextField mapSearchField;
    @FXML
    private Button searchLocationBtn;
    @FXML
    private Button getCurrentLocationBtn;
    @FXML
    private Button clearLocationBtn;
    @FXML
    private Label latLabel;
    @FXML
    private Label lngLabel;
    @FXML
    private ButtonType confirmButtonType;
    @FXML
    private ButtonType cancelButtonType;

    private WebEngine webEngine;
    private StringProperty selectedAddress = new SimpleStringProperty();
    private double selectedLat = 0;
    private double selectedLng = 0;

    private String productName;
    private int productPrice;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupMap();
        setupListeners();
    }

    public void setProductDetails(String name, int price) {
        this.productName = name;
        this.productPrice = price;
        productNameLabel.setText("Complete Your Purchase: " + name);
        priceLabel.setText("Price: " + price + " 🪙");
    }

    public void setUserDetails(String username, String email) {
        nameField.setText(username);
        emailField.setText(email);
    }

    private void setupMap() {
        webEngine = mapView.getEngine();

        // Load OpenStreetMap with Leaflet
        String mapHTML = getMapHTML();
        webEngine.loadContent(mapHTML);

        // Add Java bridge for JavaScript callbacks
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", new JavaBridge());
            }
        });
    }

    private void setupListeners() {
        // Search location
        searchLocationBtn.setOnAction(e -> searchLocation(mapSearchField.getText()));
        mapSearchField.setOnAction(e -> searchLocation(mapSearchField.getText()));

        // Get current location
        getCurrentLocationBtn.setOnAction(e -> getCurrentLocation());

        // Clear selected location
        clearLocationBtn.setOnAction(e -> clearLocation());

        // Address binding
        addressArea.textProperty().bind(selectedAddress);
    }

    private void searchLocation(String query) {
        if (query != null && !query.trim().isEmpty()) {
            webEngine.executeScript("searchLocation('" + query.replace("'", "\\'") + "')");
        }
    }

    private void getCurrentLocation() {
        webEngine.executeScript("getCurrentLocation()");
    }

    private void clearLocation() {
        webEngine.executeScript("clearMarker()");
        selectedAddress.set("");
        selectedLat = 0;
        selectedLng = 0;
        latLabel.setText("--");
        lngLabel.setText("--");
    }

    // Java bridge for JavaScript callbacks
    public class JavaBridge {
        public void setLocation(String address, double lat, double lng) {
            selectedAddress.set(address);
            selectedLat = lat;
            selectedLng = lng;

            javafx.application.Platform.runLater(() -> {
                latLabel.setText(String.format("%.6f", lat));
                lngLabel.setText(String.format("%.6f", lng));
            });
        }
    }

    private String getMapHTML() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                * { margin: 0; padding: 0; }
                html, body, #map { 
                    height: 100%; 
                    width: 100%; 
                    margin: 0; 
                    padding: 0;
                }
                .leaflet-container { 
                    background: #f8faff;
                    height: 100%;
                }
                .leaflet-control-attribution {
                    font-size: 9px;
                }
                .loading {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    background: white;
                    padding: 10px 20px;
                    border-radius: 5px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    z-index: 1000;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <div class="loading" id="loading">Loading map...</div>
            
            <script>
                var map;
                var marker = null;
                
                function initMap() {
                    try {
                        // Set default view to Tunis
                        map = L.map('map').setView([36.8065, 10.1815], 13);
                        
                        // Add OpenStreetMap tiles
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
                            maxZoom: 19,
                            detectRetina: true
                        }).addTo(map);
                        
                        // Remove loading message
                        document.getElementById('loading').style.display = 'none';
                        
                        // Handle map click
                        map.on('click', function(e) {
                            var lat = e.latlng.lat;
                            var lng = e.latlng.lng;
                            
                            // Reverse geocoding with timeout
                            fetchWithTimeout(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`, {
                                timeout: 5000
                            })
                            .then(response => response.json())
                            .then(data => {
                                var address = data.display_name || 'Selected location';
                                setMarker(lat, lng, address);
                                if (window.javaApp) {
                                    window.javaApp.setLocation(address, lat, lng);
                                }
                            })
                            .catch(error => {
                                console.error('Reverse geocoding failed:', error);
                                var address = `Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
                                setMarker(lat, lng, address);
                                if (window.javaApp) {
                                    window.javaApp.setLocation(address, lat, lng);
                                }
                            });
                        });
                        
                        // Try to get user's location
                        if (navigator.geolocation) {
                            navigator.geolocation.getCurrentPosition(
                                function(position) {
                                    var lat = position.coords.latitude;
                                    var lng = position.coords.longitude;
                                    map.setView([lat, lng], 15);
                                },
                                function(error) {
                                    console.log('Geolocation error:', error);
                                }
                            );
                        }
                        
                    } catch (error) {
                        console.error('Map initialization error:', error);
                        document.getElementById('loading').innerHTML = 'Failed to load map. Please try again.';
                    }
                }
                
                function fetchWithTimeout(url, options = {}) {
                    const { timeout = 5000 } = options;
                    return Promise.race([
                        fetch(url),
                        new Promise((_, reject) => 
                            setTimeout(() => reject(new Error('Request timeout')), timeout)
                        )
                    ]);
                }
                
                function setMarker(lat, lng, address) {
                    try {
                        if (marker) {
                            marker.setLatLng([lat, lng]);
                        } else {
                            marker = L.marker([lat, lng], { 
                                draggable: true,
                                autoPan: true
                            }).addTo(map);
                            
                            marker.on('dragend', function(e) {
                                var pos = e.target.getLatLng();
                                fetchWithTimeout(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${pos.lat}&lon=${pos.lng}&zoom=18&addressdetails=1`)
                                    .then(response => response.json())
                                    .then(data => {
                                        var address = data.display_name || `Location (${pos.lat.toFixed(6)}, ${pos.lng.toFixed(6)})`;
                                        marker.bindPopup(address).openPopup();
                                        if (window.javaApp) {
                                            window.javaApp.setLocation(address, pos.lat, pos.lng);
                                        }
                                    })
                                    .catch(() => {
                                        var address = `Location (${pos.lat.toFixed(6)}, ${pos.lng.toFixed(6)})`;
                                        marker.bindPopup(address).openPopup();
                                        if (window.javaApp) {
                                            window.javaApp.setLocation(address, pos.lat, pos.lng);
                                        }
                                    });
                            });
                        }
                        
                        marker.bindPopup(address).openPopup();
                        map.setView([lat, lng], 16);
                    } catch (error) {
                        console.error('Error setting marker:', error);
                    }
                }
                
                function clearMarker() {
                    if (marker) {
                        map.removeLayer(marker);
                        marker = null;
                    }
                }
                
                function searchLocation(query) {
                    if (!query || query.trim() === '') return;
                    
                    fetchWithTimeout(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`)
                        .then(response => response.json())
                        .then(data => {
                            if (data && data.length > 0) {
                                var lat = parseFloat(data[0].lat);
                                var lng = parseFloat(data[0].lon);
                                var displayName = data[0].display_name;
                                setMarker(lat, lng, displayName);
                                if (window.javaApp) {
                                    window.javaApp.setLocation(displayName, lat, lng);
                                }
                            } else {
                                alert('Location not found');
                            }
                        })
                        .catch(error => {
                            console.error('Search error:', error);
                            alert('Search failed. Please try again.');
                        });
                }
                
                function getCurrentLocation() {
                    if (navigator.geolocation) {
                        navigator.geolocation.getCurrentPosition(
                            function(position) {
                                var lat = position.coords.latitude;
                                var lng = position.coords.longitude;
                                
                                fetchWithTimeout(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`)
                                    .then(response => response.json())
                                    .then(data => {
                                        var address = data.display_name || `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
                                        setMarker(lat, lng, address);
                                        if (window.javaApp) {
                                            window.javaApp.setLocation(address, lat, lng);
                                        }
                                    })
                                    .catch(() => {
                                        var address = `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`;
                                        setMarker(lat, lng, address);
                                        if (window.javaApp) {
                                            window.javaApp.setLocation(address, lat, lng);
                                        }
                                    });
                            },
                            function(error) {
                                alert('Unable to get your location: ' + error.message);
                            }
                        );
                    } else {
                        alert('Geolocation is not supported by your browser');
                    }
                }
                
                // Initialize map when page loads
                window.onload = initMap;
            </script>
        </body>
        </html>
    """;
    }

    // Getters for purchase data
    public String getBuyerName() { return nameField.getText(); }
    public String getBuyerEmail() { return emailField.getText(); }
    public String getBuyerPhone() { return phoneField.getText(); }
    public String getBuyerAddress() { return selectedAddress.get(); }
    public String getDeliveryInstructions() { return instructionsArea.getText(); }
    public double getSelectedLat() { return selectedLat; }
    public double getSelectedLng() { return selectedLng; }
    public boolean isAddressSelected() { return selectedLat != 0 && selectedLng != 0; }
}