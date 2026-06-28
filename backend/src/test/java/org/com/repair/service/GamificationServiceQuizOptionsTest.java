package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.com.repair.DTO.QuizQuestionResponse;
import org.com.repair.entity.GreenQuiz;
import org.com.repair.entity.User;
import org.com.repair.repository.GreenQuizRepository;
import org.com.repair.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class GamificationServiceQuizOptionsTest {

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private GreenQuizRepository greenQuizRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldNormalizeH2ImportedEscapedQuizOptions() {
        User user = createTestUser();
        GreenQuiz quiz = new GreenQuiz();
        quiz.setCityIndex(0);
        quiz.setEventTitle("H2 escaped options");
        quiz.setEventDescription("regression test");
        quiz.setEventTheme("rain");
        quiz.setQuestion("湿滑道路上使用能量回收时，更合理的做法是？");
        quiz.setOptions("{\\\"A\\\":\\\"突然切到最高回收\\\",\\\"B\\\":\\\"平顺减速\\\"}");
        quiz.setCorrectAnswer("B");
        quiz.setEnergyReward(20);
        quiz.setIsDefaultForCity(false);
        greenQuizRepository.save(quiz);

        QuizQuestionResponse response = gamificationService.getQuizQuestionForCity(user.getId(), 0);
        JsonNode options = assertDoesNotThrow(() -> objectMapper.readTree(response.options()));

        assertEquals("突然切到最高回收", options.get("A").asText());
        assertEquals("平顺减速", options.get("B").asText());
    }

    private User createTestUser() {
        String suffix = String.valueOf(System.nanoTime());
        User user = new User();
        user.setUsername("quiz_options_test_" + suffix);
        user.setPassword("pwd123456");
        user.setName("Quiz Options Tester");
        user.setPhone("138" + suffix.substring(Math.max(0, suffix.length() - 8)));
        user.setEmail("quiz_options_" + suffix + "@example.com");
        user.setAddress("test");
        return userRepository.save(user);
    }
}
