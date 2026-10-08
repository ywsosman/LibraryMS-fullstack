package com.libraryms.user.repository;

import com.libraryms.user.entity.User;
import com.libraryms.user.entity.RoleName;
import com.libraryms.user.entity.Role;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByMemberIdAndIdNot(Long memberId, Long id);

    Optional<User> findByMemberId(Long memberId);

    @Query("select count(u) > 0 from User u join u.roles r where r.name = :role")
    boolean existsByRole(@Param("role") RoleName role);
}
