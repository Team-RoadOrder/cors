package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.EmailTokenEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.EmailTokenMapper;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.register.ResetPasswordResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


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
        // Mapper 메서드 이름이 select 입니다.
        EmailTokenEntity token = this.emailTokenMapper.select(email, code, salt);
        if (token == null || !token.isVerified()) {
            return ResetPasswordResult.FAILURE; // 인증되지 않은 요청
        }

        // 2. 사용자 확인
        RegisterEntity dbUser = this.registerMapper.selectByEmail(email);
        if (dbUser == null) {
            return ResetPasswordResult.FAILURE;
        }

        // 3. 비밀번호 암호화 및 변경
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);

        if (this.registerMapper.updatePassword(dbUser.getEmail(), hashedPassword) > 0) {
            // 4. 사용된 토큰 만료 처리 (선택 사항이지만 보안상 권장)
            token.setVerified(false); 
            this.emailTokenMapper.update(token);
            return ResetPasswordResult.SUCCESS;
        } else {
            return ResetPasswordResult.FAILURE;
        }
    }
}
