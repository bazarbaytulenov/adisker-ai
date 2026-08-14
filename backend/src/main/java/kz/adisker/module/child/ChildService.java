package kz.adisker.module.child;

import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository repo;

    public PageResponse<ChildDto> list(UUID orgId, UUID branchId, UUID groupId, Pageable pageable) {
        Page<Child> page;
        if (branchId != null && groupId != null) {
            page = repo.findByOrganizationIdAndBranchIdAndGroupIdAndDeletedFalse(orgId, branchId, groupId, pageable);
        } else if (branchId != null) {
            page = repo.findByOrganizationIdAndBranchIdAndDeletedFalse(orgId, branchId, pageable);
        } else if (groupId != null) {
            page = repo.findByOrganizationIdAndGroupIdAndDeletedFalse(orgId, groupId, pageable);
        } else {
            page = repo.findByOrganizationIdAndDeletedFalse(orgId, pageable);
        }
        return PageResponse.from(page.map(this::toDto));
    }

    public ChildDto getById(UUID id, UUID orgId) {
        return toDto(repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Child", id)));
    }

    @Transactional
    public ChildDto create(UUID orgId, ChildRequest req) {
        Child child = Child.builder()
                .organizationId(orgId)
                .branchId(req.getBranchId())
                .groupId(req.getGroupId())
                .lastName(req.getLastName())
                .firstName(req.getFirstName())
                .middleName(req.getMiddleName())
                .birthDate(req.getBirthDate())
                .gender(req.getGender())
                .iin(req.getIin())
                .admissionDate(req.getAdmissionDate())
                .admissionOrderNum(req.getAdmissionOrderNum())
                .parentName(req.getParentName())
                .parentPhone(req.getParentPhone())
                .parentEmail(req.getParentEmail())
                .notes(req.getNotes())
                .status("active")
                .build();
        return toDto(repo.save(child));
    }

    @Transactional
    public ChildDto update(UUID id, UUID orgId, ChildRequest req) {
        Child child = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Child", id));
        child.setLastName(req.getLastName());
        child.setFirstName(req.getFirstName());
        child.setMiddleName(req.getMiddleName());
        child.setBirthDate(req.getBirthDate());
        child.setGender(req.getGender());
        child.setIin(req.getIin());
        child.setGroupId(req.getGroupId());
        child.setBranchId(req.getBranchId());
        child.setAdmissionDate(req.getAdmissionDate());
        child.setAdmissionOrderNum(req.getAdmissionOrderNum());
        child.setParentName(req.getParentName());
        child.setParentPhone(req.getParentPhone());
        child.setParentEmail(req.getParentEmail());
        child.setNotes(req.getNotes());
        return toDto(repo.save(child));
    }

    @Transactional
    public void discharge(UUID id, UUID orgId, java.time.LocalDate dischargeDate, String reason, String orderNum) {
        Child child = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Child", id));
        child.setStatus("discharged");
        child.setDischargeDate(dischargeDate);
        child.setDischargeReason(reason);
        child.setDischargeOrderNum(orderNum);
        child.setGroupId(null);
        repo.save(child);
    }

    private ChildDto toDto(Child c) {
        ChildDto dto = new ChildDto();
        dto.setId(c.getId());
        dto.setOrganizationId(c.getOrganizationId());
        dto.setBranchId(c.getBranchId());
        dto.setGroupId(c.getGroupId());
        dto.setLastName(c.getLastName());
        dto.setFirstName(c.getFirstName());
        dto.setMiddleName(c.getMiddleName());
        dto.setBirthDate(c.getBirthDate());
        dto.setGender(c.getGender());
        dto.setIin(c.getIin());
        dto.setPhotoUrl(c.getPhotoUrl());
        dto.setAdmissionDate(c.getAdmissionDate());
        dto.setAdmissionOrderNum(c.getAdmissionOrderNum());
        dto.setDischargeDate(c.getDischargeDate());
        dto.setDischargeReason(c.getDischargeReason());
        dto.setStatus(c.getStatus());
        dto.setNotes(c.getNotes());
        dto.setParentName(c.getParentName());
        dto.setParentPhone(c.getParentPhone());
        dto.setParentEmail(c.getParentEmail());
        return dto;
    }
}
