package com.roome.domain.point.repository;

import com.roome.domain.point.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointRepository extends JpaRepository<Point, Long> {

	Optional<Point> findByUserId(Long userId);
}
