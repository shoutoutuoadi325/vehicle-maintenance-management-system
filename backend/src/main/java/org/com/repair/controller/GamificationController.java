package org.com.repair.controller;

import org.com.repair.DTO.QuizAnswerRequest;
import org.com.repair.DTO.QuizAnswerResultResponse;
import org.com.repair.DTO.JourneyCheckinRequest;
import org.com.repair.DTO.JourneyStateResponse;
import org.com.repair.DTO.QuizQuestionResponse;
import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.service.GamificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/gamification")
@Tag(name = "Gamification", description = "零碳公路之旅游戏化接口")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/account/{userId}")
    @Operation(summary = "获取或初始化用户绿色能量账户")
    public ResponseEntity<GreenEnergyAccount> getOrCreateUserAccount(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        GreenEnergyAccount account = gamificationService.getOrCreateUserAccount(userId);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @GetMapping("/quiz/random")
    @Operation(summary = "随机获取一道环保问答题")
    public ResponseEntity<QuizQuestionResponse> getRandomQuiz() {
        QuizQuestionResponse quiz = gamificationService.getRandomQuizQuestion();
        return new ResponseEntity<>(quiz, HttpStatus.OK);
    }

    @GetMapping("/journey/state/{userId}")
    @Operation(summary = "获取零碳公路旅程状态（服务端权威状态）")
    public ResponseEntity<JourneyStateResponse> getJourneyState(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        JourneyStateResponse state = gamificationService.getJourneyState(userId);
        return new ResponseEntity<>(state, HttpStatus.OK);
    }

    @PostMapping("/journey/checkin")
    @Operation(summary = "城市节点答题打卡并结算奖励")
    public ResponseEntity<QuizAnswerResultResponse> checkinAndAnswer(
            @Valid @RequestBody JourneyCheckinRequest request) {
        QuizAnswerResultResponse result = gamificationService.checkinAndAnswer(request);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/quiz/answer")
    @Operation(summary = "通用答题结算（兼容接口）")
    public ResponseEntity<QuizAnswerResultResponse> answerQuizAndReward(
            @Valid @RequestBody QuizAnswerRequest request) {
        QuizAnswerResultResponse result = gamificationService.checkinAndAnswer(new JourneyCheckinRequest(
                request.userId(),
                request.cityIndex(),
                request.quizId(),
                request.selectedAnswer()));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
