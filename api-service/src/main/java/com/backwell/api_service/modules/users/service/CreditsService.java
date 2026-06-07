package com.backwell.api_service.modules.users.service;

import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.invitations.config.InvitationCommissionProperties;
import com.backwell.api_service.modules.invitations.entity.InvitationTrace;
import com.backwell.api_service.modules.users.entity.UserInfo;
import com.backwell.api_service.modules.users.entity.credit.CreditTransaction;
import com.backwell.api_service.modules.users.entity.credit.CreditTransactionType;
import com.backwell.api_service.modules.users.entity.credit.UserCredit;
import com.backwell.api_service.modules.users.repo.CreditTransactionRepository;
import com.backwell.api_service.modules.users.repo.UserCreditRepository;
import com.backwell.api_service.modules.users.repo.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditsService {
    private final UserCreditRepository userCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final InvitationCommissionProperties invitationCommissionProperties;
    private final UUIDService uuidService;
    private final UserInfoRepository userInfoRepository;

    @Transactional
    public CreditTransaction buildCommissionTransaction(InvitationTrace md, BigDecimal amount) {
        UserInfo invitingUser =  md.getInvitingUser();

        BigDecimal commission = invitationCommissionProperties.getCalculationStrategy()
                .calculate(amount, invitationCommissionProperties);

        return new CreditTransaction(
                uuidService.next(),
                invitingUser,
                CreditTransactionType.COMMISSION,
                commission
        );
    }


    /**
     * Given a not persisted transaction, it applies it to the credit table as well as persisting the transaction object*/
    @Transactional
    public UserCredit updateUserCredit(CreditTransaction creditTransaction) {

        UserCredit credit = userCreditRepository.getOrThrow(creditTransaction.getUserInfo().getUuid());

        BigDecimal newBalance = credit.getBalance()
                .add(creditTransaction.getDelta())
                .setScale(2, RoundingMode.HALF_UP);

        credit.setBalance(newBalance);
        creditTransactionRepository.save(creditTransaction);
        return userCreditRepository.save(credit);
    }
}
