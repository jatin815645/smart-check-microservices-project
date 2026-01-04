package com.smartcheck.auth.service.impl;

import com.smartcheck.auth.dto.request.LoginRequest;
import com.smartcheck.auth.dto.request.RegisterRequest;
import com.smartcheck.auth.dto.response.AuthResponse;
import com.smartcheck.auth.entity.Role;
import com.smartcheck.auth.entity.User;
import com.smartcheck.auth.entity.enums.UserStatus;
import com.smartcheck.auth.repository.RoleRepository;
import com.smartcheck.auth.repository.UserRepository;
import com.smartcheck.auth.security.JwtTokenProvider;
import com.smartcheck.auth.service.AuthAuditService;
import com.smartcheck.auth.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;

    @Override
    public AuthResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        String token = jwtTokenProvider.generateToken(authentication);

        authAuditService.logAsync(
                request.getUsername(),
                "LOGIN"
        );

        return new AuthResponse(token, "Bearer ");
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())){
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);

        Set<Role> roles = request.getRoles().stream()
                .map(roleName ->
                        roleRepository.findByName(roleName)
                                .orElseThrow(() ->
                                        new RuntimeException("Role not found")))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        userRepository.save(user);

        authAuditService.logAsync(
                request.getUsername(),
                "REGISTER"
        );
    }

}
