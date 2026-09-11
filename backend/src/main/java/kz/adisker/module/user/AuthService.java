package kz.adisker.module.user;

import kz.adisker.common.exception.BusinessException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.security.JwtService;
import kz.adisker.security.RefreshToken;
import kz.adisker.security.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public TokenResponse login(LoginRequest req, String ipAddress, String userAgent) {
        // Проверка блокировки до аутентификации
        User existing = userRepository.findByEmailAndDeletedFalse(req.getEmail()).orElse(null);
        if (existing != null && existing.getLockedUntil() != null
                && existing.getLockedUntil().isAfter(Instant.now())) {
            auditService.recordAuth("LOGIN", existing.getId(), existing.getOrganizationId(),
                    existing.getEmail(), "Отклонён вход: аккаунт временно заблокирован");
            throw new BusinessException("Аккаунт временно заблокирован из-за превышения числа попыток. "
                    + "Повторите позже.");
        }

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Неудачная попытка: инкремент счётчика в отдельной транзакции
            loginAttemptService.registerFailure(req.getEmail());
            throw ex;
        }

        User user = userRepository.findByEmailAndDeletedFalse(req.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.isActive()) {
            throw new BusinessException("Account is disabled");
        }

        // Reset failed login counter
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditService.recordAuth("LOGIN", user.getId(), user.getOrganizationId(),
                user.getEmail(), "Успешный вход");

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRoleCode(), user.getOrganizationId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Persist hashed refresh token
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiry()))
                .createdAt(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiry() / 1000)
                .userId(user.getId())
                .roleCode(user.getRoleCode())
                .organizationId(user.getOrganizationId())
                .fullName(user.getLastName() + " " + user.getFirstName())
                .build();
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }

        var stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(refreshToken))
                .orElseThrow(() -> new BusinessException("Refresh token not found or revoked"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token expired");
        }

        // Revoke used token (rotation)
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = userRepository.findByIdAndDeletedFalse(stored.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRoleCode(), user.getOrganizationId());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(newRefreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiry()))
                .createdAt(Instant.now())
                .build());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiry() / 1000)
                .userId(user.getId())
                .roleCode(user.getRoleCode())
                .organizationId(user.getOrganizationId())
                .fullName(user.getLastName() + " " + user.getFirstName())
                .build();
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        auditService.recordAuth("LOGOUT", userId, null, null, "Выход из системы");
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
