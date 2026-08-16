package mx.gob.impepac.redicap.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TokenResponse {
    private String token;
    private String username;
    private String rol;
    private long expiresInMs;
}
