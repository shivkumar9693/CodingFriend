package com.aicodingmentor.user.seed;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class UserDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Seed default Admin user if not present
        String adminEmail = "admin@aicodingmentor.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User(
                    adminEmail,
                    passwordEncoder.encode("Admin123!"),
                    "System Administrator",
                    UserRole.ADMIN
            );
            admin.setBio("Platform Lead & Java Systems Architect");
            admin.setGithubUsername("aicodingmentor-admin");
            userRepository.save(admin);
            log.info("Seeded default admin user: {}", adminEmail);
        }

        // Seed default Student / Learner user if not present
        String studentEmail = "student@aicodingmentor.com";
        if (!userRepository.existsByEmail(studentEmail)) {
            User student = new User(
                    studentEmail,
                    passwordEncoder.encode("Student123!"),
                    "Alex Learner",
                    UserRole.USER
            );
            student.setBio("Aspiring Java backend developer working on algorithms and data structures.");
            student.setGithubUsername("alex-learner");
            userRepository.save(student);
            log.info("Seeded default student user: {}", studentEmail);
        }
    }
}
