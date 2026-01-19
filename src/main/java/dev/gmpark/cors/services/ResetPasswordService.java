package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ResetPasswordService {
    private final RegisterMapper registerMapper;

    public RegisterEntity UpdatePassword(String email, String password) {
        if(email.isEmpty()) {
            return null;
        }
        if(password.length() < 6) {
            return null;
        }
        RegisterEntity dbUser = this.registerMapper.selectByEmail(email);
        if (dbUser == null) {
            return null;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);

        return this.registerMapper.updatePassword(dbUser.getEmail(), hashedPassword) > 0 ? dbUser : null;
    }
}
