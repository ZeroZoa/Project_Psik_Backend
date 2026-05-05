package com.zerozoa.psik.global.init;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.contents.IngredientType;
import com.zerozoa.psik.domain.contents.Product;
import com.zerozoa.psik.domain.member.SkinConcern;
import com.zerozoa.psik.repository.contents.IngredientRepository;
import com.zerozoa.psik.repository.contents.MemberProductRepository;
import com.zerozoa.psik.repository.contents.ProductRepository;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import com.zerozoa.psik.repository.diary.SkinDiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("disabled")
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;
    private final MemberProductRepository memberProductRepository;
    private final SkinDiaryRepository skinDiaryRepository;
    private final SkinAnalysisRepository skinAnalysisRepository;

    @Override
    @Transactional
    public void run(String... args) {

        // 수정 후
        log.info("[DataInit] 기존 데이터 초기화 시작...");
        skinAnalysisRepository.deleteAll();  // 1. skin_analysis (skin_diary 참조)
        skinDiaryRepository.deleteAll();     // 2. skin_diary + skin_diary_product (cascade)
        memberProductRepository.deleteAll(); // 3. member_product (product 참조)
        ingredientRepository.deleteAll();    // 4. ingredient (product 참조)
        productRepository.deleteAll();       // 5. product (참조 없음)
        log.info("[DataInit] 초기 데이터 적재 시작...");

        log.info("[DataInit] 초기 데이터 적재 시작...");

        // =============================================================
        // 1단계: Product 독립 생성 및 저장
        // =============================================================

        Product retinolSerum = productRepository.save(Product.builder()
                .name("이니스프리 레티놀 시카 세럼")
                .brand("이니스프리")
                .price(25000L)
                .imageUrl("https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/syi/syi02261/g/61.jpg")

                .description("레티놀과 시카 성분이 함유된 세럼")
                .build());

        Product niacinamideSerum = productRepository.save(Product.builder()
                .name("디오디너리 나이아신아마이드 10%")
                .brand("디오디너리")
                .price(25000L)
                .link("https://kr.iherb.com/pr/advanced-clinicals-5-niacinamide-serum-1-75-fl-oz-52-ml/110665")
                .imageUrl("https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/ptt/ptt85040/g/69.jpg")
                .description("나이아신아마이드와 시카 성분이 함유된 세럼")
                .build());

        Product azelaicAcidCream = productRepository.save(Product.builder()
                .name("아젤리아아젤리아아젤리아아젤라산 크림")
                .brand("Gurene")
                .price(20000L)
                .imageUrl("https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg")
                .link("https://www.coupang.com/vp/products/9349780637?itemId=27736546563&vendorItemId=94790326802&sourceType=srp_product_ads&clickEventId=21a86bf0-30ac-11f1-b66c-18260eb832cb&korePlacement=15&koreSubPlacement=1&clickEventId=21a86bf0-30ac-11f1-b66c-18260eb832cb&korePlacement=15&koreSubPlacement=1&traceId=mnlak611")
                .description("아젤리아 크림")
                .build());

        log.info("[DataInit] Product 생성 완료 - 총 {}개", productRepository.count());

        // =============================================================
        // 2단계: Ingredient 생성
        // =============================================================

        // ── 레티놀 ──
        Ingredient retinoid = Ingredient.builder()
                .name("레티놀/Retinol")
                .type(IngredientType.GENERAL)
                .effectSummary("주름을 개선하고 피부 탄력을 높여주는 안티에이징 필수 성분")
                .description("한마디로 비타민 A에요! 피부 미백, 주름, 회복 등 다양한 방면에서 좋은 성분이에요.")
                .build();

        retinoid.addEffect("피부결개선");
        retinoid.addEffect("색소침착완화");
        retinoid.addEffect("좁쌀여드름예방");
        retinoid.addEffect("피부재생");
        retinoid.addEffect("주름개선");

        retinoid.addCaution("유효 농도 0.1% 이상 사용하는게 좋아요!");
        retinoid.addCaution("산화가 빨라 개봉하지 않더라도 빨리 사용하는게 좋고 직사광선을 피해 보관해야해요!");
        retinoid.addCaution("미개봉이더라도 6개월 정도면 산화되어 효과가 없어요!");

        retinoid.addSkinConcern(SkinConcern.WHITEHEAD);
        retinoid.addSkinConcern(SkinConcern.RECOVERY);
        retinoid.addSkinConcern(SkinConcern.WRINKLE);
        retinoid.addSkinConcern(SkinConcern.WHITENING);

        // ── 나이아신아마이드 ──
        Ingredient niacinamide = Ingredient.builder()
                .name("나이아신아마이드/Niacinamide")
                .type(IngredientType.GENERAL)
                .effectSummary("피지를 조절하고 피부 장벽을 튼튼하게 만들어 피부톤을 정리해주는 성분")
                .description("한마디로 비타민B3의 한 종류에요! 피부장벽 강화, 피지 조절, 항염증, 붉은 기를 개선해주는 올라운더 성분이에요.")
                .build();

        niacinamide.addEffect("여드름개선");
        niacinamide.addEffect("주름개선");
        niacinamide.addEffect("피지조절");
        niacinamide.addEffect("피부장벽");
        niacinamide.addEffect("잡티개선");

        niacinamide.addCaution("2~5%의 농도로도 충분해요!");

        niacinamide.addSkinConcern(SkinConcern.ACNE);
        niacinamide.addSkinConcern(SkinConcern.WRINKLE);
        niacinamide.addSkinConcern(SkinConcern.WHITENING);
        niacinamide.addSkinConcern(SkinConcern.RECOVERY);

        // ── 아젤라산 ──
        Ingredient azelaicAcid = Ingredient.builder()
                .name("아젤라산/Azelaic acid")
                .type(IngredientType.OTC)
                .effectSummary("염증을 진정시키고 여드름 압출 후 남은 붉은 기를 완화하는 성분")
                .description("통곡물 시리얼에서 발견되는 식이 성분이에요! 여드름을 억제해주고, 여드름 압출 전후로 쓰면 좋아요!")
                .build();

        azelaicAcid.addEffect("붉은기개선");
        azelaicAcid.addEffect("여드름억제");

        azelaicAcid.addCaution("피부 자극이 있을 수 있어요!");

        azelaicAcid.addSkinConcern(SkinConcern.ACNE);
        azelaicAcid.addSkinConcern(SkinConcern.SCAR);

        // ── 아다팔렌 ──
        Ingredient adapalene = Ingredient.builder()
                .name("아다팔렌/Adapalene")
                .type(IngredientType.PRESCRIPTION)
                .effectSummary("피부 턴오버를 촉진해 블랙헤드·화이트헤드를 예방해주는 성분")
                .description("3세대 레티노이드에요! 흔히들 디페린이라고 부르죠 기존 레티노이드보다 빛에 안정적이고 자극이 적어 좁쌀 여드름 치료에 효과적입니다. 한국에선 처방이 필요하지만 미국 등에선 마트에서 구매 가능합니다.")
                .build();

        adapalene.addEffect("좁쌀여드름예방");
        adapalene.addEffect("피부결개선");
        adapalene.addEffect("안티에이징");

        adapalene.addCaution("자극이 강해 따가움, 붉어짐 등이 발생할 수 있어요!");
        adapalene.addCaution("처음에는 조금씩 바르고, 점차 양을 늘려가면 좋아요!");
        adapalene.addCaution("밤에만 사용하고, 낮에는 자외선 차단제를 꼼꼼히 바르세요.");

        adapalene.addSkinConcern(SkinConcern.BLACKHEAD);
        adapalene.addSkinConcern(SkinConcern.WHITEHEAD);
        adapalene.addSkinConcern(SkinConcern.RECOVERY);
        adapalene.addSkinConcern(SkinConcern.WRINKLE);

        // ── 아데노신 ──
        Ingredient adenosine = Ingredient.builder()
                .name("아데노신/Adenosine")
                .type(IngredientType.GENERAL)
                .effectSummary("피부에 에너지를 공급해 주름을 개선하고 콜라겐을 채워주는 성분")
                .description("PDRN이 분해된 분자가 아데노신이에요! 바르는 화장품에서는 PDRN보다 아데노신이 효과가 더 좋아요!")
                .build();

        adenosine.addEffect("피부재생");
        adenosine.addEffect("주름개선");
        adenosine.addEffect("피부콜라겐증가");

        adenosine.addCaution("유효 농도 0.1%이상 사용하는게 좋아요!");
        adenosine.addCaution("산화 걱정이 없어요!");
        adenosine.addCaution("예민하고 자극적이지 않아요!");

        adenosine.addSkinConcern(SkinConcern.RECOVERY);
        adenosine.addSkinConcern(SkinConcern.WRINKLE);
        adenosine.addSkinConcern(SkinConcern.AGING);

        // =============================================================
        // 3단계: 성분 저장 + 제품 연결
        // =============================================================

        retinoid.addProduct(retinolSerum);
        retinoid.addProduct(niacinamideSerum);
        retinoid.addProduct(azelaicAcidCream);
        ingredientRepository.save(retinoid);

        niacinamide.addProduct(niacinamideSerum);
        niacinamide.addProduct(retinolSerum);
        niacinamide.addProduct(azelaicAcidCream);
        ingredientRepository.save(niacinamide);

        azelaicAcid.addProduct(niacinamideSerum);
        azelaicAcid.addProduct(retinolSerum);
        azelaicAcid.addProduct(azelaicAcidCream);
        ingredientRepository.save(azelaicAcid);

        adapalene.addProduct(azelaicAcidCream);
        adapalene.addProduct(niacinamideSerum);
        adapalene.addProduct(azelaicAcidCream);
        ingredientRepository.save(adapalene);

        adenosine.addProduct(retinolSerum);
        adenosine.addProduct(azelaicAcidCream);
        adenosine.addProduct(niacinamideSerum);
        ingredientRepository.save(adenosine);

        log.info("[DataInit] 초기 데이터 적재 완료! Ingredient: {}, Product: {}",
                ingredientRepository.count(), productRepository.count());
    }
}