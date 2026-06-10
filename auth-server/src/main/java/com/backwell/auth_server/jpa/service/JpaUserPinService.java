package com.backwell.auth_server.jpa.service;

import com.backwell.auth_server.dto.request.UpdatePinRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.exception.pin.AccountLockedException;
import com.backwell.auth_server.exception.pin.InvalidPinException;
import com.backwell.auth_server.exception.pin.PinUniqueConstraintViolation;
import com.backwell.auth_server.exception.pin.PinVerificationInProgressException;
import com.backwell.auth_server.jpa.entity.User;
import com.backwell.auth_server.jpa.entity.UserPin;
import com.backwell.auth_server.jpa.repo.UserPinRepository;
import com.backwell.auth_server.jpa.repo.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class JpaUserPinService {
    private final PasswordEncoder passwordEncoder;
    private final UserPinRepository userPinRepository;
    private final UserRepository userRepository;

    private final ConcurrentHashMap<UUID, ReentrantLock> userLocks = new ConcurrentHashMap<>();


    @Transactional
    public MessageResponse setUpUserPin(@NotNull UUID userId, @NotNull String rawPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Requested User with Id: " + userId + " was not found."));

        if (userPinRepository.findForUserId(user.getId()).isPresent()) {
            throw new PinUniqueConstraintViolation("This user has already configured a pin.");
        }

        UserPin userPin = new UserPin(
                user,
                passwordEncoder.encode(rawPin)
        );
        userPinRepository.save(userPin);
        log.debug("Pin saved successfully.");
        return new MessageResponse("Pin was successfully set up.");
    }

    @Transactional
    public MessageResponse updateUserPin(@NotNull UUID userId, UpdatePinRequest req) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            throw new PinVerificationInProgressException("A verification is already in progress for this user.");
        }

        try {
            // Primero verificar el PIN actual
            UserPin userPin = verifyCurrentPin(userId, req.currentPin());

            // Si pasó la verificación, actualizar al nuevo PIN
            String newPinHash = passwordEncoder.encode(req.newPin());
            userPin.setPinHash(newPinHash);
            userPin.setFailedAttempts(0); // Resetear intentos fallidos al cambiar PIN
            userPin.setLockedUntil(null);  // Limpiar bloqueo si existía

            userPinRepository.save(userPin);

            log.info("PIN updated successfully for user: {}", userId);

            return new MessageResponse("PIN actualizado correctamente");

        } finally {
            lock.unlock();
            userLocks.remove(userId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {AccountLockedException.class, InvalidPinException.class})
    public void checkUserPin(@NotNull UUID userId, @NotNull String rawPin) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            throw new PinVerificationInProgressException("A verification is already in progress for this user.");
        }
        try {
            verifyCurrentPin(userId, rawPin);

        } finally {
            lock.unlock();
            userLocks.remove(userId);
        }
    }

    /**
     * Private method containing the common logic for verifying the current PIN
     * @param userId User ID
     * @param rawPin Plaintext PIN to verify
     * @return {@link UserPin} entity if verification is successful
     * @throws InvalidPinException if the PIN is incorrect
     * @throws AccountLockedException if the account is locked
     */
    private UserPin verifyCurrentPin(@NotNull UUID userId, @NotNull String rawPin) {
        // Obtener el PIN del usuario
        UserPin pin = userPinRepository.findForUserId(userId)
                .orElseThrow(() -> new InvalidPinException("User has no pin configured"));

        // Verificar si la cuenta está bloqueada
        Instant lockedUntil = pin.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            throw new AccountLockedException(
                    "Too many failed attempts. Account is locked until %s.".formatted(lockedUntil)
            );
        }

        // Verificar el PIN
        if (!passwordEncoder.matches(rawPin, pin.getPinHash())) {
            log.debug("PIN does not match for user: {}", userId);

            // Incrementar intentos fallidos
            pin.setFailedAttempts(pin.getFailedAttempts() + 1);

            // Verificar si se alcanzó el límite de intentos
            if (pin.getFailedAttempts() >= 3) {
                log.warn("Too many failed attempts for user: {}. Locking account.", userId);

                pin.setFailedAttempts(0);
                pin.setLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));

                userPinRepository.save(pin);
                throw new AccountLockedException(
                        "Too many failed attempts. Account is locked until %s".formatted(pin.getLockedUntil())
                );
            }

            userPinRepository.save(pin);
            throw new InvalidPinException("Invalid PIN");
        }

        // PIN correcto - resetear intentos fallidos si los había
        if (pin.getFailedAttempts() > 0 || pin.getLockedUntil() != null) {
            pin.setFailedAttempts(0);
            pin.setLockedUntil(null);
            userPinRepository.save(pin);
            log.debug("Reset failed attempts for user: {}", userId);
        }

        log.debug("PIN verified successfully for user: {}", userId);
        return pin;
    }
}
