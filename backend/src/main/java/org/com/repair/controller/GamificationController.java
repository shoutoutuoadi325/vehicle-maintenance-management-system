package org.com.repair.controller;

import org.com.repair.DTO.QuizAnswerRequest;
import org.com.repair.DTO.QuizAnswerResultResponse;
import org.com.repair.DTO.ClaimGrandPrizeRequest;
import org.com.repair.DTO.ClaimGrandPrizeResponse;
import org.com.repair.DTO.JourneyCheckinRequest;
import org.com.repair.DTO.JourneyConfigResponse;
import org.com.repair.DTO.JourneyStateResponse;
import org.com.repair.DTO.QuizQuestionResponse;
import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.GamificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/gamification")
@Tag(name = "Gamification", description = "零碳公路之旅游戏化接口")
public class GamificationController {

    private final GamificationService gamificationService;
    private final RequestUserContextResolver requestUserContextResolver;

    public GamificationController(GamificationService gamificationService,
                                  RequestUserContextResolver requestUserContextResolver) {
        this.gamificationService = gamificationService;
        this.requestUserContextResolver = requestUserContextResolver;
    }

    @GetMapping("/account/{userId}")
    @Operation(summary = "获取或初始化用户绿色能量账户")
    public ResponseEntity<GreenEnergyAccount> getOrCreateUserAccount(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        requestUserContextResolver.ensurePathUserMatch(servletRequest, userId);
        GreenEnergyAccount account = gamificationService.getOrCreateUserAccount(userId);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @GetMapping("/account/me")
    @Operation(summary = "获取当前登录用户绿色能量账户")
    public ResponseEntity<GreenEnergyAccount> getCurrentUserAccount(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        GreenEnergyAccount account = gamificationService.getOrCreateUserAccount(userId);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @GetMapping("/journey/quiz")
    @Operation(summary = "按城市节点获取专属情景事件题")
    public ResponseEntity<QuizQuestionResponse> getJourneyQuizByCity(
            @Parameter(description = "城市节点索引", required = true)
            @RequestParam Integer cityIndex,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        QuizQuestionResponse quiz = gamificationService.getQuizQuestionForCity(cityIndex);
        return new ResponseEntity<>(quiz, HttpStatus.OK);
    }

    @GetMapping("/journey/state/{userId}")
    @Operation(summary = "获取零碳公路旅程状态（服务端权威状态）")
    public ResponseEntity<JourneyStateResponse> getJourneyState(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        requestUserContextResolver.ensurePathUserMatch(servletRequest, userId);
        JourneyStateResponse state = gamificationService.getJourneyState(userId);
        return new ResponseEntity<>(state, HttpStatus.OK);
    }

    @GetMapping("/journey/state/me")
    @Operation(summary = "获取当前登录用户零碳公路状态")
    public ResponseEntity<JourneyStateResponse> getCurrentJourneyState(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        JourneyStateResponse state = gamificationService.getJourneyState(userId);
        return new ResponseEntity<>(state, HttpStatus.OK);
    }

    @GetMapping("/journey/config")
    @Operation(summary = "获取零碳公路路线节点配置")
    public ResponseEntity<JourneyConfigResponse> getJourneyConfig(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        JourneyConfigResponse config = gamificationService.getJourneyConfig();
        return new ResponseEntity<>(config, HttpStatus.OK);
    }

    @PostMapping("/journey/checkin")
    @Operation(summary = "城市节点答题打卡并结算奖励")
    public ResponseEntity<QuizAnswerResultResponse> checkinAndAnswer(
            @Valid @RequestBody JourneyCheckinRequest request,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        QuizAnswerResultResponse result = gamificationService.checkinAndAnswer(userId, request);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/journey/claim-grand-prize")
    @Operation(summary = "申领年度环保车主终极奖励")
    public ResponseEntity<ClaimGrandPrizeResponse> claimGrandPrize(
            @Valid @RequestBody ClaimGrandPrizeRequest request,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        ClaimGrandPrizeResponse result = gamificationService.claimGrandPrize(userId, request);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/quiz/answer")
    @Operation(summary = "通用答题结算（兼容接口）")
    public ResponseEntity<QuizAnswerResultResponse> answerQuizAndReward(
            @Valid @RequestBody QuizAnswerRequest request,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        QuizAnswerResultResponse result = gamificationService.checkinAndAnswer(userId, new JourneyCheckinRequest(
                request.cityIndex(),
                request.quizId(),
            request.selectedAnswer()));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
