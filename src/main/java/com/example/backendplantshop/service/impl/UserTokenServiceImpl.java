package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.entity.UserTokens;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.UserTokenMapper;
import com.example.backendplantshop.service.intf.UserTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserTokenServiceImpl implements UserTokenService {
    private final UserTokenMapper userTokenMapper;

    @Override
    public void saveToken(UserTokens token) {
        token.setCreated_at(LocalDateTime.now());
        userTokenMapper.insertToken(token);
    }

    @Override
    public boolean revokeTokenById(int tokenId) {
        UserTokens existing = userTokenMapper.findById(tokenId);
        if (existing == null) {
            throw new AppException(ErrorCode.TOKEN_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(existing.getRevoked())) {
            throw new AppException(ErrorCode.TOKEN_ALREADY_REVOKED);
        }
        userTokenMapper.revokeToken(tokenId);
        return false;
    }

    @Override
    public UserTokens revokeTokensByUser(int userId) {
        userTokenMapper.revokeTokensByUser(userId);
        return null;
    }

    @Override
    public UserTokens findByToken(String token) {
        var tokenEntity = userTokenMapper.findByToken(token);
        if (tokenEntity == null) {
            throw new AppException(ErrorCode.TOKEN_NOT_EXISTS);
        }
        return tokenEntity;
    }

    @Override
    public UserTokens findById(int tokenId) {
        var token = userTokenMapper.findById(tokenId);
        if (token == null) {
            throw new AppException(ErrorCode.TOKEN_NOT_EXISTS);
        }
        return token;
    }

    @Override
    public UserTokens findTokenByUser(Integer userId) {
        if (userId == null) {
            throw  new AppException(ErrorCode.USER_NOT_EXISTS);
        }
        return userTokenMapper.findTokensByUserId(userId);
    }


    @Override
    public List<UserTokens> findValidTokensByUser(int userId) {
        List<UserTokens> tokens = userTokenMapper.findValidTokensByUser(userId, LocalDateTime.now());
        if (tokens == null || tokens.isEmpty()) {
            throw new AppException(ErrorCode.TOKEN_HAS_EXPIRED);
        }
        return userTokenMapper.findValidTokensByUser(userId, LocalDateTime.now());
    }
}
