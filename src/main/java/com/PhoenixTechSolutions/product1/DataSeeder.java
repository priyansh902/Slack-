package com.PhoenixTechSolutions.product1;

import com.PhoenixTechSolutions.product1.model.Profile;
import com.PhoenixTechSolutions.product1.model.Projects;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.ProjectRepository;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepositiory userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already has data — skipping seed.");
            return;
        }
        log.info("Seeding demo data...");
        seedUsers();
        log.info("Demo data seeded successfully.");
    }

    private void seedUsers() {
        // ── Admin user ────────────────────────────────────────────────────
        User admin = createUser("Priyanshu Kumar", "priyanshu", "admin@devconnect.dev",
            "admin123", "ROLE_ADMIN");

        Profile adminProfile = Profile.builder()
            .bio("Full-stack developer & founder of HustlenStack. Passionate about building tools that help developers connect and grow together.")
            .skills("Spring Boot, Flutter, Java, Dart, MySQL, Docker, AWS")
            .githubUrl("https://github.com/priyanshu")
            .linkedinUrl("https://linkedin.com/in/priyanshu")
            .build();
        admin.setProfile(adminProfile);
        userRepository.save(admin);

        addProjects(admin, new String[][]{
            {"HustlenStack Platform", "A developer community app built with Flutter + Spring Boot. Features include profiles, projects showcase, search, and portfolio.", "Flutter, Spring Boot, MySQL, JWT", "https://github.com/priyanshu/devconnect", "https://devconnect.dev"},
            {"JWT Auth Library", "Lightweight JWT authentication library for Spring Boot applications with refresh token support.", "Java, Spring Security, JWT", "https://github.com/priyanshu/jwt-auth", null},
        });

        // ── Demo users ─────────────────────────────────────────────────────
        User sara = createUser("Sara Chen", "saradev", "sara@devconnect.dev", "demo123", "ROLE_USER");
        Profile saraProfile = Profile.builder()
            .bio("Frontend engineer obsessed with pixel-perfect UIs. I build design systems and love TypeScript + React. Currently exploring Flutter for mobile.")
            .skills("React, TypeScript, Flutter, Tailwind CSS, Figma, Next.js")
            .githubUrl("https://github.com/saradev")
            .linkedinUrl("https://linkedin.com/in/sara-chen")
            .build();
        sara.setProfile(saraProfile);
        userRepository.save(sara);
        addProjects(sara, new String[][]{
            {"React Design System", "A comprehensive component library built with React + TypeScript featuring 50+ accessible components and full dark mode support.", "React, TypeScript, Storybook, Tailwind CSS", "https://github.com/saradev/design-system", "https://ds.saradev.io"},
            {"Portfolio Builder", "Drag-and-drop portfolio website builder for developers. Generate a beautiful site from your GitHub profile.", "Next.js, TypeScript, Prisma", "https://github.com/saradev/portfolio-builder", "https://portfoliobuilder.dev"},
            {"Dark Mode Toggle", "A zero-dependency, 2KB dark mode library that respects system preferences and persists user choice.", "JavaScript, CSS", "https://github.com/saradev/dark-mode", null},
        });

        User arjun = createUser("Arjun Sharma", "arjunsh", "arjun@devconnect.dev", "demo123", "ROLE_USER");
        Profile arjunProfile = Profile.builder()
            .bio("Backend engineer & DevOps enthusiast. I architect scalable microservices and automate everything that moves. K8s certified.")
            .skills("Go, Kubernetes, Docker, PostgreSQL, gRPC, Terraform, AWS")
            .githubUrl("https://github.com/arjunsh")
            .linkedinUrl(null)
            .build();
        arjun.setProfile(arjunProfile);
        userRepository.save(arjun);
        addProjects(arjun, new String[][]{
            {"GoAPI Framework", "Minimal, blazing-fast REST framework for Go with automatic OpenAPI documentation generation.", "Go, gRPC, PostgreSQL", "https://github.com/arjunsh/goapi", null},
            {"K8s Cost Monitor", "Real-time Kubernetes cost monitoring and optimization tool. Saves 40% on cloud bills by identifying wasted resources.", "Go, Kubernetes, Prometheus, Grafana", "https://github.com/arjunsh/k8s-cost", "https://k8scost.io"},
        });

        User mia = createUser("Mia Johnson", "mia_codes", "mia@devconnect.dev", "demo123", "ROLE_USER");
        Profile miaProfile = Profile.builder()
            .bio("ML engineer turning data into products. I build recommendation systems and LLM-powered apps. Currently at a Series B startup.")
            .skills("Python, PyTorch, FastAPI, LangChain, Redis, PostgreSQL, Scikit-learn")
            .githubUrl("https://github.com/mia-codes")
            .linkedinUrl("https://linkedin.com/in/mia-johnson-ml")
            .build();
        mia.setProfile(miaProfile);
        userRepository.save(mia);
        addProjects(mia, new String[][]{
            {"CodeReviewer AI", "AI-powered code review tool that catches bugs, security vulnerabilities, and suggests improvements using GPT-4.", "Python, FastAPI, LangChain, React", "https://github.com/mia-codes/code-reviewer", "https://codereviewerai.dev"},
            {"RecSys Toolkit", "Open-source toolkit for building production-ready recommendation systems with collaborative filtering and content-based methods.", "Python, PyTorch, FastAPI, Redis", "https://github.com/mia-codes/recsys-toolkit", null},
            {"LLM Playground", "Interactive web interface for experimenting with different LLM prompts, comparing outputs, and saving prompt templates.", "Python, React, TypeScript, OpenAI", "https://github.com/mia-codes/llm-playground", "https://llmplay.dev"},
        });

        User dev = createUser("Dev Patel", "devpatel99", "dev@devconnect.dev", "demo123", "ROLE_USER");
        Profile devProfile = Profile.builder()
            .bio("Mobile developer specialising in Flutter. I've shipped 8 apps with a combined 500K+ downloads. Available for freelance.")
            .skills("Flutter, Dart, Firebase, Swift, Kotlin, GraphQL")
            .githubUrl("https://github.com/devpatel99")
            .linkedinUrl("https://linkedin.com/in/dev-patel-flutter")
            .build();
        dev.setProfile(devProfile);
        userRepository.save(dev);
        addProjects(dev, new String[][]{
            {"Flutter Auth Kit", "Complete authentication solution for Flutter — Google, Apple, Email/Password with beautiful pre-built screens.", "Flutter, Dart, Firebase", "https://github.com/devpatel99/flutter-auth-kit", "https://pub.dev/packages/flutter_auth_kit"},
            {"Habit Tracker", "Minimalist habit tracking app with streaks, insights, and widgets. 100K+ downloads on Play Store.", "Flutter, Dart, Hive, Firebase", "https://github.com/devpatel99/habit-tracker", "https://habittrack.app"},
        });

        log.info("Seeded {} users with profiles and {} projects.",
            userRepository.count(), projectRepository.count());
    }

    private User createUser(String name, String username, String email,
                            String password, String role) {
        return User.builder()
            .name(name)
            .username(username)
            .email(email.toLowerCase())
            .password(passwordEncoder.encode(password))
            .role(role)
            .createdAt(LocalDateTime.now().minusDays((long)(Math.random() * 90)))
            .build();
    }

    private void addProjects(User user, String[][] data) {
        for (String[] d : data) {
            Projects p = Projects.builder()
                .title(d[0])
                .description(d[1])
                .techStack(d[2])
                .githubLink(d[3])
                .liveLink(d.length > 4 ? d[4] : null)
                .build();
            p.setUser(user);
            projectRepository.save(p);
        }
    }
}
