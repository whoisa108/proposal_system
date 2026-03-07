package com.esg.proposal.service;

import com.esg.proposal.dto.LoginRequest;
import com.esg.proposal.dto.RegisterRequest;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.UserRepository;
import com.esg.proposal.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterRequest req) {
        if (userRepository.existsByEmployeeId(req.getEmployeeId())) {
            throw new RuntimeException("工號已被註冊");
        }

        User user = new User();
        user.setEmployeeId(req.getEmployeeId());
        user.setName(req.getName());
        user.setDepartment(req.getDepartment());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("USER");
        userRepository.save(user);
    }

    public Map<String, Object> login(LoginRequest req) {
        User user = userRepository.findByEmployeeId(req.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("工號或密碼錯誤"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("工號或密碼錯誤");
        }

        String token = jwtUtil.generateToken(user.getEmployeeId(), user.getRole());

        return Map.of(
                "token", token,
                "employeeId", user.getEmployeeId(),
                "name", user.getName(),
                "department", user.getDepartment(),
                "role", user.getRole()
        );
    }
}
