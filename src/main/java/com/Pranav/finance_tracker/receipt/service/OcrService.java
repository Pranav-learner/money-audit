package com.Pranav.finance_tracker.receipt.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class OcrService {

    @Value("${ocr.tesseract.datapath}")
    private String tessDataPath;

    @Value("${ocr.tesseract.language}")
    private String language;

    @PostConstruct
    void init() {
        File dataDir = new File(tessDataPath);
        if (!dataDir.exists()) {
            log.warn("Tesseract data path '{}' does not exist; relying on default", tessDataPath);
        }
    }

    /**
     * Creates a thread-confined ITesseract instance.
     * Tess4J's underlying C++ Tesseract engine is not thread-safe; sharing a single
     * instance across multiple concurrent request threads causes JNI memory corruption
     * and JVM crashes.
     */
    private ITesseract createTesseract() {
        ITesseract instance = new Tesseract();
        File dataDir = new File(tessDataPath);
        if (dataDir.exists()) {
            instance.setDatapath(tessDataPath);
        }
        instance.setLanguage(language);
        instance.setPageSegMode(6);
        return instance;
    }

    public String extractText(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                throw new IllegalArgumentException("Could not read image: " + imageFile.getName());
            }
            ITesseract instance = createTesseract();
            return instance.doOCR(image);
        } catch (TesseractException | IOException e) {
            log.error("OCR failed for {}: {}", imageFile.getName(), e.getMessage());
            throw new RuntimeException("OCR processing failed: " + e.getMessage(), e);
        }
    }
}
