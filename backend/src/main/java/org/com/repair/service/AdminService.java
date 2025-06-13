package org.com.repair.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.com.repair.DTO.AdminResponse;
import org.com.repair.DTO.NewAdminRequest;
import org.com.repair.entity.Admin;
import org.com.repair.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    
    private final AdminRepository adminRepository;
    
    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }
    
    @Transactional
    public AdminResponse registerAdmin(NewAdminRequest request) {
        if (adminRepository.existsByUsername(request.username())) {
            throw new RuntimeException("管理员用户名已存在");
        }
        
        Admin admin = new Admin();
        admin.setUsername(request.username());
        admin.setPassword(request.password());
        admin.setName(request.name());
        admin.setPhone(request.phone());
        admin.setEmail(request.email());
        admin.setRole(request.role());
        
        Admin savedAdmin = adminRepository.save(admin);
        return new AdminResponse(savedAdmin);
    }
    
    public Optional<AdminResponse> getAdminById(Long id) {
        return adminRepository.findById(id)
                .map(AdminResponse::new);
    }
    
    public Optional<AdminResponse> getAdminByUsername(String username) {
        return adminRepository.findByUsername(username)
                .map(AdminResponse::new);
    }
    
    @Transactional
    public AdminResponse updateAdmin(Long id, NewAdminRequest request) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        
        // 如果用户名更改了，检查新用户名是否已存在
        if (!admin.getUsername().equals(request.username()) &&
            adminRepository.existsByUsername(request.username())) {
            throw new RuntimeException("管理员用户名已存在");
        }
        
        admin.setUsername(request.username());
        if (request.password() != null && !request.password().isEmpty()) {
            admin.setPassword(request.password());
        }
        admin.setName(request.name());
        admin.setPhone(request.phone());
        admin.setEmail(request.email());
        admin.setRole(request.role());
        
        Admin updatedAdmin = adminRepository.save(admin);
        return new AdminResponse(updatedAdmin);
    }
    
    @Transactional
    public boolean deleteAdmin(Long id) {
        if (adminRepository.existsById(id)) {
            adminRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<AdminResponse> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(AdminResponse::new)
                .collect(Collectors.toList());
    }
    
    public Optional<AdminResponse> getAdminByRole(String role) {
        return adminRepository.findByRole(role)
                .map(AdminResponse::new);
    }
    
    public Optional<AdminResponse> login(String username, String password) {
        return adminRepository.findByUsernameAndPassword(username, password)
                .map(AdminResponse::new);
    }
} 