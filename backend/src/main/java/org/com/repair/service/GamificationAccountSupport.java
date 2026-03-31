package org.com.repair.service;

import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.exception.GamificationErrorCode;
import org.com.repair.exception.GamificationException;
import org.com.repair.repository.GreenEnergyAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class GamificationAccountSupport {

    private static final int DEFAULT_WRONG_ANSWER_ENERGY_PENALTY = 50;
    private static final int DEFAULT_WRONG_ANSWER_COOLDOWN_MINUTES = 120;

    private final GreenEnergyAccountRepository greenEnergyAccountRepository;
    private final GamificationRuleService gamificationRuleService;

    public GamificationAccountSupport(GreenEnergyAccountRepository greenEnergyAccountRepository,
                                      GamificationRuleService gamificationRuleService) {
        this.greenEnergyAccountRepository = greenEnergyAccountRepository;
        this.gamificationRuleService = gamificationRuleService;
    }

    public void normalizeJourneyStatus(GreenEnergyAccount account, String journeyStatusNormal) {
        boolean changed = false;
        if (account.getJourneyStatus() == null || account.getJourneyStatus().isBlank()) {
            account.setJourneyStatus(journeyStatusNormal);
            changed = true;
        }
        if (account.getFrozenMileage() == null) {
            account.setFrozenMileage(0);
            changed = true;
        }
        if (journeyStatusNormal.equals(account.getJourneyStatus()) && account.getRandomEventNextRetryTime() != null) {
            account.setRandomEventNextRetryTime(null);
            changed = true;
        }
        if (changed) {
            greenEnergyAccountRepository.save(account);
        }
    }

    public GreenEnergyAccount loadAccountOrThrow(Long userId, String failureMessage) {
        return greenEnergyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationException(
                        GamificationErrorCode.ACCOUNT_READ_AFTER_SETTLEMENT_FAILED,
                        failureMessage));
    }

    public int getWrongAnswerCooldownMinutes() {
        return Math.max(0, gamificationRuleService.getInt(
                "WRONG_ANSWER_COOLDOWN_MINUTES",
                DEFAULT_WRONG_ANSWER_COOLDOWN_MINUTES));
    }

    public void applyWrongAnswerPenalty(Long userId) {
        int penalty = getWrongAnswerPenalty();
        if (penalty <= 0) {
            return;
        }

        GreenEnergyAccount latest = loadAccountOrThrow(userId, "惩罚扣减前读取账户失败");
        int currentEnergy = latest.getTotalEnergy() == null ? 0 : latest.getTotalEnergy();
        latest.setTotalEnergy(Math.max(0, currentEnergy - penalty));
        greenEnergyAccountRepository.save(latest);
    }

    private int getWrongAnswerPenalty() {
        return Math.max(0, gamificationRuleService.getInt(
                "WRONG_ANSWER_ENERGY_PENALTY",
                DEFAULT_WRONG_ANSWER_ENERGY_PENALTY));
    }
}
