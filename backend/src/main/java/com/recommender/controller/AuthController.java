package com.recommender.controller;

import com.recommender.config.JwtUtil;
import com.recommender.model.User;
import com.recommender.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${google.client.id}")
    private String googleClientId;

    /**
     * POST /api/auth/google
     * Accepts a Google OAuth access_token from the frontend,
     * validates it by calling Google's userinfo endpoint,
     * upserts the user in the database, and returns a JWT.
     */
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleAuth(@RequestBody Map<String, String> body) {
        String accessToken = body.get("idToken"); // key kept as "idToken" for compatibility
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token is required"));
        }

        try {
            // Call Google userinfo endpoint to validate the access token
            RestTemplate restTemplate = new RestTemplate();
            String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Map> googleResponse = restTemplate.exchange(
                    userInfoUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<?, ?> googleUser = googleResponse.getBody();
            if (googleUser == null || googleUser.get("sub") == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid Google token"));
            }

            String googleId = (String) googleUser.get("sub");
            String email    = (String) googleUser.get("email");
            String name     = (String) googleUser.get("name");
            String picture  = (String) googleUser.get("picture");

            // Upsert user in MySQL
            Optional<User> existingUser = userRepository.findByGoogleId(googleId);
            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setName(name);
                user.setPicture(picture);
                userRepository.save(user);
            } else {
                user = new User();
                user.setGoogleId(googleId);
                user.setEmail(email);
                user.setName(name);
                user.setPicture(picture);
                user = userRepository.save(user);
            }

            String jwt = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName());

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id",      user.getId());
            userMap.put("email",   user.getEmail());
            userMap.put("name",    user.getName());
            userMap.put("picture", user.getPicture());

            return ResponseEntity.ok(Map.of("token", jwt, "user", userMap));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/auth/me
     * Returns current user info from the JWT Authorization header.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        try {
            String token = authHeader.substring(7);
            Long userId  = jwtUtil.getUserId(token);
            User user    = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id",      user.getId());
            userMap.put("email",   user.getEmail());
            userMap.put("name",    user.getName());
            userMap.put("picture", user.getPicture());
            return ResponseEntity.ok(userMap);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired token"));
        }
    }
}
