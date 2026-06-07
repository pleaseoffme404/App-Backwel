package com.backwell.api_service.modules.invitations.service;

import com.backwell.api_service.common.config.RedisSpacePrefixes;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.invitations.InvitationMetadata;
import com.backwell.api_service.modules.invitations.entity.InvitationTrace;
import com.backwell.api_service.modules.invitations.repo.ReferralTraceRepository;
import com.backwell.api_service.modules.users.dto.CompleteAccountRequest;
import com.backwell.api_service.modules.users.entity.UserInfo;
import com.backwell.api_service.modules.users.entity.credit.CreditTransaction;
import com.backwell.api_service.modules.users.repo.UserInfoRepository;
import com.backwell.api_service.modules.users.service.CreditsService;
import com.backwell.api_service.modules.users.service.CuponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.backwell.api_service.common.exception.codes.UserErrorCode.*;

@Service
@RequiredArgsConstructor
public class InvitationService {
    private final UUIDService uuidService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisSpacePrefixes redisSpacePrefixes;

    private final UserInfoRepository userRepository;
    private final ReferralTraceRepository referralTraceRepository;
    private final CuponService cuponService;
    private final CreditsService creditsService;


    public String createInvitation(UUID referrerId, String targetEmail) {
        String emailKey = redisSpacePrefixes.INVITATION_EMAIL_INDEX_PREFIX + targetEmail;

        if (userRepository.existsByEmail(targetEmail) || redisTemplate.hasKey(emailKey)) {
            throw new BusinessException("Invitation could not be sent to this email.", INVITATION_FAILED);
        }

        String token = UUID.randomUUID().toString();
        InvitationMetadata metadata = InvitationMetadata.builder()
                .invitingUserId(referrerId)
                .invitedEmail(targetEmail.toLowerCase())
                .createdAt(Instant.now())
                .build();

        String tokenKey = redisSpacePrefixes.INVITATION_CODE_PREFIX + token;
        redisTemplate.opsForValue().set(tokenKey, metadata, 30, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(emailKey, targetEmail, 30, TimeUnit.DAYS);
        return tokenKey;
    }

    @Transactional
    public Optional<InvitationMetadata> validateAndGet(CompleteAccountRequest request, String providedEmail) {
        if (request.invitationCode() == null) return Optional.empty();

        String tokenKey = redisSpacePrefixes.INVITATION_CODE_PREFIX + request.invitationCode();

        InvitationMetadata metadata = redisTemplate.opsForValue().get(tokenKey) instanceof InvitationMetadata
                ? (InvitationMetadata) redisTemplate.opsForValue().get(tokenKey)
                : null;

        if (metadata == null) {
            throw new BusinessException("Invitation code has expired or is not valid", INVITATION_FAILED);
        }

        if (!metadata.getInvitedEmail().equals(providedEmail)) {
            throw new BusinessException("Invitation code does not match its targeted email", INVITATION_FAILED);
        }

        return Optional.of(metadata);
    }

    /**
     * Elimina el registro del código de invitación en redis y lo persiste en db listo para ser quemado.
     * Usar el cupón genera un X porciento de descuento al usuario invitado
     * <p></p>*/
    @Transactional
    public void persistUsage(UserInfo targetedUser, String invitationCode, InvitationMetadata invitation) {

        UserInfo invitingUser = userRepository.findByUuid(invitation.getInvitingUserId())
                .orElseThrow(() -> new SystemException(String.format(
                        "Inviting User with Id: `%s` was not found.", invitation.getInvitingUserId()
                )));

        String tokenKey = redisSpacePrefixes.INVITATION_CODE_PREFIX + invitationCode;
        String emailKey = redisSpacePrefixes.INVITATION_EMAIL_INDEX_PREFIX + targetedUser.getEmail();

        InvitationTrace trace = new InvitationTrace(
                uuidService.next(),
                invitingUser,
                targetedUser,
                invitationCode
        );
        referralTraceRepository.save(trace);

        cuponService.invitationCodeCupon(targetedUser);

        redisTemplate.delete(tokenKey);
        redisTemplate.delete(emailKey);
    }

    @Transactional
    public void burnCupon(InvitationTrace trace, BigDecimal amount) {

        trace.setBurnedAt(Instant.now());

        CreditTransaction transaction = creditsService.buildCommissionTransaction(trace, amount);

        creditsService.updateUserCredit(transaction);

        referralTraceRepository.save(trace);

    }
}
