package utils;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import org.bytedeco.opencv.opencv_face.*;

import static org.bytedeco.opencv.global.opencv_core.*;
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

public class FaceRecognitionUtil {

    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer faceRecognizer;
    private static boolean opencvLoaded = false;
    private static boolean faceModuleAvailable = false;

    // Store training data
    private List<Mat> trainingImages = new ArrayList<>();
    private List<Integer> trainingLabels = new ArrayList<>();
    private boolean modelTrained = false;

    static {
        System.out.println("=== OpenCV Initialization Debug ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));

        try {
            // JavaCV automatically loads native libraries
            opencvLoaded = true;
            System.out.println("✅ JavaCV loaded successfully!");
            System.out.println("OpenCV version: " + opencv_core.CV_VERSION);

            // Test if face module is available
            try {
                LBPHFaceRecognizer test = LBPHFaceRecognizer.create();
                test.setThreshold(120.0);
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
            // Create resources directory if it doesn't exist
            File resourcesDir = new File("src/main/resources");
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs();
            }

            // Load face detection classifier
            String classifierPath = "src/main/resources/haarcascade_frontalface_default.xml";
            File classifierFile = new File(classifierPath);

            if (!classifierFile.exists()) {
                downloadHaarCascade(classifierPath);
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
                    faceRecognizer.setThreshold(120.0); // Higher = more tolerant
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

    private void downloadHaarCascade(String savePath) throws IOException {
        String url = "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml";
        System.out.println("📥 Downloading Haar cascade to: " + savePath);
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(savePath));
            System.out.println("✅ Haar cascade downloaded successfully");
        }
    }

    /**
     * ULTRA-TOLERANT face detection - matches the successful parameters from EnhancedFaceCaptureDialog
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

            int originalWidth = image.cols();
            int originalHeight = image.rows();
            System.out.println("📸 Processing image: " + originalWidth + "x" + originalHeight);

            // Convert to grayscale
            Mat gray = new Mat();
            cvtColor(image, gray, COLOR_BGR2GRAY);

            // Enhance contrast
            equalizeHist(gray, gray);

            // EXACTLY the same parameter sets that worked in EnhancedFaceCaptureDialog
            int[][] paramSets = {
                    {3, 100, 1},  // min neighbors 3, min size 100, scale 1.1
                    {2, 80, 1},   // min neighbors 2, min size 80, scale 1.1
                    {1, 60, 1},   // min neighbors 1, min size 60, scale 1.1
                    {2, 60, 2},   // min neighbors 2, min size 60, scale 1.05
                    {1, 40, 2},   // min neighbors 1, min size 40, scale 1.05
                    {1, 30, 2},   // Even smaller
                    {1, 20, 2},   // Very small
                    {1, 15, 3}    // Minimum size
            };

            RectVector faceDetections = new RectVector();
            boolean found = false;

            for (int[] params : paramSets) {
                double scaleFactor = params[2] == 1 ? 1.1 : (params[2] == 2 ? 1.05 : 1.03);

                faceDetector.detectMultiScale(
                        gray,
                        faceDetections,
                        scaleFactor,
                        params[0],
                        0,
                        new Size(params[1], params[1]),
                        new Size(gray.cols(), gray.rows())
                );

                if (!faceDetections.empty()) {
                    System.out.println("✅ Face detected with parameters: minNeighbors=" + params[0] +
                            ", minSize=" + params[1] + ", scale=" + scaleFactor);
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Last resort: extremely tolerant detection
                System.out.println("⚠️ Trying extremely tolerant detection...");
                faceDetector.detectMultiScale(
                        gray,
                        faceDetections,
                        1.01,  // very small scale factor
                        1,     // min neighbors = 1
                        0,
                        new Size(15, 15),
                        new Size(gray.cols(), gray.rows())
                );

                if (faceDetections.empty()) {
                    System.out.println("❌ No face detected after all attempts");
                    return null;
                }
                System.out.println("✅ Face detected with extremely tolerant parameters");
            }

            // Get the largest face
            Rect face = faceDetections.get(0);
            for (int i = 1; i < faceDetections.size(); i++) {
                Rect r = faceDetections.get(i);
                if (r.width() * r.height() > face.width() * face.height()) {
                    face = r;
                }
            }

            System.out.println("✅ Face found at: x=" + face.x() + " y=" + face.y() +
                    " w=" + face.width() + " h=" + face.height());

            // Add margin
            int margin = 30;
            int x = Math.max(0, face.x() - margin);
            int y = Math.max(0, face.y() - margin);
            int w = Math.min(gray.cols() - x, face.width() + 2 * margin);
            int h = Math.min(gray.rows() - y, face.height() + 2 * margin);

            Mat faceROI = new Mat(gray, new Rect(x, y, w, h));

            // Resize to standard size
            Mat resized = new Mat();
            resize(faceROI, resized, new Size(200, 200));

            System.out.println("✅ Face extracted and resized to 200x200");
            return resized;

        } catch (Exception e) {
            System.err.println("❌ Error in face detection: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Train the recognizer with a face and user ID
     */
    public void trainFace(int userId, byte[] faceImage) {
        if (!faceModuleAvailable || faceRecognizer == null) {
            System.out.println("⚠️ Face module not available, skipping training");
            return;
        }

        Mat face = detectFace(faceImage);
        if (face != null) {
            trainingImages.add(face.clone());
            trainingLabels.add(userId);
            modelTrained = false;
            System.out.println("✅ Added face for user " + userId + " to training set");

            // Build model immediately
            buildModel();
        } else {
            System.out.println("❌ Could not detect face in training image");
        }
    }

    /**
     * Build the recognition model from all training images
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
            // Create a new recognizer
            if (faceRecognizer != null) {
                faceRecognizer.close();
            }
            faceRecognizer = LBPHFaceRecognizer.create();
            faceRecognizer.setThreshold(120.0);

            MatVector images = new MatVector(trainingImages.size());
            Mat labels = new Mat(trainingLabels.size(), 1, opencv_core.CV_32SC1);

            for (int i = 0; i < trainingImages.size(); i++) {
                labels.ptr(i, 0).putInt(trainingLabels.get(i));
                images.put(i, trainingImages.get(i));
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
     * Compare faces with user ID for training (LBPH if available, otherwise template matching)
     */
    public double compareFacesWithUser(byte[] capturedFace, byte[] storedFace, int userId) {
        if (!opencvLoaded) {
            System.out.println("❌ OpenCV not loaded");
            return 0;
        }

        try {
            // First, detect face in captured image
            Mat capturedMat = detectFace(capturedFace);
            if (capturedMat == null || capturedMat.empty()) {
                System.out.println("❌ No face detected in captured image");
                return 0;
            }

            // If userId is -1, we're just comparing two faces without training
            if (userId == -1) {
                // Try LBPH if available, otherwise template matching
                if (faceModuleAvailable && faceRecognizer != null) {
                    // For simple comparison, we need to train temporarily
                    Mat storedMat = detectFace(storedFace);
                    if (storedMat != null && !storedMat.empty()) {
                        // Create a temporary recognizer
                        LBPHFaceRecognizer tempRecognizer = LBPHFaceRecognizer.create();
                        tempRecognizer.setThreshold(120.0);

                        MatVector tempImages = new MatVector(1);
                        Mat tempLabels = new Mat(1, 1, opencv_core.CV_32SC1);
                        tempLabels.ptr(0, 0).putInt(1);
                        tempImages.put(0, storedMat);

                        tempRecognizer.train(tempImages, tempLabels);

                        IntPointer labelPtr = new IntPointer(1);
                        DoublePointer confidencePtr = new DoublePointer(1);
                        tempRecognizer.predict(capturedMat, labelPtr, confidencePtr);

                        double confidence = confidencePtr.get();
                        double similarity = Math.max(0, 1.0 - (confidence / 100.0));
                        similarity = Math.min(1.0, Math.max(0, similarity));

                        tempRecognizer.close();
                        return similarity;
                    }
                }
                // Fallback to template matching
                return compareFacesTemplate(capturedMat, storedFace);
            }

            // If face module is available, use LBPH with existing model
            if (faceModuleAvailable && faceRecognizer != null) {
                return compareFacesLBPH(capturedMat, storedFace, userId);
            } else {
                // Fallback to template matching
                System.out.println("⚠️ Using template matching fallback");
                return compareFacesTemplate(capturedMat, storedFace);
            }
        } catch (Exception e) {
            System.err.println("❌ Error in face comparison: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Compare two face images without user ID
     * @param capturedFace captured face image bytes
     * @param storedFace stored face image bytes
     * @return similarity score (0-1)
     */
    public double compareFaces(byte[] capturedFace, byte[] storedFace) {
        return compareFacesWithUser(capturedFace, storedFace, -1);
    }

    /**
     * LBPH-based face comparison
     */
    private double compareFacesLBPH(Mat capturedMat, byte[] storedFace, int userId) {
        try {
            // Add stored face to training if not already present
            if (userId > 0 && storedFace != null && storedFace.length > 0) {
                boolean alreadyTrained = trainingLabels.contains(userId);
                if (!alreadyTrained) {
                    Mat storedMat = detectFace(storedFace);
                    if (storedMat != null && !storedMat.empty()) {
                        trainingImages.add(storedMat.clone());
                        trainingLabels.add(userId);
                        modelTrained = false;
                        System.out.println("✅ Added user " + userId + " to training set");
                    }
                }
            }

            // Build model if needed
            if (!modelTrained && !trainingImages.isEmpty()) {
                buildModel();
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

                    // Check if we got a valid prediction
                    if (predictedLabel == -1 || confidence > 120.0) {
                        System.out.println("⚠️ No valid match found");
                        return 0;
                    }

                    // If we have a specific user to match against
                    if (userId > 0) {
                        if (predictedLabel == userId) {
                            // Convert confidence to similarity (lower confidence = better match)
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
                        return similarity;
                    }
                } finally {
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
    private double compareFacesTemplate(Mat capturedMat, byte[] storedFace) {
        try {
            Mat storedMat = imdecode(new Mat(storedFace), IMREAD_GRAYSCALE);
            if (storedMat == null || storedMat.empty()) {
                System.out.println("❌ Stored face image is empty");
                return 0;
            }

            // Resize both to same size
            Mat resizedCaptured = new Mat();
            Mat resizedStored = new Mat();
            Size sz = new Size(200, 200);
            resize(capturedMat, resizedCaptured, sz);
            resize(storedMat, resizedStored, sz);

            // Equalize histograms
            Mat equalizedCaptured = new Mat();
            Mat equalizedStored = new Mat();
            equalizeHist(resizedCaptured, equalizedCaptured);
            equalizeHist(resizedStored, equalizedStored);

            // Template matching
            Mat result = new Mat();
            matchTemplate(equalizedCaptured, equalizedStored, result, TM_CCOEFF_NORMED);

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
     * Extract face features for database storage
     */
    public byte[] extractFaceFeatures(byte[] imageData) {
        if (!opencvLoaded) {
            System.err.println("OpenCV not loaded");
            return null;
        }

        System.out.println("Extracting face features from image: " + (imageData != null ? imageData.length + " bytes" : "null"));

        Mat face = detectFace(imageData);
        if (face == null || face.empty()) {
            System.out.println("❌ No face detected, cannot extract features");
            return null;
        }

        try {
            System.out.println("Face detected, size: " + face.cols() + "x" + face.rows());

            // Resize face to standard size
            Mat resized = new Mat();
            resize(face, resized, new Size(200, 200));

            // Equalize histogram to improve quality
            Mat equalized = new Mat();
            equalizeHist(resized, equalized);

            // Create MatVector for encoding
            MatVector buf = new MatVector();

            // Encode the face as JPEG
            boolean success = imencode(".jpg", equalized, buf.asByteBuffer());

            if (success && !buf.empty()) {
                Mat encoded = buf.get(0);
                if (encoded != null && !encoded.empty()) {
                    // Get the data pointer
                    BytePointer dataPointer = encoded.data();
                    int size = (int) (encoded.total() * encoded.channels());

                    if (dataPointer != null && !dataPointer.isNull() && size > 0) {
                        byte[] result = new byte[size];
                        dataPointer.get(result);

                        System.out.println("✅ Face features extracted, size: " + result.length + " bytes");
                        return result;
                    } else {
                        System.out.println("❌ Data pointer is null or size is 0");
                    }
                } else {
                    System.out.println("❌ Encoded mat is null or empty");
                }
            } else {
                System.out.println("❌ imencode failed or buf is empty");

                // Fallback: try ImageIO method
                System.out.println("Trying ImageIO fallback...");
                return extractFaceFeaturesImageIO(imageData);
            }

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
        if (face == null || face.empty()) {
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