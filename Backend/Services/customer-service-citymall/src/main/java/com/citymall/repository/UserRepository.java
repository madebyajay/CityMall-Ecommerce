package com.citymall.repository;

import com.citymall.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Admin customer listing — paginated, restricted to the CUSTOMER role. */
    Page<User> findByRole_Name(String roleName, Pageable pageable);

    /** Superadmin user search — loginId / email / mobile partial match, optional role filter. */
    @Query(
            value = """
                    select u from User u where
                      (:roleName is null or u.role.name = :roleName) and (
                        :query is null
                        or lower(u.userLoginId) like lower(concat('%', cast(:query as string), '%'))
                        or lower(u.emailId) like lower(concat('%', cast(:query as string), '%'))
                        or str(u.mobileNumber) like concat('%', cast(:query as string), '%')
                      )
                    """,
            countQuery = """
                    select count(u) from User u where
                      (:roleName is null or u.role.name = :roleName) and (
                        :query is null
                        or lower(u.userLoginId) like lower(concat('%', cast(:query as string), '%'))
                        or lower(u.emailId) like lower(concat('%', cast(:query as string), '%'))
                        or str(u.mobileNumber) like concat('%', cast(:query as string), '%')
                      )
                    """)
    Page<User> search(@Param("query") String query,
                      @Param("roleName") String roleName,
                      Pageable pageable);

    long countByRole_Name(String roleName);

    long countByRole_NameAndActiveStatus(String roleName, Boolean activeStatus);
}
