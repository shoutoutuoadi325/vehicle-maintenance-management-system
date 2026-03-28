package org.com.repair.controller;

import java.util.List;

import org.com.repair.DTO.QuizAnswerRequest;
import org.com.repair.DTO.QuizAnswerResultResponse;
import org.com.repair.DTO.RandomEventAnswerRequest;
import org.com.repair.DTO.ClaimGrandPrizeRequest;
import org.com.repair.DTO.ClaimGrandPrizeResponse;
import org.com.repair.DTO.CouponRedeemRequest;
import org.com.repair.DTO.CouponRedeemResponse;
import org.com.repair.DTO.JourneyGrandPrizeStatusResponse;
import org.com.repair.DTO.JourneyFootprintPageResponse;
import org.com.repair.DTO.JourneyGameplayOverviewResponse;
import org.com.repair.DTO.JourneyCheckinRequest;
import org.com.repair.DTO.JourneyConfigResponse;
import org.com.repair.DTO.JourneyMapSelectRequest;
import org.com.repair.DTO.JourneyMapSelectResponse;
import org.com.repair.DTO.JourneyStateResponse;
import org.com.repair.DTO.LeaderboardResponse;
import org.com.repair.DTO.LeaderboardType;
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
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        QuizQuestionResponse quiz = gamificationService.getQuizQuestionForCity(userId, cityIndex);
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

    @GetMapping("/journey/overview")
    @Operation(summary = "获取零碳公路聚合玩法视图")
    public ResponseEntity<JourneyGameplayOverviewResponse> getJourneyOverview(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        JourneyGameplayOverviewResponse response = gamificationService.getJourneyGameplayOverview(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/journey/config")
    @Operation(summary = "获取零碳公路路线节点配置")
    public ResponseEntity<JourneyConfigResponse> getJourneyConfig(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        JourneyConfigResponse config = gamificationService.getJourneyConfig(userId);
        return new ResponseEntity<>(config, HttpStatus.OK);
    }

    @GetMapping("/journey/eco-tips")
    @Operation(summary = "获取AI等待期间展示的绿色导向文案")
    public ResponseEntity<List<String>> getEcoTips(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        return new ResponseEntity<>(gamificationService.getEcoWaitingTips(), HttpStatus.OK);
    }

    @PostMapping("/journey/map/select")
    @Operation(summary = "选择零碳公路路线（仅初始状态可切换）")
    public ResponseEntity<JourneyMapSelectResponse> selectJourneyMap(
            @Valid @RequestBody JourneyMapSelectRequest request,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        JourneyMapSelectResponse response = gamificationService.selectJourneyMap(userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

    @GetMapping("/journey/random-event/quiz")
    @Operation(summary = "获取当前待处理的路途随机突发事件题目")
    public ResponseEntity<QuizQuestionResponse> getPendingRandomEventQuiz(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        QuizQuestionResponse response = gamificationService.getPendingRandomEventQuiz(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/journey/random-event/answer")
    @Operation(summary = "提交路途随机突发事件答案并解除里程冻结")
    public ResponseEntity<QuizAnswerResultResponse> answerPendingRandomEvent(
            @Valid @RequestBody RandomEventAnswerRequest request,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        QuizAnswerResultResponse response = gamificationService.answerPendingRandomEvent(userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/coupon/redeem")
    @Operation(summary = "核销用户卡包优惠券")
    public ResponseEntity<CouponRedeemResponse> redeemCoupon(
            @Valid @RequestBody CouponRedeemRequest request,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        CouponRedeemResponse response = gamificationService.redeemCoupon(userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/journey/footprints")
    @Operation(summary = "分页查询当前用户旅行足迹")
    public ResponseEntity<JourneyFootprintPageResponse> getJourneyFootprints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        JourneyFootprintPageResponse response = gamificationService.getJourneyFootprints(userId, page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "获取零碳先锋排行榜")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam LeaderboardType type,
            HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        LeaderboardResponse response = gamificationService.getLeaderboard(type);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

    @GetMapping("/journey/grand-prize/me")
    @Operation(summary = "获取当前用户通关奖励状态")
    public ResponseEntity<JourneyGrandPrizeStatusResponse> getMyGrandPrizeStatus(HttpServletRequest servletRequest) {
        requestUserContextResolver.requireCustomerRole(servletRequest);
        Long userId = requestUserContextResolver.requireUserId(servletRequest);
        JourneyGrandPrizeStatusResponse result = gamificationService.getGrandPrizeStatus(userId);
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
