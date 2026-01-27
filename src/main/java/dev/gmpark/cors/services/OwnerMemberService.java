package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
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
    private final RegisterMapper registerMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public RegisterResult addMember(RegisterEntity member,String loginUserEmail, Integer loginUserLevel) {

        //최고관리자 확인
        if(loginUserLevel == null || loginUserLevel!=3){
            return RegisterResult.FAILURE;
        }

        //유효성 검사
        if (!OwnerMemberValidator.validateEmail(member.getEmail()) ||
                !OwnerMemberValidator.validateName(member.getName()) ||
                !OwnerMemberValidator.validatePhone(member.getPhone()) ||
                !OwnerMemberValidator.validatePassword(member.getPassword())) {

            return RegisterResult.FAILURE; // 형식이 맞지 않으면 FAILURE 반환
        }
        //중복체크
        if (this.registerMapper.selectByEmail(member.getEmail()) != null) {
            return RegisterResult.FAILURE_EMAIL_DUPLICATE;
        }
        // 소속: 세션의 사장님 이메일 주입
        member.setOwnerEmail(loginUserEmail);
        member.setPassword(encoder.encode(member.getPassword()));
        member.setUsertype("owner"); //서비스 정책에 따른 고정값

        return this.registerMapper.insertMember(member) > 0
                ? RegisterResult.SUCCESS
                : RegisterResult.FAILURE;
    }
    public List<RegisterEntity> getMembers(String ownerEmail, Integer level, String keyword) {
        // level이 0이거나 null이면 전체 조회로 처리하기 위해 null로 통일
        if (level != null && level == 0) {
            level = null;
        }
        return this.registerMapper.selectMembersByOwner(ownerEmail, level, keyword);
    }
@Transactional
public RegisterResult modifyMember(String email, String name, Integer level, String currentPassword, String loginUserEmail, Integer loginUserLevel) {
    // 1. 최고관리자 권한 확인
    if (loginUserLevel == null || loginUserLevel != 3) {
        return RegisterResult.FAILURE;
    }
    RegisterEntity dbMember = this.registerMapper.selectByEmail(email);
    // [수정 포인트] dbMember가 null이거나, DB의 ownerEmail이 null인 경우를 먼저 체크합니다.
    if (dbMember == null || dbMember.getOwnerEmail() == null) {
        return RegisterResult.FAILURE;
    }
    // [안전한 비교] 확실히 값이 있는 loginUserEmail을 앞에 두어 NPE를 방지합니다.
    if (!loginUserEmail.equals(dbMember.getOwnerEmail())) {
        return RegisterResult.FAILURE;
    }
    // [수정 1] 비밀번호 대조
    if (!encoder.matches(currentPassword, dbMember.getPassword())) {
        return RegisterResult.FAILURE;
    }
    if (!OwnerMemberValidator.validateName(name)) {
        return RegisterResult.FAILURE;
    }
    dbMember.setName(name);
    dbMember.setLevel(level);
    return this.registerMapper.updateMember(dbMember) > 0
            ? RegisterResult.SUCCESS
            : RegisterResult.FAILURE;
}
@Transactional
public RegisterResult removeMember(String targetEmail, String loginUserEmail, Integer loginUserLevel) {
    // 최고 관리자 권한 확인 및 본인 삭제 방지
    if (loginUserLevel == null || loginUserLevel != 3 || loginUserEmail.equals(targetEmail)) {
        return RegisterResult.FAILURE;
    }

    RegisterEntity targetMember = this.registerMapper.selectByEmail(targetEmail);

    // [핵심 수정] 삭제 대상이 없거나 ownerEmail이 null인 경우 500 에러 방지
    if (targetMember == null || targetMember.getOwnerEmail() == null) {
        return RegisterResult.FAILURE;
    }

    // 소속 대조
    if (!loginUserEmail.equals(targetMember.getOwnerEmail())) {
        return RegisterResult.FAILURE;
    }

    return this.registerMapper.delete(targetMember) > 0
            ? RegisterResult.SUCCESS
            : RegisterResult.FAILURE;
}

    @Transactional
    public void updateLogoutTime(String email) {
        this.registerMapper.updateLastLogout(email);
    }

}