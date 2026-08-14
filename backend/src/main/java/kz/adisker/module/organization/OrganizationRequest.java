package kz.adisker.module.organization;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationRequest {
    @NotBlank
    private String name;
    private String legalName;
    private String bin;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
}
