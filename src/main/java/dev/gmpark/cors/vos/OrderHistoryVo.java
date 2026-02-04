package dev.gmpark.cors.vos;

import dev.gmpark.cors.entities.OrderItemEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class OrderHistoryVo extends OrderItemEntity {
    private LocalDateTime createdAt;
    private String imagePath;
    private String itemName;
    private int status;
    // ⬇️ 아래 필드들을 추가해주세요 (SQL에서 조회하고 HTML에서 사용하는 값들)
    private String courier;
    private String trackingNumber;
    private String receiverName;    // 수령인 이름 (에러 원인)
    private String receiverPhone;   // 연락처
    private String address;         // 주소
    private String addressDetail;   // 상세 주소
    private String userEmail;       // 주문자 이메일
    private String request;
}