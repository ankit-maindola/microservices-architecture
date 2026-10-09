package com.example.order_service.controlller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    @GetMapping("/placeOrder/{orderName}")
    String placeOrder(@PathVariable("orderName") String orderName){
        return "Order "+orderName+ " is placed SuccessFully .";
    }
}
