package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.EmailTokenEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.EmailTokenMapper;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.register.ResetPasswordResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class ResetPasswordService {
    private final RegisterMapper registerMapper;
    private final EmailTokenMapper emailTokenMapper;

    public ResetPasswordResult UpdatePassword(String email, String password, String code, String salt) {
        if(email == null || email.isEmpty() || password == null || password.length() < 6 || code == null || salt == null) {
            return ResetPasswordResult.FAILURE;
        }
        
        // 1. 이메일 인증 토큰 확인
        EmailTokenEntity token = this.emailTokenMapper.select(email, code, salt);
        if (token == null) {
            return ResetPasswordResult.FAILURE; // 토큰 없음
        }
        
        // 2. 토큰 만료 확인 (3분 또는 10분 등 설정된 시간)
        if (token.getExpiresAt() != null && LocalDateTime.now().isAfter(token.getExpiresAt())) {
            return ResetPasswordResult.FAILURE; // 만료됨
        }

        // 3. 인증 완료 여부 확인
        if (!token.isVerified()) {
            return ResetPasswordResult.FAILURE; // 인증되지 않음
        }

        // 4. 사용자 확인 (이메일 존재 여부)
        RegisterEntity dbUser = this.registerMapper.selectByEmail(email);
        if (dbUser == null) {
            return ResetPasswordResult.FAILURE; // 존재하지 않는 사용자
        }

        // 5. 비밀번호 암호화 및 변경
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);

        if (this.registerMapper.updatePassword(dbUser.getEmail(), hashedPassword) > 0) {
            // 6. 사용된 토큰 만료 처리 (선택 사항이지만 보안상 권장)
            token.setVerified(false); 
            this.emailTokenMapper.update(token);
            return ResetPasswordResult.SUCCESS;
        } else {
            return ResetPasswordResult.FAILURE;
        }
    }
}
