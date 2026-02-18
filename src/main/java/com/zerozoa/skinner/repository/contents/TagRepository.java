package com.zerozoa.skinner.repository.contents;

import com.zerozoa.skinner.domain.contents.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    /**
     *태그 이름으로 조회
     *태그 중복 저장 방지 (이미 존재하는 태그면 재사용)
     *태그 검색 기능
     */
    Optional<Tag> findByName(String name);
}