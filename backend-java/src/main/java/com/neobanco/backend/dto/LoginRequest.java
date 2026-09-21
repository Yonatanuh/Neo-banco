package com.neobanco.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String token2FA;
}
