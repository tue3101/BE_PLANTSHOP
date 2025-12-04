package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.EmailOtp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailOtpMapper {
    void insertOtp(EmailOtp emailOtp);
    EmailOtp findByEmailAndOtp(@Param("email") String email, @Param("otpCode") String otpCode);
    void markAsUsed(@Param("otpId") int otpId);
//    void deleteExpiredOtps();
//    EmailOtp findLatestByEmail(@Param("email") String email);
    void updateUserIdByEmailAndOtp(@Param("email") String email, @Param("otpCode") String otpCode, @Param("userId") Integer userId);
//    EmailOtp findByEmailAndOtpForMark(@Param("email") String email, @Param("otpCode") String otpCode);
}

