package org.com.repair.controller;

import org.com.repair.exception.GamificationErrorCode;
import org.com.repair.exception.GamificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GamificationExceptionContractTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn400ForInvalidCityIndex() throws Exception {
        mockMvc.perform(get("/test/gamification/invalid-city").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CITY_INDEX"))
                .andExpect(jsonPath("$.message").value("非法城市节点索引"));
    }

    @Test
    void shouldReturn409ForConflictTypeGamificationErrors() throws Exception {
        mockMvc.perform(get("/test/gamification/not-unlocked").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NODE_NOT_UNLOCKED"))
                .andExpect(jsonPath("$.message").value("当前城市尚未解锁，禁止越级打卡"));
    }

    @RestController
    @RequestMapping("/test/gamification")
    static class ThrowingController {

        @GetMapping("/invalid-city")
        String invalidCity() {
            throw new GamificationException(
                    GamificationErrorCode.INVALID_CITY_INDEX,
                    "非法城市节点索引");
        }

        @GetMapping("/not-unlocked")
        String notUnlocked() {
            throw new GamificationException(
                    GamificationErrorCode.NODE_NOT_UNLOCKED,
                    "当前城市尚未解锁，禁止越级打卡");
        }
    }
}
