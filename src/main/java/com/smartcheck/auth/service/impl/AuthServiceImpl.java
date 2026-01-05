package com.smartcheck.auth.service.impl;

import com.smartcheck.auth.dto.request.LoginRequest;
import com.smartcheck.auth.dto.request.RegisterRequest;
import com.smartcheck.auth.dto.response.AuthResponse;
import com.smartcheck.auth.entity.Role;
import com.smartcheck.auth.entity.User;
import com.smartcheck.auth.entity.enums.UserStatus;
import com.smartcheck.auth.exception.InvalidCredentialsException;
import com.smartcheck.auth.exception.UserAlreadyExistsException;
import com.smartcheck.auth.repository.RoleRepository;
import com.smartcheck.auth.repository.UserRepository;
import com.smartcheck.auth.security.JwtTokenProvider;
import com.smartcheck.auth.service.AuthAuditService;
import com.smartcheck.auth.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username"));

        if (!encoder.matches(request.getPassword(), user.getPassword())){
            throw new InvalidCredentialsException("Invalid password");
        }


        authAuditService.logAsync(
                request.getUsername(),
                "LOGIN"
        );

        return new AuthResponse(jwtTokenProvider.generateToken(user));
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        log.info("Registration started for username={}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())){
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + request.getEmail()
            );
        }
        log.debug("User entity created for username={}", request.getUsername());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);

        // ✅ DEFAULT ROLE HANDLING
        Set<String> roleNames =
                request.getRoles() == null || request.getRoles().isEmpty()
                        ? Set.of("ADMIN") // or USER
                        : request.getRoles();

        Set<Role> roles = roleNames.stream()
                .map(roleName ->
                        roleRepository.findByName(roleName)
                                .orElseGet(() -> {
                                    log.warn("Role {} not found. Creating.", roleName);
                                    Role role = new Role();
                                    role.setName(roleName);
                                    return roleRepository.save(role);
                                })
                )
                .collect(Collectors.toSet());

        user.setRoles(roles);

        userRepository.save(user);

        log.info("User registered successfully with roles={} username={}",
                roles.stream().map(Role::getName).toList(),
                request.getUsername()
        );

        authAuditService.logAsync(
                request.getUsername(),
                "REGISTER"
        );
    }

}
