package com.zerozoa.skinner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadRootPath;
    private final String baseUrl;

    public LocalFileStorageService(
            @Value("${app.file.upload-dir:uploads}") String uploadDir,
            @Value("${app.file.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;

        try {
            Files.createDirectories(this.uploadRootPath);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패: " + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // UUID 기반 파일명 → 중복 방지
        String storedFilename = UUID.randomUUID() + extension;

        Path targetDir = uploadRootPath.resolve(subDirectory);
        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedFilename);
            file.transferTo(targetPath.toFile());

            return baseUrl + "/uploads/" + subDirectory + "/" + storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + originalFilename, e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            String relativePath = fileUrl.replace(baseUrl + "/uploads/", "");
            Path filePath = uploadRootPath.resolve(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("파일 삭제 완료: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", fileUrl, e);
        }
    }

    @Override
    public void deleteAll(List<String> fileUrls) {
        if (fileUrls == null) return;
        fileUrls.forEach(this::delete);
    }
}