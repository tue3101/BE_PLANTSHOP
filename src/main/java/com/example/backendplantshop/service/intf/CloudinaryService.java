package com.example.backendplantshop.service.intf;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadImage(MultipartFile image, String folder);
//    void deleteImage(String publicId);
}