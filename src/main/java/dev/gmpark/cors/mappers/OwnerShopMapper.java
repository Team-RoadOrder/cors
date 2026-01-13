package dev.gmpark.cors.mappers;
import dev.gmpark.cors.entities.LikeItemEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.entities.ShopItemImagesEntity;
import dev.gmpark.cors.vos.LikeItemVo;
import dev.gmpark.cors.vos.PageVo;
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
    ShopItemEntity[] selectAll();
    ShopItemEntity[] selectItemByStyle(@Param("style") String style);
    ShopItemEntity[] selectItemByIdAndCategory(@Param("shopId") int shopId, @Param("categoryName") String categoryName);
    int updateItem(@Param(value = "item") ShopItemEntity shopItem);
    int deleteItemById(@Param("id") Long id);
    int selectCountAll();
    ShopItemEntity[] selectAllItemByPage(@Param(value = "page") PageVo pageVo);
    ShopItemEntity[] selectItemsByKeyword(@Param("keyword") String keyword);
    int selectLikeItemCount(@Param("userEmail") String userEmail, @Param("shopId") int shopId, @Param("itemId") Long itemId);

    int insertLikeItem(@Param("likeItem") LikeItemEntity likeItem);

    int deleteLikeItem(@Param("userEmail") String userEmail, @Param("shopId") int shopId, @Param("itemId") Long itemId);
    // 반환 타입은 List나 배열[]로
    LikeItemVo[] selectLikeItemsByUser(@Param("userEmail") String userEmail);
    LikeItemEntity[] selectAllLikeItems();
    ShopItemEntity[] selectItemsByCategoriesOrderByLikes(@Param("categories") String[] categories);
    ShopItemEntity[] selectItemsByCategories(@Param("categories") String[] categories);
}
