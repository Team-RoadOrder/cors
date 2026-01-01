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
        // 로그인 실패시 널값 반환하는 로직 작성 Failure를 반환하는 형태로 수정해도됨
        return registerMapper.selectByEmailAndPasswordUser(email, password);
    }
}

