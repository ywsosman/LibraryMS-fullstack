package com.libraryms.user.repository;

import com.libraryms.user.entity.User;
import com.libraryms.user.entity.RoleName;
import com.libraryms.user.entity.Role;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
