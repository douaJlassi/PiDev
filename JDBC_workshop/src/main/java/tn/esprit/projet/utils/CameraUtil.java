package tn.esprit.projet.utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CameraUtil {

    private Webcam webcam;
    private boolean isCameraOpen = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> cameraTask;
    private BufferedImage latestImage;
    private final Object imageLock = new Object();

    // Store available cameras
    private static List<Webcam> webcams = null;
    private static String[] cameraNames = null;

    public CameraUtil() {
        this(0); // Default to first camera (index 0)
    }

    public CameraUtil(int cameraIndex) {
        // Get all webcams
        if (webcams == null) {
            webcams = Webcam.getWebcams();
        }

        if (webcams != null && !webcams.isEmpty() && cameraIndex < webcams.size()) {
            webcam = webcams.get(cameraIndex);
            System.out.println("📷 Selected camera: " + webcam.getName());

            // Use HIGHER resolution for better face recognition
            Dimension[] resolutions = new Dimension[] {
                    new Dimension(1280, 720),  // HD - best quality
                    WebcamResolution.VGA.getSize(),      // 640x480 - good
                    WebcamResolution.QVGA.getSize(),     // 320x240 - fallback
            };
            webcam.setCustomViewSizes(resolutions);

            // Try to use the highest available resolution
            Dimension bestSize = null;
            for (Dimension size : webcam.getViewSizes()) {
                System.out.println("   Available size: " + size.width + "x" + size.height);
                if (size.width >= 1280 && bestSize == null) {
                    bestSize = size;
                } else if (size.width >= 640 && bestSize == null) {
                    bestSize = size;
                }
            }

            if (bestSize != null) {
                webcam.setViewSize(bestSize);
                System.out.println("📐 Using resolution: " + bestSize.width + "x" + bestSize.height);
            } else {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
                System.out.println("📐 Using VGA resolution");
            }
        } else {
            System.out.println("⚠️ No camera found at index " + cameraIndex);
            webcam = Webcam.getDefault();
        }
    }


    /**
     * Get list of available camera names
     */
    public static String[] getCameraNames() {
        if (webcams == null) {
            webcams = Webcam.getWebcams();
        }

        if (webcams != null && !webcams.isEmpty()) {
            cameraNames = new String[webcams.size()];
            for (int i = 0; i < webcams.size(); i++) {
                cameraNames[i] = i + ": " + webcams.get(i).getName();
            }
            return cameraNames;
        }
        return new String[]{"No cameras found"};
    }

    /**
     * Check if multiple cameras are available
     */
    public static boolean hasMultipleCameras() {
        if (webcams == null) {
            webcams = Webcam.getWebcams();
        }
        return webcams != null && webcams.size() > 1;
    }

    /**
     * Get number of available cameras
     */
    public static int getCameraCount() {
        if (webcams == null) {
            webcams = Webcam.getWebcams();
        }
        return webcams != null ? webcams.size() : 0;
    }

    /**
     * Open camera
     */
    public boolean openCamera() {
        if (webcam != null && !webcam.isOpen()) {
            try {
                webcam.open();
                isCameraOpen = true;

                // Start a background thread to continuously capture images
                startContinuousCapture();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * Start continuous capture in background for smoother preview
     */
    private void startContinuousCapture() {
        if (cameraTask != null && !cameraTask.isDone()) {
            cameraTask.cancel(true);
        }

        cameraTask = executor.submit(() -> {
            while (isCameraOpen && !Thread.currentThread().isInterrupted()) {
                if (webcam != null && webcam.isOpen()) {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        synchronized (imageLock) {
                            latestImage = image;
                        }
                    }
                }
                // Small delay to prevent excessive CPU usage (≈30fps)
                try {
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Get the latest captured image (non-blocking)
     */
    public BufferedImage getLatestImage() {
        synchronized (imageLock) {
            return latestImage;
        }
    }

    /**
     * Capture image from camera (blocking)
     */
    public BufferedImage captureImage() {
        if (webcam != null && webcam.isOpen()) {
            return webcam.getImage();
        }
        return null;
    }

    /**
     * Capture image as JavaFX Image (uses cached image for better performance)
     */
    public Image captureJavaFXImage() {
        BufferedImage bufferedImage = getLatestImage();
        if (bufferedImage != null) {
            WritableImage fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            return fxImage;
        }
        return null;
    }

    /**
     * Close camera and cleanup
     */
    public void closeCamera() {
        isCameraOpen = false;

        if (cameraTask != null && !cameraTask.isDone()) {
            cameraTask.cancel(true);
        }

        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }

        executor.shutdownNow();
    }

    /**
     * Check if camera is available
     */
    public boolean isCameraAvailable() {
        return webcam != null;
    }

    /**
     * Get camera name
     */
    public String getCameraName() {
        if (webcam != null) {
            return webcam.getName();
        }
        return "No camera detected";
    }

    public boolean isOpen() {
        return isCameraOpen;
    }
}