package com.roome.domain.apiUsage.repository;

import com.roome.domain.apiUsage.entity.UserApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserApiUsageRepository extends JpaRepository<UserApiUsage, Long> {
}
