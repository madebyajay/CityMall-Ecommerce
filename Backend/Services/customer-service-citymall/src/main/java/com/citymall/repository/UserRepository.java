package com.citymall.repository;

import com.citymall.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserLoginId(String userLoginId);

    Optional<User> findByEmailId(String emailId);

    Optional<User> findByMobileNumber(Long mobileNumber);

    /** Login identifier can be loginId OR email OR mobile (as string). */
    @Query("""
            select u from User u left join fetch u.role
            where u.userLoginId = :identifier
               or u.emailId = :identifier
               or str(u.mobileNumber) = :identifier
            """)
    Optional<User> findByLoginIdentifier(@Param("identifier") String identifier);

    boolean existsByUserLoginId(String userLoginId);

    boolean existsByEmailId(String emailId);

    boolean existsByMobileNumber(Long mobileNumber);
}
