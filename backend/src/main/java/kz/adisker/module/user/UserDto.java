package kz.adisker.module.user;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private UUID organizationId;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String middleName;
    private String roleCode;
    private String photoUrl;
    private boolean active;
    private String preferredLanguage;
    private Instant lastLoginAt;
    private Instant createdAt;

    public String getFullName() {
        return lastName + " " + firstName + (middleName != null ? " " + middleName : "");
    }
}
