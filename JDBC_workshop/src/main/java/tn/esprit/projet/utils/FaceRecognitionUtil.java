package tn.esprit.projet.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.face.LBPHFaceRecognizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class FaceRecognitionUtil {

    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer faceRecognizer;
    private static boolean opencvLoaded = false;
    private static boolean faceModuleAvailable = false;

    // Store training data
    private List<Mat> trainingImages = new ArrayList<>();
    private List<Integer> trainingLabels = new ArrayList<>();
    private Map<Integer, Integer> userLabelMap = new HashMap<>();
    private boolean modelTrained = false;

    static {
        System.out.println("=== OpenCV Initialization Debug ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Java home: " + System.getProperty("java.home"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("java.library.path: " + System.getProperty("java.library.path"));
        System.out.println("user.dir: " + System.getProperty("user.dir"));

        // Try multiple methods to load OpenCV
        Exception lastException = null;

        // Method 1: Try to load by name (looks in java.library.path)
        try {
            System.loadLibrary("opencv_java480");
            opencvLoaded = true;
            System.out.println("✅ Method 1: System.loadLibrary('opencv_java480') succeeded");
        } catch (UnsatisfiedLinkError e1) {
            System.out.println("❌ Method 1 failed: " + e1.getMessage());
            //lastException = e1;

            // Method 2: Try common Windows paths
            String[] possiblePaths = {
                    "C:/opencv-build/opencv/build/bin/Release/opencv_java480.dll",
                    "C:/opencv/build/java/x64/opencv_java480.dll",
                    "C:/Program Files/opencv/build/java/x64/opencv_java480.dll",
                    System.getProperty("user.dir") + "/opencv_java480.dll",
                    System.getProperty("user.dir") + "/lib/opencv_java480.dll"
            };

            for (String path : possiblePaths) {
                try {
                    File dllFile = new File(path);
                    if (dllFile.exists()) {
                        System.out.println("Found DLL at: " + path);
                        System.load(path);
                        opencvLoaded = true;
                        System.out.println("✅ Method 2: System.load('" + path + "') succeeded");
                        break;
                    }
                } catch (UnsatisfiedLinkError e2) {
                    System.out.println("❌ Failed to load from " + path + ": " + e2.getMessage());
                    //lastException = e2;
                }
            }
        }

        // Method 3: Try nu.pattern.OpenCV as fallback
        if (!opencvLoaded) {
            try {
                nu.pattern.OpenCV.loadLocally();
                opencvLoaded = true;
                System.out.println("✅ Method 3: nu.pattern.OpenCV.loadLocally() succeeded");
            } catch (Exception e3) {
                System.out.println("❌ Method 3 failed: " + e3.getMessage());
                lastException = e3;
            }
        }

        if (opencvLoaded) {
            System.out.println("✅ OpenCV loaded successfully!");
            System.out.println("OpenCV version: " + org.opencv.core.Core.VERSION);

            // Test if face module is available
            try {
                // Try to create a face recognizer to test if module is available
                LBPHFaceRecognizer test = LBPHFaceRecognizer.create();
                test.setThreshold(80.0);
                faceModuleAvailable = true;
                System.out.println("✅ Face module is available!");
            } catch (UnsatisfiedLinkError e) {
                System.out.println("⚠️ Face module not available: " + e.getMessage());
                System.out.println("Will use template matching fallback");
                faceModuleAvailable = false;
            } catch (Exception e) {
                System.out.println("⚠️ Face module test failed: " + e.getMessage());
                faceModuleAvailable = false;
            }
        } else {
            System.err.println("❌ Failed to load OpenCV!");
            if (lastException != null) {
                System.err.println("Last error: " + lastException.getMessage());
                lastException.printStackTrace();
            }
        }
    }

    public FaceRecognitionUtil() {
        if (!opencvLoaded) {
            System.err.println("⚠️ OpenCV not loaded, face recognition disabled");
            return;
        }

        try {
            // Load face detection classifier
            String classifierPath = "src/main/resources/haarcascade_frontalface_default.xml";
            File classifierFile = new File(classifierPath);

            if (!classifierFile.exists()) {
                // Try to load from classpath
                URL resourceUrl = getClass().getResource("/haarcascade_frontalface_default.xml");
                if (resourceUrl != null) {
                    classifierPath = resourceUrl.getPath();
                    classifierFile = new File(classifierPath);
                } else {
                    try {
                        downloadHaarCascade();
                    } catch (IOException e) {
                        System.err.println("Failed to download Haar cascade: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            faceDetector = new CascadeClassifier(classifierPath);
            if (faceDetector.empty()) {
                System.err.println("❌ Failed to load face detector from: " + classifierPath);
            } else {
                System.out.println("✅ Face detector loaded successfully");
            }

            // Create LBPH Face Recognizer if module is available
            if (faceModuleAvailable) {
                try {
                    faceRecognizer = LBPHFaceRecognizer.create();
                    faceRecognizer.setThreshold(80.0); // Lower = stricter, Higher = more tolerant
                    System.out.println("✅ LBPH Face Recognizer created");
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("❌ Failed to create face recognizer: " + e.getMessage());
                    faceModuleAvailable = false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error initializing FaceRecognitionUtil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void downloadHaarCascade() throws IOException {
        String url = "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml";
        String savePath = "src/main/resources/haarcascade_frontalface_default.xml";

        // Create directories if they don't exist
        File saveFile = new File(savePath);
        saveFile.getParentFile().mkdirs();

        System.out.println("Downloading Haar cascade to: " + savePath);
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(savePath));
            System.out.println("✅ Haar cascade downloaded successfully");
        }
    }

    /**
     * Detect face in image and return face region
     */
    public Mat detectFace(byte[] imageData) {
        if (!opencvLoaded || faceDetector == null || faceDetector.empty()) {
            System.out.println("❌ Face detector not available");
            return null;
        }

        try {
            Mat image = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                System.out.println("❌ Empty image data");
                return null;
            }

            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(gray, faceDetections, 1.1, 3, 0, new Size(100, 100), new Size(500, 500));

            Rect[] faces = faceDetections.toArray();
            if (faces.length == 0) {
                System.out.println("❌ No face detected");
                return null;
            }

            // Add margin around face
            Rect face = faces[0];
            int margin = 20;
            int x = Math.max(0, face.x - margin);
            int y = Math.max(0, face.y - margin);
            int w = Math.min(gray.width() - x, face.width + 2 * margin);
            int h = Math.min(gray.height() - y, face.height + 2 * margin);

            Mat faceROI = new Mat(gray, new Rect(x, y, w, h));

            // Resize to standard size for recognition
            Mat resized = new Mat();
            Imgproc.resize(faceROI, resized, new Size(200, 200));

            System.out.println("✅ Face detected and extracted, size: " + resized.size());
            return resized;

        } catch (Exception e) {
            System.err.println("❌ Error in face detection: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Train the recognizer with a face and user ID (LBPH method)
     */
    public void trainFace(int userId, byte[] faceImage) {
        if (!faceModuleAvailable || faceRecognizer == null) {
            System.out.println("⚠️ Face module not available, skipping training");
            return;
        }

        Mat face = detectFace(faceImage);
        if (face != null) {
            trainingImages.add(face);
            trainingLabels.add(userId);
            userLabelMap.put(userId, userId);
            modelTrained = false;
            System.out.println("✅ Added face for user " + userId + " to training set");
        }
    }

    /**
     * Build the recognition model from all training images (LBPH method)
     */
    private void buildModel() {
        if (!faceModuleAvailable || faceRecognizer == null) {
            return;
        }

        if (trainingImages.isEmpty()) {
            System.out.println("⚠️ No training images available");
            return;
        }

        try {
            List<Mat> images = new ArrayList<>();
            Mat labels = new Mat(trainingLabels.size(), 1, CvType.CV_32SC1);

            for (int i = 0; i < trainingLabels.size(); i++) {
                labels.put(i, 0, trainingLabels.get(i));
                images.add(trainingImages.get(i));
            }

            faceRecognizer.train(images, labels);
            modelTrained = true;
            System.out.println("✅ Model trained with " + trainingLabels.size() + " faces");
        } catch (Exception e) {
            System.err.println("❌ Error building model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Compare faces using LBPH (if available) or fallback to template matching
     */
    public double compareFaces(byte[] capturedFace, byte[] storedFace) {
        return compareFacesWithUser(capturedFace, storedFace, -1);
    }

    /**
     * Compare faces with user ID for training (LBPH if available, otherwise template matching)
     */
    public double compareFacesWithUser(byte[] capturedFace, byte[] storedFace, int userId) {
        if (!opencvLoaded) {
            System.out.println("❌ OpenCV not loaded");
            return 0;
        }

        // If face module is available, use LBPH
        if (faceModuleAvailable && faceRecognizer != null) {
            return compareFacesLBPH(capturedFace, storedFace, userId);
        } else {
            // Fallback to template matching
            System.out.println("⚠️ Using template matching fallback");
            return compareFacesTemplate(capturedFace, storedFace);
        }
    }

    /**
     * LBPH-based face comparison
     */
    private double compareFacesLBPH(byte[] capturedFace, byte[] storedFace, int userId) {
        try {
            // Add stored face to training if not already (for known users)
            if (userId > 0) {
                boolean alreadyTrained = false;
                for (Integer label : trainingLabels) {
                    if (label == userId) {
                        alreadyTrained = true;
                        break;
                    }
                }

                if (!alreadyTrained && storedFace != null && storedFace.length > 0) {
                    Mat storedMat = detectFace(storedFace);
                    if (storedMat != null) {
                        trainingImages.add(storedMat);
                        trainingLabels.add(userId);
                        userLabelMap.put(userId, userId);
                        modelTrained = false;
                        System.out.println("✅ Added user " + userId + " to training set");
                    }
                }
            }

            // Build model if needed
            if (!modelTrained && !trainingImages.isEmpty()) {
                buildModel();
            }

            // Detect face in captured image
            Mat capturedMat = detectFace(capturedFace);
            if (capturedMat == null) {
                System.out.println("❌ No face detected in captured image");
                return 0;
            }

            // Use trained model for prediction
            if (modelTrained && !trainingImages.isEmpty()) {
                int[] predictedLabel = new int[1];
                double[] confidence = new double[1];

                faceRecognizer.predict(capturedMat, predictedLabel, confidence);

                System.out.println("📊 LBPH Prediction - Label: " + predictedLabel[0] +
                        ", Confidence: " + confidence[0]);

                // If we have a specific user to match against
                if (userId > 0) {
                    if (predictedLabel[0] == userId) {
                        // Convert confidence to similarity (lower confidence = better match)
                        double similarity = 1.0 - Math.min(confidence[0] / 100.0, 1.0);
                        similarity = Math.max(0, Math.min(1, similarity));
                        System.out.println("✅ Match found! Similarity: " + String.format("%.3f", similarity));
                        return similarity;
                    } else {
                        System.out.println("❌ Wrong user predicted: " + predictedLabel[0] + " (expected: " + userId + ")");
                        return 0;
                    }
                } else {
                    // No specific user, return confidence-based similarity
                    double similarity = 1.0 - Math.min(confidence[0] / 100.0, 1.0);
                    similarity = Math.max(0, Math.min(1, similarity));
                    System.out.println("📊 Similarity score: " + String.format("%.3f", similarity));
                    return similarity;
                }
            }

            return 0;

        } catch (Exception e) {
            System.err.println("❌ Error in LBPH comparison: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Template matching fallback method
     */
    private double compareFacesTemplate(byte[] face1, byte[] face2) {
        try {
            Mat mat1 = Imgcodecs.imdecode(new MatOfByte(face1), Imgcodecs.IMREAD_GRAYSCALE);
            Mat mat2 = Imgcodecs.imdecode(new MatOfByte(face2), Imgcodecs.IMREAD_GRAYSCALE);

            if (mat1.empty() || mat2.empty()) {
                System.out.println("❌ One or both face images are empty");
                return 0;
            }

            // Resize to same size
            Mat resized1 = new Mat();
            Mat resized2 = new Mat();
            Size sz = new Size(200, 200);
            Imgproc.resize(mat1, resized1, sz);
            Imgproc.resize(mat2, resized2, sz);

            // Apply histogram equalization
            Mat equalized1 = new Mat();
            Mat equalized2 = new Mat();
            Imgproc.equalizeHist(resized1, equalized1);
            Imgproc.equalizeHist(resized2, equalized2);

            // Template matching
            Mat result = new Mat();
            Imgproc.matchTemplate(equalized1, equalized2, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            double similarity = mmr.maxVal;
            System.out.println("📊 Template matching similarity: " + String.format("%.3f", similarity));

            return similarity;

        } catch (Exception e) {
            System.err.println("❌ Error in template matching: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Extract face features for database storage
     */
    public byte[] extractFaceFeatures(byte[] imageData) {
        Mat face = detectFace(imageData);
        if (face == null) {
            System.out.println("❌ No face detected, cannot extract features");
            return null;
        }

        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", face, mob);
            byte[] result = mob.toArray();
            System.out.println("✅ Face features extracted, size: " + result.length + " bytes");
            return result;
        } catch (Exception e) {
            System.err.println("❌ Error extracting face features: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if face module is available
     */
    public static boolean isFaceModuleAvailable() {
        return faceModuleAvailable;
    }

    /**
     * Check if OpenCV is loaded
     */
    public static boolean isOpenCVLoaded() {
        return opencvLoaded;
    }
}