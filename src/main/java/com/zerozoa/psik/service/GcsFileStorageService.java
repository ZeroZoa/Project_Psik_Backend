package com.zerozoa.psik.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Google Cloud Storage 기반 이미지 저장 서비스 (운영 환경 전용)
 * - Cloud Run Workload Identity로 자동 인증 (별도 키 불필요)
 * - 이미지 저장 시 Thumbnailator로 1280x1280 압축 후 업로드
 * - @Profile("prod") — 로컬은 LocalFileStorageService 사용
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class GcsFileStorageService implements FileStorageService {

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    @Value("${app.file.base-url}")
    private String baseUrl;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif"
    );

    /**
     * 파일을 GCS에 업로드하고 공개 URL 반환
     * @param file         저장할 MultipartFile
     * @param subDirectory 저장할 하위 디렉토리 (예: "posts", "analysis")
     * @return GCS 공개 URL (예: https://storage.googleapis.com/psik-bucket/posts/uuid.jpg)
     */
    @Override
    public String store(MultipartFile file, String subDirectory) {
        String extension = extractExtension(file);
        String objectName = subDirectory + "/" + UUID.randomUUID() + ".webp";

        try {
            byte[] imageBytes = ImmutableImage.loader()
                    .fromStream(file.getInputStream())
                    .bound(600, 600)
                    .bytes(WebpWriter.DEFAULT.withQ(85));

            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/webp")
                    .build();

            // google-cloud-storage 2.x API
            storage.create(blobInfo, imageBytes);

            log.debug("[GCS] 업로드 완료: gs://{}/{}", bucketName, objectName);
            return baseUrl + "/" + objectName;

        } catch (IOException e) {
            log.error("[GCS] 업로드 실패: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다.");
        }
    }

    /**
     * GCS에서 파일 삭제
     * 파일 없으면 무시, 실패 시 경고 로그만 남김 (삭제 실패가 비즈니스 로직 중단시키지 않음)
     * @param fileUrl 삭제할 파일의 접근 URL
     */
    @Override
    public void delete(String fileUrl) {
        try {
            // URL에서 objectName 추출
            // 예: "https://storage.googleapis.com/psik-bucket/posts/uuid.jpg" → "posts/uuid.jpg"
            String objectName = fileUrl.replace(baseUrl + "/", "");
            BlobId blobId = BlobId.of(bucketName, objectName);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.debug("[GCS] 삭제 완료: gs://{}/{}", bucketName, objectName);
            } else {
                log.warn("[GCS] 파일 없음 (이미 삭제됨): gs://{}/{}", bucketName, objectName);
            }
        } catch (Exception e) {
            log.warn("[GCS] 삭제 실패: {}", fileUrl, e);
        }
    }

    /**
     * 여러 파일 URL 일괄 삭제
     * null이나 빈 리스트 안전 처리
     * @param fileUrls 삭제할 파일 URL 목록
     */
    @Override
    public void deleteAll(List<String> fileUrls) {
        if (fileUrls == null) return;
        fileUrls.forEach(this::delete);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    /**
     * MultipartFile에서 확장자 추출
     * 파일명에서 실패 시 ContentType 폴백, 허용되지 않는 확장자 차단
     */
    private String extractExtension(MultipartFile file) {
        String extension = "";
        String originalFilename = file.getOriginalFilename();

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (extension.isEmpty() && file.getContentType() != null) {
            extension = switch (file.getContentType()) {
                case "image/jpeg" -> ".jpg";
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif"  -> ".gif";
                case "image/bmp"  -> ".bmp";
                default           -> ".jpg";
            };
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "허용되지 않는 파일 형식입니다: " + extension);
        }

        return extension;
    }

    /**
     * 확장자 → ContentType 변환
     */
    private String resolveContentType(String extension) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png"          -> "image/png";
            case ".webp"         -> "image/webp";
            case ".gif"          -> "image/gif";
            case ".bmp"          -> "image/bmp";
            default              -> "image/jpeg";
        };
    }
}