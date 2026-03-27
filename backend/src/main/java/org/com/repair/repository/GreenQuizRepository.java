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

    @Query(value = "SELECT * FROM green_quiz WHERE city_index = :cityIndex AND is_default_for_city = 0 ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<GreenQuiz> findRandomScenarioQuizByCityIndex(Integer cityIndex);

    @Query(value = "SELECT * FROM green_quiz WHERE city_index = :cityIndex AND is_default_for_city = 1 ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Optional<GreenQuiz> findDefaultQuizByCityIndex(Integer cityIndex);

    @Query(value = "SELECT * FROM green_quiz WHERE city_index IS NULL ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<GreenQuiz> findRandomRoadEventQuiz();
}
