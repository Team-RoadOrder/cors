package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.RegisterEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegisterMapper {
     int insertRegister(@Param(value = "register") RegisterEntity register);
    RegisterEntity selectByEmailAndPasswordUser(@Param(value = "email") String email , @Param(value = "password") String password);
    RegisterEntity selectByEmail(@Param(value = "email") String email);
    int update(@Param(value = "user") RegisterEntity user);
}
