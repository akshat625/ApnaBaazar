package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.InvalidImageFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region.static}")
    private String region;

    private static final String BASE_PATH = "users/";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".bmp");

    public String uploadProfileImage(String username, MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String key = BASE_PATH + username + extension;

        log.info("Uploading new profile image for user: {} with key: {}", username, key);

        // Delete any existing profile image
        boolean deleted = deleteProfileImage(username);
        if (deleted) {
            log.info("Previous profile image deleted for user: {}", username);
        } else {
            log.info("No existing profile image found for user: {}", username);
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl("public-read")
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        log.info("Successfully uploaded image to S3 with key: {}", key);

        return key;
    }

    public boolean doesObjectExist(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.getObject(request);
            log.debug("Object exists: {}", key);
            return true;
        } catch (Exception e) {
            log.debug("Object not found: {}", key);
            return false;
        }
    }

    public String getProfileImageUrl(String username, String defaultImageUrl) {
        for (String ext : ALLOWED_EXTENSIONS) {
            String key = BASE_PATH + username + ext;
            if (doesObjectExist(key)) {
                String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
                log.info("Found profile image for {}: {}", username, imageUrl);
                return imageUrl;
            }
        }
        log.warn("No profile image found for {}, using default image", username);
        return defaultImageUrl;
    }

    public boolean deleteProfileImage(String username) {
        log.info("Attempting to delete profile image for user: {}", username);
        for (String ext : ALLOWED_EXTENSIONS) {
            String key = BASE_PATH + username + ext;
            if (doesObjectExist(key)) {
                s3Client.deleteObject(builder -> builder.bucket(bucket).key(key));
                log.info("Deleted image with key: {}", key);
                return true;
            }
        }
        log.warn("No image found for user: {}", username);
        return false;
    }

    public String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new InvalidImageFormatException("Invalid file name: " + filename);
        }
        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageFormatException("Unsupported image format: " + extension);
        }
        return extension;
    }

    public String uploadProductVariationImage(String productId, String variationId, MultipartFile image, boolean isPrimary) throws IOException {
        String extension = getExtension(image.getOriginalFilename());
        String key = "products/" + productId + "/variations/";

        if (isPrimary) {
            key += variationId + extension;
        } else {
            // Add timestamp for unique secondary image names
            key += variationId + "_" + System.currentTimeMillis() + extension;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl("public-read")
                .contentType(image.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(image.getBytes()));

        return key;
    }

    public void deleteObject(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("Cannot delete object with null or empty key");
            return;
        }

        log.info("Deleting object with key: {}", key);
        try {
            s3Client.deleteObject(builder -> builder.bucket(bucket).key(key));
            log.info("Successfully deleted object: {}", key);
        } catch (Exception e) {
            log.error("Error deleting object {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to delete object: " + e.getMessage());
        }
    }


    public String getObjectUrl(String key) throws IOException {
        if (!doesObjectExist(key)) {
            throw new IOException("Object not found: " + key);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    public List<String> getSecondaryImageUrls(String productId, String variationId) {
        List<String> urls = new ArrayList<>();
        String prefix = "products/" + productId + "/variations/" + variationId + "_";

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            response.contents().forEach(object -> {
                String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, object.key());
                urls.add(url);
            });
        } catch (Exception e) {
            log.error("Error listing secondary images for product variation {}: {}", variationId, e.getMessage());
        }
        return urls;
    }

}
