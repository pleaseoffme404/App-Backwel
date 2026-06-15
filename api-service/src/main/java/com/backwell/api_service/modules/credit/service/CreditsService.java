package com.backwell.api_service.modules.credit.service;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.idempotency.GlobalIdempotencyCache;
import com.backwell.api_service.common.idempotency.IdempotencyDomain;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.credit.controller.req.UpdateCreditRequest;
import com.backwell.api_service.modules.credit.controller.res.CreditBalanceDTO;
import com.backwell.api_service.modules.invitations.config.InvitationCommissionProperties;
import com.backwell.api_service.modules.invitations.entity.InvitationTrace;
import com.backwell.api_service.modules.users.entity.UserInfo;
import com.backwell.api_service.modules.credit.entity.CreditTransaction;
import com.backwell.api_service.modules.credit.entity.CreditTransactionType;
import com.backwell.api_service.modules.credit.entity.UserCredit;
import com.backwell.api_service.modules.credit.repo.CreditTransactionRepository;
import com.backwell.api_service.modules.credit.repo.UserCreditRepository;
import com.backwell.api_service.modules.users.repo.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import static com.backwell.api_service.common.exception.codes.CreditErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditsService {
    private final UserCreditRepository userCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final InvitationCommissionProperties invitationCommissionProperties;
    private final UUIDService uuidService;
    private final UserInfoRepository userInfoRepository;
    private final GlobalIdempotencyCache idempotencyCache;

    @Transactional(readOnly = true)
    public CreditBalanceDTO getUserCreditBalance(UserSession session) {
        return userCreditRepository.getBalanceOrThrow(session.uuid());
    }

    @Transactional
    public CreditTransaction buildCommissionTransaction(InvitationTrace md, BigDecimal amount) {
        UserInfo invitingUser =  md.getInvitingUser();

        BigDecimal commission = invitationCommissionProperties.getCalculationStrategy()
                .calculate(amount, invitationCommissionProperties);

        return new CreditTransaction(
                uuidService.next(),
                // TODO this is a weak implementation XD but I have no id to precisely identify
                //  this as an idempotent transaction as other parameters already set this
                //  an idempotent operation
                uuidService.next(),
                invitingUser,
                CreditTransactionType.COMMISSION,
                commission
        );
    }

    @Transactional
    public CreditBalanceDTO updateUserCreditBalance(UserSession currentUser, UpdateCreditRequest req) {
        if (!idempotencyCache.startRequest(IdempotencyDomain.CREDIT, req.userId(), req.idempotencyKey(), Duration.ofMinutes(10))) {
            throw new BusinessException("The operation is already underway or has been completed.", DUPLICATE_REQUEST);
        }

        try {
            UserInfo targetUser = userInfoRepository.getOrThrow(req.userId());
            UserInfo actorUser = userInfoRepository.getOrThrow(currentUser);

            CreditTransaction transaction = new CreditTransaction(
                    uuidService.next(),
                    req.idempotencyKey(),
                    targetUser,
                    actorUser,
                    CreditTransactionType.DELTA_UPDATE,
                    req.delta().setScale(2, RoundingMode.HALF_UP)
            );

            UserCredit creditEntity = updateUserCredit(transaction);

            idempotencyCache.completeRequest(IdempotencyDomain.CREDIT, req.userId(), req.idempotencyKey(), Duration.ofMinutes(60));

            return CreditBalanceDTO.fromEntity(creditEntity);

        } catch (Exception e) {
            idempotencyCache.removeKey(IdempotencyDomain.CREDIT, req.userId(), req.idempotencyKey());
            throw e;
        }
    }

    /**
     * Given a not persisted transaction, it applies it to the credit table as well as persisting the transaction object*/
    @Transactional
    public UserCredit updateUserCredit(CreditTransaction creditTransaction) {

        UserCredit credit = userCreditRepository.getOrThrow(creditTransaction.getUserInfo().getUuid());

        BigDecimal newBalance = getNewBalance(creditTransaction, credit);

        credit.setBalance(newBalance);
        creditTransactionRepository.save(creditTransaction);
        try {
            return userCreditRepository.save(credit);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("El saldo fue actualizado por otra operación, por favor reintenta.", OPTIMISTIC_LOCK_ERROR);
        }
    }

    @NotNull
    private BigDecimal getNewBalance(CreditTransaction creditTransaction, UserCredit credit) {
        BigDecimal newBalance = credit.getBalance()
                .add(creditTransaction.getDelta())
                .setScale(2, RoundingMode.HALF_UP);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El saldo resultante no puede ser negativo", NEGATIVE_BALANCE_ERROR);
        }

        BigDecimal maxAllowed = new BigDecimal("999999999999.99");
        if (newBalance.compareTo(maxAllowed) > 0) {
            throw new BusinessException("El saldo excede el límite permitido de precisión", PRECISION_LIMIT_EXCEEDED);
        }
        return newBalance;
    }
}
