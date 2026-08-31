package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.AccountStatus;
import com.pvp.travelmatch.entity.Role;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByUsernameIgnoreCase(String username);

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    long countByRole(Role role);
    long countByAccountStatus(AccountStatus accountStatus);
}
