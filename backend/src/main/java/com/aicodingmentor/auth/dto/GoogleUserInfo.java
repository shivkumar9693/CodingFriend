package com.aicodingmentor.auth.dto;

public record GoogleUserInfo(
        String sub,
        String email,
        boolean emailVerified,
        String name,
        String picture,
        String givenName,
        String familyName
) {
}
