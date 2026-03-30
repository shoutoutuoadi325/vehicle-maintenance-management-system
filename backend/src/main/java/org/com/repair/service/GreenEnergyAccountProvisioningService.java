package org.com.repair.service;

import org.com.repair.entity.GreenEnergyAccount;
import org.com.repair.repository.GreenEnergyAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GreenEnergyAccountProvisioningService {

    private final GreenEnergyAccountRepository greenEnergyAccountRepository;

    public GreenEnergyAccountProvisioningService(GreenEnergyAccountRepository greenEnergyAccountRepository) {
        this.greenEnergyAccountRepository = greenEnergyAccountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GreenEnergyAccount createAccountInNewTransaction(Long userId) {
        try {
            GreenEnergyAccount newAccount = new GreenEnergyAccount();
            newAccount.setUserId(userId);
            newAccount.setTotalEnergy(0);
            newAccount.setCurrentMileage(0);
            return greenEnergyAccountRepository.save(newAccount);
        } catch (DataIntegrityViolationException duplicate) {
            throw new ConcurrentAccountCreationException("账户并发创建冲突", duplicate);
        }
    }

    public static class ConcurrentAccountCreationException extends RuntimeException {
        public ConcurrentAccountCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}