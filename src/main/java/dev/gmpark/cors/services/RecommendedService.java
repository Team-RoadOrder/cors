package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendedService {
    private final ShopInfoMapper shopInfoMapper;
    private final OwnerShopMapper ownerShopMapper;

    public Map<String, Object> getRecommendedData(RegisterEntity sessionUser) {
        Map<String, Object> result = new HashMap<>();

        // 1. 사용자 스타일 기반 카테고리 분류
        List<String> categories = this.getCategoriesFromUserStyle(sessionUser.getStyle());
        String[] categoriesArray = categories.toArray(new String[0]);
        String styleText = String.join(", ", categories);

        // 2. 데이터 조회
        ShopInfoEntity[] recommendedShops = this.shopInfoMapper.selectShopsByCategories(categoriesArray);
        ShopItemEntity[] popularItems = this.ownerShopMapper.selectItemsByCategoriesOrderByLikes(categoriesArray);
        ShopItemEntity[] allItems = this.ownerShopMapper.selectItemsByCategories(categoriesArray);

        // 3. 결과 맵에 담기
        result.put("userStyle", styleText);
        result.put("recommendedShops", recommendedShops);
        result.put("popularItems", popularItems);
        result.put("allItems", allItems);

        return result;
    }

    private List<String> getCategoriesFromUserStyle(String style) {
        List<String> categories = new ArrayList<>();
        if (style == null || style.isEmpty()) {
            categories.add("미니멀"); // 기본값
            return categories;
        }

        String[] styles = style.split(",");
        for (String s : styles) {
            String trimmedStyle = s.trim();
            // [수정] 숫자가 아닌 문자열 그대로 사용하도록 변경
            // 기존 숫자 매핑 로직 제거하고 입력된 문자열 그대로 리스트에 추가
            if (!trimmedStyle.isEmpty()) {
                categories.add(trimmedStyle);
            }
        }

        if (categories.isEmpty()) {
            categories.add("미니멀"); // 변환 후에도 비어있을 경우 기본값
        }

        return categories;
    }
}
