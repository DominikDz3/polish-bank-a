package com.polishbank.bank_a.domain.auth;

import com.polishbank.bank_a.domain.auth.dto.AuthResponse;
import com.polishbank.bank_a.domain.auth.dto.LoginRequest;
import com.polishbank.bank_a.domain.auth.dto.RegisterRequest;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.domain.user.UserRole;
import com.polishbank.bank_a.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    @Test
    void register_success_returnsTokenAndRole() {
        RegisterRequest request = new RegisterRequest("Jan", "Kowalski", "jan@test.pl", "password123");

        when(userRepository.existsByEmail("jan@test.pl")).thenReturn(false);
        when(userRepository.existsByCustomerNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(eq("jan@test.pl"), eq("CUSTOMER"), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("jan@test.pl");
        assertThat(response.role()).isEqualTo("CUSTOMER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsIllegalArgument() {
        RegisterRequest request = new RegisterRequest("Jan", "Kowalski", "jan@test.pl", "password123");
        when(userRepository.existsByEmail("jan@test.pl")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email już jest w użyciu");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = new LoginRequest("12345678", "password123");
        User user = User.builder()
                .customerNumber("12345678")
                .email("jan@test.pl")
                .passwordHash("$2a$hashed")
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.findByCustomerNumber("12345678")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(eq("jan@test.pl"), eq("CUSTOMER"), eq("12345678"))).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any());
    }
}