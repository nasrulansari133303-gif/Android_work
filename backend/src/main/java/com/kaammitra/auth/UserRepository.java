package com.kaammitra.auth;

import com.kaammitra.common.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    Optional<UserEntity> findByMobile(String mobile);
    Optional<UserEntity> findByEmailIgnoreCaseOrMobile(String email, String mobile);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobile(String mobile);
    long countByRole(Role role);
}
