package com.backwell.api_service.modules.sales.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales")
public class SalesController {

    @GetMapping("/list")
    public ResponseEntity<?> listSales(){
        //todo finish implementation
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
