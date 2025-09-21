package com.roome.domain.cdtemplate.repository;

import com.roome.domain.cdtemplate.entity.CdTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CdTemplateRepository extends JpaRepository<CdTemplate, Long> {

	Optional<CdTemplate> findByMyCdId(Long myCdId);

	boolean existsByMyCdId(Long myCdId); // 존재 여부 확인

	Long countByUserId(Long userId); // 특정 사용자의 템플릿 개수 조회

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CdTemplate t WHERE t.myCd.id IN :myCdIds")
    void deleteByMyCdIdIn(List<Long> myCdIds);
}
