package com.zerozoa.psik.repository.contents;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.contents.IngredientType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.zerozoa.psik.domain.contents.QIngredient.ingredient;

//QueryDSL을 사용한 성분 리포지토리 구현체
@RequiredArgsConstructor
public class IngredientRepositoryImpl implements IngredientRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Ingredient> search(String keyword, IngredientType type, Pageable pageable) {

        //데이터 조회 쿼리
        List<Ingredient> content = queryFactory
                .selectFrom(ingredient)
                .where(
                        containKeyword(keyword), // 동적 쿼리
                        eqType(type)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifier(pageable)) // 정렬 적용
                .fetch();

        //카운트 쿼리 (최적화: 데이터가 적거나 페이지 초과 시 실행 안 함)
        JPAQuery<Long> countQuery = queryFactory
                .select(ingredient.count())
                .from(ingredient)
                .where(
                        containKeyword(keyword),
                        eqType(type)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // --- 동적 쿼리 조건 (BooleanExpression) ---
    // null을 반환하면 QueryDSL의 where 절에서 자동으로 제외됩니다.

    private BooleanExpression containKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        // [수정] contains -> containsIgnoreCase (대소문자 무시 검색)
        return ingredient.name.containsIgnoreCase(keyword)
                .or(ingredient.description.containsIgnoreCase(keyword));
    }

    private BooleanExpression eqType(IngredientType type) {
        if (type == null) {
            return null;
        }
        return ingredient.type.eq(type);
    }

    //정렬(Sort) 지원 메서드
    //Pageable의 Sort 객체를 QueryDSL의 OrderSpecifier로 변환
    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        if (!pageable.getSort().isEmpty()) {
            for (Sort.Order order : pageable.getSort()) {

                // 정렬 방향 (ASC / DESC)
                Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

                // 정렬 기준 컬럼 (PathBuilder를 사용하여 동적으로 처리)
                PathBuilder<Ingredient> pathBuilder = new PathBuilder<>(ingredient.getType(), ingredient.getMetadata());

                return new OrderSpecifier(direction, pathBuilder.get(order.getProperty()));
            }
        }
        // 정렬 조건이 없으면 기본적으로 ID 역순(최신순) 정렬
        return ingredient.id.desc();
    }
}