package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
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
}
