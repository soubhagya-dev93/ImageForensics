package com.deepsecure.util;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;

public class ImageUtils {
    // Converts OpenCV Matrix to JavaFX Image
    public static Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}