package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final OwnerShopMapper ownerShopMapper;

    public Map<String, Object> search(String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 기본 유효성 검사 및 정규화
        String normalizedKeyword = this.normalizeKeyword(keyword);
        
        if (normalizedKeyword == null) {
            result.put("searchResults", new ShopItemEntity[0]);
            result.put("shopResults", new ShopInfoEntity[0]);
            return result;
        }

        // 공백으로 분리하여 키워드 배열 생성
        String[] keywords = normalizedKeyword.split("\\s+");

        // 2. 상품 검색
        ShopItemEntity[] searchResults = this.ownerShopMapper.selectItemsByKeyword(keywords);
        
        // 3. 매장 검색
        ShopInfoEntity[] shopResults = this.ownerShopMapper.selectShopsByKeyword(keywords);

        result.put("searchResults", searchResults);
        result.put("shopResults", shopResults);
        
        return result;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String trimmed = keyword.trim();

        // 길이 제한 (최대 50자)
        if (trimmed.length() > 50) {
            trimmed = trimmed.substring(0, 50);
        }

        trimmed = trimmed.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

        return trimmed;
    }
}
