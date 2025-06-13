package org.com.repair.DTO;

public record NewUserRequest(
    String username,
    String password,
    String name,
    String phone,
    String email,
    String address
) {
}