package kz.adisker.module.user;

import kz.adisker.common.RoleCode;
import kz.adisker.common.RolePermissions;
import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserDto> getByOrg(UUID orgId, Pageable pageable) {
        return PageResponse.from(
                userRepository.findByOrganizationIdAndDeletedFalse(orgId, pageable).map(this::toDto));
    }

    /** Список директоров (админов) организации — для супер-админа. */
    public java.util.List<UserDto> getOrgAdmins(UUID orgId) {
        return userRepository
                .findByOrganizationIdAndRoleCodeAndDeletedFalse(orgId, RoleCode.DIRECTOR)
                .stream().map(this::toDto).toList();
    }

    /**
     * Сброс пароля пользователю.
     * SYSTEM_ADMIN может сбросить любому; DIRECTOR — только сотруднику своей организации
     * (и не другому директору/себе через этот путь).
     */
    @Transactional
    public void resetPassword(UUID id, String newPassword, UserPrincipal principal) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("Пароль должен быть не короче 6 символов");
        }
        User user = findOrThrow(id);
        boolean isSystemAdmin = principal.getRoleCode().equals(RoleCode.SYSTEM_ADMIN);
        if (!isSystemAdmin) {
            // директор: только своя организация и только назначаемые им роли (не другой директор)
            if (!user.getOrganizationId().equals(principal.getOrganizationId())
                    || !RolePermissions.canAssign(principal.getRoleCode(), user.getRoleCode())) {
                throw new kz.adisker.common.exception.AccessDeniedException();
            }
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // разблокировать и активировать на случай, если был заблокирован
        user.setActive(true);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    public UserDto getById(UUID id, UserPrincipal principal) {
        User user = findOrThrow(id);
        // Non-admin users can only view users in their own org
        if (!principal.getRoleCode().equals(RoleCode.SYSTEM_ADMIN) &&
            !user.getOrganizationId().equals(principal.getOrganizationId())) {
            throw new kz.adisker.common.exception.AccessDeniedException();
        }
        return toDto(user);
    }

    public UserDto getMe(UserPrincipal principal) {
        return toDto(findOrThrow(principal.getId()));
    }

    /** Смена собственного пароля: требует текущий пароль. */
    @Transactional
    public void changeOwnPassword(UserPrincipal principal, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("Новый пароль должен быть не короче 6 символов");
        }
        User user = findOrThrow(principal.getId());
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("Текущий пароль указан неверно");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("Новый пароль должен отличаться от текущего");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** Роли, которые текущий пользователь вправе назначать (для UI-формы). */
    public java.util.List<String> assignableRoles(UserPrincipal principal) {
        return new java.util.ArrayList<>(RolePermissions.assignableRoles(principal.getRoleCode()));
    }

    @Transactional
    public UserDto create(UserRequest req, UserPrincipal principal) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email already in use: " + req.getEmail());
        }
        if (!RolePermissions.canAssign(principal.getRoleCode(), req.getRoleCode())) {
            throw new BusinessException(
                    "Недостаточно прав для назначения роли: " + req.getRoleCode());
        }
        UUID orgId = principal.getRoleCode().equals(RoleCode.SYSTEM_ADMIN)
                ? req.getOrganizationId() : principal.getOrganizationId();
        if (orgId == null) {
            throw new BusinessException("Не указана организация для пользователя");
        }

        User user = User.builder()
                .email(req.getEmail())
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .middleName(req.getMiddleName())
                .roleCode(req.getRoleCode())
                .photoUrl(req.getPhotoUrl())
                .preferredLanguage(req.getPreferredLanguage() != null ? req.getPreferredLanguage() : "ru")
                .active(true)
                .build();
        user.setOrganizationId(orgId);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(UUID id, UserRequest req, UserPrincipal principal) {
        User user = findOrThrow(id);
        if (!principal.getRoleCode().equals(RoleCode.SYSTEM_ADMIN) &&
            !user.getOrganizationId().equals(principal.getOrganizationId())) {
            throw new kz.adisker.common.exception.AccessDeniedException();
        }
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setMiddleName(req.getMiddleName());
        user.setPhone(req.getPhone());
        user.setPhotoUrl(req.getPhotoUrl());
        if (req.getPreferredLanguage() != null) user.setPreferredLanguage(req.getPreferredLanguage());
        if (req.getRoleCode() != null && !req.getRoleCode().equals(user.getRoleCode())) {
            if (!RolePermissions.canAssign(principal.getRoleCode(), req.getRoleCode())) {
                throw new BusinessException(
                        "Недостаточно прав для назначения роли: " + req.getRoleCode());
            }
            user.setRoleCode(req.getRoleCode());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = findOrThrow(id);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = findOrThrow(id);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setOrganizationId(user.getOrganizationId());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setMiddleName(user.getMiddleName());
        dto.setRoleCode(user.getRoleCode());
        dto.setPhotoUrl(user.getPhotoUrl());
        dto.setActive(user.isActive());
        dto.setPreferredLanguage(user.getPreferredLanguage());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
