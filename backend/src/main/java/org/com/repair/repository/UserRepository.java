package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据用户名和密码查找用户（用于登录）
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    Optional<User> findByUsernameAndPassword(String username, String password);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 根据电话号码查找用户
     * @param phone 电话号码
     * @return 用户信息
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * 根据电子邮件查找用户
     * @param email 电子邮件
     * @return 用户信息
     */
    Optional<User> findByEmail(String email);
} 