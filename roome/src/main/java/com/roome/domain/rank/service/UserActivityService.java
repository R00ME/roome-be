package com.roome.domain.rank.service;

import com.roome.domain.rank.entity.ActivityType;
import com.roome.domain.rank.entity.UserActivity;
import com.roome.domain.rank.repository.UserActivityRepository;
import com.roome.domain.room.entity.Room;
import com.roome.domain.room.repository.RoomRepository;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityService {

	private final RedisService redisService;
	private final RedisTemplate<String, Object> redisTemplate;
	private final UserActivityRepository userActivityRepository;
	private final UserRepository userRepository;
	private final RoomRepository roomRepository;

	// 사용자 활동 기록 및 점수 부여
	@Transactional
	public boolean recordUserActivity(Long userId, ActivityType activityType, Long relatedEntityId,
									  Integer contentLength) {

		// 콘텐츠 길이 체크
		if (contentLength != null) {
			if (activityType == ActivityType.BOOK_REVIEW && contentLength < 30) {
				log.info("서평 길이 제한 미달: 유저={}, 길이={}", userId, contentLength);
				return false;
			} else if (activityType == ActivityType.GUESTBOOK && contentLength < 15) {
				log.info("방명록 길이 제한 미달: 유저={}, 길이={}", userId, contentLength);
				return false;
			}
		}

		String lockKey = "lock:user:activity:" + userId;
		return redisService.executeWithLock(lockKey, 200, 2000, () -> {
			// 제한 조건 체크
			if (!checkDailyLimit(userId, activityType, relatedEntityId)) {
				log.info("일일 한도 초과: 유저={}, 활동={}", userId, activityType);
				return false;
			}

			// 사용자 정보 조회
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

			// 활동 기록 및 점수 부여
			UserActivity activity = new UserActivity();
			activity.setUser(user);
			activity.setActivityType(activityType);
			activity.setCreatedAt(LocalDateTime.now());
			activity.setScore(activityType.getScore());
			activity.setRelatedEntityId(relatedEntityId);
			userActivityRepository.save(activity);

			// Redis에 점수 업데이트
			updateRedisScore(userId, activityType.getScore());

			// 일일 활동 카운트 증가
			incrementDailyActivityCount(userId, activityType);

			log.info("활동 기록 완료: 유저={}, 활동={}, 점수={}", userId, activityType, activityType.getScore());
			return true;
		});
	}

	// 콘텐츠 길이가 필요 없는 활동
	@Transactional
	public boolean recordUserActivity(Long userId, ActivityType activityType, Long relatedEntityId) {
		return recordUserActivity(userId, activityType, relatedEntityId, null);
	}

	// 사용자 팔로우 활동 기록
	@Transactional
	public boolean recordFollowActivity(Long followerId, Long followingId) {
		// 본인 팔로우 제외
		if (followerId.equals(followingId)) {
			log.info("본인 팔로우 제외: followerId={}, followingId={}", followerId, followingId);
			return false;
		}

		try {
			// 팔로잉 받은 사람이 존재하는지 확인
			User followingUser = userRepository.findById(followingId)
					.orElseThrow(() -> new IllegalArgumentException("팔로잉 대상 사용자를 찾을 수 없습니다: " + followingId));

			// 팔로워가 존재하는지 확인
			User followerUser = userRepository.findById(followerId)
					.orElseThrow(() -> new IllegalArgumentException("팔로워 사용자를 찾을 수 없습니다: " + followerId));

			// 팔로잉 받은 사람에게 점수 부여 (+5)
			return recordUserActivity(followingId, ActivityType.FOLLOWER_INCREASE, followerId);

		} catch (DataAccessException e) {
			log.error("팔로우 활동 점수 부여 중 데이터 접근 오류: followerId={}, followingId={}, 오류={}",
					followerId, followingId, e.getMessage());
			return false;
		} catch (Exception e) {
			log.error("팔로우 활동 점수 부여 중 오류 발생: followerId={}, followingId={}, 오류={}",
					followerId, followingId, e.getMessage());
			return false;
		}
	}

	// 방문 기록 및 점수 부여
	@Transactional
	public boolean recordVisit(Long visitorId, Long hostId) {
		// 본인 방문 제외
		if (visitorId.equals(hostId)) {
			log.info("본인 방문 제외: visitorId={}, hostId={}", visitorId, hostId);
			return false;
		}

		LocalDate today = LocalDate.now();
		String visitKey = "user:visit:" + hostId + ":" + visitorId + ":" + today;

		// 이미 방문한 경우 체크
		if (Boolean.TRUE.equals(redisTemplate.hasKey(visitKey))) {
			log.info("이미 방문함: visitorId={}, hostId={}", visitorId, hostId);
			return false;
		}

		// 방문 기록 (24시간 동안 유효)
		redisTemplate.opsForValue().set(visitKey, 1, 24, TimeUnit.HOURS);

		// 방문 점수 기록 -> 방문자에게 점수 부여
		Room room = roomRepository.findByUserId(hostId)
				.orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: userId=" + hostId));

		recordUserActivity(visitorId, ActivityType.ROOM_VISIT, room.getId());

		// 방문자 수 증가 -> 호스트에게 점수 부여
		recordUserActivity(hostId, ActivityType.VISITOR_COUNT, null);

		log.info("방문 기록 완료: 방문자={}, 호스트={}", visitorId, hostId);
		return true;
	}

	// Redis에 사용자 점수 업데이트
	private void updateRedisScore(Long userId, int score) {
		// 총점 업데이트
		String key = "user:total:" + userId;
		redisTemplate.opsForValue().increment(key, score);

		// 랭킹 점수 업데이트
		redisService.updateUserScoreAsync(userId, score);
	}

	// 일일 활동 제한 체크
	private boolean checkDailyLimit(Long userId, ActivityType activityType, Long relatedEntityId) {
		LocalDate today = LocalDate.now();
		String key = "user:daily:" + activityType + ":" + userId + ":" + today;

		Long count = redisTemplate.opsForValue().increment(key, 0);
		// 키가 없는 경우 (첫 활동인 경우)
		count = (count == null) ? 0L : count;

		// 방명록은 방마다 하루에 하나만 허용
		if (activityType == ActivityType.GUESTBOOK && relatedEntityId != null) {
			String roomGuestbookKey = "user:guestbook:" + userId + ":" + relatedEntityId + ":" + today;
			Boolean alreadyWritten = redisTemplate.hasKey(roomGuestbookKey);
			if (Boolean.TRUE.equals(alreadyWritten)) {
				log.info("같은 방에 이미 방명록 작성함: userId={}, roomId={}", userId, relatedEntityId);
				return false;
			}
			// 방명록 작성 기록 (24시간 유효)
			redisTemplate.opsForValue().set(roomGuestbookKey, 1, 24, TimeUnit.HOURS);
		}

		// 활동별 제한
		switch (activityType) {
			case ATTENDANCE:
				// 오전 / 오후 체크
				int hour = LocalDateTime.now().getHour();
				String timeKey =
						"user:daily:" + activityType + ":" + userId + ":" + today + ":" + (hour < 12 ? "AM"
								: "PM");
				Boolean exists = redisTemplate.hasKey(timeKey);
				if (Boolean.TRUE.equals(exists)) {
					return false;
				}
				redisTemplate.opsForValue().set(timeKey, 1, 24, TimeUnit.HOURS);
				return true;

			case BOOK_REGISTRATION:
				return count < 1; // 하루 최대 1번

			case MUSIC_REGISTRATION:
				return count < 1; // 하루 최대 1번

			case BOOK_REVIEW:
				return count < 3; // 하루 최대 3번

			case MUSIC_COMMENT:
				return count < 5; // 하루 최대 5번

			case ROOM_VISIT:
				return count < 10; // 하루 최대 10번

			case GUESTBOOK:
				return count < 5; // 하루 최대 5번

			default:
				return true; // 제한 없음
		}
	}

	// 일일 활동 횟수 증가
	private void incrementDailyActivityCount(Long userId, ActivityType activityType) {
		LocalDate today = LocalDate.now();
		String key = "user:daily:" + activityType + ":" + userId + ":" + today;
		redisTemplate.opsForValue().increment(key, 1);
		redisTemplate.expire(key, 24, TimeUnit.HOURS);
	}
}
