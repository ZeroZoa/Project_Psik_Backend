package com.zerozoa.skinner.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 파일 저장 인터페이스
 * 구현체를 교체하면 로컬 or S3 전환 가능
 */
public interface FileStorageService {

    /**
     * 파일 저장
     * @param file 저장할 파일
     * @param subDirectory 저장할 하위 디렉토리
     * @return 저장된 파일의 접근 URL
     */
    String store(MultipartFile file, String subDirectory);

    /**
     * 단일 파일 삭제
     * @param fileUrl 삭제할 파일의 URL
     */
    void delete(String fileUrl);

    /**
     * 다수 파일 일괄 삭제
     * @param fileUrls 삭제할 파일 URL 목록
     */
    void deleteAll(List<String> fileUrls);
}