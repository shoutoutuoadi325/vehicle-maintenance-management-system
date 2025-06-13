package org.com.repair.DTO;

import org.com.repair.entity.Admin;

public record AdminResponse(
    Long id,
    String username,
    String name,
    String phone,
    String email,
    String role
) {
    public AdminResponse(Admin admin) {
        this(admin.getId(), admin.getUsername(), admin.getName(), 
             admin.getPhone(), admin.getEmail(), admin.getRole());
    }
} 