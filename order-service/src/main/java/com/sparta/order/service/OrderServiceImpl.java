package com.sparta.order.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.order.client.CompanyClient;
import com.sparta.order.client.DeliveryClient;
import com.sparta.order.client.ProductClient;
import com.sparta.order.client.dto.CompanyResponse;
import com.sparta.order.client.dto.DeliveryCreateRequest;
import com.sparta.order.client.dto.DeliveryCreateResponse;
import com.sparta.order.client.dto.ProductResponse;
import com.sparta.order.dto.request.CreateOrderRequest;
import com.sparta.order.dto.request.OrderSearchRequest;
import com.sparta.order.dto.request.UpdateOrderRequest;
import com.sparta.order.dto.request.UpdateOrderStatusRequest;
import com.sparta.order.dto.response.OrderResponse;
import com.sparta.order.entity.Order;
import com.sparta.order.entity.OrderItem;
import com.sparta.order.entity.OrderStatus;
import com.sparta.order.exception.OrderErrorCode;
import com.sparta.order.repository.OrderRepository;
import com.sparta.order.repository.query.OrderSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final CompanyClient companyClient;
    private final DeliveryClient deliveryClient;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, OrderServiceContext context) {
        validateCreatePermission(context.userRole());

        //업체 조회
        CompanyResponse company = requireData(
                companyClient.getCompany(request.companyId()),
                "업체 정보를 조회할 수 없습니다."
        );
        validateSupplierCompanyAccess(context, company.id());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .companyId(company.id())
                .hubId(company.hubId())
                .requestNote(request.requestNote())
                .deliveryDeadline(request.deliveryDeadline())
                .status(OrderStatus.READY)
                .build();

        UUID originHubId = null;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.orderItems()) {
            //상품 조회
            ProductResponse product = requireData(
                    productClient.getProduct(itemRequest.productId()),
                    "상품 정보를 조회할 수 없습니다."
            );

            //재고 처리
            validateStock(product, itemRequest.quantity());
            productClient.decreaseStock(itemRequest.productId(), itemRequest.quantity());

            if (originHubId == null) {
                originHubId = product.hubId();
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .quantity(itemRequest.quantity())
                    .build();

            //OrderItem 연결
            order.addOrderItem(orderItem);
        }

        //Order 생성
        Order savedOrder = orderRepository.save(order);

        //배송 생성
        DeliveryCreateResponse delivery = requireData(
                deliveryClient.createDelivery(new DeliveryCreateRequest(
                        savedOrder.getId(),
                        originHubId,
                        company.hubId(),
                        company.address(),
                        company.name(),
                        null
                )),
                "배송 생성에 실패했습니다."
        );

        savedOrder.setDeliveryId(delivery.deliveryId());

        return OrderResponse.from(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(
            OrderSearchRequest request,
            Pageable pageable,
            OrderServiceContext context
    ) {
        validateReadPermission(context.userRole());

        Pageable resolvedPageable = OrderSearchPolicy.resolvePageable(request, pageable);
        OrderSearchCriteria criteria = OrderSearchPolicy.toCriteria(request, context);

        Page<OrderResponse> page = orderRepository.searchOrders(criteria, resolvedPageable)
                .map(OrderResponse::from);

        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, OrderServiceContext context) {
        validateReadPermission(context.userRole());

        return OrderResponse.from(getAccessibleOrder(orderId, context));
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(
            UUID orderId,
            UpdateOrderRequest request,
            OrderServiceContext context
    ) {
        validateUpdatePermission(context.userRole());

        Order order = getAccessibleOrder(orderId, context);
        validateUpdatableStatus(order);

        order.updateDetails(request.requestNote(), request.deliveryDeadline());

        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(
            UUID orderId,
            UpdateOrderStatusRequest request,
            OrderServiceContext context
    ) {
        validateStatusUpdatePermission(context.userRole());

        Order order = getAccessibleOrder(orderId, context);
        validateStatusChange(order, request.status());

        order.updateStatus(request.status());

        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, OrderServiceContext context) {
        validateCancelPermission(context.userRole());

        Order order = getAccessibleOrder(orderId, context);
        validateCancelableStatus(order);

        for (OrderItem orderItem : order.getOrderItems()) {
            productClient.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
        }

        if (order.getDeliveryId() != null) {
            deliveryClient.cancelDelivery(order.getDeliveryId());
        }

        order.updateStatus(OrderStatus.CANCELLED);

        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public void deleteOrder(UUID orderId, OrderServiceContext context) {
        validateDeletePermission(context.userRole());

        Order order = getAccessibleOrder(orderId, context);
        order.softDelete(context.requestUserId());
    }

    private <T> T requireData(ApiResponse<T> response, String message) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, message);
        }
        return response.getData();
    }

    private void validateStock(ProductResponse product, Integer quantity) {
        if (product.stockQuantity() == null || product.stockQuantity() < quantity.longValue()) {
            throw new BusinessException(OrderErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private Order getAccessibleOrder(UUID orderId, OrderServiceContext context) {
        OrderSearchCriteria criteria = new OrderSearchCriteria(
                null,
                null,
                context.requestUserId(),
                context.requestHubId(),
                context.requestCompanyId(),
                context.requestDeliveryManagerId(),
                context.userRole()
        );

        return orderRepository.findDetailById(criteria, orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ACCESSIBLE_ORDER_NOT_FOUND));
    }

    private void validateCreatePermission(UserRole userRole) {
        if (userRole != UserRole.MASTER
                && userRole != UserRole.HUB_MANAGER
                && userRole != UserRole.SUPPLIER_MANAGER) {
            throw new BusinessException(OrderErrorCode.FORBIDDEN_CREATE_ORDER);
        }
    }

    private void validateReadPermission(UserRole userRole) {
        if (userRole == null) {
            throw new BusinessException(OrderErrorCode.FORBIDDEN_READ_ORDER);
        }
    }

    private void validateUpdatePermission(UserRole userRole) {
        if (userRole != UserRole.MASTER
                && userRole != UserRole.HUB_MANAGER
                && userRole != UserRole.SUPPLIER_MANAGER) {
            throw new BusinessException(OrderErrorCode.FORBIDDEN_UPDATE_ORDER);
        }
    }

    private void validateStatusUpdatePermission(UserRole userRole) {
        if (userRole != UserRole.MASTER && userRole != UserRole.HUB_MANAGER) {
            throw new BusinessException(OrderErrorCode.FORBIDDEN_UPDATE_ORDER_STATUS);
        }
    }

    private void validateCancelPermission(UserRole userRole) {
        if (userRole != UserRole.MASTER
                && userRole != UserRole.HUB_MANAGER
                && userRole != UserRole.SUPPLIER_MANAGER) {
            throw new BusinessException(OrderErrorCode.FORBIDDEN_CANCEL_ORDER);
        }
    }

    private void validateDeletePermission(UserRole userRole) {
        if (userRole != UserRole.MASTER && userRole != UserRole.HUB_MANAGER) {
            throw new BusinessException(OrderErrorCode.FORBIDDEN_DELETE_ORDER);
        }
    }

    private void validateUpdatableStatus(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_UPDATABLE);
        }
    }

    private void validateStatusChange(Order order, OrderStatus nextStatus) {
        if (nextStatus == null) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_REQUIRED);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_NOT_CHANGEABLE);
        }
        if (!isAllowedTransition(order.getStatus(), nextStatus)) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
    }

    private void validateCancelableStatus(Order order) {
        if (order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_CANCELABLE);
        }
    }

    private void validateSupplierCompanyAccess(OrderServiceContext context, UUID companyId) {
        if (context.userRole() == UserRole.SUPPLIER_MANAGER
                && context.requestCompanyId() != null
                && !context.requestCompanyId().equals(companyId)) {
            throw new BusinessException(OrderErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
    }

    private boolean isAllowedTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return true;
        }

        return switch (current) {
            case READY -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED || next == OrderStatus.FAILED;
            case PENDING ->
                    next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED || next == OrderStatus.FAILED;
            case CONFIRMED ->
                    next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED || next == OrderStatus.FAILED;
            case PREPARING ->
                    next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED || next == OrderStatus.FAILED;
            case SHIPPING -> next == OrderStatus.COMPLETED || next == OrderStatus.FAILED;
            case COMPLETED, CANCELLED, FAILED -> false;
        };
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
