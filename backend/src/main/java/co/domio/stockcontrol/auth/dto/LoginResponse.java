package co.domio.stockcontrol.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        String username,
        String fullName,
        String role) {
}
