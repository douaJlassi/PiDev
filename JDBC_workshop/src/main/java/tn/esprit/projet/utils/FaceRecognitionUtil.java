package tn.esprit.projet.utils;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import org.bytedeco.opencv.opencv_face.*;

import static org.bytedeco.opencv.global.opencv_core.minMaxLoc;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;
import org.bytedeco.opencv.global.opencv_core;

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

        try {
            // JavaCV automatically loads native libraries
            opencvLoaded = true;
            System.out.println("✅ JavaCV loaded successfully!");
            System.out.println("OpenCV version: " + opencv_core.CV_VERSION);

            // Test if face module is available
            try {
                LBPHFaceRecognizer test = LBPHFaceRecognizer.create();
                test.setThreshold(80.0);
                faceModuleAvailable = true;
                System.out.println("✅ Face module is available!");
            } catch (Throwable e) {
                System.out.println("⚠️ Face module not available: " + e.getMessage());
                System.out.println("Will use template matching fallback");
                faceModuleAvailable = false;
            }

        } catch (Throwable t) {
            System.err.println("❌ Failed to load JavaCV: " + t.getMessage());
            t.printStackTrace();
            opencvLoaded = false;
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
                    faceRecognizer.setThreshold(120.0); // Higher = more tolerant (80-120 is good range)
                    System.out.println("✅ LBPH Face Recognizer created with threshold: 120.0");
                } catch (Throwable e) {
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
            Mat image = imdecode(new Mat(imageData), IMREAD_COLOR);
            if (image == null || image.empty()) {
                System.out.println("❌ Empty image data");
                return null;
            }

            Mat gray = new Mat();
            cvtColor(image, gray, COLOR_BGR2GRAY);
            equalizeHist(gray, gray);

            RectVector faceDetections = new RectVector();
            faceDetector.detectMultiScale(gray, faceDetections, 1.1, 3, 0, new Size(100, 100), new Size(500, 500));

            if (faceDetections.empty()) {
                System.out.println("❌ No face detected");
                return null;
            }

            Rect face = faceDetections.get(0);

            int margin = 20;
            int x = Math.max(0, face.x() - margin);
            int y = Math.max(0, face.y() - margin);
            int w = Math.min(gray.cols() - x, face.width() + 2 * margin);
            int h = Math.min(gray.rows() - y, face.height() + 2 * margin);

            Mat faceROI = new Mat(gray, new Rect(x, y, w, h));

            Mat resized = new Mat();
            resize(faceROI, resized, new Size(200, 200));

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
            // Create a new recognizer to avoid memory issues
            if (faceRecognizer != null) {
                faceRecognizer.close();
            }
            faceRecognizer = LBPHFaceRecognizer.create();
            faceRecognizer.setThreshold(120.0); // Higher threshold = more tolerant

            MatVector images = new MatVector(trainingImages.size());
            Mat labels = new Mat(trainingLabels.size(), 1, opencv_core.CV_32SC1);

            for (int i = 0; i < trainingLabels.size(); i++) {
                labels.ptr(i, 0).putInt(trainingLabels.get(i));
                // Make a clone to ensure we own the data
                images.put(i, trainingImages.get(i).clone());
            }

            faceRecognizer.train(images, labels);
            modelTrained = true;
            System.out.println("✅ Model trained with " + trainingLabels.size() + " faces");

            // Clean up
            images.close();
            labels.close();

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

        try {
            // If face module is available, use LBPH
            if (faceModuleAvailable && faceRecognizer != null) {
                return compareFacesLBPH(capturedFace, storedFace, userId);
            } else {
                // Fallback to template matching
                System.out.println("⚠️ Using template matching fallback");
                return compareFacesTemplate(capturedFace, storedFace);
            }
        } catch (Exception e) {
            System.err.println("❌ Error in face comparison: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    /**
     * LBPH-based face comparison
     */
    /**
     * LBPH-based face comparison - FIXED VERSION
     */
    private double compareFacesLBPH(byte[] capturedFace, byte[] storedFace, int userId) {
        try {
            // Only add to training if we have a valid stored face and it's not already trained
            if (userId > 0 && storedFace != null && storedFace.length > 0) {
                boolean alreadyTrained = false;
                for (Integer label : trainingLabels) {
                    if (label == userId) {
                        alreadyTrained = true;
                        break;
                    }
                }

                if (!alreadyTrained) {
                    Mat storedMat = detectFace(storedFace);
                    if (storedMat != null && !storedMat.isNull()) {
                        trainingImages.add(storedMat.clone());
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
            if (capturedMat == null || capturedMat.isNull()) {
                System.out.println("❌ No face detected in captured image");
                return 0;
            }

            // Use trained model for prediction
            if (modelTrained && !trainingImages.isEmpty() && faceRecognizer != null) {
                IntPointer labelPtr = new IntPointer(1);
                DoublePointer confidencePtr = new DoublePointer(1);

                try {
                    faceRecognizer.predict(capturedMat, labelPtr, confidencePtr);

                    int predictedLabel = labelPtr.get();
                    double confidence = confidencePtr.get();

                    System.out.println("📊 LBPH Prediction - Label: " + predictedLabel +
                            ", Confidence: " + confidence);

                    // Check if we got a valid prediction (not -1)
                    if (predictedLabel == -1 || confidence > 100.0) {
                        System.out.println("⚠️ No valid match found in database");
                        return 0;
                    }

                    // If we have a specific user to match against
                    if (userId > 0) {
                        if (predictedLabel == userId) {
                            // Convert confidence to similarity (lower confidence = better match)
                            // LBPH confidence: 0 = perfect match, higher = worse match
                            double similarity = Math.max(0, 1.0 - (confidence / 100.0));
                            similarity = Math.min(1.0, Math.max(0, similarity));
                            System.out.println("✅ Match found! Similarity: " + String.format("%.3f", similarity));
                            return similarity;
                        } else {
                            System.out.println("❌ Wrong user predicted: " + predictedLabel + " (expected: " + userId + ")");
                            return 0;
                        }
                    } else {
                        // No specific user, return confidence-based similarity
                        double similarity = Math.max(0, 1.0 - (confidence / 100.0));
                        similarity = Math.min(1.0, Math.max(0, similarity));
                        System.out.println("📊 Similarity score: " + String.format("%.3f", similarity));
                        return similarity;
                    }
                } finally {
                    // Clean up pointers
                    labelPtr.close();
                    confidencePtr.close();
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
            Mat mat1 = imdecode(new Mat(face1), IMREAD_GRAYSCALE);
            Mat mat2 = imdecode(new Mat(face2), IMREAD_GRAYSCALE);

            if (mat1 == null || mat2 == null || mat1.empty() || mat2.empty()) {
                System.out.println("❌ One or both face images are empty");
                return 0;
            }

            Mat resized1 = new Mat();
            Mat resized2 = new Mat();
            Size sz = new Size(200, 200);
            resize(mat1, resized1, sz);
            resize(mat2, resized2, sz);

            Mat equalized1 = new Mat();
            Mat equalized2 = new Mat();
            equalizeHist(resized1, equalized1);
            equalizeHist(resized2, equalized2);

            Mat result = new Mat();
            matchTemplate(equalized1, equalized2, result, TM_CCOEFF_NORMED);

            DoublePointer minVal = new DoublePointer(1);
            DoublePointer maxVal = new DoublePointer(1);
            Point minPt = new Point();
            Point maxPt = new Point();

            minMaxLoc(result, minVal, maxVal, minPt, maxPt, null);

            double similarity = maxVal.get();
            System.out.println("📊 Template matching similarity: " + String.format("%.3f", similarity));

            return similarity;

        } catch (Exception e) {
            System.err.println("❌ Error in template matching: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Extract face features for database storage - FIXED VERSION
     */
    public byte[] extractFaceFeatures(byte[] imageData) {
        Mat face = detectFace(imageData);
        if (face == null) {
            System.out.println("❌ No face detected, cannot extract features");
            return null;
        }

        try {
            // Create a MatOfByte to store the encoded image
            MatVector buf = new MatVector();

            // Encode the face as JPEG - FIXED: correct imencode signature
            boolean success = imencode(".jpg", face, buf.asByteBuffer());

            if (success && !buf.empty()) {
                // Get the encoded data
                Mat encoded = buf.get(0);
                if (encoded != null && !encoded.empty()) {
                    // Get the data pointer
                    BytePointer dataPointer = encoded.data();
                    int size = (int) (encoded.total() * encoded.channels());

                    if (dataPointer != null && !dataPointer.isNull()) {
                        byte[] result = new byte[size];
                        dataPointer.get(result);

                        System.out.println("✅ Face features extracted, size: " + result.length + " bytes");
                        return result;
                    }
                }
            }

            System.err.println("❌ Failed to encode face image");
            return null;

        } catch (Exception e) {
            System.err.println("❌ Error extracting face features: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Alternative method using ImageIO as fallback
     */
    public byte[] extractFaceFeaturesImageIO(byte[] imageData) {
        Mat face = detectFace(imageData);
        if (face == null) {
            System.out.println("❌ No face detected, cannot extract features");
            return null;
        }

        try {
            // Convert Mat to BufferedImage
            int width = face.cols();
            int height = face.rows();
            int channels = face.channels();

            byte[] data = new byte[width * height * channels];
            face.data().get(data);

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte b = data[idx++];
                    byte g = data[idx++];
                    byte r = data[idx++];

                    int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                    bufferedImage.setRGB(x, y, rgb);
                }
            }

            // Write to byte array using ImageIO
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            byte[] result = baos.toByteArray();

            System.out.println("✅ Face features extracted using ImageIO, size: " + result.length + " bytes");
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error extracting face features with ImageIO: " + e.getMessage());
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