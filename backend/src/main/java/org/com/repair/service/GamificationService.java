package org.com.repair.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.com.repair.DTO.JourneyCheckinRequest;
import org.com.repair.DTO.JourneyNodeResponse;
import org.com.repair.DTO.JourneyStateResponse;
import org.com.repair.DTO.QuizAnswerResultResponse;
import org.com.repair.DTO.QuizQuestionResponse;
import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.entity.GreenJourneyNodeState;
import org.com.repair.entity.GreenJourneyNodeState.NodeState;
import org.com.repair.entity.GreenQuiz;
import org.com.repair.entity.GreenRewardLedger;
import org.com.repair.event.EmissionReducedEvent;
import org.com.repair.repository.GreenEnergyAccountRepository;
import org.com.repair.repository.GreenJourneyNodeStateRepository;
import org.com.repair.repository.GreenQuizRepository;
import org.com.repair.repository.GreenRewardLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class GamificationService {

    private static final Logger logger = LoggerFactory.getLogger(GamificationService.class);

    private static final String[] CITY_NAMES = { "成都", "康定", "理塘", "林芝", "拉萨" };
    private static final int[] CITY_MILESTONES = { 0, 120, 260, 420, 580 };

    private static final String SOURCE_TYPE_ORDER = "REPAIR_ORDER";
    private static final String SOURCE_TYPE_JOURNEY = "JOURNEY_CITY";
    private static final String ACTION_EMISSION_REWARD = "EMISSION_REWARD";
    private static final String ACTION_CITY_CHECKIN_REWARD = "CITY_CHECKIN_REWARD";

    private static final double DEFAULT_EMISSION_TO_ENERGY_RATIO = 100.0;
    private static final int DEFAULT_DAILY_ENERGY_CAP = 1200;
    private static final int DEFAULT_ORDER_REWARD_CAP = 600;
    private static final int DEFAULT_DAILY_QUIZ_CHECKIN_LIMIT = 8;

    private final GreenEnergyAccountRepository greenEnergyAccountRepository;
    private final GreenQuizRepository greenQuizRepository;
    private final GreenJourneyNodeStateRepository greenJourneyNodeStateRepository;
    private final GreenRewardLedgerRepository greenRewardLedgerRepository;
    private final GamificationRuleService gamificationRuleService;

    public GamificationService(
            GreenEnergyAccountRepository greenEnergyAccountRepository,
            GreenQuizRepository greenQuizRepository,
            GreenJourneyNodeStateRepository greenJourneyNodeStateRepository,
            GreenRewardLedgerRepository greenRewardLedgerRepository,
            GamificationRuleService gamificationRuleService) {
        this.greenEnergyAccountRepository = greenEnergyAccountRepository;
        this.greenQuizRepository = greenQuizRepository;
        this.greenJourneyNodeStateRepository = greenJourneyNodeStateRepository;
        this.greenRewardLedgerRepository = greenRewardLedgerRepository;
        this.gamificationRuleService = gamificationRuleService;
    }

    @Transactional
    public GreenEnergyAccount getOrCreateUserAccount(Long userId) {
        GreenEnergyAccount account = getOrCreateAccountInternal(userId);
        ensureJourneyNodesInitialized(userId);
        reconcileNodeStates(userId, account.getCurrentMileage());
        return account;
    }

    public QuizQuestionResponse getRandomQuizQuestion() {
        GreenQuiz quiz = greenQuizRepository.findRandomQuiz()
                .orElseThrow(() -> new IllegalStateException("环保题库暂无可用题目"));
        return new QuizQuestionResponse(quiz.getId(), quiz.getQuestion(), quiz.getOptions());
    }

    @Transactional
    public JourneyStateResponse getJourneyState(Long userId) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        List<GreenJourneyNodeState> nodes = greenJourneyNodeStateRepository.findByUserIdOrderByCityIndexAsc(userId);

        List<JourneyNodeResponse> nodeResponses = new ArrayList<>();
        for (GreenJourneyNodeState node : nodes) {
            int idx = node.getCityIndex();
            nodeResponses.add(new JourneyNodeResponse(
                    idx,
                    CITY_NAMES[idx],
                    CITY_MILESTONES[idx],
                    node.getNodeState()));
        }

        return new JourneyStateResponse(
                userId,
                account.getTotalEnergy(),
                account.getCurrentMileage(),
                nodeResponses);
    }

    @Transactional
    public QuizAnswerResultResponse checkinAndAnswer(JourneyCheckinRequest request) {
        Long userId = request.userId();
        Integer cityIndex = request.cityIndex();
        Long quizId = request.quizId();
        String selectedAnswer = normalizeOption(request.selectedAnswer());

        validateCityIndex(cityIndex);
        GreenEnergyAccount account = getOrCreateUserAccount(userId);

        GreenJourneyNodeState targetNode = greenJourneyNodeStateRepository.findByUserIdAndCityIndex(userId, cityIndex)
                .orElseThrow(() -> new IllegalStateException("节点状态不存在"));

        if (NodeState.CHECKED_IN.name().equals(targetNode.getNodeState())) {
            throw new IllegalStateException("该城市已打卡，拒绝重复结算");
        }

        if (!NodeState.UNLOCKED.name().equals(targetNode.getNodeState())) {
            throw new IllegalStateException("当前城市尚未解锁，禁止越级打卡");
        }

        Integer firstUnlockedIdx = firstUnlockedIndex(userId);
        if (firstUnlockedIdx == null || !cityIndex.equals(firstUnlockedIdx)) {
            throw new IllegalStateException("只能挑战当前可打卡的首个节点，禁止并发抢跑");
        }

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long dailyQuizCheckins = greenRewardLedgerRepository.countBySourceTypeInWindow(userId, SOURCE_TYPE_JOURNEY, start, end);
        int quizLimit = gamificationRuleService.getInt("DAILY_QUIZ_CHECKIN_LIMIT", DEFAULT_DAILY_QUIZ_CHECKIN_LIMIT);
        if (dailyQuizCheckins >= quizLimit) {
            throw new IllegalStateException("今日打卡次数已达上限，请明日再试");
        }

        GreenQuiz quiz = greenQuizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在"));
        String correctAnswer = normalizeOption(quiz.getCorrectAnswer());
        boolean isCorrect = correctAnswer.equals(selectedAnswer);

        if (!isCorrect) {
            return new QuizAnswerResultResponse(
                    userId,
                    quizId,
                    false,
                    0,
                    account.getTotalEnergy(),
                    account.getCurrentMileage());
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
            throw new IllegalStateException("奖励结算被风控系统拒绝或已结算");
        }

        targetNode.setNodeState(NodeState.CHECKED_IN.name());
        targetNode.setCheckinAt(LocalDateTime.now());
        greenJourneyNodeStateRepository.save(targetNode);

        reconcileNodeStates(userId, grant.currentMileage());

        return new QuizAnswerResultResponse(
                userId,
                quizId,
                true,
                grant.rewardEnergy(),
                grant.totalEnergy(),
                grant.currentMileage());
    }

    @TransactionalEventListener
    @Transactional
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
            GreenEnergyAccount newAccount = GreenEnergyAccount.builder()
                    .userId(userId)
                    .totalEnergy(0)
                    .currentMileage(0)
                    .build();
            return greenEnergyAccountRepository.save(newAccount);
        } catch (DataIntegrityViolationException duplicate) {
            return greenEnergyAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("账户初始化失败：并发创建后未查到账户"));
        }
    }

    private void ensureJourneyNodesInitialized(Long userId) {
        List<GreenJourneyNodeState> states = greenJourneyNodeStateRepository.findByUserIdOrderByCityIndexAsc(userId);
        if (states.size() == CITY_NAMES.length) {
            return;
        }

        for (int i = 0; i < CITY_NAMES.length; i++) {
            int cityIdx = i;
            boolean exists = states.stream().anyMatch(s -> s.getCityIndex() == cityIdx);
            if (exists) {
                continue;
            }

            GreenJourneyNodeState node = GreenJourneyNodeState.builder()
                    .userId(userId)
                    .cityIndex(i)
                    .nodeState(i == 0 ? NodeState.UNLOCKED.name() : NodeState.LOCKED.name())
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

            int idx = state.getCityIndex();
            if (currentMileage >= CITY_MILESTONES[idx]) {
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
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Integer rewardedToday = greenRewardLedgerRepository.sumRewardedEnergyInWindow(userId, start, end);
        int availableToday = Math.max(0, dailyCap - (rewardedToday == null ? 0 : rewardedToday));
        int finalEnergy = Math.min(requestedEnergy, availableToday);
        int finalMileage = Math.min(requestedMileage, finalEnergy);

        if (finalEnergy <= 0) {
            GreenEnergyAccount account = getOrCreateAccountInternal(userId);
            return RewardGrantResult.notGranted(account.getTotalEnergy(), account.getCurrentMileage());
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

        getOrCreateAccountInternal(userId);
        int updatedRows = greenEnergyAccountRepository.atomicIncrease(userId, finalEnergy, finalMileage);
        if (updatedRows <= 0) {
            throw new IllegalStateException("账户原子结算失败，未命中用户账户");
        }

        GreenEnergyAccount account = greenEnergyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("结算成功但账户读取失败"));

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
        if (cityIndex == null || cityIndex < 0 || cityIndex >= CITY_NAMES.length) {
            throw new IllegalArgumentException("非法城市节点索引");
        }
    }

    private String normalizeOption(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record RewardGrantResult(boolean granted, int rewardEnergy, int totalEnergy, int currentMileage) {
        private static RewardGrantResult granted(int rewardEnergy, int totalEnergy, int currentMileage) {
            return new RewardGrantResult(true, rewardEnergy, totalEnergy, currentMileage);
        }

        private static RewardGrantResult notGranted(int totalEnergy, int currentMileage) {
            return new RewardGrantResult(false, 0, totalEnergy, currentMileage);
        }
    }
}
