package com.libraryms.repository;

import com.libraryms.model.User;
import com.libraryms.enums.RoleName;
import com.libraryms.model.Role;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
