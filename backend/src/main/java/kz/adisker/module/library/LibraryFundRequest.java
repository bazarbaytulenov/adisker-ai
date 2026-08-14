package kz.adisker.module.library;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class LibraryFundRequest {

    @NotNull
    private UUID branchId;

    private String ageGroup;
    private String language = "ru";
    private String publicationType;

    @NotBlank
    private String title;

    private String authors;
    private String publisher;
    private Integer publishYear;
    private String inventoryNumber;
    private Integer quantity = 1;
    private LocalDate receiptDate;
    private boolean electronic = false;
    private String orderNumber;
    private String listItem;
    private LocalDate checkDate;
    private String docLink;
    private String condition = "good";
    private String notes;
}
