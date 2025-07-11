package com.roome.domain.notification.dto;

import com.roome.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateNotificationRequest {
	private Long senderId;
	private Long targetId;
	private Long receiverId;
	private NotificationType type;
}
