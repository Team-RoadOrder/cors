package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.register.RegisterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {
    private final RegisterMapper registerMapper;

    @Autowired
    public RegisterService(RegisterMapper registerMapper) {
        this.registerMapper = registerMapper;
    }
     public RegisterResult register(RegisterEntity register) {
         if (register == null || register.getName() == null || register.getName().isEmpty()
                 || register.getEmail() == null || register.getEmail().isEmpty()
                 || register.getPassword() == null || register.getPassword().isEmpty()
                 || register.getUsertype() == null || register.getUsertype().isEmpty()
                 || register.getPhone() == null || register.getPhone().isEmpty()
                 || register.getAddress() == null || register.getAddress().isEmpty()
                 || register.getAddressDetail() == null || register.getAddressDetail().isEmpty()) {
             return RegisterResult.FAILURE;
         }
            // 섣불리 수정했다가 터질 가능성이 높은것 customer와 owner를 구분해주는 아주 중요한 로직
         if (register.getUsertype().equals("customer")) {
             if (register.getGender() == null || register.getGender().isEmpty()) {
                 return RegisterResult.FAILURE;   // 무슨 실패인지 로직 확장해서 작성하기
             }
         } else if (register.getUsertype().equals("owner")) {
             if (register.getStoreName() == null || register.getStoreName().isEmpty()
                     || register.getBusinessNum() == null || register.getBusinessNum().isEmpty()) {
                 return RegisterResult.FAILURE;    // 마찬가지로 확장하기
             }
         } else {
             return RegisterResult.FAILURE;
         }
         return  this.registerMapper.insertRegister(register) < 1 ? RegisterResult.FAILURE : RegisterResult.SUCCESS;

     }
}
