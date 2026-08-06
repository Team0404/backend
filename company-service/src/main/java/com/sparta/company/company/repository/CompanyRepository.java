package com.sparta.company.company.repository;

import com.sparta.company.company.entity.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);
}
