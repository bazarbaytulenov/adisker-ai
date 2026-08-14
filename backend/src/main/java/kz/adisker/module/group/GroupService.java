package kz.adisker.module.group;

import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.child.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository repo;
    private final ChildRepository childRepo;

    public PageResponse<GroupDto> list(UUID orgId, UUID branchId, Pageable pageable) {
        return PageResponse.from(
                repo.findByOrganizationIdAndBranchIdAndDeletedFalse(orgId, branchId, pageable)
                        .map(this::toDto));
    }

    public List<GroupDto> listActiveByBranch(UUID branchId) {
        return repo.findByBranchIdAndDeletedFalseAndActiveTrue(branchId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public GroupDto getById(UUID id, UUID orgId) {
        return toDto(repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", id)));
    }

    @Transactional
    public GroupDto create(UUID orgId, GroupRequest req) {
        Group group = Group.builder()
                .organizationId(orgId)
                .branchId(req.getBranchId())
                .name(req.getName())
                .language(req.getLanguage() != null ? req.getLanguage() : "ru")
                .groupType(req.getGroupType())
                .ageFromMonths(req.getAgeFromMonths())
                .ageToMonths(req.getAgeToMonths())
                .academicYear(req.getAcademicYear())
                .educatorId(req.getEducatorId())
                .educatorPhone(req.getEducatorPhone())
                .educatorEmail(req.getEducatorEmail())
                .educatorInfo(req.getEducatorInfo())
                .active(true)
                .build();
        return toDto(repo.save(group));
    }

    @Transactional
    public GroupDto update(UUID id, UUID orgId, GroupRequest req) {
        Group group = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", id));
        group.setName(req.getName());
        group.setLanguage(req.getLanguage());
        group.setGroupType(req.getGroupType());
        group.setAgeFromMonths(req.getAgeFromMonths());
        group.setAgeToMonths(req.getAgeToMonths());
        group.setAcademicYear(req.getAcademicYear());
        group.setEducatorId(req.getEducatorId());
        group.setEducatorPhone(req.getEducatorPhone());
        group.setEducatorEmail(req.getEducatorEmail());
        group.setEducatorInfo(req.getEducatorInfo());
        return toDto(repo.save(group));
    }

    @Transactional
    public void delete(UUID id, UUID orgId) {
        Group group = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", id));
        long activeChildren = childRepo.countByGroupIdAndStatusAndDeletedFalse(id, "active");
        if (activeChildren > 0) {
            throw new BusinessException("Нельзя удалить группу: в ней " + activeChildren + " активных детей");
        }
        group.setDeleted(true);
        group.setDeletedAt(Instant.now());
        repo.save(group);
    }

    private GroupDto toDto(Group g) {
        GroupDto dto = new GroupDto();
        dto.setId(g.getId());
        dto.setOrganizationId(g.getOrganizationId());
        dto.setBranchId(g.getBranchId());
        dto.setName(g.getName());
        dto.setAgeFromMonths(g.getAgeFromMonths());
        dto.setAgeToMonths(g.getAgeToMonths());
        dto.setLanguage(g.getLanguage());
        dto.setGroupType(g.getGroupType());
        dto.setEducatorId(g.getEducatorId());
        dto.setEducatorPhone(g.getEducatorPhone());
        dto.setEducatorEmail(g.getEducatorEmail());
        dto.setEducatorInfo(g.getEducatorInfo());
        dto.setEducatorPhotoUrl(g.getEducatorPhotoUrl());
        dto.setAcademicYear(g.getAcademicYear());
        dto.setActive(g.isActive());
        dto.setActiveChildrenCount(childRepo.countByGroupIdAndStatusAndDeletedFalse(g.getId(), "active"));
        return dto;
    }
}
