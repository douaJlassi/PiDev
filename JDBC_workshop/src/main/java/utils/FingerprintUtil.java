package utils;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import java.io.*;
import java.util.*;

public class FingerprintUtil {

    private SerialPort serialPort;
    private BufferedReader reader;
    private OutputStream outputStream;
    private boolean connected = false;
    private String connectedPort = "";

    // For reading Arduino output
    private StringBuilder currentResponse = new StringBuilder();
    private boolean readingComplete = false;

    // Store mappings between fingerprint IDs and user IDs
    private Map<Integer, Integer> fingerprintToUserMap = new HashMap<>();

    // Singleton instance
    private static FingerprintUtil instance;

    private FingerprintUtil() {
        System.out.println("🔧 FingerprintUtil initialized");
    }

    public static FingerprintUtil getInstance() {
        if (instance == null) {
            instance = new FingerprintUtil();
        }
        return instance;
    }

    /**
     * Connect to fingerprint sensor via serial port
     */
    public boolean connect(String portName) {
        try {
            System.out.println("🔌 Connecting to fingerprint sensor on " + portName);

            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(9600); // Match Arduino's baud rate
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

            if (serialPort.openPort()) {
                reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
                outputStream = serialPort.getOutputStream();
                connected = true;
                connectedPort = portName;

                // Wait for Arduino to initialize
                Thread.sleep(2000);

                // Clear any initial messages
                while (reader.ready()) {
                    reader.readLine();
                }

                System.out.println("✅ Fingerprint sensor connected on " + portName);
                return true;
            } else {
                System.out.println("❌ Failed to open port " + portName);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error connecting to sensor: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Disconnect from fingerprint sensor
     */
    public void disconnect() {
        try {
            connected = false;
            if (reader != null) reader.close();
            if (outputStream != null) outputStream.close();
            if (serialPort != null && serialPort.isOpen()) serialPort.closePort();
            System.out.println("🔌 Fingerprint sensor disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send command to Arduino
     */
    public void sendCommand(String command) {
        if (!connected || outputStream == null) {
            System.out.println("❌ Not connected to sensor");
            return;
        }

        try {
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();
            System.out.println("📤 Sent command: " + command);

            // Small delay for Arduino to process
            Thread.sleep(100);
        } catch (Exception e) {
            System.err.println("❌ Error sending command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Read response from Arduino
     */
    public String readResponse(int timeoutMs) {
        if (!connected || reader == null) return null;

        StringBuilder response = new StringBuilder();
        long startTime = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (reader.ready()) {
                    String line = reader.readLine();
                    if (line != null) {
                        response.append(line).append("\n");
                        System.out.println("📥 Arduino: " + line);
                    }
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response.length() > 0 ? response.toString() : null;
    }

    /**
     * Enroll a new fingerprint for a user
     */
    public boolean enrollFingerprint(int userId) {
        if (!connected) {
            System.out.println("❌ Not connected to sensor");
            return false;
        }

        try {
            System.out.println("📝 Starting fingerprint enrollment for user ID: " + userId);

            // Clear any pending data
            while (reader.ready()) reader.readLine();

            // Send 'e' command to start enrollment
            sendCommand("e");

            // Wait for Arduino to ask for ID
            String response = waitForPrompt("Entrez l'ID", 10000);

            if (response != null) {
                // Send the user ID as fingerprint ID
                sendCommand(String.valueOf(userId));

                // Wait for first finger prompt
                response = waitForPrompt("Placez votre doigt", 10000);

                if (response != null) {
                    System.out.println("⏳ Waiting for first finger...");

                    // Wait for first scan to complete
                    response = waitForSuccess("Première capture OK", 30000);

                    if (response != null) {
                        System.out.println("✅ First scan successful");

                        // Wait for second finger prompt
                        response = waitForPrompt("Placez le MÊME doigt", 10000);

                        if (response != null) {
                            System.out.println("⏳ Waiting for second finger...");

                            // Wait for second scan to complete
                            response = waitForSuccess("Deuxième capture OK", 30000);

                            if (response != null) {
                                System.out.println("✅ Second scan successful");

                                // Wait for verification result
                                response = waitForAny(new String[]{
                                        "Les deux empreintes correspondent",
                                        "Les empreintes sont différentes"
                                }, 10000);

                                if (response != null && response.contains("correspondent")) {
                                    System.out.println("✅ Fingerprints match!");

                                    // Wait for storage confirmation
                                    response = waitForSuccess("ENREGISTREMENT RÉUSSI", 10000);

                                    if (response != null) {
                                        System.out.println("✅ Fingerprint stored successfully!");

                                        // Add mapping
                                        fingerprintToUserMap.put(userId, userId);

                                        return true;
                                    }
                                } else {
                                    System.out.println("❌ Fingerprints don't match!");
                                    return false;
                                }
                            }
                        }
                    }
                }
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Search for a fingerprint and return the user ID
     */
    public int searchFingerprint() {
        if (!connected) return -1;

        try {
            System.out.println("🔍 Searching for fingerprint...");

            // Clear any pending data
            while (reader.ready()) reader.readLine();

            // Send 's' command to search
            sendCommand("s");

            // Wait for finger prompt
            String response = waitForPrompt("Placez votre doigt", 10000);

            if (response != null) {
                System.out.println("⏳ Waiting for finger...");

                // Wait for search result
                response = waitForAny(new String[]{
                        "EMPREINTE TROUVÉE",
                        "AUCUNE CORRESPONDANCE"
                }, 30000);

                if (response != null && response.contains("TROUVÉE")) {
                    // Extract fingerprint ID from response
                    int fingerprintId = extractIdFromResponse(response);
                    System.out.println("✅ Found fingerprint ID: " + fingerprintId);

                    // Map to user ID (in this implementation, they're the same)
                    return fingerprintId;
                }
            }

            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Verify a specific fingerprint ID
     */
    public boolean verifyFingerprintId(int fingerprintId) {
        if (!connected) return false;

        try {
            System.out.println("🔍 Verifying fingerprint for ID: " + fingerprintId);

            // Clear any pending data
            while (reader.ready()) reader.readLine();

            // Send 's' command
            sendCommand("s");

            // Wait for ID prompt
            String response = waitForPrompt("Entrez l'ID", 10000);

            if (response != null) {
                // Send the ID to verify
                sendCommand(String.valueOf(fingerprintId));

                // Wait for finger prompt
                response = waitForPrompt("Placez votre doigt", 10000);

                if (response != null) {
                    System.out.println("⏳ Waiting for finger...");

                    // Wait for verification result
                    response = waitForAny(new String[]{
                            "ACCÈS AUTORISÉ",
                            "ACCÈS REFUSÉ"
                    }, 30000);

                    return response != null && response.contains("AUTORISÉ");
                }
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete fingerprint for a specific ID
     */
    public boolean deleteFingerprint(int fingerprintId) {
        if (!connected) return false;

        try {
            System.out.println("🗑️ Deleting fingerprint for ID: " + fingerprintId);

            // Clear any pending data
            while (reader.ready()) reader.readLine();

            // Send 'd' command
            sendCommand("d");

            // Wait for ID prompt
            String response = waitForPrompt("ID à supprimer", 10000);

            if (response != null) {
                // Send the ID to delete
                sendCommand(String.valueOf(fingerprintId));

                // Wait for deletion result
                response = waitForAny(new String[]{
                        "succès",
                        "Aucune empreinte"
                }, 5000);

                boolean success = response != null && response.contains("succès");

                if (success) {
                    fingerprintToUserMap.remove(fingerprintId);
                }

                return success;
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper method to wait for a specific prompt from Arduino
     */
    private String waitForPrompt(String prompt, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        StringBuilder response = new StringBuilder();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    response.append(line).append("\n");
                    System.out.println("📥 Arduino: " + line);

                    if (line.contains(prompt)) {
                        return response.toString();
                    }
                }
            }
            Thread.sleep(10);
        }

        return null;
    }

    /**
     * Helper method to wait for a success message
     */
    private String waitForSuccess(String successMsg, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        StringBuilder response = new StringBuilder();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    response.append(line).append("\n");
                    System.out.println("📥 Arduino: " + line);

                    if (line.contains(successMsg)) {
                        return response.toString();
                    }
                }
            }
            Thread.sleep(10);
        }

        return null;
    }

    /**
     * Helper method to wait for any of multiple possible responses
     */
    private String waitForAny(String[] expected, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        StringBuilder response = new StringBuilder();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    response.append(line).append("\n");
                    System.out.println("📥 Arduino: " + line);

                    for (String exp : expected) {
                        if (line.contains(exp)) {
                            return response.toString();
                        }
                    }
                }
            }
            Thread.sleep(10);
        }

        return null;
    }

    /**
     * Extract fingerprint ID from Arduino response
     */
    private int extractIdFromResponse(String response) {
        try {
            // Look for "ID: X" pattern
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("ID:")) {
                    String[] parts = line.split("ID:");
                    if (parts.length > 1) {
                        String numPart = parts[1].trim().split(" ")[0];
                        return Integer.parseInt(numPart);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Get list of available COM ports
     */
    public static String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            portNames[i] = ports[i].getSystemPortName();
        }
        return portNames;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getConnectedPort() {
        return connectedPort;
    }

    public void addMapping(int fingerprintId, int userId) {
        fingerprintToUserMap.put(fingerprintId, userId);
    }

    public int getUserIdFromFingerprintId(int fingerprintId) {
        return fingerprintToUserMap.getOrDefault(fingerprintId, -1);
    }
}