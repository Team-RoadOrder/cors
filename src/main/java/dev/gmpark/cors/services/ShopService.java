package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

}
