package com.sparta.order.service;

import com.sparta.common.response.PageResponse;
import com.sparta.order.dto.request.CreateOrderRequest;
import com.sparta.order.dto.request.OrderSearchRequest;
import com.sparta.order.dto.request.UpdateOrderRequest;
import com.sparta.order.dto.request.UpdateOrderStatusRequest;
import com.sparta.order.dto.response.OrderResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, OrderServiceContext context);

    PageResponse<OrderResponse> getOrders(
            OrderSearchRequest request,
            Pageable pageable,
            OrderServiceContext context
    );

    OrderResponse getOrder(UUID orderId, OrderServiceContext context);

    OrderResponse updateOrder(
            UUID orderId,
            UpdateOrderRequest request,
            OrderServiceContext context
    );

    OrderResponse updateOrderStatus(
            UUID orderId,
            UpdateOrderStatusRequest request,
            OrderServiceContext context
    );

    OrderResponse cancelOrder(UUID orderId, OrderServiceContext context);

    void deleteOrder(UUID orderId, OrderServiceContext context);
}
