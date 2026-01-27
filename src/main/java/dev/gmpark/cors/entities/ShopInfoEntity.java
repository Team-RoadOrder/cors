package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(of = "shopId") // PK를 기준으로 객체 비교
public class ShopInfoEntity {
    private int shopId;        // PK: 매장 고유 ID (Auto Increment)
    private String userEmail;  // FK: 사장님 이메일
    private String shopName;
    private String shopTime;
    private String shopCategory;
    private String shopAddress;
    private String shopTel;
    private String profileImage;// DB에는 이미지 경로(String)가 저장됨
    private MultipartFile profileImageFile;
    private String backgroundImage; // DB에는 이미지 경로(String)가 저장됨
    private MultipartFile backgroundImageFile;
    private int likeCount; // 관심 등록 수
    public boolean getIsOpen() {
        if (this.shopTime == null || this.shopTime.trim().isEmpty()) {
            return false;
        }

        try {
            String[] times = this.shopTime.split("[~-]");

            if (times.length < 2) return false;

            String startStr = times[0].trim();
            String endStr = times[1].trim();
            LocalTime now = LocalTime.now();
            LocalTime startTime = LocalTime.parse(startStr);
            LocalTime endTime = LocalTime.parse(endStr);

            if (endTime.isAfter(startTime)) {
                return !now.isBefore(startTime) && !now.isAfter(endTime);
            }
            else {
                return !now.isBefore(startTime) || !now.isAfter(endTime);
            }

        } catch (Exception e) {
            return false;
        }
    }
}

