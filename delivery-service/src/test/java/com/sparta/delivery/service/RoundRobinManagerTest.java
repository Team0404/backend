package com.sparta.delivery.service;

import com.sparta.delivery.domain.entity.DeliveryManager;
import com.sparta.delivery.domain.entity.DeliveryManagerCursor;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import com.sparta.delivery.repository.DeliveryManagerCursorRepository;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoundRobinManagerTest {

    private DeliveryManagerRepository deliveryManagerRepository;
    private DeliveryManagerCursorRepository deliveryManagerCursorRepository;
    private RoundRobinManager roundRobinManager;

    @BeforeEach
    void setUp() {
        deliveryManagerRepository = mock(DeliveryManagerRepository.class);
        deliveryManagerCursorRepository = mock(DeliveryManagerCursorRepository.class);
        roundRobinManager = new RoundRobinManager(deliveryManagerRepository, deliveryManagerCursorRepository);

        when(deliveryManagerCursorRepository.save(any(DeliveryManagerCursor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assignNextManager_cyclesThroughGroupInSequenceOrder() {
        UUID hubId = UUID.randomUUID();
        UUID managerA = UUID.randomUUID();
        UUID managerB = UUID.randomUUID();
        UUID managerC = UUID.randomUUID();

        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of(companyManager(managerA, hubId, 0), companyManager(managerB, hubId, 1), companyManager(managerC, hubId, 2)));

        DeliveryManagerCursor cursor = DeliveryManagerCursor.builder()
                .type(DeliveryManagerType.COMPANY).hubId(hubId).lastAssignedManagerId(managerB)
                .build();
        when(deliveryManagerCursorRepository.findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(Optional.of(cursor));

        UUID next = roundRobinManager.assignNextManager(DeliveryManagerType.COMPANY, hubId);

        assertThat(next).isEqualTo(managerC);
        assertThat(cursor.getLastAssignedManagerId()).isEqualTo(managerC);
    }

    @Test
    void assignNextManager_emptyGroup_returnsNull() {
        UUID hubId = UUID.randomUUID();
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of());

        UUID next = roundRobinManager.assignNextManager(DeliveryManagerType.COMPANY, hubId);

        assertThat(next).isNull();
    }

    @Test
    void assignNextManager_noCursorYet_assignsFirstInSequence() {
        UUID hubId = UUID.randomUUID();
        UUID managerA = UUID.randomUUID();
        UUID managerB = UUID.randomUUID();

        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of(companyManager(managerA, hubId, 0), companyManager(managerB, hubId, 1)));
        when(deliveryManagerCursorRepository.findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(Optional.empty());

        UUID next = roundRobinManager.assignNextManager(DeliveryManagerType.COMPANY, hubId);

        assertThat(next).isEqualTo(managerA);
    }

    @Test
    void assignNextManager_lastInSequence_wrapsAroundToFirst() {
        UUID hubId = UUID.randomUUID();
        UUID managerA = UUID.randomUUID();
        UUID managerB = UUID.randomUUID();

        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of(companyManager(managerA, hubId, 0), companyManager(managerB, hubId, 1)));

        DeliveryManagerCursor cursor = DeliveryManagerCursor.builder()
                .type(DeliveryManagerType.COMPANY).hubId(hubId).lastAssignedManagerId(managerB)
                .build();
        when(deliveryManagerCursorRepository.findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(Optional.of(cursor));

        UUID next = roundRobinManager.assignNextManager(DeliveryManagerType.COMPANY, hubId);

        assertThat(next).isEqualTo(managerA);
    }

    @Test
    void assignNextManager_hubType_usesGlobalCursorHubId() {
        UUID originHubId = UUID.randomUUID();
        UUID managerA = UUID.randomUUID();

        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.HUB, null))
                .thenReturn(List.of(companyManager(managerA, null, 0)));
        when(deliveryManagerCursorRepository.findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(
                DeliveryManagerType.HUB, DeliveryManagerCursor.GLOBAL_HUB_ID))
                .thenReturn(Optional.empty());

        UUID next = roundRobinManager.assignNextManager(DeliveryManagerType.HUB, originHubId);

        assertThat(next).isEqualTo(managerA);
    }

    private DeliveryManager companyManager(UUID userId, UUID hubId, int sequence) {
        return DeliveryManager.builder()
                .userId(userId).type(DeliveryManagerType.COMPANY).hubId(hubId).sequence(sequence)
                .build();
    }
}
