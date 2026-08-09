# SENTINEL Forensics v2.0

**Digital Image Forensics & Tampering Analyser** — a desktop tool for
screening photos for signs of editing or manipulation.

🔗 Repository: [github.com/soubhagya-dev93/ImageForensics](https://github.com/soubhagya-dev93/ImageForensics)

---

## What it does

SENTINEL Forensics loads an image and runs a set of classic, well-established
forensic checks to help flag whether it may have been edited or manipulated
— useful as a first-pass investigative screening tool.

### Analysis modes

- **Face Map** — detects human faces (OpenCV Haar cascade), with grayscale +
  histogram-equalization preprocessing and a resolution-aware minimum face
  size to reduce false positives on higher-resolution photos.
- **Edge Map** — computes edge density (Canny) across the image; unusually
  high edge density can indicate sharp, artificial transitions consistent
  with editing.
- **Noise Analysis** — measures the image's noise level (Laplacian);
  unusually low noise can indicate smoothing or blending consistent with
  editing.
- **Error Level Analysis (ELA)** — re-compresses the image at a known JPEG
  quality and diffs it against the original. Regions edited or pasted in
  after the image's last "real" save tend to carry a different compression
  history than the rest of the photo, which shows up as a brightness
  inconsistency in the ELA output.

### Forensic report

**Generate Report** runs all analyses together and produces:

- A per-analysis breakdown (face, edge, noise, ELA) with plain-language
  observations
- A combined **Tampering Score**, computed from edge density, noise level,
  and ELA score together (rather than each metric being judged in
  isolation), with an overall verdict: Authentic / Suspicious / Highly
  Suspicious

---

## Note

This tool uses classic image forensics heuristics (edge, noise, ELA, face detection) as an investigative screening aid — results are leads to investigate further, not standalone proof of tampering.

---

## Tech stack

- **Java 17**, **JavaFX 20** (UI)
- **OpenCV 4.5.1** (via the `org.openpnp:opencv` Maven artifact) for image
  processing
- **Maven** build

## Project structure

```
ImageForensics/
├── src/main/java/
│   ├── analysis/
│   │   └── ELAAnalyzer.java          # Error Level Analysis
│   └── com/deepsecure/
│       ├── MainApp.java              # JavaFX UI, wiring, report generation
│       ├── service/
│       │   └── FaceDetector.java     # Face/edge/noise analysis + combined tampering score
│       └── util/
│           └── ImageUtils.java       # OpenCV Mat ↔ JavaFX Image conversion
├── src/main/resources/
│   └── haarcascade_frontalface_default.xml
├── pom.xml
```

## Running locally

1. Open the project in Eclipse (or any IDE with Maven support).
2. Let Maven resolve dependencies (`javafx-controls`, `opencv`).
3. Run `MainApp.java` (or `mvn javafx:run` from the terminal).
4. Click **Upload Image**, select a photo, choose an analysis mode, then
   **Run Analysis** or **Generate Report**.
