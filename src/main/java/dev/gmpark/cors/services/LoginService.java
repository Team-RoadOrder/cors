package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LoginService {
    private final RegisterMapper registerMapper;

    @Autowired
    public LoginService(RegisterMapper registerMapper) {
        this.registerMapper = registerMapper;
    }
    public RegisterEntity CheckLogin(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            return null;
        }
        return registerMapper.selectUser(email, password);
    }
}

