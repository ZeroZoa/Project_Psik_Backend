package com.zerozoa.skinner.global.init;

import com.zerozoa.skinner.domain.contents.Ingredient;
import com.zerozoa.skinner.domain.contents.IngredientType;
import com.zerozoa.skinner.domain.contents.Tag;
import com.zerozoa.skinner.repository.contents.IngredientRepository;
import com.zerozoa.skinner.repository.contents.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final IngredientRepository ingredientRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void run(String... args) {

        if (ingredientRepository.count() > 0) {
            log.info("[DataInit] 이미 데이터가 존재하여 초기화를 건너뜁니다.");
            return;
        }

        log.info("[DataInit] 초기 데이터 적재 시작...");

        // 태그 생성
        Tag acneTag = tagRepository.save(new Tag("#여드름"));
        Tag inflammationTag = tagRepository.save(new Tag("#염증"));
        Tag soothingTag = tagRepository.save(new Tag("#진정"));
        Tag recoveryTag = tagRepository.save(new Tag("#회복"));
        Tag scarTag = tagRepository.save(new Tag("#흉터/자국"));

        Tag poreTag = tagRepository.save(new Tag("#모공"));
        Tag sebumTag = tagRepository.save(new Tag("#피지조절"));
        Tag whiteHeadTag = tagRepository.save(new Tag("#좁쌀/화이트헤드"));
        Tag blackHeadTag = tagRepository.save(new Tag("#블랙헤드"));

        Tag whiteningTag = tagRepository.save(new Tag("#미백"));
        Tag wrinkleTag = tagRepository.save(new Tag("#주름"));

        // =============================================================
        // 레티노이드/레티놀
        // =============================================================
        Ingredient retinoid = Ingredient.builder()
                .name("레티놀")
                .type(IngredientType.GENERAL)
                .description("한마디로 비타민 A에요! 피부 미백, 주름, 회복 등 다양한 방면에서 좋은 성분이에요. ")
                .build();

        retinoid.addEffect("피부 결 개선");
        retinoid.addEffect("색소 침착 완화");
        retinoid.addEffect("좁쌀 여드름 예방");
        retinoid.addEffect("피부 콜라겐 합성 증가");
        retinoid.addEffect("피부 재생 촉진");
        retinoid.addEffect("피부 주름 개선");

        retinoid.addCaution("유효 농도 0.1% 이상 사용하는게 좋아요!");
        retinoid.addCaution("산화가 빨라 개봉하지 않더라도 빨리 사용하는게 좋고 직사광선을 피해 보관해야해요!");
        retinoid.addCaution("미개봉이더라도 6개월 정도면 산화되어 효과가 없어요!");

        retinoid.addTag(whiteHeadTag);
        retinoid.addTag(recoveryTag);
        retinoid.addTag(wrinkleTag);
        retinoid.addTag(whiteningTag);

        // [변경] Product 생성 — ingredient 파라미터 제거, addProduct()로 관계 설정
        // 나중에 제품 데이터가 추가되면 이런 식으로:
        // Product retinolSerum = Product.builder()
        //         .name("이니스프리 레티놀 시카 세럼")
        //         .brand("이니스프리")
        //         .price(25000L)
        //         .description("레티놀과 시카 성분이 함유된 세럼")
        //         .build();
        // retinoid.addProduct(retinolSerum);
        // niacinamide.addProduct(retinolSerum); // 같은 제품을 여러 성분에 매핑 가능!

        ingredientRepository.save(retinoid);

        // =============================================================
        // 나이아신아마이드
        // =============================================================
        Ingredient niacinamide = Ingredient.builder()
                .name("나이아신아마이드")
                .type(IngredientType.GENERAL)
                .description("한마디로 비타민B3의 한 종류에요! 피부장벽 강화, 피지 조절, 항염증, 붉은 기를 개선해주는 올라운더 성분이에요.")
                .build();

        niacinamide.addEffect("항염증");
        niacinamide.addEffect("피부 주름 개선");
        niacinamide.addEffect("피지 조절");
        niacinamide.addEffect("피부 장벽 개선");
        niacinamide.addEffect("붉은 기 개선");

        niacinamide.addCaution("2~5%의 농도로도 충분해요!");

        niacinamide.addTag(sebumTag);
        niacinamide.addTag(wrinkleTag);
        niacinamide.addTag(inflammationTag);
        niacinamide.addTag(soothingTag);

        ingredientRepository.save(niacinamide);

        // =============================================================
        // 아젤라산
        // =============================================================
        Ingredient azelaicAcid = Ingredient.builder()
                .name("아젤라산")
                .type(IngredientType.OTC)
                .description("통곡물 시리얼에서 발견되는 식이 성분이에요! 여드름을 억제해주고, 여드름 압출 전후로 쓰면 좋아요!")
                .build();

        azelaicAcid.addEffect("여드름 압출 후 붉은 기 개선");
        azelaicAcid.addEffect("여드름 염증 억제");
        azelaicAcid.addEffect("화농성 여드름 억제");
        azelaicAcid.addEffect("좁쌀 여드름 억제");

        azelaicAcid.addCaution("피부 자극이 있을수있어요!");

        azelaicAcid.addTag(acneTag);
        azelaicAcid.addTag(inflammationTag);
        azelaicAcid.addTag(soothingTag);

        ingredientRepository.save(azelaicAcid);

        // =============================================================
        // 아다팔렌
        // =============================================================
        Ingredient adapalene = Ingredient.builder()
                .name("아다팔렌")
                .type(IngredientType.PRESCRIPTION)
                .description("3세대 레티노이드에요! 흔히들 디페린이라고 부르죠 기존 레티노이드보다 빛에 안정적이고 자극이 적어 좁쌀 여드름 치료에 효과적입니다. 한국에선 처방이 필요하지만 미국 등에선 마트에서 구매 가능합니다.")
                .build();

        adapalene.addEffect("좁쌀 여드름 배출 및 예방");
        adapalene.addEffect("피부 턴오버 주기 정상화 (각질 제거 효과)");

        adapalene.addCaution("자극이 강해 따가움, 붉어짐 등이 발생할 수 있어요!");
        adapalene.addCaution("처음에는 조금씩 바르고, 점차 양을 늘려가면 좋아요!");
        adapalene.addCaution("밤에만 사용하고, 낮에는 자외선 차단제를 꼼꼼히 바르세요.");

        adapalene.addTag(blackHeadTag);
        adapalene.addTag(whiteHeadTag);
        adapalene.addTag(recoveryTag);
        adapalene.addTag(wrinkleTag);

        ingredientRepository.save(adapalene);

        // =============================================================
        // 아데노신
        // =============================================================
        Ingredient adenosine = Ingredient.builder()
                .name("아데노신")
                .type(IngredientType.GENERAL)
                .description("PDRN이 분해된 분자가 아데노신이에요! 바르는 화장품에서는 PDRN보다 아데노신이 효과가 더 좋아요!")
                .build();

        adenosine.addEffect("피부 재생 촉진");
        adenosine.addEffect("피부 주름 개선");
        adenosine.addEffect("피부 콜라겐 증가");

        adenosine.addCaution("유효 농도 0.1%이상 사용하는게 좋아요!");
        adenosine.addCaution("산화 걱정이 없어요!");
        adenosine.addCaution("예민하고 자극적이지 않아요!");

        adenosine.addTag(recoveryTag);
        adenosine.addTag(wrinkleTag);
        adenosine.addTag(soothingTag);

        ingredientRepository.save(adenosine);

        log.info("[DataInit] 초기 데이터 적재 완료! (Ingredient: {})", ingredientRepository.count());
    }
}