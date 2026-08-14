package kz.adisker.module.library;

import jakarta.persistence.*;
import kz.adisker.common.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "library_fund")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class LibraryFund extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "age_group")
    private String ageGroup;

    @Column(nullable = false, length = 2)
    private String language = "ru";

    @Column(name = "publication_type")
    private String publicationType;

    @Column(nullable = false, length = 512)
    private String title;

    private String authors;

    @Column(length = 255)
    private String publisher;

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(name = "inventory_number", length = 100)
    private String inventoryNumber;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "is_electronic", nullable = false)
    private boolean electronic = false;

    @Column(name = "order_number", length = 100)
    private String orderNumber;

    @Column(name = "list_item", length = 255)
    private String listItem;

    @Column(name = "check_date")
    private LocalDate checkDate;

    @Column(name = "doc_link", length = 512)
    private String docLink;

    @Column(nullable = false, length = 50)
    private String condition = "good";  // good / repair / write_off

    private String notes;
}
