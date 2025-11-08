package com.imbilalbutt.authservice.controller;

import com.imbilalbutt.authservice.dto.LoginRequestDTO;
import com.imbilalbutt.authservice.dto.LoginResponseDTO;
import com.imbilalbutt.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary= "Generate token on user login.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {

        Optional<String> tokenOptional = authService.authenticate(loginRequestDTO);

        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String  token = tokenOptional.get();
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @Operation(summary = "Validate Token")
    @GetMapping("/validate")
    //    Any request that get to validate end-point, Spring is going to get the Authorization header from RequestHeader
//    and pass that header to us as variable called authHeader.
    public ResponseEntity<Void> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        // One of the header is Authorization, and standard is to first keep Bearer string and then token.
        // Authorization : Bearer <header>
        // Authorization: Bearer <token>
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // removes Bearer and only consider Token from string and returns True or False
        return authService.validateToken(authHeader.substring(7))
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
