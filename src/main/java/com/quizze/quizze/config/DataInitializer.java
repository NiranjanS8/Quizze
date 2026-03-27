package com.quizze.quizze.config;

import com.quizze.quizze.user.domain.Role;
import com.quizze.quizze.user.domain.RoleType;
import com.quizze.quizze.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        createRoleIfMissing(RoleType.ADMIN, "Administrator with full system access");
        createRoleIfMissing(RoleType.USER, "Standard quiz platform user");
    }

    private void createRoleIfMissing(RoleType roleType, String description) {
        roleRepository.findByName(roleType).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleType);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }
}
