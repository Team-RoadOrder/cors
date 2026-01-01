package dev.gmpark.cors.mappers;


import dev.gmpark.cors.entities.ShopInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShopInfoMapper {
    int insert(@Param(value = "info")ShopInfoEntity shopInfo);
    ShopInfoEntity selectShopByUserEmail(@Param("email") String email);
    ShopInfoEntity selectShopByShopId(@Param("shopId") int shopId);
    int update(@Param("info") ShopInfoEntity shopInfo);
    ShopInfoEntity[] selectAllShops();
}
