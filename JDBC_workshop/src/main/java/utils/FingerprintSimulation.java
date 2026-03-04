package utils;

import java.util.*;

public class FingerprintSimulation implements FingerprintInterface {

    private boolean connected = false;
    private String connectedPort = "";
    private List<String> responseLog = new ArrayList<>();
    private Map<Integer, Integer> fingerprintToUserMap = new HashMap<>();
    private Random random = new Random();

    public FingerprintSimulation() {
        System.out.println("🔧 FingerprintSimulation initialized");
        // Add some test data
        fingerprintToUserMap.put(1, 1);
        fingerprintToUserMap.put(2, 2);
    }

    @Override
    public boolean connect(String portName) {
        System.out.println("🔌 [SIM] Connecting to " + portName);
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        connected = true;
        connectedPort = portName;
        System.out.println("✅ [SIM] Connected");
        return true;
    }

    @Override
    public void disconnect() {
        connected = false;
        connectedPort = "";
        responseLog.clear();
        System.out.println("🔌 [SIM] Disconnected");
    }

    @Override
    public void sendCommand(String command) {
        if (!connected) return;
        System.out.println("📤 [SIM] Command: " + command);
        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }

        responseLog.clear();
        if (command.equals("e")) {
            responseLog.add("=== ENREGISTREMENT ===");
            responseLog.add("🔢 Entrez l'ID pour sauvegarder (1-150): ");
        } else if (command.equals("s")) {
            responseLog.add("=== RECHERCHE D'EMPREINTE ===");
            responseLog.add("👉 Placez votre doigt: ");
        } else if (command.equals("d")) {
            responseLog.add("=== SUPPRESSION ===");
            responseLog.add("ID à supprimer (1-150): ");
        } else if (command.equals("l")) {
            responseLog.add("=== LISTE DES EMPREINTES ===");
            responseLog.add("📊 Total: " + fingerprintToUserMap.size() + "/150");
            responseLog.add("🔍 IDs occupés:");
            for (Integer id : fingerprintToUserMap.keySet()) {
                responseLog.add("  #" + id);
            }
            responseLog.add("✅ Fin de la liste");
        } else if (command.equals("c")) {
            responseLog.add("⚠️ EFFACER TOUTES LES EMPREINTES ? (o/n)");
        } else if (command.matches("e\\d+")) {
            // Handle command with ID like "e5"
            String idStr = command.substring(1);
            int id = Integer.parseInt(idStr);
            responseLog.add("=== ENREGISTREMENT AVEC ID #" + id);
            responseLog.add("");
            responseLog.add("🔍 Vérification ID #" + id);
            responseLog.add("✅ ID libre - prêt pour enregistrement");
            responseLog.add("");
            responseLog.add("1) Placez votre doigt: .........OK");
            responseLog.add("✅ Première capture OK");
            responseLog.add("   Retirez votre doigt...");
            responseLog.add("2) Placez le MÊME doigt: .........OK");
            responseLog.add("✅ Deuxième capture OK");
            responseLog.add("   Vérification des empreintes...");
            responseLog.add("✅ Les deux empreintes correspondent !");
            responseLog.add("💾 Enregistrement dans ID #" + id);
            responseLog.add("");
            responseLog.add("🎉 ENREGISTREMENT RÉUSSI !");
            responseLog.add("✅ Empreinte sauvegardée dans ID: " + id);
        } else if (command.matches("\\d+")) {
            int id = Integer.parseInt(command);
            responseLog.add("1) Placez votre doigt: .........OK");
            responseLog.add("✅ Première capture OK");
            responseLog.add("   Retirez votre doigt...");
            responseLog.add("2) Placez le MÊME doigt: .........OK");
            responseLog.add("✅ Deuxième capture OK");
            responseLog.add("   Vérification des empreintes...");
            responseLog.add("✅ Les deux empreintes correspondent !");
            responseLog.add("💾 Enregistrement dans ID #" + id);
            responseLog.add("");
            responseLog.add("🎉 ENREGISTREMENT RÉUSSI !");
            responseLog.add("✅ Empreinte sauvegardée dans ID: " + id);
        }
    }

    @Override
    public String readResponse(int timeoutMs) {
        if (!connected) return null;
        try { Thread.sleep(800); } catch (InterruptedException e) { e.printStackTrace(); }
        StringBuilder response = new StringBuilder();
        for (String line : responseLog) {
            response.append(line).append("\n");
        }
        responseLog.clear();
        return response.length() > 0 ? response.toString() : null;
    }

    @Override
    public int enrollFingerprint(int userId) {
        if (!connected) return -1;

        System.out.println("📝 [SIM] Starting fingerprint enrollment for user ID: " + userId);

        // Find an available slot (simulate finding first available)
        int slotId = 1;
        while (fingerprintToUserMap.containsKey(slotId) && slotId <= 150) {
            slotId++;
        }

        if (slotId > 150) {
            System.out.println("❌ [SIM] No available slots");
            return -1;
        }

        System.out.println("🔢 [SIM] Using fingerprint slot #" + slotId);

        // Send the command with the slot ID
        sendCommand("e" + slotId);

        // Simulate successful enrollment
        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        // Store mapping
        fingerprintToUserMap.put(slotId, userId);
        System.out.println("✅ [SIM] Fingerprint enrolled successfully in slot #" + slotId);

        // Return the actual slot ID used
        return slotId;
    }

    @Override
    public int searchFingerprint() {
        if (!connected) return -1;

        System.out.println("🔍 [SIM] Searching for fingerprint...");
        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        if (!fingerprintToUserMap.isEmpty()) {
            List<Integer> ids = new ArrayList<>(fingerprintToUserMap.keySet());
            int randomId = ids.get(random.nextInt(ids.size()));
            System.out.println("✅ [SIM] Found fingerprint in slot #" + randomId);
            return randomId;
        }
        System.out.println("❌ [SIM] No fingerprint found");
        return -1;
    }

    @Override
    public boolean verifyFingerprintId(int fingerprintId) {
        if (!connected) return false;
        boolean exists = fingerprintToUserMap.containsKey(fingerprintId);
        if (exists) {
            System.out.println("✅ [SIM] Fingerprint verified for slot #" + fingerprintId);
        } else {
            System.out.println("❌ [SIM] Verification failed for slot #" + fingerprintId);
        }
        return exists;
    }

    @Override
    public boolean deleteFingerprint(int fingerprintId) {
        if (!connected) return false;
        boolean removed = fingerprintToUserMap.remove(fingerprintId) != null;
        if (removed) {
            System.out.println("✅ [SIM] Deleted fingerprint slot #" + fingerprintId);
        } else {
            System.out.println("❌ [SIM] Failed to delete fingerprint slot #" + fingerprintId);
        }
        return removed;
    }

    @Override
    public void addMapping(int fingerprintId, int userId) {
        fingerprintToUserMap.put(fingerprintId, userId);
        System.out.println("✅ [SIM] Mapping added: slot #" + fingerprintId + " -> user " + userId);
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
        return new String[]{
                "COM1 (Simulated)",
                "COM2 (Simulated)",
                "COM3 (Simulated)",
                "COM4 (Simulated)",
                "COM5 (Simulated)"
        };
    }
}