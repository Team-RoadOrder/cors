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

    // [AI 추가] Python 서버 추천 호출
    public List<ShopItemVo> getRelatedItems(Long itemId) {
        List<ShopItemVo> relatedItems = new ArrayList<>();
        try {
            // 동적으로 할당된 포트 사용
            int port = CorsApplication.AI_SERVER_PORT;
            WebClient webClient = WebClient.create("http://127.0.0.1:" + port);

            Map response = webClient.get()
                    .uri("/recommend/" + itemId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // 동기 호출 (간편 구현)

            if (response != null && response.containsKey("recommendations")) {
                List<Map<String, Object>> recs = (List<Map<String, Object>>) response.get("recommendations");
                for (Map<String, Object> rec : recs) {
                    Long id = ((Number) rec.get("id")).longValue();
                    // 추천된 ID로 DB 정보 조회
                    ShopItemVo item = this.getItemById(id);
                    if (item != null) {
                        relatedItems.add(item);
                    }
                }
            }
        } catch (Exception e) {
            // AI 서버가 꺼져 있어도 메인 로직은 돌아가야 함
            System.out.println("⚠ [AI 서버 연결 실패] : " + e.getMessage());
        }
        return relatedItems;
    }

    public ShopItemEntity[] getAllItems() {
        return this.ownerShopMapper.selectAll();
    }
    public ShopItemEntity[] getAllItemsByPage(PageVo pageVo) {
        return this.ownerShopMapper.selectAllItemByPage(pageVo);
    }
    public int getCountAll(){
        return this.ownerShopMapper.selectCountAll();
    }
}