package kz.adisker.module.user;

import kz.adisker.module.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Учёт неудачных попыток входа (ТЗ п.10). Инкремент выполняется в отдельной
 * транзакции (REQUIRES_NEW), чтобы сохраниться несмотря на откат основной
 * транзакции login при выбросе AuthenticationException.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final long LOCK_MINUTES = 15;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(String email) {
        userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            int attempts = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plusSeconds(LOCK_MINUTES * 60));
                auditService.recordAuth("LOGIN", user.getId(), user.getOrganizationId(),
                        user.getEmail(), "Аккаунт заблокирован на " + LOCK_MINUTES
                                + " мин (превышение попыток)");
            }
            userRepository.save(user);
        });
    }
}
