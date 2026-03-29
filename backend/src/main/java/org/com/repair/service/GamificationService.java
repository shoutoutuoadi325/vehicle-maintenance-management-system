package org.com.repair.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.com.repair.DTO.AdminUpdateJourneyShipmentRequest;
import org.com.repair.DTO.ClaimGrandPrizeRequest;
import org.com.repair.DTO.ClaimGrandPrizeResponse;
import org.com.repair.DTO.CouponDrawResultResponse;
import org.com.repair.DTO.CouponRedeemRequest;
import org.com.repair.DTO.CouponRedeemResponse;
import org.com.repair.DTO.JourneyCheckinRequest;
import org.com.repair.DTO.JourneyCityConfigResponse;
import org.com.repair.DTO.JourneyConfigResponse;
import org.com.repair.DTO.JourneyGrandPrizeStatusResponse;
import org.com.repair.DTO.JourneyMapSelectRequest;
import org.com.repair.DTO.JourneyMapSelectResponse;
import org.com.repair.DTO.JourneyMapSummaryResponse;
import org.com.repair.DTO.JourneyNodeResponse;
import org.com.repair.DTO.JourneyStateResponse;
import org.com.repair.DTO.QuizAnswerResultResponse;
import org.com.repair.DTO.QuizQuestionResponse;
import org.com.repair.DTO.RandomEventAnswerRequest;
import org.com.repair.entity.BrandPartner;
import org.com.repair.entity.Coupon;
import org.com.repair.entity.GreenDailyQuota;
import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.entity.GreenJourneyNodeState;
import org.com.repair.entity.GreenJourneyNodeState.NodeState;
import org.com.repair.entity.GreenQuiz;
import org.com.repair.entity.GreenRewardLedger;
import org.com.repair.entity.JourneyCompletionRecord;
import org.com.repair.entity.JourneyMap;
import org.com.repair.entity.JourneyMapNode;
import org.com.repair.entity.UserCouponWallet;
import org.com.repair.event.EmissionReducedEvent;
import org.com.repair.exception.GamificationErrorCode;
import org.com.repair.exception.GamificationException;
import org.com.repair.repository.BrandPartnerRepository;
import org.com.repair.repository.CouponRepository;
import org.com.repair.repository.GreenDailyQuotaRepository;
import org.com.repair.repository.GreenEnergyAccountRepository;
import org.com.repair.repository.GreenJourneyNodeStateRepository;
import org.com.repair.repository.GreenQuizRepository;
import org.com.repair.repository.GreenRewardLedgerRepository;
import org.com.repair.repository.JourneyCompletionRecordRepository;
import org.com.repair.repository.JourneyMapNodeRepository;
import org.com.repair.repository.JourneyMapRepository;
import org.com.repair.repository.UserCouponWalletRepository;
import org.com.repair.repository.TechnicianRepository;
import org.com.repair.service.GreenEnergyAccountProvisioningService.ConcurrentAccountCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class GamificationService {

    private static final Logger logger = LoggerFactory.getLogger(GamificationService.class);

    private static final String SOURCE_TYPE_ORDER = "REPAIR_ORDER";
    private static final String SOURCE_TYPE_JOURNEY = "JOURNEY_CITY";
    private static final String ACTION_EMISSION_REWARD = "EMISSION_REWARD";
    private static final String ACTION_CITY_CHECKIN_REWARD = "CITY_CHECKIN_REWARD";
    private static final String ACTION_RANDOM_EVENT_TRIGGER = "RANDOM_EVENT_TRIGGER";
    private static final String JOURNEY_STATUS_NORMAL = "NORMAL";
    private static final String JOURNEY_STATUS_PENDING_RANDOM_EVENT = "PENDING_RANDOM_EVENT";

    private static final double DEFAULT_EMISSION_TO_ENERGY_RATIO = 100.0;
    private static final int DEFAULT_DAILY_ENERGY_CAP = 1200;
    private static final int DEFAULT_ORDER_REWARD_CAP = 600;
    private static final int DEFAULT_DAILY_QUIZ_CHECKIN_LIMIT = 8;
    private static final double DEFAULT_RANDOM_EVENT_TRIGGER_PROBABILITY = 0.2;
    private static final int DEFAULT_WRONG_ANSWER_ENERGY_PENALTY = 50;
    private static final int DEFAULT_WRONG_ANSWER_COOLDOWN_MINUTES = 120;

    private final GreenEnergyAccountRepository greenEnergyAccountRepository;
    private final GreenDailyQuotaRepository greenDailyQuotaRepository;
    private final GreenQuizRepository greenQuizRepository;
    private final GreenJourneyNodeStateRepository greenJourneyNodeStateRepository;
    private final GreenRewardLedgerRepository greenRewardLedgerRepository;
    private final BrandPartnerRepository brandPartnerRepository;
    private final CouponRepository couponRepository;
    private final UserCouponWalletRepository userCouponWalletRepository;
    private final TechnicianRepository technicianRepository;
    private final JourneyCompletionRecordRepository journeyCompletionRecordRepository;
    private final JourneyMapRepository journeyMapRepository;
    private final JourneyMapNodeRepository journeyMapNodeRepository;
    private final GamificationRuleService gamificationRuleService;
    private final GreenEnergyAccountProvisioningService greenEnergyAccountProvisioningService;

    public GamificationService(
            GreenEnergyAccountRepository greenEnergyAccountRepository,
            GreenDailyQuotaRepository greenDailyQuotaRepository,
            GreenQuizRepository greenQuizRepository,
            GreenJourneyNodeStateRepository greenJourneyNodeStateRepository,
            GreenRewardLedgerRepository greenRewardLedgerRepository,
            BrandPartnerRepository brandPartnerRepository,
            CouponRepository couponRepository,
            UserCouponWalletRepository userCouponWalletRepository,
            TechnicianRepository technicianRepository,
            JourneyCompletionRecordRepository journeyCompletionRecordRepository,
            JourneyMapRepository journeyMapRepository,
            JourneyMapNodeRepository journeyMapNodeRepository,
            GamificationRuleService gamificationRuleService,
            GreenEnergyAccountProvisioningService greenEnergyAccountProvisioningService) {
        this.greenEnergyAccountRepository = greenEnergyAccountRepository;
        this.greenDailyQuotaRepository = greenDailyQuotaRepository;
        this.greenQuizRepository = greenQuizRepository;
        this.greenJourneyNodeStateRepository = greenJourneyNodeStateRepository;
        this.greenRewardLedgerRepository = greenRewardLedgerRepository;
        this.brandPartnerRepository = brandPartnerRepository;
        this.couponRepository = couponRepository;
        this.userCouponWalletRepository = userCouponWalletRepository;
        this.technicianRepository = technicianRepository;
        this.journeyCompletionRecordRepository = journeyCompletionRecordRepository;
        this.journeyMapRepository = journeyMapRepository;
        this.journeyMapNodeRepository = journeyMapNodeRepository;
        this.gamificationRuleService = gamificationRuleService;
        this.greenEnergyAccountProvisioningService = greenEnergyAccountProvisioningService;
    }

    @Transactional
    public ClaimGrandPrizeResponse claimGrandPrize(Long userId, ClaimGrandPrizeRequest request) {
        JourneyCompletionRecord record = journeyCompletionRecordRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.JOURNEY_NOT_COMPLETED,
                        "尚未完成全程，暂不可申领奖励"));

        record.setConsigneeName(request.consigneeName().trim());
        record.setConsigneePhone(request.consigneePhone().trim());
        record.setShippingAddress(request.shippingAddress().trim());
        record.setStickerClaimed(true);
        record.setGrandPrizeGranted(true);
        record.setShippingStatus("PREPARING");

        JourneyCompletionRecord saved = journeyCompletionRecordRepository.save(record);
        return new ClaimGrandPrizeResponse(
                userId,
                true,
                Boolean.TRUE.equals(saved.getStickerClaimed()),
                Boolean.TRUE.equals(saved.getGrandPrizeGranted()),
                saved.getShippingStatus());
    }

    @Transactional(readOnly = true)
    public JourneyGrandPrizeStatusResponse getGrandPrizeStatus(Long userId) {
        return toGrandPrizeStatusResponse(userId, journeyCompletionRecordRepository.findByUserId(userId).orElse(null));
    }

    @Transactional
    public JourneyGrandPrizeStatusResponse adminUpdateGrandPrizeShipment(Long userId, AdminUpdateJourneyShipmentRequest request) {
        JourneyCompletionRecord record = journeyCompletionRecordRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.JOURNEY_NOT_COMPLETED,
                        "该用户尚未完成全程"));

        String shippingStatus = normalizeShippingStatus(request.shippingStatus());
        record.setShippingStatus(shippingStatus);

        String trackingNo = request.shipmentTrackingNo();
        if (trackingNo != null) {
            String trimmed = trackingNo.trim();
            record.setShipmentTrackingNo(trimmed.isBlank() ? null : trimmed);
        }

        if ("SHIPPED".equals(shippingStatus) && record.getShippedAt() == null) {
            record.setShippedAt(LocalDateTime.now());
        }

        JourneyCompletionRecord saved = journeyCompletionRecordRepository.save(record);
        return toGrandPrizeStatusResponse(userId, saved);
    }

    @Transactional
    public GreenEnergyAccount getOrCreateUserAccount(Long userId) {
        GreenEnergyAccount account = getOrCreateAccountInternal(userId);
        normalizeJourneyStatus(account);
        Long mapId = ensureCurrentMapAssigned(account);
        ensureJourneyNodesInitialized(userId, mapId);
        reconcileNodeStates(userId, mapId, account.getCurrentMileage());
        return account;
    }

    @Transactional
    public JourneyMapSelectResponse selectJourneyMap(Long userId, JourneyMapSelectRequest request) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        Long targetMapId = request.mapId();
        JourneyMap targetMap = getEnabledMapById(targetMapId);

        Long currentMapId = account.getCurrentMapId();
        boolean hasMileageProgress = account.getCurrentMileage() != null && account.getCurrentMileage() > 0;
        boolean hasCheckedIn = currentMapId != null
                && greenJourneyNodeStateRepository.existsByUserIdAndMapIdAndNodeState(
                        userId,
                        currentMapId,
                        NodeState.CHECKED_IN.name());

        if (hasMileageProgress || hasCheckedIn) {
            throw new GamificationException(
                    GamificationErrorCode.MAP_SELECTION_LOCKED,
                    "已有旅程进度，当前账号不可切换路线");
        }

        account.setCurrentMapId(targetMapId);
        GreenEnergyAccount saved = greenEnergyAccountRepository.save(account);
        ensureJourneyNodesInitialized(userId, targetMapId);
        reconcileNodeStates(userId, targetMapId, saved.getCurrentMileage());

        return new JourneyMapSelectResponse(userId, targetMapId, targetMap.getMapName());
    }

    @Transactional(readOnly = true)
    public JourneyConfigResponse getJourneyConfig(Long userId) {
        GreenEnergyAccount account = getOrCreateAccountInternal(userId);
        Long currentMapId = account.getCurrentMapId();
        List<JourneyMap> enabledMaps = journeyMapRepository.findByEnabledTrueOrderByIdAsc();
        if (enabledMaps.isEmpty()) {
            throw new GamificationException(
                    GamificationErrorCode.MAP_NOT_ACTIVE,
                    "当前暂无可用路线");
        }

        final Long configuredMapId = currentMapId;
        boolean hasConfiguredMap = configuredMapId != null
            && enabledMaps.stream().anyMatch(m -> m.getId().equals(configuredMapId));

        if (!hasConfiguredMap) {
            currentMapId = enabledMaps.get(0).getId();
        }

        final Long selectedMapId = currentMapId;
        List<JourneyMapSummaryResponse> maps = enabledMaps.stream()
                .map(map -> new JourneyMapSummaryResponse(
                        map.getId(),
                        map.getMapName(),
                        map.getId().equals(selectedMapId)))
                .toList();

        List<JourneyCityConfigResponse> nodes = buildJourneyCityConfig(currentMapId);
        return new JourneyConfigResponse(maps, currentMapId, nodes);
    }

    public QuizQuestionResponse getQuizQuestionForCity(Long userId, Integer cityIndex) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        Long mapId = account.getCurrentMapId();
        JourneyMapNode node = getMapNodeOrThrow(mapId, cityIndex);

        GreenQuiz quiz = greenQuizRepository.findRandomScenarioQuizByCityIndex(cityIndex)
                .or(() -> greenQuizRepository.findDefaultQuizByCityIndex(cityIndex))
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.QUIZ_NOT_AVAILABLE,
                        "当前城市暂无可用题目"));

        return toQuizQuestionResponse(quiz, cityIndex, node.getCityName());
    }

    public QuizQuestionResponse getRandomQuizQuestion() {
        GreenQuiz quiz = greenQuizRepository.findRandomQuiz()
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.QUIZ_NOT_AVAILABLE,
                        "环保题库暂无可用题目"));
        Integer cityIndex = quiz.getCityIndex();
        return toQuizQuestionResponse(quiz, cityIndex, null);
    }

    @Transactional(readOnly = true)
    public QuizQuestionResponse getPendingRandomEventQuiz(Long userId) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        ensureRandomEventPending(account);
        ensureRandomEventRetryWindowReady(account);

        Long quizId = account.getPendingRandomQuizId();
        GreenQuiz quiz = greenQuizRepository.findById(quizId)
            .orElseThrow(() -> new GamificationException(
                GamificationErrorCode.QUIZ_NOT_FOUND,
                "随机事件题目不存在"));

        return toQuizQuestionResponse(quiz, null, null);
    }

    @Transactional
    public QuizAnswerResultResponse answerPendingRandomEvent(Long userId, RandomEventAnswerRequest request) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        ensureRandomEventPending(account);
        ensureRandomEventRetryWindowReady(account);

        Long pendingQuizId = account.getPendingRandomQuizId();
        if (!pendingQuizId.equals(request.quizId())) {
            throw new GamificationException(
                GamificationErrorCode.RANDOM_EVENT_QUIZ_MISMATCH,
                "随机事件题目不匹配，请重新获取当前题目");
        }

        GreenQuiz quiz = greenQuizRepository.findById(pendingQuizId)
            .orElseThrow(() -> new GamificationException(
                GamificationErrorCode.QUIZ_NOT_FOUND,
                "随机事件题目不存在"));

        String selectedAnswer = normalizeOption(request.selectedAnswer());
        String correctAnswer = normalizeOption(quiz.getCorrectAnswer());
        boolean isCorrect = correctAnswer.equals(selectedAnswer);

        JourneyCompletionRecord completion = journeyCompletionRecordRepository.findByUserId(userId).orElse(null);
        if (!isCorrect) {
            applyWrongAnswerPenalty(userId);
            LocalDateTime nextRetryTime = LocalDateTime.now().plusMinutes(getWrongAnswerCooldownMinutes());
            GreenEnergyAccount refreshed = greenEnergyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                    GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                    "随机事件冷却写入前读取账户失败"));
            refreshed.setRandomEventNextRetryTime(nextRetryTime);
            GreenEnergyAccount penaltySaved = greenEnergyAccountRepository.save(refreshed);

            return new QuizAnswerResultResponse(
                userId,
                pendingQuizId,
                false,
                0,
            penaltySaved.getTotalEnergy(),
            penaltySaved.getCurrentMileage(),
                null,
                completion != null,
                completion != null && Boolean.TRUE.equals(completion.getStickerClaimed()),
                completion != null ? completion.getShippingStatus() : "NOT_CLAIMED");
        }

        int frozenMileage = account.getFrozenMileage() == null ? 0 : account.getFrozenMileage();
        if (frozenMileage > 0) {
            int updated = greenEnergyAccountRepository.atomicIncrease(userId, 0, frozenMileage);
            if (updated <= 0) {
            throw new GamificationException(
                GamificationErrorCode.ACCOUNT_ATOMIC_SETTLEMENT_FAILED,
                "冻结里程解冻失败");
            }
        }

        GreenEnergyAccount reloaded = greenEnergyAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new GamificationException(
                GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                "解冻后读取账户失败"));
        reloaded.setJourneyStatus(JOURNEY_STATUS_NORMAL);
        reloaded.setPendingRandomQuizId(null);
        reloaded.setFrozenMileage(0);
        reloaded.setRandomEventNextRetryTime(null);
        GreenEnergyAccount saved = greenEnergyAccountRepository.save(reloaded);

        reconcileNodeStates(userId, saved.getCurrentMapId(), saved.getCurrentMileage());

        return new QuizAnswerResultResponse(
            userId,
            pendingQuizId,
            true,
            0,
            saved.getTotalEnergy(),
            saved.getCurrentMileage(),
            null,
            completion != null,
            completion != null && Boolean.TRUE.equals(completion.getStickerClaimed()),
            completion != null ? completion.getShippingStatus() : "NOT_CLAIMED");
    }

            @Transactional
            public CouponRedeemResponse redeemCoupon(Long userId, CouponRedeemRequest request) {
            Long shopId = request.shopId();
            Long technicianId = request.technicianId();
            if (shopId == null && technicianId == null) {
                throw new GamificationException(
                    GamificationErrorCode.REDEEM_OPERATOR_MISSING,
                    "核销门店或技师至少提供一个");
            }

            UserCouponWallet wallet = userCouponWalletRepository.findByIdAndUserId(request.walletId(), userId)
                .orElseThrow(() -> new GamificationException(
                    GamificationErrorCode.COUPON_WALLET_NOT_FOUND,
                    "卡包记录不存在或不属于当前用户"));

            if (!"NEW".equals(wallet.getCouponStatus())) {
                throw new GamificationException(
                    GamificationErrorCode.COUPON_NOT_REDEEMABLE,
                    "当前卡券状态不可核销");
            }

            LocalDateTime now = LocalDateTime.now();
            if (wallet.getExpireTime() != null && wallet.getExpireTime().isBefore(now)) {
                throw new GamificationException(
                    GamificationErrorCode.COUPON_EXPIRED,
                    "卡券已过期，无法核销");
            }

            if (shopId != null && !brandPartnerRepository.existsById(shopId)) {
                throw new GamificationException(
                    GamificationErrorCode.REDEEM_SHOP_NOT_FOUND,
                    "核销门店不存在");
            }

            if (technicianId != null && !technicianRepository.existsById(technicianId)) {
                throw new GamificationException(
                    GamificationErrorCode.REDEEM_TECHNICIAN_NOT_FOUND,
                    "核销技师不存在");
            }

            wallet.setCouponStatus("REDEEMED");
            wallet.setRedeemTime(now);
            wallet.setRedeemShopId(shopId);
            wallet.setRedeemTechnicianId(technicianId);
            UserCouponWallet saved = userCouponWalletRepository.save(wallet);

            return new CouponRedeemResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getCouponStatus(),
                saved.getRedeemTime() != null ? saved.getRedeemTime().toString() : null,
                saved.getRedeemShopId(),
                saved.getRedeemTechnicianId());
            }

    @Transactional
    public JourneyStateResponse getJourneyState(Long userId) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        Long mapId = account.getCurrentMapId();
        JourneyMap map = getEnabledMapById(mapId);
        List<JourneyMapNode> mapNodes = getMapNodesOrThrow(mapId);

        List<GreenJourneyNodeState> states = greenJourneyNodeStateRepository.findByUserIdAndMapIdOrderByCityIndexAsc(userId, mapId);
        Map<Integer, String> nodeStateMap = new HashMap<>();
        for (GreenJourneyNodeState node : states) {
            nodeStateMap.put(node.getCityIndex(), node.getNodeState());
        }

        List<JourneyNodeResponse> nodeResponses = new ArrayList<>();
        for (JourneyMapNode node : mapNodes) {
            nodeResponses.add(new JourneyNodeResponse(
                    node.getCityIndex(),
                    node.getCityName(),
                    node.getRequiredMileage(),
                    nodeStateMap.getOrDefault(node.getCityIndex(), NodeState.LOCKED.name())));
        }

        JourneyPositionSnapshot position = calculateJourneyPosition(mapNodes, account.getCurrentMileage());

        return new JourneyStateResponse(
                userId,
                mapId,
                map.getMapName(),
                account.getJourneyStatus(),
                account.getFrozenMileage(),
                account.getTotalEnergy(),
                account.getCurrentMileage(),
                position.progressPercent(),
                position.remainingToNextStationPercent(),
                position.currentX(),
                position.currentY(),
                nodeResponses);
    }

    @Transactional
    public QuizAnswerResultResponse checkinAndAnswer(Long userId, JourneyCheckinRequest request) {
        Integer cityIndex = request.cityIndex();
        Long quizId = request.quizId();
        String selectedAnswer = normalizeOption(request.selectedAnswer());

        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        Long mapId = account.getCurrentMapId();
        getMapNodeOrThrow(mapId, cityIndex);

        GreenJourneyNodeState targetNode = greenJourneyNodeStateRepository.findByUserIdAndMapIdAndCityIndex(userId, mapId, cityIndex)
            .orElseThrow(() -> new GamificationException(
                GamificationErrorCode.NODE_STATE_MISSING,
                "节点状态不存在"));

        if (NodeState.CHECKED_IN.name().equals(targetNode.getNodeState())) {
            throw new GamificationException(
                GamificationErrorCode.NODE_ALREADY_CHECKED_IN,
                "该城市已打卡，拒绝重复结算");
        }

        if (!NodeState.UNLOCKED.name().equals(targetNode.getNodeState())) {
            throw new GamificationException(
                GamificationErrorCode.NODE_NOT_UNLOCKED,
                "当前城市尚未解锁，禁止越级打卡");
        }

        ensureNodeRetryWindowReady(targetNode);

        Integer firstUnlockedIdx = firstUnlockedIndex(userId, mapId);
        if (firstUnlockedIdx == null || !cityIndex.equals(firstUnlockedIdx)) {
            throw new GamificationException(
                GamificationErrorCode.NODE_NOT_CURRENT_UNLOCKED,
                "只能挑战当前可打卡的首个节点，禁止并发抢跑");
        }

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long dailyQuizCheckins = greenRewardLedgerRepository.countBySourceTypeInWindow(userId, SOURCE_TYPE_JOURNEY, start, end);
        int quizLimit = gamificationRuleService.getInt("DAILY_QUIZ_CHECKIN_LIMIT", DEFAULT_DAILY_QUIZ_CHECKIN_LIMIT);
        if (dailyQuizCheckins >= quizLimit) {
            throw new GamificationException(
                GamificationErrorCode.DAILY_QUIZ_LIMIT_REACHED,
                "今日打卡次数已达上限，请明日再试");
        }

        GreenQuiz quiz = greenQuizRepository.findById(quizId)
            .orElseThrow(() -> new GamificationException(
                GamificationErrorCode.QUIZ_NOT_FOUND,
                "题目不存在"));

        if (quiz.getCityIndex() != null && !cityIndex.equals(quiz.getCityIndex())) {
            throw new GamificationException(
                    GamificationErrorCode.QUIZ_CITY_MISMATCH,
                    "题目与城市节点不匹配，请重新获取题目");
        }

        String correctAnswer = normalizeOption(quiz.getCorrectAnswer());
        boolean isCorrect = correctAnswer.equals(selectedAnswer);
        JourneyCompletionRecord existingCompletion = journeyCompletionRecordRepository.findByUserId(userId).orElse(null);

        if (!isCorrect) {
            applyWrongAnswerPenalty(userId);
            LocalDateTime nextRetryTime = LocalDateTime.now().plusMinutes(getWrongAnswerCooldownMinutes());
            targetNode.setNextRetryTime(nextRetryTime);
            greenJourneyNodeStateRepository.save(targetNode);

            GreenEnergyAccount reloaded = greenEnergyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                    GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                    "惩罚结算后账户读取失败"));
            return new QuizAnswerResultResponse(
                    userId,
                    quizId,
                    false,
                    0,
                reloaded.getTotalEnergy(),
                reloaded.getCurrentMileage(),
                    null,
                    existingCompletion != null,
                    existingCompletion != null && Boolean.TRUE.equals(existingCompletion.getStickerClaimed()),
                    existingCompletion != null ? existingCompletion.getShippingStatus() : "NOT_CLAIMED");
        }

        String sourceId = userId + "-" + mapId + "-" + cityIndex;
        RewardGrantResult grant = tryGrantReward(
                userId,
                quiz.getEnergyReward(),
                quiz.getEnergyReward(),
                SOURCE_TYPE_JOURNEY,
                sourceId,
                ACTION_CITY_CHECKIN_REWARD,
                "低碳问答节点打卡成功");

        if (!grant.granted()) {
            throw new GamificationException(
                    GamificationErrorCode.REWARD_ALREADY_GRANTED,
                    "奖励结算被风控系统拒绝或已结算");
        }

        targetNode.setNodeState(NodeState.CHECKED_IN.name());
        targetNode.setCheckinAt(LocalDateTime.now());
        targetNode.setNextRetryTime(null);
        greenJourneyNodeStateRepository.save(targetNode);

        reconcileNodeStates(userId, mapId, grant.currentMileage());

        CouponDrawResultResponse couponDrawResult = drawCouponForCity(userId, cityIndex, sourceId);
        JourneyCompletionRecord completionRecord = ensureJourneyCompletionIfFinished(userId, mapId, cityIndex);

        return new QuizAnswerResultResponse(
                userId,
                quizId,
                true,
                grant.rewardEnergy(),
                grant.totalEnergy(),
                grant.currentMileage(),
                couponDrawResult,
                completionRecord != null,
                completionRecord != null && Boolean.TRUE.equals(completionRecord.getStickerClaimed()),
                completionRecord != null ? completionRecord.getShippingStatus() : "NOT_CLAIMED");
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEmissionReduced(EmissionReducedEvent event) {
        Long userId = event.getUserId();
        Double emissionReduction = event.getEmissionReduction();
        Long repairOrderId = event.getRepairOrderId();

        if (userId == null || emissionReduction == null || emissionReduction <= 0 || repairOrderId == null) {
            logger.warn("忽略无效减排事件: {}", event);
            return;
        }

        double ratio = gamificationRuleService.getDouble("EMISSION_TO_ENERGY_RATIO", DEFAULT_EMISSION_TO_ENERGY_RATIO);
        double maxEmissionPerOrder = gamificationRuleService.getDouble("MAX_EMISSION_PER_ORDER", 20.0);
        int orderRewardCap = gamificationRuleService.getInt("ORDER_REWARD_CAP", DEFAULT_ORDER_REWARD_CAP);

        double safeEmission = Math.min(emissionReduction, maxEmissionPerOrder);
        int rawReward = (int) Math.round(safeEmission * ratio);
        rawReward = Math.min(rawReward, orderRewardCap);

        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        boolean pendingRandomEvent = JOURNEY_STATUS_PENDING_RANDOM_EVENT.equals(account.getJourneyStatus());

        RewardGrantResult grant = tryGrantReward(
                userId,
                rawReward,
            pendingRandomEvent ? 0 : rawReward,
                SOURCE_TYPE_ORDER,
                String.valueOf(repairOrderId),
                ACTION_EMISSION_REWARD,
            pendingRandomEvent ? "维修工单减排奖励（里程冻结中）" : "维修工单减排奖励");

        if (!grant.granted()) {
            logger.warn("维修工单 {} 的奖励未发放（幂等或风控拦截）", repairOrderId);
            return;
        }

        if (pendingRandomEvent) {
            GreenEnergyAccount latest = greenEnergyAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new GamificationException(
                            GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                            "里程冻结时读取账户失败"));
            int rawFrozen = latest.getFrozenMileage() == null ? 0 : latest.getFrozenMileage();
            latest.setFrozenMileage(rawFrozen + rawReward);
            greenEnergyAccountRepository.save(latest);
            logger.info("工单 {} 触发里程冻结累计: userId={}, frozenMileageDelta={}", repairOrderId, userId, rawReward);
            return;
        }

        reconcileNodeStates(userId, account.getCurrentMapId(), grant.currentMileage());

        maybeTriggerRandomEvent(userId, repairOrderId);

        logger.info(
                "工单 {} 奖励结算成功: userId={}, emission={}kg, reward={}, totalEnergy={}, mileage={}",
                repairOrderId,
                userId,
                String.format(Locale.ROOT, "%.2f", emissionReduction),
                grant.rewardEnergy(),
                grant.totalEnergy(),
                grant.currentMileage());
    }

    private GreenEnergyAccount getOrCreateAccountInternal(Long userId) {
        Optional<GreenEnergyAccount> existing = greenEnergyAccountRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return greenEnergyAccountProvisioningService.createAccountInNewTransaction(userId);
        } catch (ConcurrentAccountCreationException duplicate) {
            return greenEnergyAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new GamificationException(
                            GamificationErrorCode.ACCOUNT_INIT_FAILED,
                            "账户初始化失败：并发创建后未查到账户"));
        }
    }

    private void normalizeJourneyStatus(GreenEnergyAccount account) {
        boolean changed = false;
        if (account.getJourneyStatus() == null || account.getJourneyStatus().isBlank()) {
            account.setJourneyStatus(JOURNEY_STATUS_NORMAL);
            changed = true;
        }
        if (account.getFrozenMileage() == null) {
            account.setFrozenMileage(0);
            changed = true;
        }
        if (JOURNEY_STATUS_NORMAL.equals(account.getJourneyStatus()) && account.getRandomEventNextRetryTime() != null) {
            account.setRandomEventNextRetryTime(null);
            changed = true;
        }
        if (changed) {
            greenEnergyAccountRepository.save(account);
        }
    }

    private int getWrongAnswerPenalty() {
        return Math.max(0, gamificationRuleService.getInt(
                "WRONG_ANSWER_ENERGY_PENALTY",
                DEFAULT_WRONG_ANSWER_ENERGY_PENALTY));
    }

    private int getWrongAnswerCooldownMinutes() {
        return Math.max(0, gamificationRuleService.getInt(
                "WRONG_ANSWER_COOLDOWN_MINUTES",
                DEFAULT_WRONG_ANSWER_COOLDOWN_MINUTES));
    }

    private void applyWrongAnswerPenalty(Long userId) {
        int penalty = getWrongAnswerPenalty();
        if (penalty <= 0) {
            return;
        }

        GreenEnergyAccount latest = greenEnergyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                        "惩罚扣减前读取账户失败"));
        int currentEnergy = latest.getTotalEnergy() == null ? 0 : latest.getTotalEnergy();
        latest.setTotalEnergy(Math.max(0, currentEnergy - penalty));
        greenEnergyAccountRepository.save(latest);
    }

    private void ensureNodeRetryWindowReady(GreenJourneyNodeState targetNode) {
        LocalDateTime now = LocalDateTime.now();
        if (targetNode.getNextRetryTime() != null && targetNode.getNextRetryTime().isAfter(now)) {
            throw new GamificationException(
                    GamificationErrorCode.RETRY_COOLDOWN_ACTIVE,
                    "当前节点处于冷却中，请在 " + targetNode.getNextRetryTime() + " 后重试");
        }
    }

    private void ensureRandomEventRetryWindowReady(GreenEnergyAccount account) {
        LocalDateTime now = LocalDateTime.now();
        if (account.getRandomEventNextRetryTime() != null && account.getRandomEventNextRetryTime().isAfter(now)) {
            throw new GamificationException(
                    GamificationErrorCode.RETRY_COOLDOWN_ACTIVE,
                    "随机事件处于冷却中，请在 " + account.getRandomEventNextRetryTime() + " 后重试");
        }
    }

    private void ensureRandomEventPending(GreenEnergyAccount account) {
        if (!JOURNEY_STATUS_PENDING_RANDOM_EVENT.equals(account.getJourneyStatus()) || account.getPendingRandomQuizId() == null) {
            throw new GamificationException(
                    GamificationErrorCode.RANDOM_EVENT_NOT_PENDING,
                    "当前没有待完成的随机突发事件");
        }
    }

    private void maybeTriggerRandomEvent(Long userId, Long repairOrderId) {
        GreenEnergyAccount latest = greenEnergyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                        "触发随机事件时读取账户失败"));
        if (!JOURNEY_STATUS_NORMAL.equals(latest.getJourneyStatus())) {
            return;
        }

        double probability = gamificationRuleService.getDouble(
                "RANDOM_EVENT_TRIGGER_PROBABILITY",
                DEFAULT_RANDOM_EVENT_TRIGGER_PROBABILITY);
        probability = Math.max(0.0, Math.min(1.0, probability));
        boolean triggered = ThreadLocalRandom.current().nextDouble() < probability;
        if (!triggered) {
            return;
        }

        GreenQuiz randomQuiz = greenQuizRepository.findRandomRoadEventQuiz()
                .or(() -> greenQuizRepository.findRandomQuiz())
                .orElse(null);
        if (randomQuiz == null) {
            return;
        }

        latest.setJourneyStatus(JOURNEY_STATUS_PENDING_RANDOM_EVENT);
        latest.setPendingRandomQuizId(randomQuiz.getId());
        greenEnergyAccountRepository.save(latest);

        logger.info("工单 {} 触发随机突发事件: userId={}, quizId={}", repairOrderId, userId, randomQuiz.getId());
    }

    private Long ensureCurrentMapAssigned(GreenEnergyAccount account) {
        Long currentMapId = account.getCurrentMapId();
        if (currentMapId != null) {
            Optional<JourneyMap> current = journeyMapRepository.findById(currentMapId);
            if (current.isPresent() && Boolean.TRUE.equals(current.get().getEnabled())) {
                return currentMapId;
            }
        }

        JourneyMap fallback = journeyMapRepository.findFirstByEnabledTrueOrderByIdAsc()
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.MAP_NOT_ACTIVE,
                        "当前暂无可用路线"));
        account.setCurrentMapId(fallback.getId());
        greenEnergyAccountRepository.save(account);
        return fallback.getId();
    }

    private JourneyMap getEnabledMapById(Long mapId) {
        JourneyMap map = journeyMapRepository.findById(mapId)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.MAP_NOT_FOUND,
                        "路线不存在"));
        if (!Boolean.TRUE.equals(map.getEnabled())) {
            throw new GamificationException(
                    GamificationErrorCode.MAP_NOT_ACTIVE,
                    "所选路线当前不可用");
        }
        return map;
    }

    private List<JourneyMapNode> getMapNodesOrThrow(Long mapId) {
        List<JourneyMapNode> nodes = journeyMapNodeRepository.findByMapIdOrderByCityIndexAsc(mapId);
        if (nodes.isEmpty()) {
            throw new GamificationException(
                    GamificationErrorCode.MAP_NOT_ACTIVE,
                    "路线节点未配置");
        }
        return nodes;
    }

    private JourneyMapNode getMapNodeOrThrow(Long mapId, Integer cityIndex) {
        if (cityIndex == null) {
            throw new GamificationException(
                    GamificationErrorCode.INVALID_CITY_INDEX,
                    "非法城市节点索引");
        }
        return journeyMapNodeRepository.findByMapIdAndCityIndex(mapId, cityIndex)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.INVALID_CITY_INDEX,
                        "非法城市节点索引"));
    }

    private void ensureJourneyNodesInitialized(Long userId, Long mapId) {
        List<JourneyMapNode> mapNodes = getMapNodesOrThrow(mapId);
        List<GreenJourneyNodeState> states = greenJourneyNodeStateRepository.findByUserIdAndMapIdOrderByCityIndexAsc(userId, mapId);
        if (states.size() == mapNodes.size()) {
            return;
        }

        for (JourneyMapNode mapNode : mapNodes) {
            int cityIdx = mapNode.getCityIndex();
            boolean exists = states.stream().anyMatch(s -> s.getCityIndex() == cityIdx);
            if (exists) {
                continue;
            }

            GreenJourneyNodeState node = GreenJourneyNodeState.builder()
                    .userId(userId)
                    .mapId(mapId)
                    .cityIndex(cityIdx)
                    .nodeState(cityIdx == 0 ? NodeState.UNLOCKED.name() : NodeState.LOCKED.name())
                    .build();
            greenJourneyNodeStateRepository.save(node);
        }
    }

    private void reconcileNodeStates(Long userId, Long mapId, Integer currentMileage) {
        List<GreenJourneyNodeState> states = greenJourneyNodeStateRepository.findByUserIdAndMapIdOrderByCityIndexAsc(userId, mapId);
        for (GreenJourneyNodeState state : states) {
            if (NodeState.CHECKED_IN.name().equals(state.getNodeState())) {
                continue;
            }

            JourneyMapNode config = getMapNodeOrThrow(mapId, state.getCityIndex());
            if (currentMileage >= config.getRequiredMileage()) {
                state.setNodeState(NodeState.UNLOCKED.name());
            } else {
                state.setNodeState(NodeState.LOCKED.name());
            }
            greenJourneyNodeStateRepository.save(state);
        }
    }

    private RewardGrantResult tryGrantReward(Long userId,
                                             int requestedEnergy,
                                             int requestedMileage,
                                             String sourceType,
                                             String sourceId,
                                             String actionKey,
                                             String reason) {
        if (requestedEnergy <= 0 || requestedMileage <= 0) {
            GreenEnergyAccount account = getOrCreateAccountInternal(userId);
            return RewardGrantResult.notGranted(account.getTotalEnergy(), account.getCurrentMileage());
        }

        int dailyCap = gamificationRuleService.getInt("DAILY_ENERGY_CAP", DEFAULT_DAILY_ENERGY_CAP);
        int finalEnergy = requestedEnergy;
        int finalMileage = requestedMileage;

        if (greenRewardLedgerRepository.existsBySourceTypeAndSourceIdAndActionKey(sourceType, sourceId, actionKey)) {
            GreenEnergyAccount account = getOrCreateAccountInternal(userId);
            return RewardGrantResult.notGranted(account.getTotalEnergy(), account.getCurrentMileage());
        }

        if (finalEnergy <= 0 || finalMileage <= 0) {
            GreenEnergyAccount account = getOrCreateAccountInternal(userId);
            return RewardGrantResult.notGranted(account.getTotalEnergy(), account.getCurrentMileage());
        }

        LocalDate quotaDate = LocalDate.now();
        greenDailyQuotaRepository.initQuotaRow(userId, quotaDate);

        GreenDailyQuota quotaRow = greenDailyQuotaRepository.findByUserIdAndQuotaDateForUpdate(userId, quotaDate)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.QUOTA_ROW_LOCK_FAILED,
                        "奖励配额行锁定失败，请稍后重试"));

        int usedEnergy = quotaRow.getUsedEnergy() == null ? 0 : quotaRow.getUsedEnergy();
        int remainingEnergy = dailyCap - usedEnergy;
        if (remainingEnergy < finalEnergy) {
            throw new GamificationException(
                    GamificationErrorCode.DAILY_ENERGY_CAP_EXCEEDED,
                    "今日奖励额度已达上限");
        }

        try {
            GreenRewardLedger ledger = GreenRewardLedger.builder()
                    .userId(userId)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .actionKey(actionKey)
                    .energyDelta(finalEnergy)
                    .mileageDelta(finalMileage)
                    .riskLevel(finalEnergy < requestedEnergy ? "MEDIUM" : "LOW")
                    .reason(reason)
                    .build();
            greenRewardLedgerRepository.save(ledger);
        } catch (DataIntegrityViolationException duplicate) {
            GreenEnergyAccount account = getOrCreateAccountInternal(userId);
            return RewardGrantResult.notGranted(account.getTotalEnergy(), account.getCurrentMileage());
        }

        int quotaUpdated = greenDailyQuotaRepository.tryConsumeQuota(userId, quotaDate, finalEnergy, dailyCap);
        if (quotaUpdated <= 0) {
            throw new GamificationException(
                    GamificationErrorCode.QUOTA_ATOMIC_UPDATE_FAILED,
                    "奖励配额扣减失败，请稍后重试");
        }

        getOrCreateAccountInternal(userId);
        int updatedRows = greenEnergyAccountRepository.atomicIncrease(userId, finalEnergy, finalMileage);
        if (updatedRows <= 0) {
            throw new GamificationException(
                    GamificationErrorCode.ACCOUNT_ATOMIC_SETTLEMENT_FAILED,
                    "账户原子结算失败，未命中用户账户");
        }

        GreenEnergyAccount account = greenEnergyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                        "结算成功但账户读取失败"));

        return RewardGrantResult.granted(finalEnergy, account.getTotalEnergy(), account.getCurrentMileage());
    }

    private Integer firstUnlockedIndex(Long userId, Long mapId) {
        return greenJourneyNodeStateRepository.findByUserIdAndMapIdOrderByCityIndexAsc(userId, mapId)
                .stream()
                .filter(state -> NodeState.UNLOCKED.name().equals(state.getNodeState()))
                .map(GreenJourneyNodeState::getCityIndex)
                .findFirst()
                .orElse(null);
    }

    private JourneyPositionSnapshot calculateJourneyPosition(List<JourneyMapNode> nodes, Integer currentMileage) {
        int mileage = Math.max(0, currentMileage == null ? 0 : currentMileage);
        JourneyMapNode first = nodes.get(0);
        JourneyMapNode last = nodes.get(nodes.size() - 1);

        int totalDistance = Math.max(0, last.getRequiredMileage());
        double progressPercent = totalDistance <= 0
                ? 100.0
                : Math.min(100.0, (mileage * 100.0) / totalDistance);

        if (nodes.size() == 1) {
            return new JourneyPositionSnapshot(
                    round2(progressPercent),
                    0.0,
                    first.getX().doubleValue(),
                    first.getY().doubleValue());
        }

        if (mileage >= last.getRequiredMileage()) {
            return new JourneyPositionSnapshot(
                    round2(progressPercent),
                    0.0,
                    last.getX().doubleValue(),
                    last.getY().doubleValue());
        }

        JourneyMapNode prev = first;
        JourneyMapNode next = nodes.get(1);
        for (int i = 1; i < nodes.size(); i++) {
            JourneyMapNode candidate = nodes.get(i);
            if (mileage < candidate.getRequiredMileage()) {
                prev = nodes.get(i - 1);
                next = candidate;
                break;
            }
        }

        int segmentDistance = next.getRequiredMileage() - prev.getRequiredMileage();
        double ratio = segmentDistance <= 0
                ? 1.0
                : clamp01((mileage - prev.getRequiredMileage()) * 1.0 / segmentDistance);

        double currentX = prev.getX() + (next.getX() - prev.getX()) * ratio;
        double currentY = prev.getY() + (next.getY() - prev.getY()) * ratio;
        double remainingToNext = (1.0 - ratio) * 100.0;

        return new JourneyPositionSnapshot(
                round2(progressPercent),
                round2(remainingToNext),
                round2(currentX),
                round2(currentY));
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<JourneyCityConfigResponse> buildJourneyCityConfig(Long mapId) {
        List<JourneyCityConfigResponse> nodes = new ArrayList<>();
        for (JourneyMapNode node : getMapNodesOrThrow(mapId)) {
            Optional<Coupon> cityCoupon = couponRepository.findTopByCityIndexAndEnabledTrueOrderByIdAsc(node.getCityIndex());
            if (cityCoupon.isEmpty()) {
                nodes.add(new JourneyCityConfigResponse(
                        node.getCityIndex(),
                        node.getCityName(),
                        node.getRequiredMileage(),
                        node.getX(),
                        node.getY(),
                        false,
                        null,
                        null));
                continue;
            }

            Coupon coupon = cityCoupon.get();
            BrandPartner brand = brandPartnerRepository.findById(coupon.getBrandPartnerId()).orElse(null);
            nodes.add(new JourneyCityConfigResponse(
                    node.getCityIndex(),
                    node.getCityName(),
                    node.getRequiredMileage(),
                    node.getX(),
                    node.getY(),
                    true,
                    brand != null ? brand.getBrandName() : null,
                    brand != null ? brand.getLogoUrl() : null));
        }
        return nodes;
    }

    private QuizQuestionResponse toQuizQuestionResponse(GreenQuiz quiz, Integer fallbackCityIndex, String cityName) {
        Integer resolvedCityIndex = quiz.getCityIndex() != null ? quiz.getCityIndex() : fallbackCityIndex;

        String eventTitle = quiz.getEventTitle();
        String eventDescription = quiz.getEventDescription();
        String eventTheme = quiz.getEventTheme();

        if (eventTitle == null || eventTitle.isBlank()) {
            eventTitle = cityName != null ? ("抵达" + cityName + "补给站") : "低碳突发事件";
        }
        if (eventDescription == null || eventDescription.isBlank()) {
            eventDescription = cityName != null
                    ? ("你已到达" + cityName + "，完成本次绿色知识挑战即可打卡前进。")
                    : "完成本次绿色知识挑战即可继续前进。";
        }
        if (eventTheme == null || eventTheme.isBlank()) {
            eventTheme = "default";
        }

        return new QuizQuestionResponse(
                quiz.getId(),
                resolvedCityIndex,
                eventTitle,
                eventDescription,
                eventTheme,
                quiz.getQuestion(),
                quiz.getOptions());
    }

    private String normalizeOption(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeShippingStatus(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!"PREPARING".equals(normalized) && !"SHIPPED".equals(normalized) && !"DELIVERED".equals(normalized)) {
            throw new GamificationException(
                    GamificationErrorCode.INVALID_SHIPPING_STATUS,
                    "非法发货状态，仅支持 PREPARING/SHIPPED/DELIVERED");
        }
        return normalized;
    }

    private JourneyGrandPrizeStatusResponse toGrandPrizeStatusResponse(Long userId, JourneyCompletionRecord record) {
        if (record == null) {
            return new JourneyGrandPrizeStatusResponse(
                    userId,
                    false,
                    false,
                    false,
                    "NOT_CLAIMED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        return new JourneyGrandPrizeStatusResponse(
                userId,
                true,
                Boolean.TRUE.equals(record.getStickerClaimed()),
                Boolean.TRUE.equals(record.getGrandPrizeGranted()),
                record.getShippingStatus(),
                record.getConsigneeName(),
                record.getConsigneePhone(),
                record.getShippingAddress(),
                record.getShipmentTrackingNo(),
                record.getCompletedAt() != null ? record.getCompletedAt().toString() : null,
                record.getShippedAt() != null ? record.getShippedAt().toString() : null);
    }

    private JourneyCompletionRecord ensureJourneyCompletionIfFinished(Long userId, Long mapId, Integer cityIndex) {
        List<JourneyMapNode> nodes = getMapNodesOrThrow(mapId);
        Integer finalCityIndex = nodes.get(nodes.size() - 1).getCityIndex();
        if (cityIndex == null || !cityIndex.equals(finalCityIndex)) {
            return journeyCompletionRecordRepository.findByUserId(userId).orElse(null);
        }

        GreenJourneyNodeState finalNode = greenJourneyNodeStateRepository.findByUserIdAndMapIdAndCityIndex(userId, mapId, finalCityIndex)
                .orElse(null);
        if (finalNode == null || !NodeState.CHECKED_IN.name().equals(finalNode.getNodeState())) {
            return journeyCompletionRecordRepository.findByUserId(userId).orElse(null);
        }

        return journeyCompletionRecordRepository.findByUserId(userId)
                .orElseGet(() -> journeyCompletionRecordRepository.save(JourneyCompletionRecord.builder()
                        .userId(userId)
                        .completedAt(LocalDateTime.now())
                        .grandPrizeGranted(false)
                        .stickerClaimed(false)
                        .shippingStatus("NOT_CLAIMED")
                        .build()));
    }

    private CouponDrawResultResponse drawCouponForCity(Long userId, Integer cityIndex, String sourceId) {
        List<Coupon> cityCoupons = couponRepository.findActiveByCityIndex(cityIndex, LocalDateTime.now());
        if (cityCoupons.isEmpty()) {
            return new CouponDrawResultResponse(false, null, null, null, null, null, cityIndex);
        }

        Coupon selected = selectCouponByProbability(cityCoupons);
        if (selected == null) {
            return new CouponDrawResultResponse(false, null, null, null, null, null, cityIndex);
        }

        int updated = couponRepository.tryIssueCoupon(selected.getId());
        if (updated <= 0) {
            return new CouponDrawResultResponse(false, null, null, null, null, null, cityIndex);
        }

        BrandPartner brand = brandPartnerRepository.findById(selected.getBrandPartnerId()).orElse(null);
        UserCouponWallet wallet = UserCouponWallet.builder()
                .userId(userId)
                .couponId(selected.getId())
                .brandPartnerId(selected.getBrandPartnerId())
                .cityIndex(cityIndex)
                .couponTitle(selected.getCouponTitle())
                .couponDescription(selected.getCouponDescription())
                .couponStatus("NEW")
                .sourceAction("JOURNEY_COUPON_DRAW_" + sourceId)
                .drawTime(LocalDateTime.now())
                .expireTime(selected.getExpireTime())
                .build();
        userCouponWalletRepository.save(wallet);

        return new CouponDrawResultResponse(
                true,
                selected.getId(),
                brand != null ? brand.getBrandName() : null,
                brand != null ? brand.getLogoUrl() : null,
                selected.getCouponTitle(),
                selected.getCouponDescription(),
                cityIndex);
    }

    private Coupon selectCouponByProbability(List<Coupon> coupons) {
        double totalCouponProbability = coupons.stream()
                .map(Coupon::getWinProbability)
                .filter(value -> value != null)
                .mapToDouble(value -> Math.max(0.0, value.doubleValue()))
                .sum();

        if (totalCouponProbability <= 0) {
            return null;
        }

        double noWinWeight = Math.max(0.0, 1.0 - totalCouponProbability);
        double range = totalCouponProbability + noWinWeight;
        double r = ThreadLocalRandom.current().nextDouble(0.0, range);
        if (r > totalCouponProbability) {
            return null;
        }

        double cumulative = 0.0;
        for (Coupon coupon : coupons) {
            double weight = coupon.getWinProbability() == null ? 0.0 : Math.max(0.0, coupon.getWinProbability().doubleValue());
            cumulative += weight;
            if (r <= cumulative) {
                return coupon;
            }
        }
        return null;
    }

    private record RewardGrantResult(boolean granted, int rewardEnergy, int totalEnergy, int currentMileage) {
        private static RewardGrantResult granted(int rewardEnergy, int totalEnergy, int currentMileage) {
            return new RewardGrantResult(true, rewardEnergy, totalEnergy, currentMileage);
        }

        private static RewardGrantResult notGranted(int totalEnergy, int currentMileage) {
            return new RewardGrantResult(false, 0, totalEnergy, currentMileage);
        }
    }

    private record JourneyPositionSnapshot(
            double progressPercent,
            double remainingToNextStationPercent,
            double currentX,
            double currentY) {
    }
}
