package com.deepsecure.analysis;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;

/**
 * Error Level Analysis (ELA).
 *
 * IDEA: Re-save the image at a known JPEG quality, then diff it against the
 * original pixel-for-pixel. A JPEG image that has never been edited since
 * its last save should compress fairly uniformly — the recompression error
 * should look roughly even across the whole frame. A region that was
 * pasted in, retouched, or resaved at a different quality tends to show a
 * DIFFERENT (often higher, sometimes just visually inconsistent) error
 * level than the rest of the image, because it has a different compression
 * history baked in.
 *
 * IMPORTANT CAVEATS (worth keeping in your report/README so this doesn't
 * get overclaimed):
 *  - ELA is most meaningful on JPEG source images. If the input was a PNG
 *    (lossless) or was already heavily compressed at a very low quality,
 *    the signal is weaker/noisier and the numeric score is less reliable
 *    on its own.
 *  - A single global "ELA score" (mean error) is a coarse summary. Real
 *    forensic use looks at the ELA *image* for LOCALIZED bright regions
 *    (i.e. a small patch that lights up while the rest stays dark), not
 *    just the average. This class returns both the visual map and a
 *    global score so you can present both.
 *  - Like the edge/noise heuristics elsewhere in this project, the exact
 *    thresholds here are reasonable starting points, not calibrated
 *    ground truth. Calibrate against a labeled dataset (e.g. CASIA v2)
 *    before relying on this for real investigative conclusions.
 */
public class ELAAnalyzer {

    // Recompression quality to diff against. 90 is a common ELA default —
    // high enough that a genuinely untouched, well-compressed JPEG shows
    // low uniform error, low enough to expose regions with mismatched
    // compression history.
    private static final int ELA_JPEG_QUALITY = 90;

    // Multiply the raw pixel differences so the ELA map is actually
    // visible to a human reader. Raw differences are usually just a few
    // intensity levels out of 255 — invisible without amplification.
    private static final double ELA_AMPLIFICATION = 20.0;

    public static class ELAResult {
        /** Amplified difference image, suitable for display. */
        public final Mat elaImage;
        /** Mean of the RAW (non-amplified) pixel difference, 0-255 scale. */
        public final double elaScore;

        public ELAResult(Mat elaImage, double elaScore) {
            this.elaImage = elaImage;
            this.elaScore = elaScore;
        }
    }

    public static ELAResult analyze(String imagePath) {
        Mat original = Imgcodecs.imread(imagePath);
        if (original.empty()) {
            throw new RuntimeException("ELA: could not read image at " + imagePath);
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("ela_", ".jpg");

            MatOfInt jpegParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, ELA_JPEG_QUALITY);
            Imgcodecs.imwrite(tempFile.getAbsolutePath(), original, jpegParams);

            Mat recompressed = Imgcodecs.imread(tempFile.getAbsolutePath());

            Mat rawDiff = new Mat();
            Core.absdiff(original, recompressed, rawDiff);

            // Global score: average error across all channels. Kept
            // separate from the amplified visual so scoring doesn't
            // depend on the display-only amplification factor.
            Scalar meanDiff = Core.mean(rawDiff);
            double elaScore = (meanDiff.val[0] + meanDiff.val[1] + meanDiff.val[2]) / 3.0;

            Mat amplified = new Mat();
            Core.multiply(rawDiff, new Scalar(ELA_AMPLIFICATION, ELA_AMPLIFICATION, ELA_AMPLIFICATION), amplified);

            return new ELAResult(amplified, elaScore);

        } catch (IOException e) {
            throw new RuntimeException("ELA analysis failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }
}
