package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.LikeItemVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

}
