package dev.gmpark.cors.services;


import dev.gmpark.cors.CorsApplication;
import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.LikeItemVo;
import dev.gmpark.cors.vos.PageVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final OwnerShopMapper ownerShopMapper;
    private final AiRecommendationService aiRecommendationService;

    /*public  getItemById*/
    public ShopItemVo getItemById (Long id ) {
        return this.ownerShopMapper.selectItemVoById(id);
    }
    public ShopItemEntity[] searchItems(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ShopItemEntity[0]; // 키워드가 없으면 빈 배열 반환
        }
        return this.ownerShopMapper.selectItemsByKeyword(keyword);
    }
    public CommonResult toggleLikeItem(int shopId, Long itemId, RegisterEntity sessionUser) {
        if (sessionUser == null) {
            return CommonResult.FAILURE_SESSION;
        }

        // 1. 이미 좋아요를 눌렀는지 확인
        int count = this.ownerShopMapper.selectLikeItemCount(sessionUser.getEmail(), shopId, itemId);

        if (count > 0) {
            this.ownerShopMapper.deleteLikeItem(sessionUser.getEmail(), shopId, itemId);
            return CommonResult.FAILURE;
        } else {
            // 2-2. 없으면 -> 추가 (좋아요)
            LikeItemEntity likeItem = LikeItemEntity.builder()
                    .userEmail(sessionUser.getEmail())
                    .shopId(shopId)
                    .itemId(itemId)
                    .build();
            this.ownerShopMapper.insertLikeItem(likeItem);
            return CommonResult.SUCCESS;
        }
    }

    //  Java AI 서비스 호출로 변경
    public List<ShopItemVo> getRelatedItems(Long itemId) {
        List<ShopItemVo> relatedItems = new ArrayList<>();
        try {
            List<ShopItemEntity> recommendations = aiRecommendationService.getRecommendations(itemId);
            for (ShopItemEntity entity : recommendations) {
                // ShopItemEntity -> ShopItemVo 변환 (필요한 경우)
                // 여기서는 간단히 ID로 다시 조회하여 VO를 가져오거나, Entity를 VO로 변환하는 로직을 사용
                ShopItemVo item = this.getItemById(entity.getId());
                if (item != null) {
                    relatedItems.add(item);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠ [AI 추천 실패] : " + e.getMessage());
        }
        return relatedItems;
    }

    public ShopItemEntity[] getAllItems() {
        return this.ownerShopMapper.selectAll();
    }
    public ShopItemEntity[] getAllItemsByPage(PageVo pageVo, String address, String sort) {
        String region = "";

        // 주소 파싱 (시/도 추출)
        if (address != null && address.contains(" ")) {
            region = address.split(" ")[0];
        } else {
            region = address;
        }

        if (address == null) address = "";
        if (region == null) region = "";

        // 매퍼 호출
        return this.ownerShopMapper.selectAllItemByPage(pageVo, address, region, sort);
    }
    public int getCountAll(){
        return this.ownerShopMapper.selectCountAll();
    }
}