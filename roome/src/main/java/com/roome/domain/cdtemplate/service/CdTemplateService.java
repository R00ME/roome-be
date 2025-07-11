package com.roome.domain.cdtemplate.service;

import com.roome.domain.cdtemplate.dto.CdTemplateRequest;
import com.roome.domain.cdtemplate.dto.CdTemplateResponse;
import com.roome.domain.cdtemplate.entity.CdTemplate;
import com.roome.domain.cdtemplate.exception.CdTemplateNotFoundException;
import com.roome.domain.cdtemplate.exception.DuplicateCdTemplateException;
import com.roome.domain.cdtemplate.exception.UnauthorizedCdTemplateAccessException;
import com.roome.domain.cdtemplate.repository.CdTemplateRepository;
import com.roome.domain.mycd.entity.MyCd;
import com.roome.domain.mycd.repository.MyCdRepository;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.security.jwt.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CdTemplateService {

	private final CdTemplateRepository cdTemplateRepository;
	private final MyCdRepository myCdRepository;
	private final UserRepository userRepository;

	@Transactional
	public CdTemplateResponse createTemplate(Long myCdId, Long userId, CdTemplateRequest request) {
		MyCd myCd = myCdRepository.findById(myCdId)
				.orElseThrow(CdTemplateNotFoundException::new);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> {
					log.error("사용자를 찾을 수 없습니다. userId={}", userId);
					return new UserNotFoundException();
				});

		if (!myCd.getUser().getId().equals(userId)) {
			throw new UnauthorizedCdTemplateAccessException();
		}

		// 중복 체크 후 저장
		if (!cdTemplateRepository.existsByMyCdId(myCdId)) {
			CdTemplate cdTemplate = CdTemplate.builder()
					.myCd(myCd)
					.user(user)
					.comment1(request.getComment1())
					.comment2(request.getComment2())
					.comment3(request.getComment3())
					.comment4(request.getComment4())
					.build();

			cdTemplateRepository.save(cdTemplate);
			return CdTemplateResponse.from(cdTemplate);
		}

		throw new DuplicateCdTemplateException("이미 존재하는 템플릿입니다.");
	}

	public CdTemplateResponse getTemplate(Long myCdId) {
		CdTemplate cdTemplate = cdTemplateRepository.findByMyCdId(myCdId)
				.orElseThrow(CdTemplateNotFoundException::new);
		return CdTemplateResponse.from(cdTemplate);
	}

	@Transactional
	public CdTemplateResponse updateTemplate(Long myCdId, Long userId, CdTemplateRequest request) {
		CdTemplate cdTemplate = cdTemplateRepository.findByMyCdId(myCdId)
				.orElseThrow(CdTemplateNotFoundException::new);

		if (!cdTemplate.getMyCd().getUser().getId().equals(userId)) {
			throw new UnauthorizedCdTemplateAccessException();
		}

		cdTemplate.update(request.getComment1(), request.getComment2(), request.getComment3(),
				request.getComment4());
		return CdTemplateResponse.from(cdTemplate);
	}

	@Transactional
	public void deleteTemplate(Long myCdId, Long userId) {
		CdTemplate cdTemplate = cdTemplateRepository.findByMyCdId(myCdId)
				.orElseThrow(CdTemplateNotFoundException::new);

		if (!cdTemplate.getMyCd().getUser().getId().equals(userId)) {
			throw new UnauthorizedCdTemplateAccessException();
		}

		cdTemplateRepository.delete(cdTemplate);
	}
}
