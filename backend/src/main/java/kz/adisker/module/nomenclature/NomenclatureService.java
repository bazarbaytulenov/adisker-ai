package kz.adisker.module.nomenclature;

import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Номенклатура дел (ТЗ 5.27): CRUD, поиск, фильтрация. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NomenclatureService {

    private final NomenclatureRepository repo;

    @Transactional
    public Nomenclature create(Request req, UserPrincipal principal) {
        return repo.save(Nomenclature.builder()
                .organizationId(principal.getOrganizationId())
                .indexCode(req.getIndexCode()).title(req.getTitle())
                .retentionPeriod(req.getRetentionPeriod()).notes(req.getNotes())
                .sortOrder(req.getSortOrder()).deleted(false).build());
    }

    @Transactional
    public Nomenclature update(UUID id, Request req, UserPrincipal principal) {
        Nomenclature n = find(id, principal);
        if (req.getIndexCode() != null) n.setIndexCode(req.getIndexCode());
        if (req.getTitle() != null) n.setTitle(req.getTitle());
        if (req.getRetentionPeriod() != null) n.setRetentionPeriod(req.getRetentionPeriod());
        if (req.getNotes() != null) n.setNotes(req.getNotes());
        n.setSortOrder(req.getSortOrder());
        return repo.save(n);
    }

    public List<Nomenclature> list(String search, UserPrincipal principal) {
        if (search != null && !search.isBlank()) {
            return repo.findByOrganizationIdAndDeletedFalseAndTitleContainingIgnoreCaseOrderBySortOrder(
                    principal.getOrganizationId(), search);
        }
        return repo.findByOrganizationIdAndDeletedFalseOrderBySortOrder(principal.getOrganizationId());
    }

    @Transactional
    public void delete(UUID id, UserPrincipal principal) {
        Nomenclature n = find(id, principal);
        n.setDeleted(true);
        repo.save(n);
    }

    private Nomenclature find(UUID id, UserPrincipal principal) {
        return repo.findByIdAndOrganizationIdAndDeletedFalse(id, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Nomenclature", id));
    }

    @Data
    public static class Request {
        private String indexCode;
        private String title;
        private String retentionPeriod;
        private String notes;
        private int sortOrder = 0;
    }
}
