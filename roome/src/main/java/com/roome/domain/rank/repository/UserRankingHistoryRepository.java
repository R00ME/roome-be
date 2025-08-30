package com.roome.domain.rank.repository;

import com.roome.domain.rank.entity.UserRankingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRankingHistoryRepository extends JpaRepository<UserRankingHistory, Long> {
}
