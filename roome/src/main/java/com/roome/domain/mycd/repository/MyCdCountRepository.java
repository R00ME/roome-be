package com.roome.domain.mycd.repository;

import com.roome.domain.mycd.entity.MyCdCount;
import com.roome.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MyCdCountRepository extends JpaRepository<MyCdCount, Long> {

	Optional<MyCdCount> findByRoom(Room room);
}
