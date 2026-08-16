package mx.gob.impepac.redicap.service;

import mx.gob.impepac.redicap.dto.request.LoginRequest;
import mx.gob.impepac.redicap.dto.response.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request, String ipOrigen);

    void logout(String username, String ipOrigen, String token);
}
