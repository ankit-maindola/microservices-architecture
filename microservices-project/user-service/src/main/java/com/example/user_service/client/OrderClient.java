package com.example.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", configuration = FeignJwtConfig.class)
public interface OrderClient {
//    @GetMapping("/users/{id}")
//    UserResponse getUser(@PathVariable("id") Long id);
      @GetMapping("/api/order/placeOrder/{orderName}")
      String placeOrder(@PathVariable("orderName") String orderName);
}
