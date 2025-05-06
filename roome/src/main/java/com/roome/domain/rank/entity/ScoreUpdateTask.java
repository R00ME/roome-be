package com.roome.domain.rank.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_update_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ScoreUpdateTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private int score;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status;

	@Column(nullable = false)
	private int retryCount;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime updatedAt;

	// 새 task를 위한 생성자
	public ScoreUpdateTask(Long userId, int score) {
		this.userId = userId;
		this.score = score;
		this.status = TaskStatus.PENDING;
		this.retryCount = 0;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	public void incrementRetryCount() {
		this.retryCount++;
		this.updatedAt = LocalDateTime.now();
	}
}
