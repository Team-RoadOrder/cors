package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.LikeShopEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.results.register.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShopService {
      private final ShopInfoMapper shopInfoMapper;
      private final OwnerShopMapper ownerShopMapper;

      public ShopInfoEntity getShopInfo( int ShopId) {
          if(ShopId < 1) {
              return null;
          }
           return this.shopInfoMapper.selectShopByShopId(ShopId);
      }
    public ShopItemEntity[] getItemsByShopAndCategory(int ShopId , String categoryName) {
        return this.ownerShopMapper.selectItemByIdAndCategory(ShopId,categoryName);
    }
    public CommonResult toggleLikeInfo(LikeShopEntity likeShop, RegisterEntity sessionUser) {
        likeShop.setUserEmail(sessionUser.getEmail());

        int count = this.shopInfoMapper.selectLikeCount(likeShop);
        if (count > 0) {
            this.shopInfoMapper.deleteLikeShop(likeShop.getShopId(), sessionUser.getEmail());
            return CommonResult.FAILURE;
        } else {
            likeShop.setCreatedAt(LocalDateTime.now());
            this.shopInfoMapper.insertLikeShop(likeShop);
            return CommonResult.SUCCESS;
        }
    }
    public int getShopLikeCount(int shopId) {
        return this.shopInfoMapper.selectLikeCountByShopId(shopId);
    }
}
