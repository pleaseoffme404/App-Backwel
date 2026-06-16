package com.backwell.api_service.modules.sales.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/point-of-sale")
@RequiredArgsConstructor
public class PointOfSaleController {

    @PostMapping("/userless-order")
    public ResponseEntity<?> placeUserlessOrder() {
        throw new UnsupportedOperationException();
    }

    @PostMapping("/sale")
    public ResponseEntity<?> placeIdentifiedSale() {
        throw new UnsupportedOperationException();
    }
}
