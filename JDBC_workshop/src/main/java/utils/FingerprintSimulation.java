package utils;

import java.util.*;

public class FingerprintSimulation implements FingerprintInterface {

    private boolean connected = false;
    private String connectedPort = "";
    private List<String> responseLog = new ArrayList<>();
    private Map<Integer, Integer> fingerprintToUserMap = new HashMap<>();

    public FingerprintSimulation() {
        System.out.println("🔧 FingerprintSimulation initialized");
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
            for (Integer id : fingerprintToUserMap.keySet()) {
                responseLog.add("  #" + id);
            }
        } else if (command.equals("c")) {
            responseLog.add("⚠️ EFFACER TOUTES LES EMPREINTES ? (o/n)");
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
            responseLog.add("🎉 ENREGISTREMENT RÉUSSI !");
            responseLog.add("✅ Empreinte sauvegardée dans ID: " + id);
            fingerprintToUserMap.put(id, id);
        }
    }

    @Override
    public String readResponse(int timeoutMs) {
        if (!connected) return null;
        try { Thread.sleep(800); } catch (InterruptedException e) { e.printStackTrace(); }
        StringBuilder response = new StringBuilder();
        for (String line : responseLog) response.append(line).append("\n");
        responseLog.clear();
        return response.length() > 0 ? response.toString() : null;
    }

    @Override
    public boolean enrollFingerprint(int userId) {
        if (!connected) return false;
        sendCommand("e");
        sendCommand(String.valueOf(userId));
        fingerprintToUserMap.put(userId, userId);
        return true;
    }

    @Override
    public int searchFingerprint() {
        if (!connected) return -1;
        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        if (!fingerprintToUserMap.isEmpty()) {
            List<Integer> ids = new ArrayList<>(fingerprintToUserMap.keySet());
            int randomId = ids.get(new Random().nextInt(ids.size()));
            System.out.println("✅ [SIM] Found fingerprint for user: " + randomId);
            return randomId;
        }
        System.out.println("❌ [SIM] No fingerprint found");
        return -1;
    }

    @Override
    public boolean verifyFingerprintId(int fingerprintId) {
        return connected && fingerprintToUserMap.containsKey(fingerprintId);
    }

    @Override
    public boolean deleteFingerprint(int fingerprintId) {
        return connected && fingerprintToUserMap.remove(fingerprintId) != null;
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
        return new String[]{
                "COM1 (Simulated)",
                "COM2 (Simulated)",
                "COM3 (Simulated)",
                "COM4 (Simulated)",
                "COM5 (Simulated)"
        };
    }
}