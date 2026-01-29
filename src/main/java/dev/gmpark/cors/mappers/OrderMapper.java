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
    
    OrderEntity selectOrderById(@Param("id") Long id);
    int updateOrderStatus(OrderEntity order);

    //특정 상품 구매 완료 조회
    int selectCompleteOrderCount(@Param("itemId")Long ItemId,@Param("userEmail")String userEmail);
    OrderHistoryVo[] getAllOrders(String userEmail);

    OrderHistoryVo[] getAllOrdersByShopId(@Param("shopId") int shopId);
    int updateOrderItemStatus(@Param("id") Long id, @Param("status") int status);
    int updateOrderItemStatusAndRefundReason(@Param("id") Long id,
                                             @Param("status") int status,
                                             @Param("refundReason") String refundReason);
    // 추가: 주문 ID로 주문 아이템 조회
    List<OrderItemEntity> selectOrderItemsByOrderId(@Param("orderId") Long orderId);
    
    // 추가: ID로 주문 아이템 단건 조회
    OrderItemEntity selectOrderItemById(@Param("id") Long id);

    // PENDING 상태인 주문 삭제
    int deletePendingOrdersByUserEmail(@Param("userEmail") String userEmail);
}
