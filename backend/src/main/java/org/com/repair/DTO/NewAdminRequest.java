package org.com.repair.DTO;

public record NewAdminRequest(
    String username,
    String password,
    String name,
    String phone,
    String email,
    String role
) {
} 