package com.roome.domain.cd.repository;

import com.roome.domain.cd.entity.Cd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CdRepository extends JpaRepository<Cd, Long> {

	Optional<Cd> findByTitleAndArtist(String title, String artist);

}
