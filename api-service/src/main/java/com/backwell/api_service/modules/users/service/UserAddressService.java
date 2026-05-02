package com.backwell.api_service.modules.users.service;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.YouAreAnIdiotException;
import com.backwell.api_service.modules.users.dto.CreateAddressDTO;
import com.backwell.api_service.modules.users.dto.UpdateAddressRequest;
import com.backwell.api_service.modules.users.dto.UserAddressDTO;
import com.backwell.api_service.modules.users.entity.UserAddress;
import com.backwell.api_service.modules.users.repo.UserAddressRepository;
import com.backwell.api_service.modules.users.repo.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.backwell.api_service.common.exception.codes.UserErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressRepository userAddressRepository;
    private final UserInfoRepository userInfoRepository;

    @Transactional
    public List<UserAddressDTO> list(UserSession userSession) {
        return refetchUserAddresses(userSession);
    }

    @Transactional
    public List<UserAddressDTO> addAddress(UserSession session, CreateAddressDTO dto) {
        List<UserAddress> userAddresses = new ArrayList<>(userAddressRepository.findByUser_Uuid(session.uuid())
                .stream()
                .sorted(Comparator.comparingInt(UserAddress::getSlotIndex))
                .toList()
        );

        if (userAddresses.size() >= 4) {
            throw new BusinessException("Máximo de 4 direcciones alcanzado.", MAX_ADDRESS_LIMIT_REACHED.name());
        }

        int targetIndex = (dto.slotIndex() == null) ? userAddresses.size() : dto.slotIndex();
        if (targetIndex < 0 || targetIndex > 3) throw new IllegalArgumentException("Índice inválido.");

        UserAddress newAddress = dto.toEntity();
        newAddress.setUser(userInfoRepository.getOrThrow(session));


        int finalPos = Math.min(targetIndex, userAddresses.size());
        userAddresses.add(finalPos, newAddress);

        for (int i = 0; i < userAddresses.size(); i++) {
            userAddresses.get(i).setSlotIndex(i);
        }

        userAddressRepository.saveAll(userAddresses);
        return refetchUserAddresses(session);
    }

    @Transactional
    public List<UserAddressDTO> updateAddress(UserSession session, UpdateAddressRequest request) {
        UserAddress target = userAddressRepository.findByUser_UuidAndSlotIndex(session.uuid(), request.slotIndex())
                .orElseThrow(() -> new BusinessException("No Address was found for current ID", ADDRESS_NOT_FOUND.name()));

        if (request.addressName() != null) {
            target.setInternalName(request.addressName());
        }

        if (request.googleAddressDTO() != null) {
            target.setGoogleAddress(request.googleAddressDTO().toEntity());
        }

        userAddressRepository.saveAndFlush(target);
        return refetchUserAddresses(session);
    }

    @Transactional
    public List<UserAddressDTO> deleteAddress(UserSession session, int slotIndex) {
        if (slotIndex < 0 || slotIndex > 3) {
            throw new IllegalArgumentException("Invalid index");
        }

        int affectedRows = userAddressRepository.deleteByUser_UuidAndSlotIndex(session.uuid(), slotIndex);
        if (affectedRows == 0) {
            throw new BusinessException("Requested deletion was not found. No changes were made.", ADDRESS_NOT_FOUND.name());
        }

        // Get and Reindex
        List<UserAddress> addresses = new ArrayList<>(userAddressRepository.findByUser_Uuid(session.uuid())
                .stream()
                .sorted(Comparator.comparingInt(UserAddress::getSlotIndex))
                .toList());
        for(int i = 0; i < addresses.size(); i++) {
            addresses.get(i).setSlotIndex(i);
        }
        userAddressRepository.saveAll(addresses);
        return refetchUserAddresses(session);
    }

    @Transactional
    public List<UserAddressDTO> reorderAddress(UserSession session, int slotIndex, int newSlotIndex) {
        List<UserAddress> addresses = new ArrayList<>(userAddressRepository.findByUser_Uuid(session.uuid())
                .stream()
                .sorted(Comparator.comparingInt(UserAddress::getSlotIndex))
                .toList());

        UserAddress addressToMove = addresses
                .stream()
                .filter(a->a.getSlotIndex() == slotIndex)
                .findFirst()
                .orElseThrow(()-> new IllegalArgumentException("Address not saved yet"));

        addresses.remove(addressToMove);

        int targetPos = Math.min(newSlotIndex, addresses.size());
        addresses.add(targetPos, addressToMove);

        for (int i = 0; i < addresses.size(); i++) {
            addresses.get(i).setSlotIndex(i);
        }

        userAddressRepository.saveAllAndFlush(addresses);
        return refetchUserAddresses(session);
    }


    private List<UserAddressDTO> refetchUserAddresses(UserSession session) {
        return userAddressRepository.fetchForUser(session.uuid()).stream()
                .sorted(Comparator.comparingInt(UserAddressDTO::slotIndex))
                .toList();
    }
}