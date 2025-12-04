package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.PaymentMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMethodMapper {
    PaymentMethod findById(@Param("methodID") int methodId);
    List<PaymentMethod> getAll();
    PaymentMethod findByName(@Param("methodName") String methodName);
}

