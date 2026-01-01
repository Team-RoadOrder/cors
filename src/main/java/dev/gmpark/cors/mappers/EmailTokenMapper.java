package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.EmailTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailTokenMapper {
    int insert(@Param(value = "emailToken") EmailTokenEntity emailTokenEntity);

    EmailTokenEntity select(@Param(value = "email") String email,
                            @Param(value = "code") String code,
                            @Param(value = "salt") String salt);

    int update(@Param(value = "emailToken") EmailTokenEntity emailTokenEntity);
}
