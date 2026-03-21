package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.GreenQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GreenQuizRepository extends JpaRepository<GreenQuiz, Long> {

    /**
     * 随机获取一道环保题目
     */
    @Query(value = "SELECT * FROM green_quiz ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<GreenQuiz> findRandomQuiz();
}
