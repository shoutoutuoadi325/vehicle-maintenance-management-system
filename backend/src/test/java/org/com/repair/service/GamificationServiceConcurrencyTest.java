package org.com.repair.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.entity.User;
import org.com.repair.event.EmissionReducedEvent;
import org.com.repair.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GamificationServiceConcurrencyTest {

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private GamificationRuleService gamificationRuleService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldGrantOnlyOnceForSameRepairOrderUnderConcurrency() throws Exception {
        Long userId = createTestUser().getId();

        int dailyCap = gamificationRuleService.getInt("DAILY_ENERGY_CAP", 1200);
        double ratio = gamificationRuleService.getDouble("EMISSION_TO_ENERGY_RATIO", 100.0);
        double maxEmissionPerOrder = gamificationRuleService.getDouble("MAX_EMISSION_PER_ORDER", 20.0);
        int orderRewardCap = gamificationRuleService.getInt("ORDER_REWARD_CAP", 600);

        int targetReward = Math.max(1, Math.min(orderRewardCap, Math.max(1, dailyCap / 2)));
        double emission = Math.min(maxEmissionPerOrder, Math.max(0.01, targetReward / ratio));
        int expectedSingleReward = calculateReward(emission, ratio, maxEmissionPerOrder, orderRewardCap);

        long repairOrderId = System.nanoTime();
        runConcurrentTasks(16, index -> {
            try {
                gamificationService.handleEmissionReduced(
                        new EmissionReducedEvent(this, repairOrderId, userId, emission));
            } catch (RuntimeException ignoreExpectedCapOrDuplicate) {
                // Duplicates and cap conflicts are expected under contention.
            }
            return null;
        });

        GreenEnergyAccount account = gamificationService.getOrCreateUserAccount(userId);
        assertNotNull(account);
        assertTrue(account.getTotalEnergy() == 0 || account.getTotalEnergy() == expectedSingleReward,
                "同一工单并发奖励只能发放一次");
        assertTrue(account.getCurrentMileage() >= 0, "里程不能为负数");
        assertTrue(account.getCurrentMileage() <= account.getTotalEnergy(), "站点门禁下里程不应超过能量");
    }

    @Test
    void shouldNeverExceedDailyCapUnderHighConcurrency() throws Exception {
        Long userId = createTestUser().getId();

        int dailyCap = gamificationRuleService.getInt("DAILY_ENERGY_CAP", 1200);
        double ratio = gamificationRuleService.getDouble("EMISSION_TO_ENERGY_RATIO", 100.0);
        double maxEmissionPerOrder = gamificationRuleService.getDouble("MAX_EMISSION_PER_ORDER", 20.0);
        int orderRewardCap = gamificationRuleService.getInt("ORDER_REWARD_CAP", 600);

        int targetReward = Math.max(1, Math.min(orderRewardCap, Math.max(1, dailyCap / 3)));
        double emission = Math.min(maxEmissionPerOrder, Math.max(0.01, targetReward / ratio));

        runConcurrentTasks(40, index -> {
            try {
                gamificationService.handleEmissionReduced(
                        new EmissionReducedEvent(this, System.nanoTime() + index, userId, emission));
            } catch (RuntimeException ignoreExpectedCap) {
                // Some requests may be rejected after cap exhaustion.
            }
            return null;
        });

        GreenEnergyAccount account = gamificationService.getOrCreateUserAccount(userId);
        assertNotNull(account);
        assertTrue(account.getTotalEnergy() <= dailyCap,
                "并发结算后总能量不能超过日上限");
        assertTrue(account.getCurrentMileage() >= 0, "里程不能为负数");
        assertTrue(account.getCurrentMileage() <= account.getTotalEnergy(), "站点门禁下里程不应超过能量");
    }

    private int calculateReward(double emissionReduction,
                                double ratio,
                                double maxEmissionPerOrder,
                                int orderRewardCap) {
        double safeEmission = Math.min(emissionReduction, maxEmissionPerOrder);
        int rawReward = (int) Math.round(safeEmission * ratio);
        return Math.min(rawReward, orderRewardCap);
    }

    private User createTestUser() {
        String suffix = String.valueOf(System.nanoTime());
        User user = new User();
        user.setUsername("gamification_test_" + suffix);
        user.setPassword("pwd123456");
        user.setName("Gamification Tester");
        user.setPhone("139" + suffix.substring(Math.max(0, suffix.length() - 8)));
        user.setEmail("gamification_" + suffix + "@example.com");
        user.setAddress("test");
        return userRepository.save(user);
    }

    private void runConcurrentTasks(int taskCount, IndexedTask indexedTask) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(16, taskCount));
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<Void>> callables = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            int index = i;
            callables.add(() -> {
                startLatch.await(10, TimeUnit.SECONDS);
                return indexedTask.run(index);
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> callable : callables) {
            futures.add(executorService.submit(callable));
        }

        startLatch.countDown();
        for (Future<Void> future : futures) {
            future.get(20, TimeUnit.SECONDS);
        }

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS), "线程池未在预期时间内结束");
    }

    @FunctionalInterface
    private interface IndexedTask {
        Void run(int index) throws Exception;
    }
}