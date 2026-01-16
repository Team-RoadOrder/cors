package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.RegisterEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RegisterMapper {
    int insertRegister(@Param(value = "register") RegisterEntity register);

    RegisterEntity selectByEmailAndPasswordUser(@Param(value = "email") String email, @Param(value = "password") String password);

    RegisterEntity selectByEmail(@Param(value = "email") String email);

    int update(@Param(value = "user") RegisterEntity user);

    int delete(@Param(value = "user") RegisterEntity user);

    int insertMember(RegisterEntity member);

    int updateMember(@Param("member") RegisterEntity member);

    List<RegisterEntity> selectMembersByOwner(@Param("ownerEmail") String ownerEmail,
                                              @Param("level") Integer level,
                                              @Param("keyword") String keyword);
    void updateLastLogout(@Param("email") String email);

}
