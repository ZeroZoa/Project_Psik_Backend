-- ============================================================
-- V2: 초기 성분 및 제품 데이터 삽입
-- DataInit.java 대체 — 앱 실행에 필요한 기본 데이터
-- ============================================================

-- ── 제품 삽입 ──
INSERT INTO product (name, brand, price, description, image_url, link, owned_count)
VALUES
    -- ── 나이아신아마이드 ──
    ('디오디너리 나이아신아마이드 10% + 징크 1% 에센스', '디오디너리', 15400,
     '과도한 유분을 줄이고 칙칙한 피부 톤을 맑고 깨끗하게 가꾸어 주는 나이아신아마이드 10% 세럼',
     'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/syi/syi02261/g/61.jpg',
     'https://link.coupang.com/a/ewanCa',0),

    ('아누아 나이아신아마이드 10 TXA 4 다크 스팟 코렉팅 세럼', '아누아', 27900,
     '과도한 유분을 줄이고 칙칙한 피부 톤을 맑고 깨끗하게 가꾸어 주는 나이아신아마이드 10% 세럼',
     'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/ptt/ptt85040/g/69.jpg',
     'https://link.coupang.com/a/ewaDat', 0),

    ('나노레시피 나이아신아마이드 20% 원액', '나노레시피', 6350,
     '과도한 유분을 줄이고 칙칙한 피부 톤을 맑고 깨끗하게 가꾸어 주는 나이아신아마이드 20% 원액',
     'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/ptt/ptt85040/g/69.jpg',
     'https://link.coupang.com/a/ewXFNf', 0),

    -- ── 비타민C ──
    ('스킨스탠다드 30% 순수 비타민C 더하기세럼', '스킨스탠다드', 23000,
     '피부의 전반적인 컨디션을 생기있고 맑게 끌어올려 주는 비타민C 세럼',
     'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
     'https://link.coupang.com/a/ewhMSB', 0),

    ('더마팩토리 퓨어 비타민 CE 페룰릭 앰플', '더마팩토리', 11400,
        '피부를 생기있고 맑게해주는 데 도움을 주는 비타민C 세럼',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/ewhSrr', 0),

    -- ── 아젤라산 ──
    ('아젤리아 크림', '레오파마(LEO Pharma)', 25000,
        '여드름균 증식을 억제하고, 항염, 각질 제거, 미백 효과가 있는 크림',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        '', 0),

    -- ── 실리콘겔 ──
    ('메디폼 스카겔', '제네웰', 25000,
        '상처 부위의 색소를 옅게 하면서 흉터를 평평하게 해주며 자외선을 흉터로 부터 막아주는 겔',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/ewiCmR', 0),

    -- ── 아데노신 ──
    ('더마팩토리 아데노신 7500ppm 워터에센스', '더마팩토리', 9500,
        '피부에 보습을 돕고, 탄력있고 윤기있게 가꿔주는 아데노신 워터에센스',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/ewX0lr', 0),

    ('스킨소스 아데노신 10000 앰플', '스킨소스', 13270,
        '피부에 보습을 돕고, 탄력있고 윤기있게 가꿔주는 아데노신 앰플',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/ewX0LP', 0),

    -- ── 아다팔렌 ──
    ('디페린겔', 'GALDERMA', 35000,
     '피부 턴오버 주기를 앞당겨 염증, 노화, 블랙헤드에 좋은 3세대 레티노이드 아다팔렌 겔',
     'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
     '', 0),

    -- ── 덱스판테놀 ──
    ('스킨스탠다드 덱스판테놀 더하기세럼', '스킨스탠다드', 13270,
        '피부에 수분을 끌어당겨 유지해주고, 피부 진정 및 장벽 강화에 도움을 주는 덱스판테놀 세럼',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/ewX0LP', 0),

    ('나노레시피 D-판테놀 75%원액', '나노레시피', 11000,
        '피부에 수분을 끌어당겨 유지해주고, 피부 진정 및 장벽 강화에 도움을 주는 덱스판테놀 원액',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://www.coupang.com/vp/products/7631886117?itemId=26828005584&vendorItemId=92354387871&q=%EB%82%98%EB%85%B8%EB%A0%88%EC%8B%9C%ED%94%BC+d-%ED%8C%90%ED%85%8C%EB%86%80+75%25+%EC%9B%90%EC%95%A1&searchId=ed187a822670000&sourceType=search&itemsCount=36&searchRank=2&rank=2&traceId=moghx6qq', 0),

    -- ── AHA ──
    ('디오디너리 글리코릭 애시드 7% 엑스폴리에이팅 토너', '디오디너리', 11300,
        '피부 각질을 정리해 피부톤을 맑고 고르게해주고 피부결을 부드럽게 가꿔주는 AHA 토너',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/excqG5', 0),

    -- ── BHA ──
    ('스트라이덱스 센시티브 패드', '스트라이덱스', 11300,
        '각질을 정리해 피부톤을 맑고 고르게해주고 피부결을 부드럽게 가꿔주는 BHA 패드',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/excNp3', 0),

    ('마일드랩 베타트리칸 트러블 클렌징폼', '마일드랩', 18000,
        '각질을 정리해 피부톤을 맑고 고르게해주고 피부결을 부드럽게 가꿔주는 BHA 클렌징폼',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exc6yK', 0),

    -- ── 마데카소사이드 ──
    ('일리윤 세라마이드 아토 집중크림', '일리윤', 22900,
        '마데카소사이드가 포함되어 피부를 진정시키고 보습을 도와주는 크림',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exdbbr', 0),

    ('동국제약 마데카 크림', '동국제약', 10000,
        '마데카소사이드가 포함되어 피부를 진정시키고 보습을 도와주는 크림',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exdrVj', 0),

    -- ── 레티노이드/HPR ──
    ('디오디너리 그랜액티브 레티노이드 2% 에멀전', '디오디너리', 18400,
        '피부 주름과 모공 고민 케어하고, 피부톤을 맑고 고르게해주고 피부결을 부드럽게 가꿔주는 HPR(차세대 레티노이드) 에멀전',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exd1SB', 0),

    ('닥터오라클 레티노타이트닝 앰플', '닥터오라클', 35000,
        '피부 주름과 모공 고민 케어하고, 피부톤을 맑고 고르게해주고 피부결을 부드럽게 가꿔주는 HPR(차세대 레티노이드) 앰플',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exd6CE', 0),

    ('브이티코스메틱 시카 레티 에이 에센스 0.5', '브이티코스메틱', 28200,
        '피부 주름과 모공 고민 케어하고, 피부톤을 맑고 고르게해주고 피부결을 부드럽게 가꿔주는 HPR(차세대 레티노이드) 에센스',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exehc7', 0),

    -- ── 무기자차 ──
    ('비프루브 마린 무기자차 마일드 선크림', '비프루브', 5000,
        '징크옥사이드 무기자차로 부드럽고 가성비 있게 자외선을 막아주는 선크림 ',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://www.daisomall.co.kr/pd/pdr/SCR_PDR_0001?pdNo=1045146&recmYn=N', 0),

    ('아떼 릴리프 무기자차 선크림', '아떼', 18900,
        '징크옥사이드 무기자차로 부드럽게 자외선을 막아주는 선크림 ',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exl4qU', 0),

    ('조선미녀 데일리 틴티드 선세럼', '조선미녀', 18900,
        '징크옥사이드 무기자차로 백탁을 거의 없이 자외선을 막아주는 선크림 ',
        'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
        'https://link.coupang.com/a/exl3B0', 0);

    -- ── 유기자차 ──
--     ('메디힐 마데카소사이드 수분 선세럼 흔적 리페어', '메디힐', 12400,
--         '',
--         'https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/auu/auu73719/g/17.jpg',
--         '', 0);

-- ── 성분 삽입 ──
INSERT INTO ingredient (name, type, effect_summary, description)
VALUES
    ('레티노이드/Retinoid', 'GENERAL',
     '주름을 개선하고 피부 탄력을 높여주는 안티에이징 필수 성분',
     '한마디로 비타민 A에요! 피부 미백, 주름, 회복 등 다양한 방면에서 좋은 성분이에요!'),

    ('마데카소사이드/Madecassoside', 'GENERAL',
     '피부 재생을 촉진하고, 피부를 보호해주는 병풀에서 유래된 성분',
     '마데카솔에 들어가는 그 성분이에요. 병풀에서 추출되며, 피부재생 및 보호에 좋은 성분이에요!'),

    ('나이아신아마이드/Niacinamide', 'GENERAL',
     '피부 장벽을 튼튼하게 만들고, 피지를 조절하고, 피부톤을 정리해주는 성분',
     '한마디로 비타민B3의 한 종류에요! 피부장벽 강화, 피지 조절, 항염증, 붉은 기를 개선해주는 올라운더 성분이에요!'),

    ('아젤라산/Azelaic Acid', 'OTC',
     '여드름 염증을 진정시키고 여드름 압출 후 남은 붉은 기를 완화하는 성분',
     '통곡물에서 유래되는 성분이에요! 여드름을 억제 및 항염 효과가 뛰어나 여드름 압출 전후로 쓰면 좋아요!'),

    ('아다팔렌/Adapalene', 'PRESCRIPTION',
     '피부 턴오버를 촉진해 블랙헤드·화이트헤드를 예방해주는 성분(레티노이드)',
     '3세대 레티노이드에요. 흔히들 디페린이라고 부르죠. 기존 레티노이드보다 빛에 안정적이고 자극이 적어 좁쌀 여드름 치료에 효과적이고, 주름 및 모공 문제를 개선해줄 수 있어요!'),

    ('HPR/Hydroxypinacolone Retinoate', 'GENERAL',
     '피부 턴오버를 촉진해 블랙헤드·화이트헤드를 예방해주는 성분(레티노이드)',
     '차세대 레티노이드에요. 기존 레티노이드보다 산화에 강하고, 안정적이며, 주름 및 모공 문제를 개선해줄 수 있어요!'),

    ('아데노신/Adenosine', 'GENERAL',
     '피부에 에너지를 공급해 주름을 개선하고 콜라겐을 채워주는 성분',
     '바르는 화장품으로는 흡수가 어려운 고분자 PDRN과 달리, 피부 흡수가 용이해 주름 개선과 콜라겐 합성에 직접적인 도움을 주는 성분이에요!'),

    ('덱스판테놀/Dexpanthenol', 'GENERAL',
        '피부재생을 촉진하고, 손상된 피부 장벽 강화해주며, 피부 염증을 줄여주는 성분',
        '한마디로 비타민 B5에요! 스테로이드의 단점은 버리고 장점을 일부 갖고온 올라운더 성분이에요!'),

    ('무기자차/Physical Sunscreen', 'GENERAL',
        '자외선을 물리적으로 반사 및 산란하여 피부를 보호하는 성분',
        '미네랄 성분 등을 이용해서 자외선으로부터 피부를 보호해주는 성분이에요. 백탁이 있을 수 있어요.'),

    ('유기자차/Chemical Sunscreen', 'GENERAL',
        '자외선을 화학적으로 흡수 및 분해하여 피부를 보호하는 성분',
        '자외선을 흡수해 열로 방출하는 성분이에요. 무기자차보다 발림성이 좋지만, 피부에 자극이 있을 수 있어요.');

-- ── 성분 효과 삽입 ──
INSERT INTO ingredient_effects (ingredient_id, effect)
SELECT i.id, e.effect
FROM ingredient i
         JOIN (VALUES
                   ('레티노이드/Retinoid','피부결개선'),
                   ('레티노이드/Retinoid','피부톤개선'),
                   ('레티노이드/Retinoid','주름개선'),
                   ('레티노이드/Retinoid','피부재생'),
                   ('레티노이드/Retinoid','색소침착완화'),
                   ('레티노이드/Retinoid','블랙/화이트헤드예방'),
                   ('레티노이드/Retinoid','피부탄력증가'),

                   ('마데카소사이드/Madecassoside', '피부재생'),
                   ('마데카소사이드/Madecassoside', '피부보호'),
                   ('마데카소사이드/Madecassoside', '피부진정'),

                   ('나이아신아마이드/Niacinamide', '여드름개선'),
                   ('나이아신아마이드/Niacinamide', '피부톤개선'),
                   ('나이아신아마이드/Niacinamide', '피부결개선'),
                   ('나이아신아마이드/Niacinamide', '주름개선'),
                   ('나이아신아마이드/Niacinamide', '피지조절'),
                   ('나이아신아마이드/Niacinamide', '피부장벽강화'),
                   ('나이아신아마이드/Niacinamide', '잡티개선'),
                   ('나이아신아마이드/Niacinamide', '블랙/화이트헤드예방'),

                   ('아젤라산/Azelaic Acid', '여드름개선'),
                   ('아젤라산/Azelaic Acid', '여드름예방'),
                   ('아젤라산/Azelaic Acid', '기미개선'),
                   ('아젤라산/Azelaic Acid', '압출후붉은기개선'),

                   ('아다팔렌/Adapalene', '피부결개선'),
                   ('아다팔렌/Adapalene', '주름개선'),
                   ('아다팔렌/Adapalene', '피부재생'),
                   ('아다팔렌/Adapalene', '블랙/화이트헤드예방'),
                   ('아다팔렌/Adapalene', '색소침착완화'),

                   ('HPR/Hydroxypinacolone Retinoate', '피부결개선'),
                   ('HPR/Hydroxypinacolone Retinoate', '주름개선'),
                   ('HPR/Hydroxypinacolone Retinoate', '피부재생'),
                   ('HPR/Hydroxypinacolone Retinoate', '색소침착완화'),
                   ('HPR/Hydroxypinacolone Retinoate', '블랙/화이트헤드예방'),
                   ('HPR/Hydroxypinacolone Retinoate', '피부탄력증가'),

                   ('아데노신/Adenosine', '피부재생'),
                   ('아데노신/Adenosine', '주름개선'),
                   ('아데노신/Adenosine', '콜라겐합성촉진'),
                   ('아데노신/Adenosine', '피부장벽강화'),
                   ('아데노신/Adenosine', '여드름예방'),

                   ('덱스판테놀/Dexpanthenol', '피부장벽강화'),
                   ('덱스판테놀/Dexpanthenol', '피부진정'),
                   ('덱스판테놀/Dexpanthenol', '여드름예방'),
                   ('덱스판테놀/Dexpanthenol', '피부수분유지'),
                   ('덱스판테놀/Dexpanthenol', '피부재생'),

                   ('무기자차/Physical Sunscreen', '자외선방어'),

                   ('유기자차/Chemical Sunscreen', '자외선방어')
) AS e(name, effect) ON i.name = e.name;

-- ── 성분 주의사항 삽입 ──
INSERT INTO ingredient_cautions (ingredient_id, caution)
SELECT i.id, c.caution
FROM ingredient i
         JOIN (VALUES
                   ('레티노이드/Retinoid','유효농도 0.1%이상을 추천해요.'),
                   ('레티노이드/Retinoid','피부 자극이 있을 수도 있어요. 사용 후 이상이 있으면 중단해야 해요.'),
                   ('레티노이드/Retinoid','레티놀은 산화가 빨라요. 열과 직사광선을 피해 보관하고, 개봉 후 최대한 빨리 사용하는게 좋아요.'),

                   ('마데카소사이드/Madecassoside', '너무 낮은 유효농도는 효과가 없을 수도 있어요.'),

                   ('나이아신아마이드/Niacinamide', '고농도의 나이아신아마이드는 자극이 있을 수도 있어요. 사용 후 이상이 있으면 중단해야 해요.'),
                   ('나이아신아마이드/Niacinamide', '고농도 화장품들은 주의해서 사용해야해요.'),
                   ('나이아신아마이드/Niacinamide', '열과 직사광선을 피해 보관하고, 개봉 후 최대한 빨리 사용하는게 좋아요.'),

                   ('아젤라산/Azelaic Acid', '약국 구매를 추천해요'),

                   ('아다팔렌/Adapalene', '피부 자극이 있을 수도 있어요. 사용 후 이상이 있으면 중단해야 해요.'),
                   ('아다팔렌/Adapalene', '기존 레티노이드에 비해 산화에 안정적이에요.'),
                   ('아다팔렌/Adapalene', '병원에서 처방받아 구매할 수 있어요.'),

                   ('HPR/Hydroxypinacolone Retinoate', '유효농도 0.1%이상부터 효과가 입증됐어요.'),
                   ('HPR/Hydroxypinacolone Retinoate', '기존 레티노이드에 비해 산화에 안정적이에요.'),

                   ('아데노신/Adenosine', '유효농도 0.1%이상을 추천해요.'),
                   ('아데노신/Adenosine', 'PDRN은 분자가 커서 진피층 흡수가 어렵지만, 아데노신은 크기가 작아 바르는 화장품으로도 충분한 효과를 있어요.'),
                   ('아데노신/Adenosine', '레티놀보다 산화 걱정이 없어요.'),
                   ('아데노신/Adenosine', '나라에서 인정한 기능성 화장품 원료에요.'),

                   ('덱스판테놀/Dexpanthenol', '레티놀보다 산화 걱정이 없어요.'),

                   ('무기자차/Physical Sunscreen', '자외선방어'),

                   ('유기자차/Chemical Sunscreen', '자외선방어')
) AS c(name, caution) ON i.name = c.name;

-- ── 성분 피부고민 삽입 ──
INSERT INTO ingredient_skin_concerns (ingredient_id, skin_concern)
SELECT i.id, sc.skin_concern
FROM ingredient i
         JOIN (VALUES
                   ('레티노이드/Retinoid', 'WHITEHEAD'),
                   ('레티노이드/Retinoid', 'BLACKHEAD'),
                   ('레티노이드/Retinoid', 'RECOVERY'),
                   ('레티노이드/Retinoid', 'WRINKLE'),
                   ('레티노이드/Retinoid', 'WHITENING'),
                   ('레티노이드/Retinoid', 'KERATIN'),
                   ('레티노이드/Retinoid', 'AGING'),


                   ('마데카소사이드/Madecassoside', 'RECOVERY'),
                   ('마데카소사이드/Madecassoside', 'SCAR'),

                   ('나이아신아마이드/Niacinamide', 'ACNE'),
                   ('나이아신아마이드/Niacinamide', 'WHITEHEAD'),
                   ('나이아신아마이드/Niacinamide', 'BLACKHEAD'),
                   ('나이아신아마이드/Niacinamide', 'RECOVERY'),
                   ('나이아신아마이드/Niacinamide', 'WRINKLE'),
                   ('나이아신아마이드/Niacinamide', 'WHITENING'),
                   ('나이아신아마이드/Niacinamide', 'KERATIN'),


                   ('아젤라산/Azelaic Acid', 'ACNE'),
                   ('아젤라산/Azelaic Acid', 'SCAR'),
                   ('아젤라산/Azelaic Acid', 'SPOT'),

                   ('아다팔렌/Adapalene', 'ACNE'),
                   ('아다팔렌/Adapalene', 'BLACKHEAD'),
                   ('아다팔렌/Adapalene', 'WHITEHEAD'),
                   ('아다팔렌/Adapalene', 'RECOVERY'),
                   ('아다팔렌/Adapalene', 'WRINKLE'),
                   ('아다팔렌/Adapalene', 'AGING'),

                   ('HPR/Hydroxypinacolone Retinoate', 'ACNE'),
                   ('HPR/Hydroxypinacolone Retinoate', 'WHITEHEAD'),
                   ('HPR/Hydroxypinacolone Retinoate', 'BLACKHEAD'),
                   ('HPR/Hydroxypinacolone Retinoate', 'RECOVERY'),
                   ('HPR/Hydroxypinacolone Retinoate', 'WRINKLE'),
                   ('HPR/Hydroxypinacolone Retinoate', 'AGING'),

                   ('아데노신/Adenosine', 'ACNE'),
                   ('아데노신/Adenosine', 'RECOVERY'),
                   ('아데노신/Adenosine', 'WRINKLE'),
                   ('아데노신/Adenosine', 'AGING'),

                   ('덱스판테놀/Dexpanthenol', 'ACNE'),
                   ('덱스판테놀/Dexpanthenol', 'RECOVERY'),
                   ('덱스판테놀/Dexpanthenol', 'WRINKLE'),
                   ('덱스판테놀/Dexpanthenol', 'AGING'),

                   ('무기자차/Physical Sunscreen', 'SUN_CARE'),

                   ('유기자차/Chemical Sunscreen', 'SUN_CARE')
) AS sc(name, skin_concern) ON i.name = sc.name;

-- ── 성분-제품 연결 삽입 ──
INSERT INTO ingredient_product (ingredient_id, product_id)
SELECT i.id, p.id
FROM ingredient i
         JOIN product p ON (
    (i.name = '레티노이드/Retinoid' AND p.name IN ('디오디너리 그랜액티브 레티노이드 2% 에멀전', '브이티코스메틱 시카 레티 에이 에센스 0.5', '닥터오라클 레티노타이트닝 앰플'))
        OR (i.name = '마데카소사이드/Madecassoside' AND p.name IN ('일리윤 세라마이드 아토 집중크림', '동국제약 마데카 크림'))
        OR (i.name = '나이아신아마이드/Niacinamide' AND p.name IN ('디오디너리 나이아신아마이드 10% + 징크 1% 에센스', '아누아 나이아신아마이드 10 TXA 4 다크 스팟 코렉팅 세럼', '나노레시피 나이아신아마이드 20% 원액'))
        OR (i.name = '아젤라산/Azelaic Acid' AND p.name IN ('아젤리아 크림'))
        OR (i.name = 'HPR/Hydroxypinacolone Retinoate' AND p.name IN ('디오디너리 그랜액티브 레티노이드 2% 에멀전', '브이티코스메틱 시카 레티 에이 에센스 0.5', '닥터오라클 레티노타이트닝 앰플'))
        OR (i.name = '아다팔렌/Adapalene' AND p.name IN ('디페린겔'))
        OR (i.name = '아데노신/Adenosine' AND p.name IN ('더마팩토리 아데노신 7500ppm 워터에센스', '스킨소스 아데노신 10000 앰플'))
        OR (i.name = '덱스판테놀/Dexpanthenol' AND p.name IN ('스킨스탠다드 덱스판테놀 더하기세럼', '나노레시피 D-판테놀 75%원액'))
        OR (i.name = '무기자차/Physical Sunscreen' AND p.name IN ('비프루브 마린 무기자차 마일드 선크림', '아떼 릴리프 무기자차 선크림', '조선미녀 데일리 틴티드 선세럼'))
--         OR (i.name = '유기자차/Chemical Sunscreen' AND p.name IN ())
    );