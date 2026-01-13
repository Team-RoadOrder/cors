package dev.gmpark.cors.mappers;


import dev.gmpark.cors.entities.LikeShopEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.vos.LikeShopVo;
import dev.gmpark.cors.vos.PageVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShopInfoMapper {
    int insert(@Param(value = "info")ShopInfoEntity shopInfo);
    ShopInfoEntity selectShopByUserEmail(@Param("email") String email);
    ShopInfoEntity selectShopByShopId(@Param("shopId") int shopId);
    int update(@Param("info") ShopInfoEntity shopInfo);
    ShopInfoEntity[] selectAllShops();
    ShopInfoEntity[] selectSixShops();
    ShopInfoEntity[] selectAllByPage(@Param(value = "page")PageVo pageVo);
    ShopInfoEntity[] selectShopsByAddress(@Param("address") String address);
    int selectCountAll();
    int insertLikeShop(@Param(value = "like") LikeShopEntity likeShop);
    int deleteLikeShop(@Param("shopId") int shopId, @Param("email")  String email );
    int selectLikeCount(@Param(value = "like") LikeShopEntity likeShop);
    LikeShopVo[] selectLikedShopsByUser(@Param("userEmail") String userEmail);
    int selectLikeCountByShopId(@Param("shopId") int shopId);
    ShopInfoEntity[] selectShopsByCategories(@Param("categories") String[] categories);
}
