package com.payflow.auth.internal.repos;

import com.payflow.auth.internal.domain.RoleType;
import com.payflow.auth.internal.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface RoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByRoleName(RoleType roleType);
}
