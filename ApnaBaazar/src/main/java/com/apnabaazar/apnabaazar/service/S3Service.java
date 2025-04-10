package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.InvalidImageFormatException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region.static}")
    private String region;

    public static final String BASE_PATH = "users/";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".bmp");

    public String uploadSellerProfileImage(String username, MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String key = BASE_PATH + username + extension;

        // Delete previous versions (with any supported extension)
        for (String ext : ALLOWED_EXTENSIONS) {
            String oldKey = BASE_PATH + username + ext;
            if (!oldKey.equals(key) && doesObjectExist(oldKey)) {
                s3Client.deleteObject(builder -> builder.bucket(bucket).key(oldKey));
            }
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl("public-read")
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        return key;
    }

    public boolean doesObjectExist(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.getObject(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSellerProfileImageUrl(String username, String defaultImageUrl) {
        for (String ext : ALLOWED_EXTENSIONS) {
            String key = BASE_PATH + username + ext;
            if (doesObjectExist(key)) {
                return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
            }
        }
        return defaultImageUrl;
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
}
