package com.roome.domain.event.dto;

import com.roome.domain.event.entity.AutoEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AutoEventResponse {

	private final Long id;
	private final String eventName;
	private final int rewardPoints;
	private final int maxParticipants;
	private final LocalDateTime eventTime;

	public static AutoEventResponse fromEntity(AutoEvent event) {
		return new AutoEventResponse(
				event.getId(),
				event.getEventName(),
				event.getRewardPoints(),
				event.getMaxParticipants(),
				event.getEventTime()
		);
	}
}
