package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LoginService {
    private final RegisterMapper registerMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Autowired
    public LoginService(RegisterMapper registerMapper) {
        this.registerMapper = registerMapper;
    }
    public RegisterEntity CheckLogin(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            return null;
        }
        RegisterEntity dbUser = registerMapper.selectByEmail(email);
        if(dbUser == null) {
            return null;
        }
        if( !BCrypt.checkpw(password, dbUser.getPassword())) {
            return null;
        }
        return dbUser;
    }
}

