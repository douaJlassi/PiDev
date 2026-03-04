package utils;

import java.io.*;
import java.net.*;
import java.util.*;

public class FingerprintSocket implements FingerprintInterface {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private String connectedPort = "";
    private Map<Integer, Integer> fingerprintToUserMap = new HashMap<>();

    private static final String HOST = "localhost";
    private static final int PORT = 9999;

    public FingerprintSocket() {
        System.out.println("🔧 FingerprintSocket initialized");
    }

    @Override
    public boolean connect(String portName) {
        try {
            System.out.println("🔌 Connecting to bridge server...");
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send connection test
            out.println("CONNECT");
            String response = in.readLine();

            if ("OK".equals(response)) {
                connected = true;
                connectedPort = portName;
                System.out.println("✅ Connected to bridge server");
                return true;
            } else {
                System.err.println("❌ Bridge server returned: " + response);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to bridge: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (out != null) {
                out.println("DISCONNECT");
            }
            if (socket != null) {
                socket.close();
            }
            connected = false;
            System.out.println("🔌 Disconnected from bridge");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendCommand(String command) {
        if (!connected || out == null) {
            System.out.println("❌ Not connected to bridge");
            return;
        }

        out.println("SEND:" + command);
        System.out.println("📤 Sent to bridge: " + command);
    }

    @Override
    public String readResponse(int timeoutMs) {
        if (!connected || in == null) return null;

        try {
            socket.setSoTimeout(timeoutMs);
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                System.out.println("📥 Bridge: " + line);
                if (line.contains("TIMEOUT")) {
                    break;
                }
            }

            return response.length() > 0 ? response.toString() : null;

        } catch (SocketTimeoutException e) {
            // Timeout is expected
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean enrollFingerprint(int userId) {
        if (!connected) return false;

        System.out.println("📝 Starting fingerprint enrollment for user ID: " + userId);

        sendCommand("e");
        String response = readResponse(5000);

        if (response != null && response.contains("Entrez l'ID")) {
            sendCommand(String.valueOf(userId));
            response = readResponse(30000); // Wait for enrollment to complete

            boolean success = response != null &&
                    (response.contains("ENREGISTREMENT RÉUSSI") ||
                            response.contains("🎉"));

            if (success) {
                fingerprintToUserMap.put(userId, userId);
            }

            return success;
        }

        return false;
    }

    @Override
    public int searchFingerprint() {
        if (!connected) return -1;

        System.out.println("🔍 Searching for fingerprint...");

        sendCommand("s");
        String response = readResponse(30000);

        if (response != null && response.contains("TROUVÉE")) {
            // Extract ID from response
            int id = extractIdFromResponse(response);
            if (id > 0) {
                return id;
            }
        }

        return -1;
    }

    @Override
    public boolean verifyFingerprintId(int fingerprintId) {
        if (!connected) return false;

        sendCommand("s");
        String response = readResponse(5000);

        if (response != null && response.contains("Entrez l'ID")) {
            sendCommand(String.valueOf(fingerprintId));
            response = readResponse(30000);

            return response != null && response.contains("AUTORISÉ");
        }

        return false;
    }

    @Override
    public boolean deleteFingerprint(int fingerprintId) {
        if (!connected) return false;

        sendCommand("d");
        String response = readResponse(5000);

        if (response != null && response.contains("ID à supprimer")) {
            sendCommand(String.valueOf(fingerprintId));
            response = readResponse(5000);

            return response != null && response.contains("succès");
        }

        return false;
    }

    @Override
    public void addMapping(int fingerprintId, int userId) {
        fingerprintToUserMap.put(fingerprintId, userId);
    }

    @Override
    public int getUserIdFromFingerprintId(int fingerprintId) {
        return fingerprintToUserMap.getOrDefault(fingerprintId, -1);
    }

    @Override
    public Map<Integer, Integer> getAllMappings() {
        return new HashMap<>(fingerprintToUserMap);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String getConnectedPort() {
        return connectedPort;
    }

    private int extractIdFromResponse(String response) {
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("ID:")) {
                    String[] parts = line.split("ID:");
                    if (parts.length > 1) {
                        return Integer.parseInt(parts[1].trim().split(" ")[0]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String[] getAvailablePorts() {
        // Return the configured Arduino port
        return new String[]{"COM3 (Arduino)"};
    }
}