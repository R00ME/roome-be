package com.roome.domain.room.service;

import com.roome.domain.book.entity.repository.GenreRepository;
import com.roome.domain.cd.repository.CdGenreTypeRepository;
import com.roome.domain.cdtemplate.repository.CdTemplateRepository;
import com.roome.domain.furniture.dto.FurnitureResponseDto;
import com.roome.domain.furniture.entity.Furniture;
import com.roome.domain.furniture.entity.FurnitureType;
import com.roome.domain.furniture.repository.FurnitureRepository;
import com.roome.domain.mybook.entity.MyBookCount;
import com.roome.domain.mybook.entity.repository.MyBookCountRepository;
import com.roome.domain.mybookreview.entity.repository.MyBookReviewRepository;
import com.roome.domain.mycd.entity.MyCdCount;
import com.roome.domain.mycd.repository.MyCdCountRepository;
import com.roome.domain.point.entity.PointReason;
import com.roome.domain.point.service.PointService;
import com.roome.domain.rank.service.UserActivityService;
import com.roome.domain.room.dto.RoomResponseDto;
import com.roome.domain.room.entity.Room;
import com.roome.domain.room.entity.RoomTheme;
import com.roome.domain.room.entity.RoomThemeUnlock;
import com.roome.domain.room.repository.RoomRepository;
import com.roome.domain.room.repository.RoomThemeUnlockRepository;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.exception.BusinessException;
import com.roome.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

	private final RoomRepository roomRepository;
	private final UserRepository userRepository;
	private final FurnitureRepository furnitureRepository;
	private final MyCdCountRepository myCdCountRepository;
	private final MyBookCountRepository myBookCountRepository;
	private final MyBookReviewRepository myBookReviewRepository;
	private final CdTemplateRepository cdTemplateRepository;
	private final UserActivityService userActivityService;
	private final GenreRepository genreRepository;
	private final CdGenreTypeRepository cdGenreTypeRepository;
	private final RoomThemeUnlockRepository roomThemeUnlockRepository;
	private final PointService pointService;

	@Transactional
	public RoomResponseDto createRoom(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> {
					log.error("방 생성 실패: 사용자(userId={})를 찾을 수 없음", userId);
					return new BusinessException(ErrorCode.USER_NOT_FOUND);
				});

		Room newRoom = Room.builder()
				.user(user)
				.theme(RoomTheme.BASIC)
				.furnitures(new ArrayList<>())
				.build();

		Room savedRoom = roomRepository.save(newRoom);
		roomRepository.flush();

		log.info("방 생성 완료: 방(roomId={}) 생성됨 (userId={})", savedRoom.getId(), userId);

		// 기본 가구 추가 (책꽂이 & CD 랙)
		List<Furniture> defaultFurnitures = List.of(
				Furniture.builder()
						.room(savedRoom)
						.furnitureType(FurnitureType.BOOKSHELF)
						.isVisible(false)  // 기본값: 보이지 않음
						.level(1)  // 기본값: 1레벨
						.build(),
				Furniture.builder()
						.room(savedRoom)
						.furnitureType(FurnitureType.CD_RACK)
						.isVisible(false)
						.level(1)
						.build()
		);

		furnitureRepository.saveAll(defaultFurnitures);
		furnitureRepository.flush();

		log.info("기본 가구 추가 완료: 방(roomId={})에 책꽂이 & CD 랙 생성 완료", savedRoom.getId());

		Long savedMusic = 0L;
		Long savedBooks = 0L;
		Long writtenReviews = 0L;
		Long writtenMusicLogs = 0L;

		log.info("방 생성 완료: 방(roomId={}) 생성됨 (userId={})", savedRoom.getId(), userId);
		return RoomResponseDto.from(savedRoom, savedMusic, savedBooks, writtenReviews,
				writtenMusicLogs);

	}

	@Transactional(readOnly = true)
	public RoomResponseDto getRoomById(Long roomId) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> {
					log.error("방 조회 실패: 존재하지 않는 방 (roomId={})", roomId);
					return new BusinessException(ErrorCode.ROOM_NOT_FOUND);
				});

		Long savedMusic = fetchSavedMusicCount(room);
		Long savedBooks = fetchSavedBooksCount(roomId);
		Long writtenReviews = fetchWrittenReviewsCount(room.getUser().getId());
		Long writtenMusicLogs = fetchWrittenMusicLogsCount(room.getUser().getId());

		List<String> topBookGenres = getTop3BookGenres(roomId);
		List<String> topCdGenres = getTop3CdGenres(roomId);

		log.info("방 조회 성공: 방(roomId={}) 조회 완료", roomId);
		return RoomResponseDto.from(room, savedMusic, savedBooks, writtenReviews, writtenMusicLogs,
				topBookGenres, topCdGenres);
	}

	@Transactional(readOnly = true)
	public RoomResponseDto getRoomByUserId(Long userId) {
		Room room = roomRepository.findByUserId(userId)
				.orElseThrow(() -> {
					log.error("방 조회 실패: 해당 사용자의 방이 존재하지 않음 (userId={})", userId);
					return new BusinessException(ErrorCode.ROOM_NOT_FOUND);
				});

		Long savedMusic = fetchSavedMusicCount(room);
		Long savedBooks = fetchSavedBooksCount(room.getId());
		Long writtenReviews = fetchWrittenReviewsCount(userId);
		Long writtenMusicLogs = fetchWrittenMusicLogsCount(userId);

		List<String> topBookGenres = getTop3BookGenres(room.getId());
		List<String> topCdGenres = getTop3CdGenres(room.getId());

		log.info("방 조회 성공: 사용자의 방(roomId={})을 조회 완료 (userId={})", room.getId(), userId);

		return RoomResponseDto.from(room, savedMusic, savedBooks, writtenReviews, writtenMusicLogs,
				topBookGenres, topCdGenres);
	}

	private List<String> getTop3BookGenres(Long roomId) {
		List<String> genres = genreRepository.findGenresByRoomId(roomId);
		return genres.stream()
				.collect(Collectors.groupingBy(genre -> genre, Collectors.counting())) // 장르별 개수 집계
				.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue())) // 개수 내림차순 정렬
				.limit(3) // 상위 3개 선택
				.map(Map.Entry::getKey)
				.toList();
	}

	private List<String> getTop3CdGenres(Long roomId) {
		List<String> genres = cdGenreTypeRepository.findGenresByRoomId(roomId);
		return genres.stream()
				.collect(Collectors.groupingBy(genre -> genre, Collectors.counting())) // 장르별 개수 집계
				.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue())) // 개수 내림차순 정렬
				.limit(3) // 상위 3개 선택
				.map(Map.Entry::getKey)
				.toList();
	}


	// 에러 처리 중
	@Transactional
	public RoomResponseDto getOrCreateRoomByUserId(Long userId) {
		return roomRepository.findByUserId(userId)
				.map(this::buildRoomResponse)
				.orElseGet(() -> {
					try {
						return createRoom(userId);
					} catch (BusinessException e) {
						log.error("방을 생성하려 했으나 사용자(userId={})를 찾을 수 없음", userId, e);
						throw e; // 이미 던지고 있는 예외이므로 그대로 전달
					}
				});
	}


	@Transactional
	public int purchaseRoomTheme(Long userId, Long roomId, String themeName) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		if (!room.getUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.ROOM_ACCESS_DENIED);
		}

		RoomTheme theme = RoomTheme.fromString(themeName);

		boolean isUnlocked = roomThemeUnlockRepository.existsByUserAndTheme(room.getUser(), theme);
		if (isUnlocked) {
			throw new BusinessException(ErrorCode.THEME_ALREADY_UNLOCKED);
		}

		pointService.usePoints(room.getUser(), PointReason.THEME_PURCHASE);
		roomThemeUnlockRepository.save(RoomThemeUnlock.create(room.getUser(), theme));

		log.info("테마 구매 완료: 사용자(userId={}), 테마({})", userId, themeName);

		return room.getUser().getPoint().getBalance(); // 남은 포인트 반환
	}


	@Transactional
	public String updateRoomTheme(Long userId, Long roomId, String newTheme) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> {
					log.error("방 테마 변경 실패: 존재하지 않는 방 (roomId={})", roomId);
					return new BusinessException(ErrorCode.ROOM_NOT_FOUND);
				});

		if (!room.getUser().getId().equals(userId)) {
			log.error("방 테마 변경 실패: 사용자(userId={})가 방(roomId={})의 소유자가 아님", userId, roomId);
			throw new BusinessException(ErrorCode.ROOM_ACCESS_DENIED);
		}

		RoomTheme theme = RoomTheme.fromString(newTheme);

		boolean isUnlocked = roomThemeUnlockRepository.existsByUserAndTheme(room.getUser(), theme);

		if (!isUnlocked && !theme.equals(RoomTheme.BASIC)) {
			log.error("방 테마 변경 실패: 잠금 해제되지 않은 테마 (userId={}, theme={})", userId, newTheme);
			throw new BusinessException(ErrorCode.THEME_NOT_UNLOCKED);
		}

		room.updateTheme(theme);
		log.info("방 테마 변경 완료: 방(roomId={}) → 새 테마({})", roomId, newTheme);

		return theme.name();
	}


	@Transactional
	public FurnitureResponseDto toggleFurnitureVisibility(Long userId, Long roomId,
														  String furnitureTypeStr) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> {
					log.error("가구 상태 변경 실패: 존재하지 않는 방 (roomId={})", roomId);
					return new BusinessException(ErrorCode.ROOM_NOT_FOUND);
				});

		if (!room.getUser().getId().equals(userId)) {
			log.error("가구 상태 변경 실패: 사용자(userId={})가 방(roomId={})의 소유자가 아님", userId, roomId);
			throw new BusinessException(ErrorCode.ROOM_ACCESS_DENIED);
		}

		FurnitureType furnitureType;
		try {
			furnitureType = FurnitureType.valueOf(furnitureTypeStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			log.error("가구 상태 변경 실패: 유효하지 않은 가구 타입 (입력값={})", furnitureTypeStr);
			throw new BusinessException(ErrorCode.INVALID_FURNITURE_TYPE);
		}

		Furniture furniture = room.getFurnitures().stream()
				.filter(f -> f.getFurnitureType() == furnitureType)
				.findFirst()
				.orElseThrow(() -> {
					log.error("가구 상태 변경 실패: 방(roomId={})에 해당 가구({})가 존재하지 않음", roomId, furnitureTypeStr);
					return new BusinessException(ErrorCode.FURNITURE_NOT_FOUND);
				});

		// 가구 활성화/비활성화 토글
		boolean newVisibility = !furniture.getIsVisible();
		furniture.setVisible(newVisibility);

		List<String> topGenres = switch (furnitureType) {
			case BOOKSHELF -> getTop3BookGenres(roomId);
			case CD_RACK -> getTop3CdGenres(roomId);
		};

		log.info("가구 상태 변경 완료: 방(roomId={}), 가구({}), 새 상태={}, Top3 장르={}", roomId, furnitureType,
				newVisibility, topGenres);
		return FurnitureResponseDto.from(furniture, topGenres);
	}

	@Transactional(readOnly = true)
	public List<String> getUnlockedThemes(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		List<RoomThemeUnlock> unlockedThemes = roomThemeUnlockRepository.findByUser(user);


		List<String> themeList = new ArrayList<>(unlockedThemes.stream()
				.map(themeUnlock -> themeUnlock.getTheme().name().toUpperCase())
				.toList());

		if (!themeList.contains("BASIC")) {
			themeList.add("BASIC");
		}

		return themeList;

	}


	private RoomResponseDto buildRoomResponse(Room room) {
		Long savedMusic = fetchSavedMusicCount(room);
		Long savedBooks = fetchSavedBooksCount(room.getId());
		Long writtenReviews = fetchWrittenReviewsCount(room.getUser().getId());
		Long writtenMusicLogs = fetchWrittenMusicLogsCount(room.getUser().getId());

		log.info("방 정보 생성 완료: 방(roomId={}), 저장된 음악={}, 저장된 책={}, 작성한 리뷰={}, 작성한 음악 로그={}",
				room.getId(), savedMusic, savedBooks, writtenReviews, writtenMusicLogs);

		return RoomResponseDto.from(room, savedMusic, savedBooks, writtenReviews, writtenMusicLogs);
	}


	Long fetchSavedMusicCount(Room room) {
		try {
			return myCdCountRepository.findByRoom(room)
					.map(MyCdCount::getCount)
					.orElse(0L);
		} catch (Exception e) {
			log.error("음악 저장 개수 조회 중 오류 발생 (roomId={})", room.getId(), e);
			return 0L;
		}
	}

	Long fetchSavedBooksCount(Long roomId) {
		try {
			return myBookCountRepository.findByRoomId(roomId)
					.map(MyBookCount::getCount)
					.orElse(0L);
		} catch (Exception e) {
			log.error("책 저장 개수 조회 중 오류 발생 (roomId={})", roomId, e);
			return 0L;
		}
	}

	Long fetchWrittenReviewsCount(Long userId) {
		try {
			return myBookReviewRepository.countByUserId(userId);
		} catch (Exception e) {
			log.error("사용자의 작성한 리뷰 개수 조회 중 오류 발생 (userId={})", userId, e);
			return 0L;
		}
	}

	Long fetchWrittenMusicLogsCount(Long userId) {
		try {
			return cdTemplateRepository.countByUserId(userId);
		} catch (Exception e) {
			log.error("사용자의 작성한 음악 로그 개수 조회 중 오류 발생 (userId={})", userId, e);
			return 0L;
		}
	}

	// 방문
	@Transactional
	public RoomResponseDto visitRoomByRoomId(Long visitorId, Long roomId) {
		return visitRoomInternal(visitorId, roomId, null);
	}

	@Transactional
	public RoomResponseDto visitRoomByHostId(Long visitorId, Long hostId) {
		Room room = roomRepository.findByUserId(hostId)
				.orElseThrow(() -> {
					log.error("방 조회 실패: 해당 사용자의 방이 존재하지 않음 (userId={})", hostId);
					return new BusinessException(ErrorCode.ROOM_NOT_FOUND);
				});

		return visitRoomInternal(visitorId, room.getId(), hostId);
	}

	private RoomResponseDto visitRoomInternal(Long visitorId, Long roomId, Long hostId) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> {
					log.error("방 방문 실패: 존재하지 않는 방 (roomId={})", roomId);
					return new BusinessException(ErrorCode.ROOM_NOT_FOUND);
				});

		if (hostId == null) {
			hostId = room.getUser().getId();
		}

		// 방문 활동 기록
		userActivityService.recordVisit(visitorId, hostId);

		log.info("방 방문 처리 완료: 방문자(userId={})가 방(roomId={}, 소유자={})을 방문함", visitorId, roomId, hostId);

		return buildRoomResponse(room);
	}
}
