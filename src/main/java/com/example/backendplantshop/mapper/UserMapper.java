package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    Users findByEmail(String email);
    Users findByPhoneNumber(String phoneNumber);
    Users findByUsername(String username);
    Users findById(@Param("userID")int id);
    
    // Tìm user không quan tâm đến is_deleted (để kiểm tra tồn tại)
    Users findByEmailIgnoreDeleted(String email);
     List<Users> findAll();
    void insert(Users user);
    void update(Users user);
    void delete(@Param("userID")int id);

    List<Users> findAllUserDeleted();
    Users findByIdDeleted(@Param("userID")int id);
    void restoreUser(@Param("userID")int id);
    int changePassword(@Param("userID") int id,
                       @Param("newPassword") String newPassword);
}
