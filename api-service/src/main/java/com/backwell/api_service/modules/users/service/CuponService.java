package com.backwell.api_service.modules.users.service;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.invitations.config.InvitationDiscountProperties;
import com.backwell.api_service.modules.users.dto.*;
import com.backwell.api_service.modules.users.entity.UserInfo;
import com.backwell.api_service.modules.users.entity.cupon.Cupon;
import com.backwell.api_service.modules.users.entity.cupon.CuponType;
import com.backwell.api_service.modules.users.repo.CuponRepository;
import com.backwell.api_service.modules.users.repo.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.backwell.api_service.common.exception.codes.UserErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CuponService {
    private final CuponRepository cuponRepository;
    private final InvitationDiscountProperties invitationProperties;
    private final UUIDService uuidService;
    private final UserInfoRepository userInfoRepository;

    @Transactional
    public Cupon invitationCodeCupon(UserInfo targetUser) {
        String cuponName = String.format("Descuento por Invitación: %s", targetUser.getName());

        Cupon cupon = Cupon.builder()
                .id(uuidService.next())
                .name(cuponName)
                .type(CuponType.INVITATION)
                .user(targetUser)
                .decimalFactor(invitationProperties.getDecimalFactor())
                .build();
        return cuponRepository.save(cupon);
    }

    @Transactional
    public String giveCupon(CreateCuponRequest r) {
        List<UserInfo> targets = userInfoRepository.findWithIdIn(r.targets());

        if (targets.size() != r.targets().size()) {
            throw new BusinessException("Could not find all required user targets", USER_NOT_FOUND);
        }

        List<Cupon> cupons = targets.stream()
                .map(u-> Cupon.builder()
                        .id(uuidService.next())
                        .name(r.name())
                    .type(CuponType.PERCENTAGE)
                    .user(u)
                    .decimalFactor(r.getDecimalFactor())
                    .build())
                .toList();
        cuponRepository.saveAll(cupons);

        return String.format("Successfully added %d cupons", cupons.size());
    }
}
