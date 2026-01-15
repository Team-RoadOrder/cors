package dev.gmpark.cors.entities;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of="email")
public class OwnerMemberEntity {
    private String email;          // 이메일 (PK)
    private String name;           // 이름
    private String password;       // 비밀번호 (암호화 저장)
    private String phone;          // 연락처
    private String address;        // 기본 주소
    private String addressDetail;  // 상세 주소
    private String storeName;      // 상호명 (참고용/화면표시용)

    // 추가된 핵심 연결 고리
    private Long shopId;           // 소속 매장 고유 ID (FK 연동)

    private String usertype;       // 사용자 타입 (owner, admin, staff 등)
    private String style;          // 업무 스타일/태그
    private int level;             // 권한 레벨 (1:최고, 2:중간, 3:사원)

    private LocalDateTime createdAt;     // 계정 생성일 (입사일 대용)
    private LocalDateTime updatedAt;     // 정보 수정일
    private LocalDateTime lastLogOutAt;  // 마지막 로그아웃 시간

    // (선택사항) 현재 접속 여부나 뱃지 상태를 위해 추가 가능
    // private boolean isOnline;
}