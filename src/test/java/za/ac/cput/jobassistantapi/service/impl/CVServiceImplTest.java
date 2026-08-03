package za.ac.cput.jobassistantapi.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import za.ac.cput.jobassistantapi.dto.response.CVDataResult;
import za.ac.cput.jobassistantapi.dto.response.CVResponse;
import za.ac.cput.jobassistantapi.exception.DuplicateResourceException;
import za.ac.cput.jobassistantapi.exception.InvalidRequestException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.AIService;
import za.ac.cput.jobassistantapi.service.BlobStorageService;
import za.ac.cput.jobassistantapi.service.PdfExtractionService;
import za.ac.cput.jobassistantapi.service.TextCleaningService;
import za.ac.cput.jobassistantapi.service.WordExtractionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CVServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CVRepository cvRepository;
    @Mock private PdfExtractionService pdfExtractionService;
    @Mock private WordExtractionService wordExtractionService;
    @Mock private TextCleaningService textCleaningService;
    @Mock private AIService aiService;
    @Mock private BlobStorageService blobStorageService;

    @InjectMocks
    private CVServiceImpl cvService;

    private static final String EMAIL = "user@example.com";

    private User user() {
        return new User.Builder().setId(1L).setEmail(EMAIL).build();
    }

    private CVDataResult sampleCvData() {
        return new CVDataResult(
                List.of("Java", "Spring Boot"),
                List.of("BSc Computer Science"),
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Test
    void uploadCV_pdf_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "raw pdf bytes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(blobStorageService.upload(any(), anyString())).thenReturn("https://blob.url/cv.pdf");
        when(pdfExtractionService.extractText(file)).thenReturn("raw text");
        when(textCleaningService.clean("raw text")).thenReturn("cleaned text");
        when(aiService.extractCVData("cleaned text")).thenReturn(sampleCvData());
        when(cvRepository.save(any(CV.class))).thenAnswer(invocation -> {
            CV cv = invocation.getArgument(0);
            return new CV.Builder().copy(cv).setId(42L).build();
        });

        CVResponse response = cvService.uploadCV(file, EMAIL);

        assertEquals(42L, response.getId());
        assertEquals("CV uploaded successfully", response.getMessage());
        verify(wordExtractionService, never()).extractText(any());
        verify(aiService, never()).extractTextFromImage(any(), anyString());
    }

    @Test
    void uploadCV_docx_dispatchesToWordExtractionService() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx bytes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(blobStorageService.upload(any(), anyString())).thenReturn("https://blob.url/cv.docx");
        when(wordExtractionService.extractText(file)).thenReturn("word text");
        when(textCleaningService.clean("word text")).thenReturn("cleaned word text");
        when(aiService.extractCVData("cleaned word text")).thenReturn(sampleCvData());
        when(cvRepository.save(any(CV.class))).thenAnswer(invocation -> {
            CV cv = invocation.getArgument(0);
            return new CV.Builder().copy(cv).setId(1L).build();
        });

        cvService.uploadCV(file, EMAIL);

        verify(wordExtractionService).extractText(file);
        verify(pdfExtractionService, never()).extractText(any());
    }

    @Test
    void uploadCV_image_dispatchesToGeminiVisionExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.png", "image/png", "png bytes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(blobStorageService.upload(any(), anyString())).thenReturn("https://blob.url/cv.png");
        when(aiService.extractTextFromImage(file.getBytes(), "image/png")).thenReturn("image text");
        when(textCleaningService.clean("image text")).thenReturn("cleaned image text");
        when(aiService.extractCVData("cleaned image text")).thenReturn(sampleCvData());
        when(cvRepository.save(any(CV.class))).thenAnswer(invocation -> {
            CV cv = invocation.getArgument(0);
            return new CV.Builder().copy(cv).setId(1L).build();
        });

        cvService.uploadCV(file, EMAIL);

        verify(aiService).extractTextFromImage(file.getBytes(), "image/png");
        verify(pdfExtractionService, never()).extractText(any());
        verify(wordExtractionService, never()).extractText(any());
    }

    @Test
    void uploadCV_userNotFound_throwsResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "bytes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cvService.uploadCV(file, EMAIL));
    }

    @Test
    void uploadCV_alreadyHasCv_throwsDuplicateResourceException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "bytes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(new CV.Builder().setId(1L).build()));

        assertThrows(DuplicateResourceException.class, () -> cvService.uploadCV(file, EMAIL));
        verify(blobStorageService, never()).upload(any(), anyString());
    }

    @Test
    void uploadCV_emptyFile_throwsInvalidRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", new byte[0]);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> cvService.uploadCV(file, EMAIL));
    }

    @Test
    void uploadCV_unsupportedFileType_throwsInvalidRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.txt", "text/plain", "notes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> cvService.uploadCV(file, EMAIL));
        verify(blobStorageService, never()).upload(any(), anyString());
    }

    @Test
    void replaceCV_success_deletesOldBlobAndSavesNewData() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "new-cv.pdf", "application/pdf", "new pdf bytes".getBytes());

        CV existing = new CV.Builder().setId(1L).setUserId(1L).setBlobUrl("https://blob.url/old-cv.pdf").build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(blobStorageService.upload(any(), anyString())).thenReturn("https://blob.url/new-cv.pdf");
        when(pdfExtractionService.extractText(file)).thenReturn("new raw text");
        when(textCleaningService.clean("new raw text")).thenReturn("new cleaned text");
        when(aiService.extractCVData("new cleaned text")).thenReturn(sampleCvData());
        when(cvRepository.save(any(CV.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CVResponse response = cvService.replaceCV(file, EMAIL);

        assertEquals(1L, response.getId());
        assertEquals("CV replaced successfully", response.getMessage());
        verify(blobStorageService).deleteByUrl("https://blob.url/old-cv.pdf");
    }

    @Test
    void replaceCV_noExistingCv_throwsResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "bytes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cvService.replaceCV(file, EMAIL));
        verify(blobStorageService, never()).upload(any(), anyString());
    }

    @Test
    void replaceCV_userNotFound_throwsResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "bytes".getBytes());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cvService.replaceCV(file, EMAIL));
    }

    @Test
    void getCVByUserEmail_success() {
        CV cv = new CV.Builder().setId(1L).setUserId(1L).build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(cv));

        CV result = cvService.getCVByUserEmail(EMAIL);

        assertEquals(1L, result.getId());
    }

    @Test
    void getCVByUserEmail_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cvService.getCVByUserEmail(EMAIL));
    }

    @Test
    void getCVByUserEmail_noCv_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cvService.getCVByUserEmail(EMAIL));
    }
}
