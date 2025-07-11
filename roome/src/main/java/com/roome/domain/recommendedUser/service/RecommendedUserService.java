package com.roome.domain.recommendedUser.service;

import com.roome.domain.houseMate.repository.HousemateRepository;
import com.roome.domain.recommendedUser.entity.RecommendedUser;
import com.roome.domain.recommendedUser.repository.RecommendedUserRepository;
import com.roome.domain.user.dto.RecommendedUserDto;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.repository.UserRepository;
import com.roome.domain.userGenrePreference.service.GenrePreferenceService;
import com.roome.global.exception.BusinessException;
import com.roome.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendedUserService {

	private final UserRepository userRepository;
	private final RecommendedUserRepository recommendedUserRepository;
	private final GenrePreferenceService genrePreferenceService;
	private final HousemateRepository housemateRepository;

	/// 사용자 추천 목록 조회
	/// @param userId 사용자 ID
	/// @return 추천 사용자 DTO 리스트
	public List<RecommendedUserDto> getRecommendedUsers(Long userId) {
		List<RecommendedUser> recommendations = recommendedUserRepository.findTop5ByUserIdOrderBySimilarityScoreDesc(userId);

		return recommendations.stream()
				.map(recommendation -> RecommendedUserDto.builder()
						.userId(recommendation.getRecommendedUser().getId())
						.nickname(recommendation.getRecommendedUser().getNickname())
						.profileImage(recommendation.getRecommendedUser().getProfileImage())
						.build())
				.collect(Collectors.toList());
	}

	/// 모든 사용자의 추천 목록 새로 계산하여 저장 (배치 작업용)
	@Transactional
	public void updateAllRecommendations() {
		log.info("Starting batch job: updating all user recommendations");
		List<User> allUsers = userRepository.findAll();

		int totalUsers = allUsers.size();
		int processedUsers = 0;

		for (User user : allUsers) {
			updateUserRecommendations(user.getId());

			processedUsers++;
			if (processedUsers % 50 == 0 || processedUsers == totalUsers) {
				log.info("Batch progress: {}/{} users processed", processedUsers, totalUsers);
			}
		}

		log.info("Batch job completed: all user recommendations updated");
	}

	/// 특정 사용자의 추천 목록 새로 계산하여 저장
	/// @param userId 사용자 ID
	@Transactional
	public void updateUserRecommendations(Long userId) {
		// 기존 추천 목록 삭제
		recommendedUserRepository.deleteAllByUserId(userId);

		// 사용자 존재 확인
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 추천 사용자 계산
		List<User> recommendedUsers = calculateSimilarUsers(userId);

		// 추천 목록 저장
		for (User recommendedUser : recommendedUsers) {
			int score = calculateSimilarityScore(userId, recommendedUser.getId());
			RecommendedUser recommendation = RecommendedUser.create(user, recommendedUser, score);
			recommendedUserRepository.save(recommendation);
		}
	}

	/// 유사 사용자 계산 로직 (기존 UserProfileService의 로직 재사용)
	/// 이미 하우스메이트로 추가한 사용자는 제외
	private List<User> calculateSimilarUsers(Long userId) {
		// 1. 현재 사용자의 선호 장르 가져오기
		List<String> topCdGenres = genrePreferenceService.getTopCdGenres(userId);
		List<String> topBookGenres = genrePreferenceService.getTopBookGenres(userId);

		// 선호 장르가 없으면 빈 리스트 반환
		if (topCdGenres.isEmpty() && topBookGenres.isEmpty()) {
			return Collections.emptyList();
		}

		// 2. 이미 하우스메이트로 추가한 사용자 ID 목록 가져오기
		List<Long> addedUserIds = getAddedHousemateIds(userId);

		// 3. 다른 모든 사용자 가져오기 (이미 하우스메이트로 추가한 사용자 제외)
		List<User> otherUsers = userRepository.findByIdNot(userId).stream()
				.filter(user -> !addedUserIds.contains(user.getId()))
				.collect(Collectors.toList());

		// 4. 각 사용자별 유사도 계산
		Map<User, Integer> similarityScores = new HashMap<>();

		for (User otherUser : otherUsers) {
			int score = calculateSimilarityScore(userId, otherUser.getId());

			// 유사도 점수 기록
			if (score > 0) {
				similarityScores.put(otherUser, score);
			}
		}

		// 5. 유사도 높은 순으로 정렬하여 상위 5명 반환
		return similarityScores.entrySet().stream()
				.sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
				.limit(5)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	/// 사용자가 이미 하우스메이트로 추가한 사용자 ID 목록 조회
	/// @param userId 사용자 ID
	/// @return 하우스메이트로 추가한 사용자 ID 목록
	private List<Long> getAddedHousemateIds(Long userId) {
		// 사용자가 추가한 하우스메이트의 ID 목록만 직접 조회
		return housemateRepository.findAddedIdsByUserId(userId);
	}

	/// 두 사용자 간의 유사도 점수 계산
	private int calculateSimilarityScore(Long userId, Long otherUserId) {
		int score = 0;

		// 현재 사용자의 선호 장르
		List<String> userCdGenres = genrePreferenceService.getTopCdGenres(userId);
		List<String> userBookGenres = genrePreferenceService.getTopBookGenres(userId);

		// 비교 대상 사용자의 선호 장르
		List<String> otherCdGenres = genrePreferenceService.getTopCdGenres(otherUserId);
		List<String> otherBookGenres = genrePreferenceService.getTopBookGenres(otherUserId);

		// CD 장르 유사도
		for (String genre : userCdGenres) {
			if (otherCdGenres.contains(genre)) {
				score += 1;
			}
		}

		// 책 장르 유사도
		for (String genre : userBookGenres) {
			if (otherBookGenres.contains(genre)) {
				score += 1;
			}
		}

		return score;
	}
}
