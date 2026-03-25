package org.com.repair.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.repair.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByJtiAndRevokedFalse(String jti);

    List<AuthRefreshToken> findByUserIdAndUserRoleAndRevokedFalseOrderByUpdatedAtDesc(Long userId, String userRole);

    @Modifying
    @Query("update AuthRefreshToken t set t.revoked = true where t.userId = :userId and t.userRole = :role and t.revoked = false")
    int revokeAllByUser(@Param("userId") Long userId, @Param("role") String role);

    @Modifying
    @Query("update AuthRefreshToken t set t.revoked = true where t.jti = :jti and t.revoked = false")
    int revokeByJti(@Param("jti") String jti);

    @Modifying
    @Query("delete from AuthRefreshToken t where t.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") LocalDateTime threshold);
}
