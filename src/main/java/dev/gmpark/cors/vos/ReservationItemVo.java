package dev.gmpark.cors.vos;

import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(callSuper = true) // 부모 필드 데이터도 로그 찍힘
public class ReservationItemVo {
    private Long shopId;
    private Long itemId;
    private int reservationId;
    private String shopName;
    private String size;          // 사이즈
    private String itemName;      // 상품명 (DB: item_name -> CamelCase 자동매핑)
    private String color;         // 색상
    private int price;            // 가격
    private LocalDateTime visitDate; // 방문 날짜 (DB: visit_date)
    private String status;        // 상태
    private String imagePath;     // 이미지 경로 (서브쿼리 결과)
    private String userEmail;   // 예약자 이메일
    private String userName;    // 예약자 이름 (또는 닉네임)
    private String userTel;
}
