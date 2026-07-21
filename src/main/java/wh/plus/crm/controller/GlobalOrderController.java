package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.order.OrderDTO;
import wh.plus.crm.service.OrderService;

import java.util.List;

/** Globalna lista zamówień ze wszystkich projektów. */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class GlobalOrderController {

    private final OrderService service;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> all() {
        return ResponseEntity.ok(service.listAll());
    }
}
