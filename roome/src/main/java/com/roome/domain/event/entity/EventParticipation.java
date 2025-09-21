package com.roome.domain.event.entity;

import com.roome.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_participation",
		uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "event_id"})}) // 중복 참여 방지
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventParticipation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private AutoEvent event;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public EventParticipation(User user, AutoEvent event, LocalDateTime createdAt) {
		this.user = user;
		this.event = event;
		this.createdAt = LocalDateTime.now();
	}
}
