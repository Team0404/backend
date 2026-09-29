package com.sparta.delivery.service;

import com.sparta.delivery.domain.entity.DeliveryManager;
import com.sparta.delivery.domain.entity.DeliveryManagerCursor;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import com.sparta.delivery.repository.DeliveryManagerCursorRepository;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoundRobinManager {
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryManagerCursorRepository deliveryManagerCursorRepository;

    /**
     * 라운드로빈 담당자 배정: 그룹(HUB=전체 / COMPANY=해당 hubId)에서 sequence 이용
     * 반환값 = 배정된 담당자 userId.
     */
    @Transactional
    public UUID assignNextManager(DeliveryManagerType type, UUID hubId) {
        UUID managerHubId = type == DeliveryManagerType.HUB ? null : hubId;
        UUID cursorHubId = type == DeliveryManagerType.HUB ? DeliveryManagerCursor.GLOBAL_HUB_ID : hubId;

        List<DeliveryManager> dmList = deliveryManagerRepository
                .findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(type, managerHubId);
        if (dmList.isEmpty()) {
            return null;
        }

        DeliveryManagerCursor savedCursor = deliveryManagerCursorRepository
                .findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(type, cursorHubId)
                .orElseGet(() -> deliveryManagerCursorRepository.save(DeliveryManagerCursor.builder()
                        .type(type)
                        .hubId(cursorHubId)
                        .lastAssignedManagerId(null)
                        .build()));

        int lastIndex = -1;
        for (int i = 0; i < dmList.size(); i++) {
            if (dmList.get(i).getUserId().equals(savedCursor.getLastAssignedManagerId())) {
                lastIndex = i;
                break;
            }
        }

        UUID nextManagerId = dmList.get((lastIndex + 1) % dmList.size()).getUserId();
        savedCursor.updateLastManager(nextManagerId);
        return nextManagerId;
    }
}
