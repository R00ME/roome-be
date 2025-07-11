package com.roome.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends BaseTimeEntity {

	@CreatedBy
	@Column(updatable = false)  // 한번 저장되면 수정 불가
	private String createdBy;    // 생성자

	@LastModifiedBy
	private String updatedBy;   // 수정자
}
