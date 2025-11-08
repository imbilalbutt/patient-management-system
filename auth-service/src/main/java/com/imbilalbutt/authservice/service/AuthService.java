package com.imbilalbutt.authservice.service;

import com.imbilalbutt.authservice.dto.LoginRequestDTO;
import com.imbilalbutt.authservice.model.User;
import com.imbilalbutt.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil  jwtUtil;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }


    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO){
//        Optional<User> user = userService
//                .findByEmail(loginRequestDTO.getEmail())
////                .filter(u -> u.getPassword().equals(loginRequestDTO.getPassword()));
//                .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))
//                ;

        Optional<String> token = userService
                .findByEmail(loginRequestDTO.getEmail())
//                .filter(u -> u.getPassword().equals(loginRequestDTO.getPassword()));

                //  1. password in request -> password -> encoded -> @#%jabsdjkjsad (this value we compare in database)
//                we never decode the password
                .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))

                .map(u -> jwtUtil.generateToken(u.getEmail(), u.getRole()));

        return token;
    }

    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return true;
        } catch (JwtException e){
            return false;
        }
    }
}
