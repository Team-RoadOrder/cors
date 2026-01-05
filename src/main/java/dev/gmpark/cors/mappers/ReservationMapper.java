package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.ReservationEntity;
import dev.gmpark.cors.entities.ReservationItemsEntity;
import dev.gmpark.cors.vos.ReservationItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ReservationMapper {
    int insertReservation(@Param("reservation") ReservationEntity reservation);

    // 예약 상품들 저장 (List로 받음)
    int insertReservationItems(@Param("items") List<ReservationItemsEntity> items);
    List<ReservationItemVo> selectReservationsByEmail(@Param("email") String email);
    List<ReservationItemVo> selectReservationsByShopId(@Param("shopId") int shopId);
    int updateReservationStatus(@Param("reservationId") int reservationId, @Param("status") String status);
    int deleteReservationById(@Param("reservationId") int reservationId);
}
