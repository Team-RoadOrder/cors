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
    public RegisterResult addMember(RegisterEntity member) {

        if (!OwnerMemberValidator.validateEmail(member.getEmail()) ||
                !OwnerMemberValidator.validateName(member.getName()) ||
                !OwnerMemberValidator.validatePhone(member.getPhone()) ||
                !OwnerMemberValidator.validatePassword(member.getPassword())) {

            return RegisterResult.FAILURE; // 형식이 맞지 않으면 FAILURE 반환
        }
        if (this.registerMapper.selectByEmail(member.getEmail()) != null) {
            return RegisterResult.FAILURE_EMAIL_DUPLICATE;
        }
        member.setPassword(encoder.encode(member.getPassword()));

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
    public RegisterResult modifyMember(String email, String name, Integer level, String currentPassword) {
        RegisterEntity dbMember = this.registerMapper.selectByEmail(email);

        // [수정 1] matches 사용 (순서: 평문, 암호화된문자열)
        if (!encoder.matches(currentPassword, dbMember.getPassword())) {
            return RegisterResult.FAILURE; // 비밀번호 불일치
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
    public void updateLogoutTime(String email) {

        this.registerMapper.updateLastLogout(email);
    }

}