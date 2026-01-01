package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FindPasswordService {
    private final RegisterMapper registerMapper;

    @Autowired
    public FindPasswordService(RegisterMapper registerMapper) {
        this.registerMapper = registerMapper;
    }
    public RegisterEntity FindPassword(String email) {
        if(email.isEmpty()) {
            return null;
        }
        return registerMapper.selectByEmail(email);
    }
}
