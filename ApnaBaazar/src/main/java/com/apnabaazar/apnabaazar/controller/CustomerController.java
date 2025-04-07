package com.apnabaazar.apnabaazar.controller;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @GetMapping("/hello")
    public String testCustomer(){
        return "Hello World! from Customer";


    }
}
