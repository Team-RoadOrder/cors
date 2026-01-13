package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.vos.PageVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MainService {
    private final OwnerShopMapper ownerShopMapper;
    private final ShopInfoMapper shopInfoMapper;

    public ShopItemEntity[] getAllByStyle(String style, String address) {
        // 주소 분리 로직 (getSixShop과 동일)
        String region = "";
        if (address != null && address.contains(" ")) {
            region = address.split(" ")[0]; // "대구시" 등 추출
        } else {
            region = address;
        }
        // 매퍼 호출 시 region, address 함께 전달
        return ownerShopMapper.selectItemByStyle(style, address, region);
    }
    public ShopInfoEntity[] getSixShop(String address ) {
        String region = "";
        if (address != null && address.contains(" ")) {
            region = address.split(" ")[0];
        } else {
            region = address;
        }
        return this.shopInfoMapper.selectSixShops(address , region);
    }
    public ShopInfoEntity[] getAllShop(String address) {
        String region = "";
        if (address != null && address.contains(" ")) {
            region = address.split(" ")[0];
        } else {
            region = address;
        }
        return this.shopInfoMapper.selectAllShops(address, region);
    }
    // [수정] sort 파라미터 추가
    public ShopInfoEntity[] getAllShopByPage(PageVo pageVo, String address, String sort) {
        String region = "";
        if (address != null && address.contains(" ")) {
            region = address.split(" ")[0];
        } else {
            region = address;
        }

        if (address == null) address = "";
        if (region == null) region = "";

        // [수정] Mapper에 sort 전달
        return this.shopInfoMapper.selectAllByPage(pageVo, address, region, sort);
    }
    public int getCountAll() {
        return this.shopInfoMapper.selectCountAll();
    }


}
