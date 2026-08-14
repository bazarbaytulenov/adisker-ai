package kz.adisker.module.organization;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class OrganizationDto {
    private UUID id;
    private String name;
    private String legalName;
    private String bin;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private boolean active;
    private Instant createdAt;
}
