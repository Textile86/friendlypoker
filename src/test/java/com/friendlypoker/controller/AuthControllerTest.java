package com.friendlypoker.controller;

import com.friendlypoker.dto.AuthResponse;
import com.friendlypoker.security.JwtService;
import com.friendlypoker.security.UserDetailsServiceImpl;
import com.friendlypoker.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration  = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
public class AuthControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("POST /register with valid body returns 200 and token")
    void register_valid_returns200() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("jwt.token.here", "Alice"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alice","email":"alice@test.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.username").value("Alice"));
    }

    @Test
    @DisplayName("POST /register with blank username returns 400")
    void register_blankUsername_returns400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"alice@test.com","password":"secret123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register with invalid email returns 400")
    void register_invalidEmail_returns400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"username":"Alice","email":"wrong-email","password":"secret123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register with short password returns 400")
    void register_shortPassword_returns400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                         {"username":"Alice","email":"alice@test.com","password":"abc"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login with valid body returns 200 and token")
    void login_valid_returns200() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("jwt.token.here", "Alice"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alice", "password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Alice"));
    }
}
