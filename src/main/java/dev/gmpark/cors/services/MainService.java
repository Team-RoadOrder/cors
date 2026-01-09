package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MainService {
    private final OwnerShopMapper ownerShopMapper;
    private final ShopInfoMapper shopInfoMapper;

    public ShopItemEntity[] getAllByStyle(String style) {
        return ownerShopMapper.selectItemByStyle(style);
    }
    public ShopInfoEntity[] getSixShop() {
        return this.shopInfoMapper.selectSixShops();
    }
    public ShopInfoEntity[] getAllShop() {
        return this.shopInfoMapper.selectAllShops();
    }
}
