package com.backwell.api_service.modules.users.service;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.products.jpa.entity.cart.Cart;
import com.backwell.api_service.modules.products.jpa.entity.cart.SavedLaterList;
import com.backwell.api_service.modules.products.jpa.entity.cart.WishList;
import com.backwell.api_service.modules.products.jpa.repo.WishListRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.CartRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.SavedLaterListRepository;
import com.backwell.api_service.modules.users.dto.CompleteAccountRequest;
import com.backwell.api_service.modules.users.dto.UpdateUserInfoRequest;
import com.backwell.api_service.modules.users.dto.UserInfoDTO;
import com.backwell.api_service.modules.users.entity.UserInfo;
import com.backwell.api_service.modules.users.repo.UserInfoRepository;
import com.backwell.enums.RoleName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static com.backwell.api_service.common.exception.codes.UserErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoService {
    private final UserInfoRepository userInfoRepository;
    private final CartRepository cartRepository;
    private final SavedLaterListRepository savedLaterListRepository;
    private final WishListRepository wishListRepository;
    private final UUIDService uuidService;

    @Transactional
    public UserInfoDTO completeAccount(
            UserSession session,
            CompleteAccountRequest req
    ) {
        if (userInfoRepository.existsByUuid(session.uuid()) || userInfoRepository.existsByEmail(session.email())) {
            throw new BusinessException("This Account has already been completed.", ACCOUNT_COMPLETED.name());
        }

        // This fucking method already manages the first address creation for an easy persistence
        UserInfo userInfo = UserInfo.from(req, session);

        UserInfo saved = userInfoRepository.saveAndFlush(userInfo);
        createUserDependencies(saved);
        return buildDTO(saved, session);
    }


    @Transactional
    public UserInfoDTO updateAccount(
            UserSession session,
            UpdateUserInfoRequest req
    ) {
        UserInfo current = userInfoRepository.getOrThrow(session);

        if (req.name() != null) {
            current.setName(req.name());
        }

        if (req.surname() != null) {
            current.setSurname(req.surname());
        }

        if (req.phoneNumber() != null) {
            current.setPhoneNumber(req.phoneNumber());
        }

        if (req.pictureUrl() != null) {
            current.setPictureUrl(req.pictureUrl());
        }

        UserInfo saved = userInfoRepository.save(current);
        return buildDTO(saved, session);
    }

    public UserInfoDTO info(UserSession session) {
        UserInfo current = userInfoRepository.getOrThrow(session);
        return  buildDTO(current, session);
    }

    private UserInfoDTO buildDTO (UserInfo userInfo, UserSession session) {
        String role = RoleName.getHighestRole(session.roles()).name();
        return UserInfoDTO.fromEntity(userInfo, role);
    }

    private void createUserDependencies(UserInfo savedUser) {
        // Create the user's cart
        cartRepository.save(Cart.initForUser(uuidService.next(), savedUser));

        // Create the user's saved later list
        savedLaterListRepository.save(SavedLaterList.initForUser(uuidService.next(), savedUser));

        // Create the user's default wish list
        wishListRepository.save(WishList.initForUser(uuidService.next(), savedUser));
    }
}
