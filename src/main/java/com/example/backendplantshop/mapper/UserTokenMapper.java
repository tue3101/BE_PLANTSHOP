package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.UserTokens;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserTokenMapper {
    void insertToken(UserTokens token);

    void revokeToken(@Param("tokenId") int tokenId);

    void revokeTokensByUser(@Param("userId") int userId);


    UserTokens findByToken(@Param("token") String token);

    UserTokens findById(@Param("tokenId") int tokenId); // 🔥 thêm mới
    UserTokens findTokensByUserId( @Param("userId") int userId);


    List<UserTokens> findValidTokensByUser(@Param("userId") int userId,
                                           @Param("now") LocalDateTime now);
    
                                           

}
