package dev.gmpark.cors.mappers;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.entities.ShopItemImagesEntity;
import dev.gmpark.cors.vos.ShopItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OwnerShopMapper {
    int insertItem(@Param(value = "item") ShopItemEntity shopItem);
    int insertShopItemImage(ShopItemImagesEntity shopItemImage);
    ShopItemEntity[] selectAllByShopId(@Param("shopId") int shopId);
    ShopItemEntity selectItemById(@Param("id") Long id);
    ShopItemVo selectItemVoById(@Param("id") Long id);
    ShopItemEntity[] selectItemByStyle(@Param("style") String style);
    ShopItemEntity[] selectItemByIdAndCategory(@Param("shopId") int shopId, @Param("categoryName") String categoryName);
    int updateItem(@Param(value = "item") ShopItemEntity shopItem);
    int deleteItemById(@Param("id") Long id);
}
