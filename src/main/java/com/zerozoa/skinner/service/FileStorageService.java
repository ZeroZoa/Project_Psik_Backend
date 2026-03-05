package com.zerozoa.skinner.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 파일 저장 인터페이스
 * 구현체를 교체하면 로컬 or S3 전환 가능
 */
public interface FileStorageService {

    String store(MultipartFile file, String subDirectory);

    void delete(String fileUrl);

    void deleteAll(List<String> fileUrls);
}