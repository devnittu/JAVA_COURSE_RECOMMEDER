package com.recommender.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    /**
     * POST /api/user/register
     * Placeholder registration endpoint.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        if (username.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Username is required");
            return ResponseEntity.badRequest().body(error);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "User '" + username + "' registered successfully!");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/user/login
     * Placeholder login endpoint.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        if (username.isBlank() || password.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Username and password are required");
            return ResponseEntity.badRequest().body(error);
        }

        // Placeholder: always succeeds (add JWT auth as a bonus later)
        Map<String, String> response = new HashMap<>();
        response.put("message", "Login successful! Welcome, " + username);
        response.put("token", "sample-jwt-token-" + username);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}
