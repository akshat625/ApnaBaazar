package com.apnabaazar.apnabaazar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @GetMapping
    public String testCustomer(){
        return "Hello World! from Customer";
    }
}
