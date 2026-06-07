package com.backwell.api_service.modules.users.controller;


import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.dto.MessageResponse;
import com.backwell.api_service.modules.users.dto.CreateCuponRequest;
import com.backwell.api_service.modules.users.dto.CuponPagedResponse;
import com.backwell.api_service.modules.users.dto.CuponSearchFilters;
import com.backwell.api_service.modules.users.dto.UserCuponDTO;
import com.backwell.api_service.modules.users.repo.CuponRepository;
import com.backwell.api_service.modules.users.service.CuponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cupon")
@RequiredArgsConstructor
@Slf4j
public class CuponController {
    private final CuponService cuponService;
    private final CuponRepository cuponRepository;

    //      Admin Methods
    @PostMapping("/")
    public ResponseEntity<MessageResponse> createCupons(
            HttpServletRequest request,
            CreateCuponRequest createCuponRequest
    ) {
        log.info("createCupon request:{}", createCuponRequest);
        String message = cuponService.giveCupon(createCuponRequest);
        MessageResponse response = new MessageResponse(
                message,
                request.getRequestURI(),
                200);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<CuponPagedResponse> searchCupons(@RequestBody @Valid CuponSearchFilters filters) {
        CuponPagedResponse response = cuponRepository.getCupons(filters);
        return ResponseEntity.ok(response);
    }


    //      Public Methods
    @GetMapping("/")
    public ResponseEntity<List<UserCuponDTO>> cuponsForUser(UserSession session) {
        var response = cuponRepository.findAvailableForUserId(session.uuid());
        return ResponseEntity.ok(response);
    }





}
