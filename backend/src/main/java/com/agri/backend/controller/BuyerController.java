package com.agri.backend.controller;

import com.agri.backend.dto.ApiDtos.BuyerOrderView;
import com.agri.backend.exception.ApiException;
import com.agri.backend.model.Order;
import com.agri.backend.model.User;
import com.agri.backend.repository.OrderRepository;
import com.agri.backend.security.AuthInterceptor;
import com.agri.backend.service.OrderViewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The buyer's own orders. Seller identity is limited to the public shop name. */
@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final OrderRepository orderRepository;
    private final OrderViewService orderViews;

    public BuyerController(OrderRepository orderRepository, OrderViewService orderViews) {
        this.orderRepository = orderRepository;
        this.orderViews = orderViews;
    }

    @GetMapping("/orders")
    public List<BuyerOrderView> myOrders(HttpServletRequest http) {
        User buyer = AuthInterceptor.currentUser(http);
        return orderViews.buyerOrders(orderRepository.findByBuyerUserIdOrderByCreatedAtDesc(buyer.getId()));
    }

    @PostMapping("/orders/{id}/cancel")
    public BuyerOrderView cancel(@PathVariable Long id, HttpServletRequest http) {
        User buyer = AuthInterceptor.currentUser(http);
        Order order = orderRepository.findById(id).orElseThrow(() -> ApiException.notFound("That order was not found."));
        if (!buyer.getId().equals(order.getBuyerUserId())) throw ApiException.forbidden("That is not your order.");
        if (order.getStatus() != Order.Status.PENDING_PAYMENT) {
            throw ApiException.conflict("Only unpaid orders can be cancelled.");
        }
        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);
        return orderViews.buyerOrders(List.of(order)).get(0);
    }
}
