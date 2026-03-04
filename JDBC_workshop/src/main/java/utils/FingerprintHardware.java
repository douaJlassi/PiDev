package utils;

import com.fazecast.jSerialComm.SerialPort;
import java.io.*;
import java.util.*;

public class FingerprintHardware implements FingerprintInterface {

    private SerialPort serialPort;
    private BufferedReader reader;
    private OutputStream outputStream;
    private boolean connected = false;
    private String connectedPort = "";
    private Map<Integer, Integer> fingerprintToUserMap = new HashMap<>();

    static {
        // Force the architecture to x86_64 to fix DLL loading issue
        System.setProperty("os.arch", "x86_64");
        System.out.println("🖥️ Forced architecture to: " + System.getProperty("os.arch"));
    }

    public FingerprintHardware() {
        System.out.println("🔧 FingerprintHardware initialized");
    }

    @Override
    public boolean connect(String portName) {
        try {
            System.out.println("🔌 Connecting to fingerprint sensor on " + portName);

            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(9600);
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
                System.err.println("❌ Failed to open port " + portName);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Connection error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
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

    @Override
    public void sendCommand(String command) {
        if (!connected || outputStream == null) {
            System.out.println("❌ Not connected to sensor");
            return;
        }
        try {
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();
            System.out.println("📤 Sent: " + command);
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
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

    @Override
    public boolean enrollFingerprint(int userId) {
        if (!connected) return false;

        try {
            System.out.println("📝 Starting fingerprint enrollment for user ID: " + userId);

            // Clear any pending data
            while (reader.ready()) {
                reader.readLine();
            }

            // Send 'e' command to start enrollment
            sendCommand("e8");

            // Wait longer for Arduino to process
            Thread.sleep(2000);

            // Read and discard all menu lines until we get to the ID prompt
            String line;
            boolean idPromptDetected = false;
            long startTime = System.currentTimeMillis();

            // Keep reading for up to 10 seconds to capture the ID prompt
            while (System.currentTimeMillis() - startTime < 10000) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        System.out.println("📥 Arduino: " + line);

                        // Check if this is the ID prompt
                        if (line.contains("Entrez l'ID") || line.contains("entrez l'ID")) {
                            idPromptDetected = true;
                            System.out.println("✅ ID prompt detected!");

                            // Wait a moment before sending ID
                            Thread.sleep(500);

                            // Send the ID as a clean string without any extra characters
                            System.out.println("🤖 Automatically sending ID: " + userId);

                            // Send as bytes to ensure clean transmission
                            String idStr = String.valueOf(userId) + "\n";
                            outputStream.write(idStr.getBytes());
                            outputStream.flush();
                            System.out.println("📤 Sent: " + userId);

                            // Wait for Arduino to process the ID
                            Thread.sleep(1000);

                            // Now read the response after sending ID
                            System.out.println("⏳ Checking response after ID...");

                            // Read what Arduino says after receiving the ID
                            boolean idAccepted = false;
                            long responseTime = System.currentTimeMillis();

                            while (System.currentTimeMillis() - responseTime < 5000) {
                                if (reader.ready()) {
                                    line = reader.readLine();
                                    if (line != null) {
                                        System.out.println("📥 Arduino: " + line);

                                        if (line.contains("ID invalide")) {
                                            System.out.println("❌ Arduino rejected the ID");
                                            return false;
                                        }

                                        if (line.contains("Placez votre doigt") || line.contains("1) Placez votre doigt")) {
                                            System.out.println("✅ ID accepted! Waiting for finger...");
                                            idAccepted = true;
                                            break;
                                        }
                                    }
                                }
                                Thread.sleep(100);
                            }

                            if (!idAccepted) {
                                System.out.println("❌ No confirmation of ID acceptance");
                                return false;
                            }

                            break; // Exit the prompt detection loop
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!idPromptDetected) {
                System.out.println("❌ Did not receive ID prompt from Arduino");
                return false;
            }

            // Continue with the rest of the enrollment process...
            // Wait for first finger prompt
            System.out.println("👆 PLACE YOUR FINGER ON THE SENSOR NOW (1/2)");

            // Wait for first scan to complete
            boolean firstScanComplete = false;
            startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 30000) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        System.out.println("📥 Arduino: " + line);

                        if (line.contains("Première capture OK")) {
                            firstScanComplete = true;
                            System.out.println("✅ First scan successful!");
                            System.out.println("👆 REMOVE YOUR FINGER");
                            break;
                        }
                        if (line.contains("Erreur")) {
                            System.out.println("❌ Error during scan: " + line);
                            return false;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!firstScanComplete) {
                System.out.println("❌ First scan timed out");
                return false;
            }

            // Wait for second finger prompt
            System.out.println("⏳ Waiting for second finger prompt...");

            boolean secondPromptReceived = false;
            startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 20000) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        System.out.println("📥 Arduino: " + line);

                        if (line.contains("Placez le MÊME doigt") || line.contains("2) Placez le MÊME doigt")) {
                            secondPromptReceived = true;
                            System.out.println("👆 PLACE THE SAME FINGER AGAIN (2/2)");
                            break;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!secondPromptReceived) {
                System.out.println("❌ Did not receive second finger prompt");
                return false;
            }

            // Wait for second scan to complete
            boolean secondScanComplete = false;
            startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 30000) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        System.out.println("📥 Arduino: " + line);

                        if (line.contains("Deuxième capture OK")) {
                            secondScanComplete = true;
                            System.out.println("✅ Second scan successful!");
                            break;
                        }
                        if (line.contains("Erreur")) {
                            System.out.println("❌ Error during second scan: " + line);
                            return false;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!secondScanComplete) {
                System.out.println("❌ Second scan timed out");
                return false;
            }

            // Wait for verification result
            boolean matchSuccess = false;
            startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 10000) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        System.out.println("📥 Arduino: " + line);

                        if (line.contains("Les deux empreintes correspondent")) {
                            matchSuccess = true;
                            System.out.println("✅ Fingerprints match!");
                            break;
                        }
                        if (line.contains("Les empreintes sont différentes")) {
                            System.out.println("❌ Fingerprints don't match!");
                            return false;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!matchSuccess) {
                System.out.println("❌ Fingerprint verification failed");
                return false;
            }

            // Wait for storage confirmation
            boolean storageSuccess = false;
            startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 10000) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        System.out.println("📥 Arduino: " + line);

                        if (line.contains("ENREGISTREMENT RÉUSSI") || line.contains("🎉")) {
                            storageSuccess = true;
                            System.out.println("✅ Fingerprint stored successfully on Arduino!");
                            fingerprintToUserMap.put(userId, userId);
                            return true;
                        }
                    }
                }
                Thread.sleep(100);
            }

            System.out.println("❌ Storage confirmation not received");
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int searchFingerprint() {
        if (!connected) return -1;

        try {
            sendCommand("s");
            String response = waitForPrompt("Placez votre doigt", 10000);
            if (response != null) {
                response = waitForAny(new String[]{"TROUVÉE", "CORRESPONDANCE"}, 30000);
                if (response != null) {
                    return extractIdFromResponse(response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean verifyFingerprintId(int fingerprintId) {
        if (!connected) return false;

        try {
            sendCommand("s");
            String response = waitForPrompt("Entrez l'ID", 10000);
            if (response != null) {
                sendCommand(String.valueOf(fingerprintId));
                response = waitForPrompt("Placez votre doigt", 10000);
                if (response != null) {
                    response = waitForAny(new String[]{"AUTORISÉ", "REFUSÉ"}, 30000);
                    return response != null && response.contains("AUTORISÉ");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteFingerprint(int fingerprintId) {
        if (!connected) return false;

        try {
            sendCommand("d");
            String response = waitForPrompt("ID à supprimer", 10000);
            if (response != null) {
                sendCommand(String.valueOf(fingerprintId));
                response = waitForAny(new String[]{"succès", "Aucune"}, 5000);
                boolean success = response != null && response.contains("succès");
                if (success) fingerprintToUserMap.remove(fingerprintId);
                return success;
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public static String[] getAvailablePorts() {
        try {
            SerialPort[] ports = SerialPort.getCommPorts();
            String[] portNames = new String[ports.length];
            for (int i = 0; i < ports.length; i++) {
                portNames[i] = ports[i].getSystemPortName();
                System.out.println("📌 Found port: " + portNames[i] + " - " + ports[i].getDescriptivePortName());
            }
            return portNames;
        } catch (Throwable e) {
            System.err.println("⚠️ Could not detect serial ports: " + e.getMessage());
            e.printStackTrace();
            return new String[]{"No ports detected"};
        }
    }

    private String waitForPrompt(String prompt, int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    System.out.println("📥 Arduino: " + line);
                    if (line.contains(prompt)) {
                        return line;
                    }
                }
            }
            Thread.sleep(10);
        }
        return null;
    }

    private String waitForSuccess(String successMsg, int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    System.out.println("📥 Arduino: " + line);
                    if (line.contains(successMsg)) {
                        return line;
                    }
                }
            }
            Thread.sleep(10);
        }
        return null;
    }

    private String waitForAny(String[] expected, int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    System.out.println("📥 Arduino: " + line);
                    for (String exp : expected) {
                        if (line.contains(exp)) return line;
                    }
                }
            }
            Thread.sleep(10);
        }
        return null;
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
}