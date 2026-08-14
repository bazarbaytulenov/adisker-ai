package kz.adisker.module.library;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class LibraryFundDto {
    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private String ageGroup;
    private String language;
    private String publicationType;
    private String title;
    private String authors;
    private String publisher;
    private Integer publishYear;
    private String inventoryNumber;
    private Integer quantity;
    private LocalDate receiptDate;
    private boolean electronic;
    private String orderNumber;
    private String listItem;
    private LocalDate checkDate;
    private String docLink;
    private String condition;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
