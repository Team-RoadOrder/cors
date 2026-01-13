package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendedService {
    private final ShopInfoMapper shopInfoMapper;
    private final OwnerShopMapper ownerShopMapper;

    public ShopInfoEntity[] getShopsByCategories(String[] categories) {
        return this.shopInfoMapper.selectShopsByCategories(categories);
    }

    public ShopItemEntity[] getItemsByCategoriesOrderByLikes(String[] categories) {
        return this.ownerShopMapper.selectItemsByCategoriesOrderByLikes(categories);
    }

    public ShopItemEntity[] getItemsByCategories(String[] categories) {
        return this.ownerShopMapper.selectItemsByCategories(categories);
    }
}
