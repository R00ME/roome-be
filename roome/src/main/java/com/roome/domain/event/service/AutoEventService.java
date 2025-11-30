package com.roome.domain.event.service;

import com.roome.domain.event.entity.AutoEvent;
import com.roome.domain.event.entity.EventParticipation;
import com.roome.domain.event.entity.EventStatus;
import com.roome.domain.event.exception.AlreadyParticipatedException;
import com.roome.domain.event.exception.EventFullException;
import com.roome.domain.event.exception.EventNotFoundException;
import com.roome.domain.event.exception.EventNotStartedException;
import com.roome.domain.event.repository.AutoEventRepository;
import com.roome.domain.event.repository.EventParticipationRepository;
import com.roome.domain.point.entity.PointReason;
import com.roome.domain.point.service.PointService;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.security.jwt.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoEventService {

	private final AutoEventRepository autoEventRepository;
	private final EventParticipationRepository eventParticipationRepository;
	private final UserRepository userRepository;
	private final PointService pointService;

    @Transactional
    public void joinEvent(Long userId, Long eventId) {
        log.info("👉 [이벤트 참여 요청] userId={}, eventId={}", userId, eventId);

        AutoEvent event = autoEventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("❌ 이벤트 없음: eventId={}", eventId);
                    return new EventNotFoundException();
                });
        log.info("✅ 이벤트 조회 성공: eventId={}, status={}, eventTime={}",
                event.getId(), event.getStatus(), event.getEventTime());

        if (!event.isEventOpen()) {
            log.warn("❌ 이벤트 아직 시작 전: now={}, eventTime={}",
                    LocalDateTime.now(), event.getEventTime());
            throw new EventNotStartedException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ 유저 없음: userId={}", userId);
                    return new UserNotFoundException();
                });
        log.info("✅ 유저 조회 성공: userId={}", user.getId());

        if (eventParticipationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            log.warn("❌ 중복 참여 시도: userId={}, eventId={}", userId, eventId);
            throw new AlreadyParticipatedException();
        }

        long participantCount = eventParticipationRepository.countByEventId(eventId);
        log.info("현재 참여자 수={}, 최대 참여 가능 인원={}", participantCount, event.getMaxParticipants());
        if (participantCount >= event.getMaxParticipants()) {
            log.warn("❌ 정원 초과: userId={}, eventId={}", userId, eventId);
            throw new EventFullException();
        }

        eventParticipationRepository.save(new EventParticipation(user, event, LocalDateTime.now()));
        log.info("✅ 이벤트 참여 저장 완료: userId={}, eventId={}", user.getId(), event.getId());

        pointService.earnPoints(user, PointReason.FIRST_COME_EVENT);
        log.info("✅ 포인트 지급 완료: userId={}, reason={}", user.getId(), PointReason.FIRST_COME_EVENT);
    }

	@Transactional(readOnly = true)
	public AutoEvent getOngoingEvent() {
		return autoEventRepository.findTopByStatusOrderByEventTimeDesc(EventStatus.ONGOING)
				.orElseThrow(EventNotFoundException::new);
	}
}
