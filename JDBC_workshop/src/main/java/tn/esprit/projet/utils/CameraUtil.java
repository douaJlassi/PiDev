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

    // Current resolution
    private int currentWidth = 640;
    private int currentHeight = 480;

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

            // Try to use the best available resolution
            setBestResolution();

        } else {
            System.out.println("⚠️ No camera found at index " + cameraIndex);
            webcam = Webcam.getDefault();
        }
    }

    /**
     * Set the best available camera resolution
     */
    private void setBestResolution() {
        if (webcam == null) return;

        // Define preferred resolutions in order of preference
        Dimension[] preferredResolutions = {
                new Dimension(1280, 720),  // HD
                new Dimension(1024, 768),  // XGA
                new Dimension(800, 600),   // SVGA
                WebcamResolution.VGA.getSize(),      // 640x480
                WebcamResolution.QVGA.getSize(),     // 320x240
        };

        // Log available resolutions
        System.out.println("📐 Available resolutions:");
        Dimension[] viewSizes = webcam.getViewSizes();
        if (viewSizes.length > 0) {
            for (Dimension size : viewSizes) {
                System.out.println("   - " + size.width + "x" + size.height);
            }
        } else {
            System.out.println("   No custom resolutions available, using default");
        }

        // Try to find the best matching resolution
        Dimension bestSize = null;
        for (Dimension preferred : preferredResolutions) {
            for (Dimension available : viewSizes) {
                if (available.width == preferred.width && available.height == preferred.height) {
                    bestSize = available;
                    break;
                }
            }
            if (bestSize != null) break;
        }

        // If no preferred resolution found, use the highest available
        if (bestSize == null && viewSizes.length > 0) {
            int maxPixels = 0;
            for (Dimension size : viewSizes) {
                int pixels = size.width * size.height;
                if (pixels > maxPixels) {
                    maxPixels = pixels;
                    bestSize = size;
                }

            }
        }

        // Set the resolution
        if (bestSize != null) {
            webcam.setViewSize(bestSize);
            currentWidth = bestSize.width;
            currentHeight = bestSize.height;
            System.out.println("✅ Using resolution: " + currentWidth + "x" + currentHeight);
        } else {
            // Default to VGA
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            currentWidth = WebcamResolution.VGA.getSize().width;
            currentHeight = WebcamResolution.VGA.getSize().height;
            System.out.println("✅ Using default VGA resolution: " + currentWidth + "x" + currentHeight);
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

    /**
     * Get the best available camera resolution
     * @return array with [width, height] of best resolution
     */
    public int[] getBestResolution() {
        return new int[]{currentWidth, currentHeight};
    }

    /**
     * Get current camera width
     */
    public int getCurrentWidth() {
        return currentWidth;
    }

    /**
     * Get current camera height
     */
    public int getCurrentHeight() {
        return currentHeight;
    }


    /**
     * Force a specific resolution for better face detection
     */
    public boolean setFixedResolution(int width, int height) {
        if (webcam == null) return false;

        try {
            Dimension targetSize = new Dimension(width, height);

            // Check if this resolution is supported
            boolean supported = false;
            for (Dimension size : webcam.getViewSizes()) {
                if (size.width == width && size.height == height) {
                    supported = true;
                    break;
                }
            }

            if (supported) {
                webcam.setViewSize(targetSize);
                currentWidth = width;
                currentHeight = height;
                System.out.println("✅ Fixed resolution set to: " + width + "x" + height);
                return true;
            } else {
                System.out.println("⚠️ Resolution " + width + "x" + height + " not supported");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to set fixed resolution: " + e.getMessage());
            return false;
        }
    }
}