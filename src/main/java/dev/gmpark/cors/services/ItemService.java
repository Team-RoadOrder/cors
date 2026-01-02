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
}
