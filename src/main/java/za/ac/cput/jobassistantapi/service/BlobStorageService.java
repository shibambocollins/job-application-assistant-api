package za.ac.cput.jobassistantapi.service;

import org.springframework.web.multipart.MultipartFile;

public interface BlobStorageService {

    String upload(MultipartFile file, String blobName);

}
