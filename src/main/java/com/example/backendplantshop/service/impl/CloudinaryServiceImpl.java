package com.example.backendplantshop.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.backendplantshop.service.intf.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile image, String folder) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "public_id", System.currentTimeMillis() + "_" + image.getOriginalFilename()
            );

            Map<?, ?> uploadResult = cloudinary.uploader().upload(image.getBytes(), params);
            String imageUrl = (String) uploadResult.get("secure_url");

            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary", e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

//    @Override
//    public void deleteImage(String publicId) {
//        try {
//            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
//            log.info("Image deleted successfully: {}", publicId);
//        } catch (IOException e) {
//            log.error("Error deleting image from Cloudinary", e);
//            throw new RuntimeException("Failed to delete image", e);
//        }
//    }
}