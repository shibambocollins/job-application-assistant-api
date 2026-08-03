package za.ac.cput.jobassistantapi.service.impl;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.jobassistantapi.service.WordExtractionService;

import java.io.IOException;

@Service
public class WordExtractionServiceImpl implements WordExtractionService {

    @Override
    public String extractText(MultipartFile file) {

        String filename = file.getOriginalFilename();
        boolean isDocx = filename != null && filename.toLowerCase().endsWith(".docx");

        try {
            if (isDocx) {
                try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            } else {
                try (HWPFDocument document = new HWPFDocument(file.getInputStream());
                     WordExtractor extractor = new WordExtractor(document)) {
                    return extractor.getText();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract Word document text", e);
        }
    }
}
