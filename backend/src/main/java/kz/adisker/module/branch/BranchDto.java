package kz.adisker.module.branch;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class BranchDto {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String address;
    private String phone;
    private String headName;
    private Integer designCapacity;
    private boolean active;
    private Instant createdAt;
}
