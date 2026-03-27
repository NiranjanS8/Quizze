package com.quizze.quizze.user.repository;

import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);
}
