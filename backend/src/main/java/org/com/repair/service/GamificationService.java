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

import org.com.repair.DTO.CouponDrawResultResponse;
import org.com.repair.DTO.ClaimGrandPrizeRequest;
import org.com.repair.DTO.ClaimGrandPrizeResponse;
import org.com.repair.DTO.AdminUpdateJourneyShipmentRequest;
import org.com.repair.DTO.JourneyCheckinRequest;
import org.com.repair.DTO.JourneyCityConfigResponse;
import org.com.repair.DTO.JourneyConfigResponse;
import org.com.repair.DTO.JourneyGrandPrizeStatusResponse;
import org.com.repair.DTO.JourneyNodeResponse;
import org.com.repair.DTO.JourneyStateResponse;
import org.com.repair.DTO.QuizAnswerResultResponse;
import org.com.repair.DTO.QuizQuestionResponse;
import org.com.repair.entity.BrandPartner;
import org.com.repair.entity.Coupon;
import org.com.repair.entity.GreenDailyQuota;
import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.entity.GreenJourneyNodeState;
import org.com.repair.entity.GreenJourneyNodeState.NodeState;
import org.com.repair.entity.GreenQuiz;
import org.com.repair.entity.GreenRewardLedger;
import org.com.repair.entity.JourneyCompletionRecord;
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
import org.com.repair.repository.UserCouponWalletRepository;
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

    private static final List<JourneyNodeConfig> JOURNEY_NODES = List.of(
            new JourneyNodeConfig(0, "成都", 0, 70, 470),
            new JourneyNodeConfig(1, "康定", 120, 285, 345),
            new JourneyNodeConfig(2, "理塘", 260, 470, 270),
            new JourneyNodeConfig(3, "林芝", 420, 680, 195),
            new JourneyNodeConfig(4, "拉萨", 580, 940, 95));

    private static final String SOURCE_TYPE_ORDER = "REPAIR_ORDER";
    private static final String SOURCE_TYPE_JOURNEY = "JOURNEY_CITY";
    private static final String ACTION_EMISSION_REWARD = "EMISSION_REWARD";
    private static final String ACTION_CITY_CHECKIN_REWARD = "CITY_CHECKIN_REWARD";

    private static final double DEFAULT_EMISSION_TO_ENERGY_RATIO = 100.0;
    private static final int DEFAULT_DAILY_ENERGY_CAP = 1200;
    private static final int DEFAULT_ORDER_REWARD_CAP = 600;
    private static final int DEFAULT_DAILY_QUIZ_CHECKIN_LIMIT = 8;
    private static final int FINAL_CITY_INDEX = 4;

    private final GreenEnergyAccountRepository greenEnergyAccountRepository;
    private final GreenDailyQuotaRepository greenDailyQuotaRepository;
    private final GreenQuizRepository greenQuizRepository;
    private final GreenJourneyNodeStateRepository greenJourneyNodeStateRepository;
    private final GreenRewardLedgerRepository greenRewardLedgerRepository;
    private final BrandPartnerRepository brandPartnerRepository;
    private final CouponRepository couponRepository;
    private final UserCouponWalletRepository userCouponWalletRepository;
    private final JourneyCompletionRecordRepository journeyCompletionRecordRepository;
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
            JourneyCompletionRecordRepository journeyCompletionRecordRepository,
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
        this.journeyCompletionRecordRepository = journeyCompletionRecordRepository;
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
        ensureJourneyNodesInitialized(userId);
        reconcileNodeStates(userId, account.getCurrentMileage());
        return account;
    }

    public JourneyConfigResponse getJourneyConfig() {
        List<JourneyCityConfigResponse> nodes = JOURNEY_NODES.stream()
            .map(node -> {
                Optional<Coupon> cityCoupon = couponRepository.findTopByCityIndexAndEnabledTrueOrderByIdAsc(node.cityIndex());
                if (cityCoupon.isEmpty()) {
                return new JourneyCityConfigResponse(
                    node.cityIndex(),
                    node.cityName(),
                    node.requiredMileage(),
                    node.x(),
                    node.y(),
                    false,
                    null,
                    null);
                }

                Coupon coupon = cityCoupon.get();
                BrandPartner brand = brandPartnerRepository.findById(coupon.getBrandPartnerId()).orElse(null);
                return new JourneyCityConfigResponse(
                    node.cityIndex(),
                    node.cityName(),
                    node.requiredMileage(),
                    node.x(),
                    node.y(),
                    true,
                    brand != null ? brand.getBrandName() : null,
                    brand != null ? brand.getLogoUrl() : null);
            })
                .toList();
        return new JourneyConfigResponse(nodes);
    }

    public QuizQuestionResponse getQuizQuestionForCity(Integer cityIndex) {
        validateCityIndex(cityIndex);

        GreenQuiz quiz = greenQuizRepository.findRandomScenarioQuizByCityIndex(cityIndex)
                .or(() -> greenQuizRepository.findDefaultQuizByCityIndex(cityIndex))
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.QUIZ_NOT_AVAILABLE,
                        "当前城市暂无可用题目"));

        return toQuizQuestionResponse(quiz, cityIndex);
    }

    public QuizQuestionResponse getRandomQuizQuestion() {
        GreenQuiz quiz = greenQuizRepository.findRandomQuiz()
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.QUIZ_NOT_AVAILABLE,
                        "环保题库暂无可用题目"));
        Integer cityIndex = quiz.getCityIndex();
        return toQuizQuestionResponse(quiz, cityIndex);
    }

    @Transactional
    public JourneyStateResponse getJourneyState(Long userId) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        List<GreenJourneyNodeState> nodes = greenJourneyNodeStateRepository.findByUserIdOrderByCityIndexAsc(userId);
        Map<Integer, String> nodeStateMap = new HashMap<>();
        for (GreenJourneyNodeState node : nodes) {
            nodeStateMap.put(node.getCityIndex(), node.getNodeState());
        }

        List<JourneyNodeResponse> nodeResponses = new ArrayList<>();
        for (JourneyNodeConfig config : JOURNEY_NODES) {
            nodeResponses.add(new JourneyNodeResponse(
                    config.cityIndex(),
                    config.cityName(),
                    config.requiredMileage(),
                    nodeStateMap.getOrDefault(config.cityIndex(), NodeState.LOCKED.name())));
        }

        return new JourneyStateResponse(
                userId,
                account.getTotalEnergy(),
                account.getCurrentMileage(),
                nodeResponses);
    }

    @Transactional
    public QuizAnswerResultResponse checkinAndAnswer(Long userId, JourneyCheckinRequest request) {
        Integer cityIndex = request.cityIndex();
        Long quizId = request.quizId();
        String selectedAnswer = normalizeOption(request.selectedAnswer());

        validateCityIndex(cityIndex);
        GreenEnergyAccount account = getOrCreateUserAccount(userId);

        GreenJourneyNodeState targetNode = greenJourneyNodeStateRepository.findByUserIdAndCityIndex(userId, cityIndex)
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

        Integer firstUnlockedIdx = firstUnlockedIndex(userId);
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
            return new QuizAnswerResultResponse(
                    userId,
                    quizId,
                    false,
                    0,
                    account.getTotalEnergy(),
                    account.getCurrentMileage(),
                    null,
                    existingCompletion != null,
                    existingCompletion != null && Boolean.TRUE.equals(existingCompletion.getStickerClaimed()),
                    existingCompletion != null ? existingCompletion.getShippingStatus() : "NOT_CLAIMED");
        }

        String sourceId = userId + "-" + cityIndex;
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
        greenJourneyNodeStateRepository.save(targetNode);

        reconcileNodeStates(userId, grant.currentMileage());

        CouponDrawResultResponse couponDrawResult = drawCouponForCity(userId, cityIndex, sourceId);
        JourneyCompletionRecord completionRecord = ensureJourneyCompletionIfFinished(userId, cityIndex);

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

        RewardGrantResult grant = tryGrantReward(
                userId,
                rawReward,
                rawReward,
                SOURCE_TYPE_ORDER,
                String.valueOf(repairOrderId),
                ACTION_EMISSION_REWARD,
                "维修工单减排奖励");

        if (!grant.granted()) {
            logger.warn("维修工单 {} 的奖励未发放（幂等或风控拦截）", repairOrderId);
            return;
        }

        reconcileNodeStates(userId, grant.currentMileage());

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

    private void ensureJourneyNodesInitialized(Long userId) {
        List<GreenJourneyNodeState> states = greenJourneyNodeStateRepository.findByUserIdOrderByCityIndexAsc(userId);
        if (states.size() == JOURNEY_NODES.size()) {
            return;
        }

        for (JourneyNodeConfig nodeConfig : JOURNEY_NODES) {
            int cityIdx = nodeConfig.cityIndex();
            boolean exists = states.stream().anyMatch(s -> s.getCityIndex() == cityIdx);
            if (exists) {
                continue;
            }

            GreenJourneyNodeState node = GreenJourneyNodeState.builder()
                    .userId(userId)
                    .cityIndex(cityIdx)
                    .nodeState(cityIdx == 0 ? NodeState.UNLOCKED.name() : NodeState.LOCKED.name())
                    .build();
            greenJourneyNodeStateRepository.save(node);
        }
    }

    private void reconcileNodeStates(Long userId, Integer currentMileage) {
        List<GreenJourneyNodeState> states = greenJourneyNodeStateRepository.findByUserIdOrderByCityIndexAsc(userId);
        for (GreenJourneyNodeState state : states) {
            if (NodeState.CHECKED_IN.name().equals(state.getNodeState())) {
                continue;
            }

            JourneyNodeConfig config = getJourneyNodeConfig(state.getCityIndex());
            if (currentMileage >= config.requiredMileage()) {
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

    private Integer firstUnlockedIndex(Long userId) {
        return greenJourneyNodeStateRepository.findByUserIdOrderByCityIndexAsc(userId)
                .stream()
                .filter(state -> NodeState.UNLOCKED.name().equals(state.getNodeState()))
                .map(GreenJourneyNodeState::getCityIndex)
                .findFirst()
                .orElse(null);
    }

    private void validateCityIndex(Integer cityIndex) {
        if (cityIndex == null || cityIndex < 0 || cityIndex >= JOURNEY_NODES.size()) {
            throw new GamificationException(
                    GamificationErrorCode.INVALID_CITY_INDEX,
                    "非法城市节点索引");
        }
    }

    private JourneyNodeConfig getJourneyNodeConfig(int cityIndex) {
        if (cityIndex < 0 || cityIndex >= JOURNEY_NODES.size()) {
            throw new GamificationException(
                    GamificationErrorCode.INVALID_CITY_INDEX,
                    "非法城市节点索引");
        }
        return JOURNEY_NODES.get(cityIndex);
    }

    private QuizQuestionResponse toQuizQuestionResponse(GreenQuiz quiz, Integer fallbackCityIndex) {
        Integer resolvedCityIndex = quiz.getCityIndex() != null ? quiz.getCityIndex() : fallbackCityIndex;
        JourneyNodeConfig nodeConfig = (resolvedCityIndex != null && resolvedCityIndex >= 0 && resolvedCityIndex < JOURNEY_NODES.size())
                ? getJourneyNodeConfig(resolvedCityIndex)
                : null;

        String eventTitle = quiz.getEventTitle();
        String eventDescription = quiz.getEventDescription();
        String eventTheme = quiz.getEventTheme();

        if (eventTitle == null || eventTitle.isBlank()) {
            eventTitle = nodeConfig != null ? ("抵达" + nodeConfig.cityName() + "补给站") : "低碳突发事件";
        }
        if (eventDescription == null || eventDescription.isBlank()) {
            eventDescription = nodeConfig != null
                    ? ("你已到达" + nodeConfig.cityName() + "，完成本次绿色知识挑战即可打卡前进。")
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

    private JourneyCompletionRecord ensureJourneyCompletionIfFinished(Long userId, Integer cityIndex) {
        if (cityIndex == null || cityIndex != FINAL_CITY_INDEX) {
            return journeyCompletionRecordRepository.findByUserId(userId).orElse(null);
        }

        GreenJourneyNodeState finalNode = greenJourneyNodeStateRepository.findByUserIdAndCityIndex(userId, FINAL_CITY_INDEX)
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

    private record JourneyNodeConfig(int cityIndex, String cityName, int requiredMileage, int x, int y) {
    }
}
