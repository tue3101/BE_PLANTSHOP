package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.entity.UserTokens;

import java.util.List;

public interface UserTokenService {
    void saveToken(UserTokens token);
    boolean revokeTokenById(int tokenId);
    UserTokens revokeTokensByUser(int userId);
    UserTokens findByToken(String token);
    UserTokens findById(int tokenId);
    UserTokens findTokenByUser(Integer userId);



    List<UserTokens> findValidTokensByUser(int userId);
}