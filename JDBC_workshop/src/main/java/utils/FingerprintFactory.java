package utils;

public class FingerprintFactory {

    private static boolean useSimulation = false; // Set to false for hardware mode

    public static void setUseSimulation(boolean simulate) {
        useSimulation = simulate;
        System.out.println("🔧 Fingerprint mode: " + (simulate ? "SIMULATION" : "HARDWARE"));
    }

    public static FingerprintInterface getInstance() {
        if (useSimulation) {
            System.out.println("🔧 Using SIMULATION mode");
            return new FingerprintSimulation();
        } else {
            try {
                System.out.println("🔧 Using HARDWARE mode with jSerialComm");
                return new FingerprintHardware();
            } catch (Throwable e) {
                System.err.println("❌ Failed to load hardware implementation: " + e.getMessage());
                System.err.println("❌ Falling back to simulation mode");
                setUseSimulation(true);
                return new FingerprintSimulation();
            }
        }
    }

    public static String[] getAvailablePorts() {
        if (useSimulation) {
            return FingerprintSimulation.getAvailablePorts();
        } else {
            try {
                return FingerprintHardware.getAvailablePorts();
            } catch (Throwable e) {
                System.err.println("❌ Failed to detect hardware ports: " + e.getMessage());
                return new String[]{"No ports detected"};
            }
        }
    }

    public static boolean isSimulationMode() {
        return useSimulation;
    }
}