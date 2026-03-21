package org.com.repair.service;

import org.com.repair.DTO.QuizAnswerResultResponse;
import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.entity.GreenQuiz;
import org.com.repair.event.EmissionReducedEvent;
import org.com.repair.repository.GreenEnergyAccountRepository;
import org.com.repair.repository.GreenQuizRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GamificationService {

    private static final Logger logger = LoggerFactory.getLogger(GamificationService.class);

    // 减排比例：1kg CO2 = 100 绿色能量
    private static final double EMISSION_TO_ENERGY_RATIO = 100.0;

    private final GreenEnergyAccountRepository greenEnergyAccountRepository;
    private final GreenQuizRepository greenQuizRepository;

    public GamificationService(
            GreenEnergyAccountRepository greenEnergyAccountRepository,
            GreenQuizRepository greenQuizRepository) {
        this.greenEnergyAccountRepository = greenEnergyAccountRepository;
        this.greenQuizRepository = greenQuizRepository;
    }

    /**
     * 获取或初始化用户的绿色能量账户
     */
    @Transactional
    public GreenEnergyAccount getOrCreateUserAccount(Long userId) {
        return greenEnergyAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    GreenEnergyAccount newAccount = GreenEnergyAccount.builder()
                            .userId(userId)
                            .totalEnergy(0)
                            .currentMileage(0)
                            .build();
                    return greenEnergyAccountRepository.save(newAccount);
                });
    }

    /**
     * 随机获取一道环保题目
     */
    public GreenQuiz getRandomQuiz() {
        return greenQuizRepository.findRandomQuiz()
                .orElseThrow(() -> new RuntimeException("环保题库暂无可用题目"));
    }

    /**
     * 答题并结算能量奖励
     */
    @Transactional
    public QuizAnswerResultResponse answerQuizAndReward(Long userId, Long quizId, boolean isCorrect) {
        GreenEnergyAccount account = getOrCreateUserAccount(userId);
        GreenQuiz quiz = greenQuizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        int reward = isCorrect ? quiz.getEnergyReward() : 0;
        if (reward > 0) {
            account.setTotalEnergy(account.getTotalEnergy() + reward);
            account.setCurrentMileage(account.getCurrentMileage() + reward);
            greenEnergyAccountRepository.save(account);
        }

        return new QuizAnswerResultResponse(
                userId,
                quizId,
                isCorrect,
                reward,
                account.getTotalEnergy(),
                account.getCurrentMileage());
    }

    /**
     * 监听减排事件，将减排量转化为绿色能量奖励
     * 当维修工单完成时，将计算出的碳减排量按比例转化为用户的绿色能量
     * 比例：1kg 减排量 = 100 绿色能量
     */
    @EventListener
    @Transactional
    public void handleEmissionReduced(EmissionReducedEvent event) {
        try {
            Long userId = event.getUserId();
            Double emissionReduction = event.getEmissionReduction();
            Long repairOrderId = event.getRepairOrderId();

            if (userId == null || emissionReduction == null || emissionReduction <= 0) {
                logger.warn("无效的减排事件：userId={}, emissionReduction={}", userId, emissionReduction);
                return;
            }

            // 获取或创建用户账户
            GreenEnergyAccount account = getOrCreateUserAccount(userId);

            // 计算能量奖励：1kg 减排量 = 100 绿色能量
            int energyReward = (int) Math.round(emissionReduction * EMISSION_TO_ENERGY_RATIO);

            // 更新账户信息
            account.setTotalEnergy(account.getTotalEnergy() + energyReward);
            account.setCurrentMileage(account.getCurrentMileage() + energyReward);

            GreenEnergyAccount updatedAccount = greenEnergyAccountRepository.save(account);

            logger.info(
                "维修工单 {} 完成，用户 {} 获得绿色能量奖励：减排量 {}kg -> 能量 {} 点，" +
                "当前总能量：{}，当前里程：{}",
                repairOrderId,
                userId,
                String.format("%.2f", emissionReduction),
                energyReward,
                updatedAccount.getTotalEnergy(),
                updatedAccount.getCurrentMileage()
            );
        } catch (Exception e) {
            logger.error("处理减排事件失败：{}", event, e);
        }
    }
}
