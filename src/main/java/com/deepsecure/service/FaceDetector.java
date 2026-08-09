package com.deepsecure.service;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;

public class FaceDetector {

    private CascadeClassifier faceCascade;
    private int lastCount = 0;

    // Constructor: Load Haar Cascade XML
    public FaceDetector() {
        String xmlPath = "src/main/resources/haarcascade_frontalface_default.xml";
        File xmlFile = new File(xmlPath);

        if (!xmlFile.exists()) {
            throw new RuntimeException("Face detection XML file missing!");
        }

        this.faceCascade = new CascadeClassifier(xmlFile.getAbsolutePath());
    }

    // 👤 Face Detection
    public Mat detectFaces(String imagePath) {
        Mat src = Imgcodecs.imread(imagePath);

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faceDetections = new MatOfRect();

        int minFaceSize = Math.max(30, Math.min(gray.rows(), gray.cols()) / 6);

        faceCascade.detectMultiScale(
                gray,
                faceDetections,
                1.1,
                8,
                0,
                new Size(minFaceSize, minFaceSize),
                new Size()
        );

        lastCount = faceDetections.toArray().length;

        for (Rect rect : faceDetections.toArray()) {
            Imgproc.rectangle(
                    src,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0),
                    3
            );
        }

        return src;
    }

    // 🧩 Edge Detection (Canny)
    public Mat analyzeEdges(String imagePath) {
        Mat src = Imgcodecs.imread(imagePath);
        Mat edges = new Mat();

        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(edges, edges, 100, 200);

        return edges;
    }

    // 📊 Noise Analysis (Laplacian)
    public Mat analyzeNoise(String imagePath) {
        Mat src = Imgcodecs.imread(imagePath);
        Mat noise = new Mat();

        Imgproc.cvtColor(src, noise, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(noise, noise, CvType.CV_8U);

        Core.multiply(noise, new Scalar(15), noise);

        return noise;
    }

    // 🧠 Tampering Score Calculation (Core Logic)
    //
    // Takes the SAME edgeDensity/noiseLevel/elaScore values already
    // computed by the caller (MainApp), instead of recomputing its own
    // mismatched metrics — this is what fixed the original bug where
    // the tampering score and verdict disagreed with each other.
    public double calculateTamperingScore(double edgeDensity, double noiseLevel, double elaScore) {

        double edgeRisk = clamp((edgeDensity - 8.0) / (25.0 - 8.0), 0.0, 1.0);
        double noiseRisk = clamp((20.0 - noiseLevel) / 20.0, 0.0, 1.0);
        double elaRisk = clamp((elaScore - 3.0) / (15.0 - 3.0), 0.0, 1.0);

        double score = (edgeRisk / 3.0 + noiseRisk / 3.0 + elaRisk / 3.0) * 100.0;

        return clamp(score, 0.0, 100.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // Getter for face count
    public int getLastCount() {
        return lastCount;
    }
}