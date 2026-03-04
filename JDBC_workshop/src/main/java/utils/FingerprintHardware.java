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

    /**
     * Get list of used fingerprint IDs from the sensor
     */
    private List<Integer> getUsedFingerprintIds() throws Exception {
        List<Integer> usedIds = new ArrayList<>();

        // Clear any pending data
        while (reader.ready()) {
            reader.readLine();
        }

        // Send 'l' command to list fingerprints
        sendCommand("l");

        // Wait for response
        Thread.sleep(2000);

        // Read all lines of the response
        List<String> lines = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 5000) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    lines.add(line);
                    System.out.println("📥 Arduino: " + line);
                }
            }
            Thread.sleep(100);
        }

        // Parse the lines to find all IDs
        boolean inListSection = false;
        for (String line : lines) {
            // Look for the line that starts the list
            if (line.contains("IDs occupés")) {
                inListSection = true;
                continue;
            }

            // If we're in the list section, look for lines with #
            if (inListSection) {
                if (line.contains("#")) {
                    // Split the line by spaces
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.contains("#")) {
                            String idStr = part.replace("#", "").trim();
                            if (!idStr.isEmpty()) {
                                try {
                                    int id = Integer.parseInt(idStr);
                                    usedIds.add(id);
                                    System.out.println("✅ Found used ID: " + id);
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                }

                // Stop at the end of list marker
                if (line.contains("Fin de la liste")) {
                    break;
                }
            }
        }

        System.out.println("📋 Total used slots found: " + usedIds);
        return usedIds;
    }

    @Override
    public int enrollFingerprint(int userId) {
        if (!connected) return -1;

        try {
            // First, get the list of used IDs from the sensor
            List<Integer> usedIds = getUsedFingerprintIds();
            System.out.println("📋 Currently used fingerprint slots: " + usedIds);

            // Use the userId as the slot ID
            int targetSlotId = userId;

            System.out.println("📝 Starting fingerprint enrollment for user ID: " + userId);
            System.out.println("🔢 Targeting fingerprint slot #" + targetSlotId + " (same as user ID)");

            // Check if the target slot is already used
            if (usedIds.contains(targetSlotId)) {
                System.out.println("❌ Slot #" + targetSlotId + " is already taken!");
                System.out.println("💡 Please delete the existing fingerprint from slot #" + targetSlotId + " first");
                return -1;
            }

            if (targetSlotId > 150) {
                System.out.println("❌ User ID " + userId + " is > 150, which exceeds sensor capacity");
                return -1;
            }

            // Clear any pending data
            while (reader.ready()) {
                reader.readLine();
            }

            // Send 'e' command with the user ID directly (e.g., "e8", "e17", etc.)
            String command = "e" + targetSlotId;
            sendCommand(command);

            // Wait for Arduino to process
            Thread.sleep(2000);

            // Wait for first finger prompt
            System.out.println("👆 PLACE YOUR FINGER ON THE SENSOR NOW (1/2)");

            // Wait for first scan to complete
            String line;
            boolean firstScanComplete = false;
            long startTime = System.currentTimeMillis();

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
                            return -1;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!firstScanComplete) {
                System.out.println("❌ First scan timed out");
                return -1;
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
                return -1;
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
                            return -1;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!secondScanComplete) {
                System.out.println("❌ Second scan timed out");
                return -1;
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
                            return -1;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (!matchSuccess) {
                System.out.println("❌ Fingerprint verification failed");
                return -1;
            }

            // Wait for storage confirmation
            boolean storageSuccess = false;
            int actualSlotId = -1;
            startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 10000) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        System.out.println("📥 Arduino: " + line);

                        // Look for the line that contains the actual slot ID
                        if (line.contains("Enregistrement dans ID #")) {
                            String[] parts = line.split("#");
                            if (parts.length > 1) {
                                String idStr = parts[1].trim();
                                try {
                                    actualSlotId = Integer.parseInt(idStr);
                                    System.out.println("✅ Found actual slot ID from Arduino: " + actualSlotId);
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        }

                        if (line.contains("ENREGISTREMENT RÉUSSI") || line.contains("🎉")) {
                            storageSuccess = true;
                            System.out.println("✅ Fingerprint stored successfully on Arduino!");

                            // If we didn't find the slot ID from the specific line, try to extract from anywhere
                            if (actualSlotId <= 0) {
                                actualSlotId = extractSlotIdFromResponse(line);
                            }

                            break;
                        }
                    }
                }
                Thread.sleep(100);
            }

            if (storageSuccess && actualSlotId > 0) {
                System.out.println("✅ Using actual fingerprint slot #" + actualSlotId);

                // Verify that the actual slot ID matches what we requested
                if (actualSlotId == targetSlotId) {
                    System.out.println("✅ Slot matches requested ID " + targetSlotId);
                } else {
                    System.out.println("⚠️ Slot used (" + actualSlotId + ") is different from requested (" + targetSlotId + ")");
                }

                // Store mapping between fingerprint ID and user ID
                fingerprintToUserMap.put(actualSlotId, userId);

                // Return the actual slot ID used
                return actualSlotId;
            }

            System.out.println("❌ Storage confirmation not received or invalid slot ID");
            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Helper method to extract slot ID from any response line
    // Helper method to extract slot ID from any response line
    private int extractSlotIdFromResponse(String line) {
        try {
            // Look for patterns like "#12" or "ID: 12"
            if (line.contains("#")) {
                String[] parts = line.split("#");
                if (parts.length > 1) {
                    String afterHash = parts[1].trim();
                    // Extract just the number
                    String[] numberParts = afterHash.split("[^0-9]");
                    for (String num : numberParts) {
                        if (!num.isEmpty()) {
                            return Integer.parseInt(num);
                        }
                    }
                }
            }

            if (line.contains("ID:")) {
                String[] parts = line.split("ID:");
                if (parts.length > 1) {
                    String afterId = parts[1].trim();
                    String[] numberParts = afterId.split("[^0-9]");
                    for (String num : numberParts) {
                        if (!num.isEmpty()) {
                            return Integer.parseInt(num);
                        }
                    }
                }
            }

            // Fallback: try to find any number in the line
            String[] words = line.split(" ");
            for (String word : words) {
                try {
                    int id = Integer.parseInt(word.trim());
                    if (id > 0 && id <= 150) {
                        return id;
                    }
                } catch (NumberFormatException e) {
                    // Not a number, continue
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
    @Override
    public int searchFingerprint() {
        if (!connected) return -1;

        try {
            System.out.println("🔍 Searching for fingerprint...");

            // Clear any pending data
            while (reader.ready()) {
                String junk = reader.readLine();
                System.out.println("📥 Clearing: " + junk);
            }

            // Send 's' command to search
            sendCommand("s");

            // Wait for Arduino to process
            Thread.sleep(1500);

            // Read and discard menu lines until we get to the finger prompt
            String line;
            long startTime = System.currentTimeMillis();
            boolean fingerPromptFound = false;
            StringBuilder fullResponse = new StringBuilder();

            while (System.currentTimeMillis() - startTime < 8000) { // Increased timeout
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line != null) {
                        fullResponse.append(line).append("\n");
                        System.out.println("📥 Arduino: " + line);

                        if (line.contains("Placez votre doigt")) {
                            fingerPromptFound = true;
                            System.out.println("👆 Place your finger on the sensor...");
                            break;
                        }
                    }
                }
                Thread.sleep(50);
            }

            if (!fingerPromptFound) {
                System.out.println("❌ Did not receive finger prompt");
                System.out.println("📋 Full response received:\n" + fullResponse.toString());
                return -1;
            }

            // Now wait for the search result
            System.out.println("⏳ Waiting for fingerprint scan...");
            String result = waitForAny(new String[]{
                    "EMPREINTE TROUVÉE",
                    "AUCUNE CORRESPONDANCE",
                    "Erreur",
                    "ID:"  // Also look for ID pattern
            }, 35000); // Increased timeout

            if (result != null) {
                System.out.println("✅ Search result received");
                System.out.println("📋 Result content:\n" + result);

                if (result.contains("TROUVÉE") || result.contains("ID:")) {
                    // Extract the ID using the enhanced method
                    int id = extractIdFromResponseEnhanced(result);
                    if (id > 0) {
                        System.out.println("✅ Found fingerprint in slot #" + id);

                        // Clear any remaining data
                        while (reader.ready()) {
                            String leftover = reader.readLine();
                            System.out.println("📥 Clearing leftover: " + leftover);
                        }

                        return id;
                    } else {
                        System.out.println("❌ Could not extract ID from successful response");
                    }
                } else if (result.contains("AUCUNE")) {
                    System.out.println("❌ No matching fingerprint found in database");

                    // Clear any remaining data
                    while (reader.ready()) {
                        reader.readLine();
                    }

                    return -1;
                } else if (result.contains("Erreur")) {
                    System.out.println("❌ Error during fingerprint scan");

                    // Clear any remaining data
                    while (reader.ready()) {
                        reader.readLine();
                    }

                    return -1;
                }
            } else {
                System.out.println("❌ No response received within timeout");
            }

            System.out.println("❌ Search failed");
            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Enhanced ID extraction method
    private int extractIdFromResponseEnhanced(String response) {
        try {
            System.out.println("🔍 Enhanced ID extraction from response");
            String[] lines = response.split("\n");

            for (String line : lines) {
                System.out.println("   Checking line: " + line);

                // Look for "ID: X" pattern
                if (line.contains("ID:")) {
                    String[] parts = line.split("ID:");
                    if (parts.length > 1) {
                        String afterId = parts[1].trim();
                        // Extract first number found
                        String[] numberParts = afterId.split("[^0-9]");
                        for (String num : numberParts) {
                            if (!num.isEmpty()) {
                                try {
                                    int id = Integer.parseInt(num);
                                    System.out.println("✅ Extracted ID " + id + " from 'ID:' pattern");
                                    return id;
                                } catch (NumberFormatException e) {
                                    // Continue
                                }
                            }
                        }
                    }
                }

                // Look for "#X" pattern
                if (line.contains("#")) {
                    int hashIndex = line.indexOf("#");
                    if (hashIndex >= 0 && hashIndex < line.length() - 1) {
                        String afterHash = line.substring(hashIndex + 1).trim();
                        String[] parts = afterHash.split("[^0-9]");
                        if (parts.length > 0 && !parts[0].isEmpty()) {
                            try {
                                int id = Integer.parseInt(parts[0]);
                                System.out.println("✅ Extracted ID " + id + " from '#' pattern");
                                return id;
                            } catch (NumberFormatException e) {
                                // Continue
                            }
                        }
                    }
                }

                // Look for any standalone number that might be the ID
                String[] words = line.split(" ");
                for (String word : words) {
                    word = word.trim();
                    if (word.matches("\\d+")) {
                        try {
                            int id = Integer.parseInt(word);
                            if (id > 0 && id <= 150) {
                                System.out.println("✅ Extracted ID " + id + " from standalone number");
                                return id;
                            }
                        } catch (NumberFormatException e) {
                            // Continue
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("❌ Could not extract fingerprint ID from response");
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
                    boolean authorized = response != null && response.contains("AUTORISÉ");
                    if (authorized) {
                        System.out.println("✅ Fingerprint verified for slot #" + fingerprintId);
                    } else {
                        System.out.println("❌ Verification failed for slot #" + fingerprintId);
                    }
                    return authorized;
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
                if (success) {
                    fingerprintToUserMap.remove(fingerprintId);
                    System.out.println("✅ Deleted fingerprint slot #" + fingerprintId);
                }
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
        System.out.println("✅ Mapping added: fingerprint slot #" + fingerprintId + " -> user ID " + userId);
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
        StringBuilder fullResponse = new StringBuilder();

        while (System.currentTimeMillis() - start < timeoutMs) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    fullResponse.append(line).append("\n");
                    System.out.println("📥 Arduino: " + line);

                    for (String exp : expected) {
                        if (line.contains(exp)) {
                            System.out.println("✅ Found expected text: " + exp);
                            // Keep reading a bit more to get the full context
                            Thread.sleep(200);
                            while (reader.ready()) {
                                String extraLine = reader.readLine();
                                if (extraLine != null) {
                                    fullResponse.append(extraLine).append("\n");
                                    System.out.println("📥 Arduino: " + extraLine);
                                }
                            }
                            return fullResponse.toString();
                        }
                    }
                }
            }
            Thread.sleep(10);
        }
        System.out.println("❌ Timeout waiting for: " + String.join(", ", expected));
        return null;
    }

    private int extractIdFromResponse(String response) {
        try {
            System.out.println("🔍 Extracting ID from response: " + response);
            String[] lines = response.split("\n");

            for (String line : lines) {
                // Look for "ID: X" pattern
                if (line.contains("ID:")) {
                    String[] parts = line.split("ID:");
                    if (parts.length > 1) {
                        String idStr = parts[1].trim();
                        // Extract just the number (might be like "2 | Confiance")
                        String[] idParts = idStr.split("[^0-9]");
                        for (String part : idParts) {
                            if (!part.isEmpty()) {
                                int id = Integer.parseInt(part);
                                System.out.println("✅ Extracted fingerprint ID: " + id);
                                return id;
                            }
                        }
                    }
                }
                // Look for "#X" pattern
                else if (line.contains("#")) {
                    int hashIndex = line.indexOf("#");
                    if (hashIndex >= 0 && hashIndex < line.length() - 1) {
                        String afterHash = line.substring(hashIndex + 1).trim();
                        String[] parts = afterHash.split("[^0-9]");
                        if (parts.length > 0 && !parts[0].isEmpty()) {
                            int id = Integer.parseInt(parts[0]);
                            System.out.println("✅ Extracted fingerprint ID: " + id);
                            return id;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error extracting ID: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("❌ Could not extract fingerprint ID from response");
        return -1;
    }
}