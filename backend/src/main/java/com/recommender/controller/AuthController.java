package com.recommender.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.recommender.config.JwtUtil;
import com.recommender.model.User;
import com.recommender.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

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
     * Accepts a Google OAuth id_token (JWT) from the frontend,
     * verifies it with Google's public key, extracts user info,
     * upserts the user in the database, and returns a JWT.
     */
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleAuth(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "ID token is required"));
        }

        try {
            // Verify the Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
            .setAudience(Collections.singletonList(googleClientId))
            .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "Invalid Google token. Token verification failed."));
            }

            GoogleIdToken.Payload payload = token.getPayload();

            // Extract user info from the verified JWT payload
            String googleId = payload.getSubject(); // 'sub' claim
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            // Upsert user in MySQL
            Optional<User> existingUser = userRepository.findByGoogleId(googleId);
            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setName(name);
                user.setPicture(picture);
                user.setEmail(email);
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
            userMap.put("id", user.getId());
            userMap.put("email", user.getEmail());
            userMap.put("name", user.getName());
            userMap.put("picture", user.getPicture());

            return ResponseEntity.ok(Map.of("token", jwt, "user", userMap));

        } catch (Exception e) {
            System.err.println("Google auth error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Token verification failed: " + e.getMessage()));
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
