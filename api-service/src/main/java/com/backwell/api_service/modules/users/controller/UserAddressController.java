package com.backwell.api_service.modules.users.controller;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.YouAreAnIdiotException;
import com.backwell.api_service.modules.users.dto.CreateAddressDTO;
import com.backwell.api_service.modules.users.dto.UpdateAddressRequest;
import com.backwell.api_service.modules.users.dto.UserAddressDTO;
import com.backwell.api_service.modules.users.entity.UserAddress;
import com.backwell.api_service.modules.users.service.UserAddressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/user/address")
@RequiredArgsConstructor
@Slf4j
public class UserAddressController {
    private final UserAddressService addressService;

    @GetMapping("/")
    public ResponseEntity<List<UserAddressDTO>> listUserAddress(UserSession session) {
        return ResponseEntity.ok(addressService.list(session));
    }

    @PostMapping("/")
    public ResponseEntity<List<UserAddressDTO>> addAddress(
            UserSession userSession,
            @RequestBody @Valid CreateAddressDTO createAddressDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.addAddress(userSession, createAddressDTO));
    }

    @PatchMapping("/")
    public ResponseEntity<List<UserAddressDTO>> updateAddress(
            UserSession session,
            @RequestBody @Valid UpdateAddressRequest request
    ) {
        return ResponseEntity.ok(addressService.updateAddress(session, request));
    }

    @DeleteMapping("/")
    public ResponseEntity<List<UserAddressDTO>> deleteAddress(
            UserSession session,
            @RequestParam @NotNull @Min(0) @Max(3) Integer slotIndex
    ) {
        return ResponseEntity.ok(addressService.deleteAddress(session, slotIndex));
    }

    @PostMapping("/reorder")
    public ResponseEntity<List<UserAddressDTO>> reorderAddress(
            UserSession session,
            @RequestParam @NotNull @Min(0) @Max(3) Integer slotIndex,
            @RequestParam @NotNull @Min(0) @Max(3) Integer newSlotIndex
    ) {
        if (Objects.equals(slotIndex, newSlotIndex)) {
            throw new YouAreAnIdiotException("Stupid Query :)");
        }

        return ResponseEntity.ok(addressService.reorderAddress(session, slotIndex, newSlotIndex));
    }
}
