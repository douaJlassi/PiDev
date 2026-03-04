package utils;

import java.util.Map;

public interface FingerprintInterface {
    boolean connect(String portName);
    void disconnect();
    void sendCommand(String command);
    String readResponse(int timeoutMs);
    boolean enrollFingerprint(int userId);
    int searchFingerprint();
    boolean verifyFingerprintId(int fingerprintId);
    boolean deleteFingerprint(int fingerprintId);
    void addMapping(int fingerprintId, int userId);
    int getUserIdFromFingerprintId(int fingerprintId);
    Map<Integer, Integer> getAllMappings();
    boolean isConnected();
    String getConnectedPort();

    // Static method to get available ports (different implementations)
    static String[] getAvailablePorts() {
        // This will be overridden by implementations
        return new String[0];
    }
}