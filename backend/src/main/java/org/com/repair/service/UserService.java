package org.com.repair.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.com.repair.DTO.NewUserRequest;
import org.com.repair.DTO.UserResponse;
import org.com.repair.DTO.UserWithStatsResponse;
import org.com.repair.entity.User;
import org.com.repair.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Transactional
    public UserResponse registerUser(NewUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setName(request.name());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setAddress(request.address());
        
        User savedUser = userRepository.save(user);
        return createSafeUserResponse(savedUser);
    }
    
    public Optional<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::createSafeUserResponse);
    }
    
    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::createSafeUserResponse);
    }
    
    @Transactional
    public UserResponse updateUser(Long id, NewUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setName(request.name());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setAddress(request.address());
        
        if (request.password() != null && !request.password().isEmpty()) {
            user.setPassword(request.password());
        }
        
        User updatedUser = userRepository.save(user);
        return createSafeUserResponse(updatedUser);
    }
    
    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::createSafeUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取包含统计信息的用户列表（用于管理员界面）
     */
    public List<UserWithStatsResponse> getUsersWithStats() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserWithStatsResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getAddress(),
                    user.getVehicles() != null ? user.getVehicles().size() : 0,
                    user.getRepairOrders() != null ? user.getRepairOrders().size() : 0
                ))
                .collect(Collectors.toList());
    }
    
    public Optional<UserResponse> login(String username, String password) {
        return userRepository.findByUsernameAndPassword(username, password)
                .map(this::createSafeUserResponse);
    }
    
    public List<UserResponse> searchUsers(String searchTerm) {
        return userRepository.findAll().stream()
                .filter(user -> 
                    user.getName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    user.getUsername().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    (user.getPhone() != null && user.getPhone().contains(searchTerm)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchTerm.toLowerCase()))
                )
                .map(this::createSafeUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建安全的UserResponse，避免循环引用
     */
    private UserResponse createSafeUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getPhone(),
            user.getEmail(),
            user.getAddress(),
            List.of(), // 暂时返回空列表，避免循环引用
            List.of()  // 暂时返回空列表，避免循环引用
        );
    }
} 