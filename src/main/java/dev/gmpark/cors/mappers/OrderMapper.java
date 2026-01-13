package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.OrderEntity;
import dev.gmpark.cors.entities.OrderItemEntity;
import dev.gmpark.cors.vos.OrderHistoryVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    int insertOrder(OrderEntity order);
    int insertOrderItems(@Param("items") List<OrderItemEntity> items);
    //특정 상품 구매 완료 조회
    int selectCompleteOrderCount(@Param("itemId")Long ItemId,@Param("userEmail")String userEmail);
    OrderHistoryVo[] getAllOrders(String userEmail);
}
