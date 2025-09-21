package com.roome.domain.event.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auto_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutoEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String eventName; // 이벤트 이름

	@Column(nullable = false)
	private int rewardPoints; // 보상 포인트

	@Column(nullable = false)
	private int maxParticipants; // 최대 참여 인원

	@Column(nullable = false)
	private LocalDateTime eventTime; // 이벤트 시작 시간

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventStatus status;

	@OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<EventParticipation> participants = new ArrayList<>();

	@Builder
	public AutoEvent(String eventName, int rewardPoints, int maxParticipants,
                     LocalDateTime eventTime, EventStatus status) {
		this.eventName = eventName;
		this.rewardPoints = rewardPoints;
		this.maxParticipants = maxParticipants;
		this.eventTime = eventTime;
		this.status = status;
	}

	public boolean isEventOpen() {
		return LocalDateTime.now().isAfter(eventTime);
	}

	public void endEvent() {
		this.status = EventStatus.ENDED;
	}
}

