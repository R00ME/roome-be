package com.roome.domain.apiUsage.repository;

import com.roome.domain.apiUsage.entity.UserApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserApiUsageRepository extends JpaRepository<UserApiUsage, Long> {
    Optional<UserApiUsage> findByUserIdAndDomainAndApiUriAndDate(Long userId, String domain, String apiUrl, LocalDate date);
}
