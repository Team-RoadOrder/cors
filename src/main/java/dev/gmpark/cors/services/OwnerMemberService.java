package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.OwnerMemberEntity;
import dev.gmpark.cors.mappers.OwnerMemberMapper;
import dev.gmpark.cors.results.register.RegisterResult;
import dev.gmpark.cors.validators.OwnerMemberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerMemberService {
    private final OwnerMemberMapper memberMapper;

    // [해결 포인트] Bean 주입 대신 직접 객체 생성하여 서버 구동 에러 해결
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * [신규 추가/보완] 사장님 본인의 shopId 및 storeName 업데이트
     * 새 사업자가 매장을 등록한 직후, 사장님 계정에 shopId와 storeName을 동시에 부여합니다.
     * XML 매퍼의 SET shop_id = #{shopId}, store_name = #{storeName} 로직과 연동됩니다.
     */
    @Transactional
    public boolean updateOwnerShopId(String email, Long shopId, String storeName) {
        // 1. 이메일로 현재 사장님 정보 조회
        OwnerMemberEntity owner = memberMapper.selectMemberByEmail(email);
        if (owner == null) {
            return false;
        }

        // 2. 생성된 shopId와 storeName 세팅
        owner.setShopId(shopId);
        if (storeName != null) {
            owner.setStoreName(storeName);
        }

        // 3. DB 업데이트 (기존 updateMemberByAdmin 매퍼 활용)
        // XML에 추가된 shop_id와 store_name 컬럼이 이 시점에 users 테이블에 반영됩니다.
        return memberMapper.updateMemberByAdmin(owner) > 0;
    }


    /**
     * [조회] 임직원 목록 조회
     * 이제 텍스트인 storeName이 아닌, 고유 번호인 shopId를 기준으로 조회하여 보안을 강화합니다.
     */
    @Transactional(readOnly = true)
    public List<OwnerMemberEntity> getMembers(OwnerMemberEntity loginUser, int filterLevel, String keyword) {
        return memberMapper.selectMembersByShop(
                loginUser.getShopId(),
                loginUser.getLevel(),
                filterLevel,
                keyword
        );
    }

    /**
     * [추가] 신규 임직원 등록
     * 사장님의 shopId를 사원에게 그대로 할당하여 소속을 명확히 합니다.
     */
    @Transactional
    public RegisterResult addMember(OwnerMemberEntity loginUser, OwnerMemberEntity newMember) {
        // 1. 권한 체크: 사원(Level 3)은 등록 권한 없음
        if (loginUser.getLevel() == 3) {
            return RegisterResult.FAILURE;
        }

        // 2. 사장님의 정보를 신규 사원에게 강제 할당 (핵심: shopId 및 storeName 복사)
        OwnerMemberEntity adminInfo = memberMapper.selectMemberByEmail(loginUser.getEmail());
        if (adminInfo != null) {
            newMember.setShopId(adminInfo.getShopId());
            newMember.setStoreName(adminInfo.getStoreName());
            newMember.setAddress(adminInfo.getAddress());
            newMember.setAddressDetail(adminInfo.getAddressDetail());
        } else {
            return RegisterResult.FAILURE;
        }

        // 3. 전화번호 특수문자 제거
        if (newMember.getPhone() != null) {
            newMember.setPhone(newMember.getPhone().replaceAll("[^0-9]", ""));
        }

        // 4. 유효성 검사
        if (!OwnerMemberValidator.validate(newMember)) {
            return RegisterResult.FAILURE;
        }

        // 이메일 중복 체크
        if (memberMapper.countByEmail(newMember.getEmail()) > 0) {
            return RegisterResult.FAILURE_EMAIL_DUPLICATE;
        }

        // 5. 하극상 방지: 본인보다 높거나 같은 레벨 등록 차단
        if (loginUser.getLevel() > 1) {
            if (newMember.getLevel() <= loginUser.getLevel()) {
                return RegisterResult.FAILURE;
            }
        }

        // 6. 모든 임직원의 타입을 'owner'로 고정하여 로그인 호환성 보장
        newMember.setUsertype("owner");

        // 7. 비밀번호 암호화
        String rawPassword = (newMember.getPassword() == null || newMember.getPassword().isEmpty())
                ? "cors1234!" : newMember.getPassword();
        newMember.setPassword(passwordEncoder.encode(rawPassword));

        return memberMapper.insertMember(newMember) > 0 ? RegisterResult.SUCCESS : RegisterResult.FAILURE;
    }

    /**
     * [수정/위임] 임직원 정보 수정 및 권한 위임
     */
    @Transactional
    public RegisterResult modifyMember(OwnerMemberEntity loginUser, OwnerMemberEntity targetMember, String currentPassword) {
        if (loginUser.getEmail().equals(targetMember.getEmail())) {
            return RegisterResult.FAILURE;
        }

        OwnerMemberEntity existing = memberMapper.selectMemberByEmail(targetMember.getEmail());
        if (existing == null || !existing.getShopId().equals(loginUser.getShopId())) {
            return RegisterResult.FAILURE;
        }

        OwnerMemberEntity currentOwner = memberMapper.selectMemberByEmail(loginUser.getEmail());
        if (currentOwner == null || currentPassword == null || !passwordEncoder.matches(currentPassword, currentOwner.getPassword())) {
            return RegisterResult.FAILURE;
        }

        if (currentOwner.getLevel() > 1) {
            if (existing.getLevel() == 1) return RegisterResult.FAILURE;
            if (currentOwner.getLevel() >= existing.getLevel()) {
                return RegisterResult.FAILURE;
            }
            if (targetMember.getLevel() == 1) return RegisterResult.FAILURE;
        }

        targetMember.setUsertype("owner");
        targetMember.setShopId(existing.getShopId());
        targetMember.setStoreName(existing.getStoreName());

        if (targetMember.getPhone() == null || targetMember.getPhone().trim().isEmpty()) {
            targetMember.setPhone(existing.getPhone());
        } else {
            targetMember.setPhone(targetMember.getPhone().replaceAll("[^0-9]", ""));
        }

        if (targetMember.getPassword() != null && !targetMember.getPassword().isEmpty()) {
            if (!OwnerMemberValidator.validatePassword(targetMember.getPassword())) return RegisterResult.FAILURE;
            targetMember.setPassword(passwordEncoder.encode(targetMember.getPassword()));
        } else {
            targetMember.setPassword(existing.getPassword());
        }

        return memberMapper.updateMemberByAdmin(targetMember) > 0 ? RegisterResult.SUCCESS : RegisterResult.FAILURE;
    }

    /**
     * [삭제] 임직원 삭제
     */
    @Transactional
    public RegisterResult removeMember(OwnerMemberEntity loginUser, String targetEmail) {
        if (loginUser.getEmail().equals(targetEmail)) {
            return RegisterResult.FAILURE;
        }

        if (loginUser.getLevel() == 3) {
            return RegisterResult.FAILURE;
        }

        OwnerMemberEntity target = memberMapper.selectMemberByEmail(targetEmail);
        if (target == null || !target.getShopId().equals(loginUser.getShopId())) {
            return RegisterResult.FAILURE;
        }

        if (loginUser.getLevel() > 1 && loginUser.getLevel() <= target.getLevel()) {
            return RegisterResult.FAILURE;
        }

        if (target.getLevel() == 1 && memberMapper.countAdminByShop(loginUser.getShopId(), 1) <= 1) {
            return RegisterResult.FAILURE;
        }

        return memberMapper.deleteMemberByEmail(targetEmail) > 0 ? RegisterResult.SUCCESS : RegisterResult.FAILURE;
    }

    /**
     * 로그아웃 시각 갱신
     */
    @Transactional
    public void updateLogoutTime(String email) {
        this.memberMapper.updateLastLogOutAt(email);
    }
}