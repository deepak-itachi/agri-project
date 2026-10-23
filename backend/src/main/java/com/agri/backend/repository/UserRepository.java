package com.agri.backend.repository;

import com.agri.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndRole(String email, User.Role role);
    Optional<User> findByEmail(String email);
    boolean existsByEmailAndRole(String email, User.Role role);
    List<User> findByRoleOrderByCreatedAtDesc(User.Role role);
    List<User> findAllByOrderByCreatedAtDesc();
    long countByRole(User.Role role);
}
