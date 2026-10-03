package com.citymall.repository;

import com.citymall.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUser_UserIdAndActiveStatusTrueOrderByIsDefaultDescCreatedAtAsc(Long userId);

    long countByUser_UserIdAndActiveStatusTrue(Long userId);
}
