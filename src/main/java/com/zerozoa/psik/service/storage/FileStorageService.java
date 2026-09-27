package com.zerozoa.psik.service.storage;

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

    /**
     * URL에 해당하는 파일의 바이트 내용을 읽어옴
     * Worker가 비동기로 Gemini 분석을 수행할 때, 요청 시점에 저장해둔 이미지를 다시 읽기 위해 사용
     * @param fileUrl 읽을 파일의 접근 URL
     * @return 파일의 바이트 내용
     */
    byte[] readBytes(String fileUrl);
}