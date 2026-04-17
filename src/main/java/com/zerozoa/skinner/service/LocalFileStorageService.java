package com.zerozoa.skinner.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 로컬 파일 시스템 기반 이미지 저장 서비스
 * MultipartFile을 서버의 로컬 디렉토리에 저장하고 접근 가능한 URL을 반환
 * 배포 환경에서는 S3 등 외부 스토리지로 교체 권장
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    // 파일이 실제로 저장될 루트 디렉토리의 절대 경로
    private final Path uploadRootPath;

    // 저장된 파일에 접근할 수 있는 서버 기본 URL (예: http://localhost:8080)
    private final String baseUrl;

    //저장될 이미지 허용 확장자
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif"
    );

    /**
     * @param uploadDir 파일 업로드 루트 디렉토리 경로 (application.yaml의 app.file.upload-dir)
     * @param baseUrl   파일 접근 기본 URL (application.yaml의 app.file.base-url)
     */
    public LocalFileStorageService(
            @Value("${app.file.upload-dir:uploads}") String uploadDir,
            @Value("${app.file.base-url:http://localhost:8080}") String baseUrl
    ) {
        // 상대 경로를 절대 경로로 변환하고 경로 구분자를 정규화 (예: ./ ../ 제거)
        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;

        try {
            // 서버 시작 시 업로드 루트 디렉토리가 없으면 자동 생성
            // 이미 존재하면 예외 없이 그냥 통과
            Files.createDirectories(this.uploadRootPath);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패: " + uploadDir, e);
        }
    }

    /**
     * 파일을 서버 로컬에 저장하고 접근 가능한 URL 반환
     * @param file         저장할 MultipartFile
     * @param subDirectory 저장할 하위 디렉토리 (예: "posts", "profiles")
     * @return 저장된 파일의 접근 URL (예: http://localhost:8080/uploads/posts/uuid.jpg)
     */
    @Override
    public String store(MultipartFile file, String subDirectory) {
        String originalFilename = file.getOriginalFilename();

        // 확장자 추출
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // 확장자 없으면 ContentType으로 폴백
        if (extension.isEmpty() && file.getContentType() != null) {
            extension = switch (file.getContentType()) {
                case "image/jpeg" -> ".jpg";
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif"  -> ".gif";
                case "image/bmp"  -> ".bmp";
                default           -> ".jpg"; // 알 수 없는 경우 기본값
            };
        }

        // 허용되지 않는 확장자 차단
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("허용되지 않는 파일 형식입니다: " + extension);
        }

        // UUID 기반 파일명 생성 (원본 확장자 유지)
        String storedFilename = UUID.randomUUID() + extension;

        Path targetDir = uploadRootPath.resolve(subDirectory);
        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedFilename);

            Thumbnails.of(file.getInputStream())
                    .size(1280, 1280)
                    .outputQuality(0.8)
                    .toFile(targetPath.toFile());

            return baseUrl + "/uploads/" + subDirectory + "/" + storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + originalFilename, e);
        }
    }

    /**
     * URL에 해당하는 파일을 로컬에서 삭제
     * 파일이 존재하지 않으면 무시하고, 삭제 실패 시 예외를 던지지 않고 경고 로그만 남김
     * (삭제 실패가 서비스 전체를 중단시킬 만큼 치명적이지 않기 때문)
     * @param fileUrl 삭제할 파일의 접근 URL
     */
    @Override
    public void delete(String fileUrl) {
        try {
            // URL에서 baseUrl + "/uploads/" 부분을 제거하여 상대 경로 추출
            // 예: "http://localhost:8080/uploads/posts/uuid.jpg" → "posts/uuid.jpg"
            String relativePath = fileUrl.replace(baseUrl + "/uploads/", "");

            // 상대 경로를 루트 경로 기준으로 절대 경로로 변환
            Path filePath = uploadRootPath.resolve(relativePath);

            // 파일이 실제로 존재할 때만 삭제 시도
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("파일 삭제 완료: {}", filePath);
            }
        } catch (IOException e) {
            // 삭제 실패는 비즈니스 로직을 중단시키지 않고 경고 로그만 남김
            log.warn("파일 삭제 실패: {}", fileUrl, e);
        }
    }

    /**
     * 여러 파일 URL을 일괄 삭제
     * null이나 빈 리스트가 들어와도 안전하게 처리
     * @param fileUrls 삭제할 파일 URL 목록
     */
    @Override
    public void deleteAll(List<String> fileUrls) {
        if (fileUrls == null) return;
        // 각 URL에 대해 개별 delete 호출 → 하나 실패해도 나머지 삭제 계속 진행
        fileUrls.forEach(this::delete);
    }
}