package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    /**
     * 根据用户名查找管理员
     * @param username 用户名
     * @return 管理员信息
     */
    Optional<Admin> findByUsername(String username);
    
    /**
     * 根据用户名和密码查找管理员（用于登录）
     * @param username 用户名
     * @param password 密码
     * @return 管理员信息
     */
    Optional<Admin> findByUsernameAndPassword(String username, String password);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 根据角色查找管理员列表
     * @param role 角色
     * @return 管理员列表
     */
    Optional<Admin> findByRole(String role);
} 