package kz.adisker.module.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class UserRequest {
    @NotBlank @Email
    private String email;
    private String phone;
    @Size(min = 6)
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String middleName;
    @NotBlank
    private String roleCode;
    private UUID organizationId;
    private String photoUrl;
    private String preferredLanguage;
}
