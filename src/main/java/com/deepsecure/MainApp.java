package com.deepsecure;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import nu.pattern.OpenCV;

import com.deepsecure.analysis.ELAAnalyzer;
import com.deepsecure.service.FaceDetector;
import com.deepsecure.util.ImageUtils;

import org.opencv.core.*;

import java.io.File;

public class MainApp extends Application {

    private ImageView imageView = new ImageView();
    private String currentFilePath;
    private TextArea reportArea = new TextArea();

    private RadioButton faceBtn = new RadioButton("Face Map");
    private RadioButton edgeBtn = new RadioButton("Edge Map");
    private RadioButton noiseBtn = new RadioButton("Noise Analysis");
    private RadioButton elaBtn = new RadioButton("ELA View");

    @Override
    public void start(Stage stage) {

        OpenCV.loadLocally();

        // 🔥 TITLE
        Label title = new Label("DIGITAL IMAGE FORENSICS & TAMPERING ANALYSER");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #00e6e6;");

        // 🔘 MODES
        ToggleGroup group = new ToggleGroup();
        faceBtn.setToggleGroup(group);
        edgeBtn.setToggleGroup(group);
        noiseBtn.setToggleGroup(group);
        elaBtn.setToggleGroup(group);
        faceBtn.setSelected(true);

        faceBtn.setStyle("-fx-text-fill: white;");
        edgeBtn.setStyle("-fx-text-fill: white;");
        noiseBtn.setStyle("-fx-text-fill: white;");
        elaBtn.setStyle("-fx-text-fill: white;");

        HBox modes = new HBox(20, faceBtn, edgeBtn, noiseBtn, elaBtn);
        modes.setAlignment(Pos.CENTER);

        // 🖼 IMAGE BOX
        imageView.setPreserveRatio(true);

        StackPane imgBox = new StackPane(imageView);
        imgBox.setStyle("-fx-background-color: #121212; -fx-border-color: #00e6e6; -fx-border-width: 2;");
        imageView.fitWidthProperty().bind(imgBox.widthProperty().subtract(20));
        imageView.fitHeightProperty().bind(imgBox.heightProperty().subtract(20));

        // 📊 REPORT AREA
        reportArea.setEditable(false);
        reportArea.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #39ff14; -fx-font-family: 'Consolas'; -fx-font-size: 14px;");

        VBox reportBox = new VBox(reportArea);
        reportBox.setStyle("-fx-background-color: #0d1117; -fx-border-color: #00e6e6; -fx-border-width: 2;");
        VBox.setVgrow(reportArea, Priority.ALWAYS);

        // 🎛 BUTTONS
        Button upBtn = new Button("📂 Upload Image");
        Button runBtn = new Button("🔍 Run Analysis");
        Button reportBtn = new Button("📊 Generate Report");

        runBtn.setDisable(true);
        reportBtn.setDisable(true);

        // Styles
        upBtn.setStyle("-fx-background-color: #0072ff; -fx-text-fill: white; -fx-font-weight: bold;");
        runBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        reportBtn.setStyle("-fx-background-color: #ff4c4c; -fx-text-fill: white; -fx-font-weight: bold;");

        // 📂 UPLOAD
        upBtn.setOnAction(e -> {
            File f = new FileChooser().showOpenDialog(stage);

            if (f != null) {
                currentFilePath = f.getAbsolutePath();
                imageView.setImage(new javafx.scene.image.Image(f.toURI().toString()));

                runBtn.setDisable(false);
                reportBtn.setDisable(false);

                reportArea.clear();
                reportArea.appendText(">> Evidence Loaded: " + f.getName() + "\n");
            }
        });

        // 🔍 RUN (IMAGE ONLY)
        runBtn.setOnAction(e -> {
            try {

                FaceDetector fd = new FaceDetector();
                reportArea.clear();

                File file = new File(currentFilePath);

                reportArea.appendText("=========== FORENSIC ANALYSIS ===========\n\n");
                reportArea.appendText(">> File: " + file.getName() + "\n\n");

                Mat output;

                // ---------- FACE ----------
                Mat faceResult = fd.detectFaces(currentFilePath);
                int faceCount = fd.getLastCount();

                // ---------- EDGE ----------
                Mat edgeResult = fd.analyzeEdges(currentFilePath);
                int edgePixels = Core.countNonZero(edgeResult);
                int totalPixels = edgeResult.rows() * edgeResult.cols();
                double edgeDensity = ((double) edgePixels / totalPixels) * 100;

                // ---------- NOISE ----------
                Mat noiseResult = fd.analyzeNoise(currentFilePath);
                double noiseLevel = Core.mean(noiseResult).val[0];

                // ---------- ELA ----------
                ELAAnalyzer.ELAResult elaResult = ELAAnalyzer.analyze(currentFilePath);

                // ==========================
                // 🔥 SHOW ONLY SELECTED MODE
                // ==========================

                if (faceBtn.isSelected()) {

                    output = faceResult;

                    reportArea.appendText("[FACE ANALYSIS]\n");
                    reportArea.appendText(">> Faces Detected: " + faceCount + "\n");

                    if (faceCount == 0)
                        reportArea.appendText(">> Observation: No human faces detected\n");
                    else if (faceCount <= 3)
                        reportArea.appendText(">> Observation: Natural face distribution\n");
                    else
                        reportArea.appendText(">> Observation: Unusual number of faces\n");

                }

                else if (edgeBtn.isSelected()) {

                    output = edgeResult;

                    reportArea.appendText("[EDGE ANALYSIS]\n");
                    reportArea.appendText(">> Edge Density: " + String.format("%.2f", edgeDensity) + "%\n");

                    if (edgeDensity > 10)
                        reportArea.appendText(">> Observation: High sharp transitions (possible editing)\n");
                    else
                        reportArea.appendText(">> Observation: Normal edge distribution\n");
                }

                else if (noiseBtn.isSelected()) {

                    output = noiseResult;

                    reportArea.appendText("[NOISE ANALYSIS]\n");
                    reportArea.appendText(">> Noise Level: " + String.format("%.2f", noiseLevel) + "\n");

                    if (noiseLevel < 20)
                        reportArea.appendText(">> Observation: Low noise (possible smoothing)\n");
                    else
                        reportArea.appendText(">> Observation: Natural noise pattern\n");
                }

                else {

                    output = elaResult.elaImage;

                    reportArea.appendText("[ELA ANALYSIS]\n");
                    reportArea.appendText(">> ELA Score: " + String.format("%.2f", elaResult.elaScore) + "\n");

                    if (elaResult.elaScore > 8.0)
                        reportArea.appendText(">> Observation: Elevated recompression error (possible prior edit/re-save)\n");
                    else
                        reportArea.appendText(">> Observation: Uniform, low recompression error\n");

                    reportArea.appendText(">> Note: look for bright LOCALIZED patches in the image\n");
                    reportArea.appendText(">>       above — an isolated hotspot is a stronger signal\n");
                    reportArea.appendText(">>       than the average score alone.\n");
                }

                
                // 🖼 Show correct image
                imageView.setImage(ImageUtils.mat2Image(output));

            } catch (Exception ex) {
                reportArea.appendText(">> Error: " + ex.getMessage() + "\n");
            }
        });
        // 📊 REPORT (FULL FORENSIC REPORT)
        reportBtn.setOnAction(e -> {
            try {

                FaceDetector fd = new FaceDetector();
                reportArea.clear();

                File file = new File(currentFilePath);

                reportArea.appendText("=============== FORENSIC ANALYSIS REPORT ===============\n\n");
                reportArea.appendText(">> File Name: " + file.getName() + "\n\n");

                // ================= FACE =================
                fd.detectFaces(currentFilePath);
                int faceCount = fd.getLastCount();

                String faceStatus;
                if (faceCount == 0)
                    faceStatus = "No faces detected";
                else if (faceCount <= 3)
                    faceStatus = "Normal face distribution";
                else
                    faceStatus = "Anomaly (unusual number of faces)";

                reportArea.appendText("[FACE ANALYSIS]\n");
                reportArea.appendText(">> Faces Detected: " + faceCount + "\n");
                reportArea.appendText(">> Status: " + faceStatus + "\n\n");

                // ================= EDGE =================
                Mat edge = fd.analyzeEdges(currentFilePath);
                int edgePixels = Core.countNonZero(edge);
                int totalPixels = edge.rows() * edge.cols();
                double edgeDensity = ((double) edgePixels / totalPixels) * 100;

                String edgeStatus = (edgeDensity > 10)
                        ? "High edge density (possible editing)"
                        : "Normal edge distribution";

                reportArea.appendText("[EDGE ANALYSIS]\n");
                reportArea.appendText(">> Edge Density: " + String.format("%.2f", edgeDensity) + "%\n");
                reportArea.appendText(">> Status: " + edgeStatus + "\n\n");

                // ================= NOISE =================
                Mat noise = fd.analyzeNoise(currentFilePath);
                double noiseLevel = Core.mean(noise).val[0];

                String noiseStatus = (noiseLevel < 20)
                        ? "Low noise (possible smoothing/editing)"
                        : "Natural noise pattern";

                reportArea.appendText("[NOISE ANALYSIS]\n");
                reportArea.appendText(">> Noise Level: " + String.format("%.2f", noiseLevel) + "\n");
                reportArea.appendText(">> Status: " + noiseStatus + "\n\n");

                // ================= ELA =================
                ELAAnalyzer.ELAResult elaResult = ELAAnalyzer.analyze(currentFilePath);
                double elaScore = elaResult.elaScore;

                String elaStatus = (elaScore > 8.0)
                        ? "Elevated recompression error (possible prior edit/re-save)"
                        : "Uniform, low recompression error";

                reportArea.appendText("[ELA ANALYSIS]\n");
                reportArea.appendText(">> ELA Score: " + String.format("%.2f", elaScore) + "\n");
                reportArea.appendText(">> Status: " + elaStatus + "\n\n");

                // ================= FINAL REPORT =================
                // Pass the SAME edgeDensity/noiseLevel/elaScore already
                // computed above, so the score is fused from exactly the
                // numbers shown in the report — no separate/inconsistent
                // recomputation.
                double tamperScore = fd.calculateTamperingScore(edgeDensity, noiseLevel, elaScore);

                // Verdict is driven by tamperScore ALONE. tamperScore is
                // already the fused signal from edge + noise + ELA risk, so
                // re-checking the raw sub-metrics here would double-count
                // them and let a single weak signal override an otherwise
                // low score.
                String verdict;
                if (tamperScore > 70)
                    verdict = "HIGHLY SUSPICIOUS (STRONG TAMPERING INDICATION)";
                else if (tamperScore > 40)
                    verdict = "SUSPICIOUS (POSSIBLE MANIPULATION)";
                else
                    verdict = "AUTHENTIC (LIKELY ORIGINAL)";

                reportArea.appendText("=============== FINAL FORENSIC REPORT ===============\n");

                reportArea.appendText(">> Face Analysis     : " + faceStatus + "\n");
                reportArea.appendText(">> Edge Analysis     : " + edgeStatus + "\n");
                reportArea.appendText(">> Noise Analysis    : " + noiseStatus + "\n");
                reportArea.appendText(">> ELA Analysis      : " + elaStatus + "\n\n");

                reportArea.appendText(">> OVERALL VERDICT   : " + verdict + "\n");
                reportArea.appendText(">> TAMPERING SCORE   : " + String.format("%.2f", tamperScore) + "%\n");

                reportArea.appendText("=====================================================\n");

            } catch (Exception ex) {
                reportArea.appendText(">> Error: " + ex.getMessage() + "\n");
            }
        });

        // 📦 BUTTON ROW
        HBox buttonRow = new HBox(15, upBtn, runBtn, reportBtn);
        buttonRow.setAlignment(Pos.CENTER);

        // 📦 LEFT PANEL
        VBox leftPane = new VBox(10, imgBox);
        VBox.setVgrow(imgBox, Priority.ALWAYS);

        // 🔥 SPLITPANE
        SplitPane splitPane = new SplitPane(leftPane, reportBox);
        splitPane.setDividerPositions(0.5);

        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // 🧱 ROOT
        VBox root = new VBox(15, title, modes, buttonRow, splitPane);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f2027;");

        stage.setScene(new Scene(root, 1000, 750));
        stage.setTitle("SENTINEL Forensics v2.0");
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}