package za.ac.cput.jobassistantapi.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.jobassistantapi.service.BlobStorageService;

import java.io.IOException;

@Service
public class BlobStorageServiceImpl implements BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageServiceImpl.class);

    private final BlobContainerClient containerClient;

    public BlobStorageServiceImpl(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.container-name}") String containerName) {

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public String upload(MultipartFile file, String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            log.info("Uploaded blob {}", blobName);
            return blobClient.getBlobUrl();
        } catch (IOException e) {
            log.error("Failed to upload blob {}", blobName, e);
            throw new RuntimeException("Failed to upload CV to blob storage: " + e.getMessage());
        }
    }

    @Override
    public void deleteByUrl(String blobUrl) {
        if (blobUrl == null || blobUrl.isBlank()) {
            return;
        }
        String blobName = blobUrl.substring(blobUrl.lastIndexOf('/') + 1);
        boolean deleted = containerClient.getBlobClient(blobName).deleteIfExists();
        log.info("Delete blob {} — existed: {}", blobName, deleted);
    }
}
