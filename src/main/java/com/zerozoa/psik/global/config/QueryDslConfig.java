package com.zerozoa.psik.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *QueryDSL 설정 클래스
 *JPAQueryFactory를 Spring Bean으로 등록
 *Repository 어디서든 주입받아 사용할 수 있게 함
 *이 설정이 있어야 "private final JPAQueryFactory queryFactory;" 만으로 사용 가능
 */
@Configuration
public class QueryDslConfig {

    /**
     *EntityManager
     *@PersistenceContext는 스프링이 관리하는 EntityManager를 주입
     *이 EntityManager는 실제 객체가 아닌 '프록시(Proxy)'임
     *요청마다 별도의 영속성 컨텍스트를 할당해주므로, 동시성 문제(Thread-Safety)가 발생하지 않음
     */
    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
