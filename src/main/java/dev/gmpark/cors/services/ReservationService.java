package dev.gmpark.cors.services;


import dev.gmpark.cors.dtos.ReservationDto;
import dev.gmpark.cors.entities.ReservationEntity;
import dev.gmpark.cors.entities.ReservationItemsEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ReservationMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.vos.ReservationItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final OwnerShopMapper ownerShopMapper;
    private final ReservationMapper reservationMapper;
    private final ShopInfoMapper shopInfoMapper;
    public ShopItemEntity[] getItemsByShopId(ShopInfoEntity shopInfo) {
       return this.ownerShopMapper.selectAllByShopId(shopInfo.getShopId());
    }
    @Transactional // 예약과 상품 저장이 모두 성공하거나, 모두 실패해야 함
    public CommonResult registerReservation(ReservationDto dto, String userEmail) {
        LocalDateTime now = LocalDateTime.now();

        if (dto.getVisitDate() == null || dto.getVisitDate().isBefore(now)) {
            return CommonResult.FAILURE;
        }

        // 여러 상품을 각각의 예약으로 처리
        for (ReservationItemsEntity item : dto.getItems()) {
            ReservationEntity reservation = ReservationEntity.builder()
                    .userEmail(userEmail)
                    .shopId(dto.getShopId())
                    .visitDate(dto.getVisitDate())
                    .status("대기")
                    .createdAt(now)
                    .build();

            int insertCount = this.reservationMapper.insertReservation(reservation);
            if (insertCount == 0) {
                // 하나라도 실패하면 전체 롤백
                throw new RuntimeException("예약 생성에 실패했습니다.");
            }

            // 생성된 예약 ID를 상품에 설정
            item.setReservationId(reservation.getId());

            // 단일 상품을 리스트에 담아 insert
            this.reservationMapper.insertReservationItems(Collections.singletonList(item));
        }

        return CommonResult.SUCCESS;
    }
    public CommonResult deleteReservation( int reservationId ) {
        ReservationItemVo dbReservation = this.reservationMapper.selectReservationById(reservationId);
        if (dbReservation == null) return CommonResult.FAILURE;
        if (!dbReservation.getStatus().equals("대기")) {
            return CommonResult.FAILURE;
        }
        // 대기 상태일때를 제외하고는 삭제불가함
      return   this.reservationMapper.deleteReservationById(reservationId) > 0
                ? CommonResult.SUCCESS
                : CommonResult.FAILURE;
    }


}
