package com.sparta.hub.repository;

import com.sparta.hub.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {
    List<Hub> findAllByDeletedAtIsNull();
    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);




}
