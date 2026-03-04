package utils;

import java.util.Map;

public interface FingerprintInterface {
    boolean connect(String portName);
    void disconnect();
    void sendCommand(String command);
    String readResponse(int timeoutMs);

    // Change this to return int (slot ID) instead of boolean
    int enrollFingerprint(int userId);  // Returns slot ID on success, -1 on failure

    int searchFingerprint();
    boolean verifyFingerprintId(int fingerprintId);
    boolean deleteFingerprint(int fingerprintId);
    void addMapping(int fingerprintId, int userId);
    int getUserIdFromFingerprintId(int fingerprintId);
    Map<Integer, Integer> getAllMappings();
    boolean isConnected();
    String getConnectedPort();
}