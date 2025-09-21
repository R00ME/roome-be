package com.roome.domain.event.repository;

import com.roome.domain.event.entity.EventStatus;
import com.roome.domain.event.entity.AutoEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoEventRepository extends JpaRepository<AutoEvent, Long> {

	List<AutoEvent> findByStatus(EventStatus status);

	// 진행 중인 가장 최신 이벤트 조회
	Optional<AutoEvent> findTopByStatusOrderByEventTimeDesc(EventStatus status);

}

