package utils;

import com.fazecast.jSerialComm.SerialPort;

public class SerialTest {
    public static void main(String[] args) {
        System.out.println("=== Serial Port Test ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS architecture: " + System.getProperty("os.arch"));

        try {
            SerialPort[] ports = SerialPort.getCommPorts();
            System.out.println("Found " + ports.length + " serial ports:");

            for (int i = 0; i < ports.length; i++) {
                System.out.println("  Port " + i + ": " + ports[i].getSystemPortName() +
                        " - " + ports[i].getDescriptivePortName());
            }

            if (ports.length == 0) {
                System.out.println("❌ No serial ports detected!");
                System.out.println("This is likely due to jSerialComm native library issues.");
            } else {
                System.out.println("✅ Serial ports detected successfully!");
            }

        } catch (Throwable e) {
            System.err.println("❌ Error accessing serial ports: " + e.getMessage());
            e.printStackTrace();
        }
    }
}