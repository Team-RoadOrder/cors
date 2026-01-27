package dev.gmpark.cors.entities;
import lombok.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public class ShopItemImagesEntity {
    private Long id;                // BIGINT (PK)
    private Long productId;         // ProductEntity의 id와 연결 (FK)
    private String imagePath;       // 실제 저장 경로 (예: /assets/images/products/uuid_name.png)
    private String originalName;    // 사용자가 올린 원래 파일명
    private LocalDateTime createdAt;
}
