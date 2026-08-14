package kz.adisker.module.user;

import kz.adisker.common.RoleCode;
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

    @Transactional
    public UserDto create(UserRequest req, UserPrincipal principal) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email already in use: " + req.getEmail());
        }
        UUID orgId = principal.getRoleCode().equals(RoleCode.SYSTEM_ADMIN)
                ? req.getOrganizationId() : principal.getOrganizationId();

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
        if (req.getRoleCode() != null) user.setRoleCode(req.getRoleCode());
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
