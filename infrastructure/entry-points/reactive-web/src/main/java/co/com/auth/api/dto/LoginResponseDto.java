package co.com.auth.api.dto;

public record LoginResponseDto(String accessToken, long expiresAtEpochSeconds) {
}
