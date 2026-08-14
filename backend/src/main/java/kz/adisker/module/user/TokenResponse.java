package kz.adisker.module.user;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UUID userId;
    private String roleCode;
    private UUID organizationId;
    private String fullName;
}
