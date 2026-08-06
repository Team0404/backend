package com.sparta.delivery.security;

import com.sparta.common.entity.UserRole;

import java.util.UUID;

public record AuthUser(
        UUID userId,
        String username,
        UserRole role
) {

    public boolean isMaster() {
        return UserRole.MASTER.equals(role);
    }

    public boolean isHubManager() {
        return UserRole.HUB_MANAGER.equals(role);
    }

    public boolean isDeliveryManager() {
        return UserRole.DELIVERY_MANAGER.equals(role);
    }

    public boolean isSupplierManager() {
        return UserRole.SUPPLIER_MANAGER.equals(role);
    }
}