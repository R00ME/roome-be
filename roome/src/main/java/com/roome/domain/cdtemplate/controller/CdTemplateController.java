package com.roome.domain.cdtemplate.controller;

import com.roome.domain.cdtemplate.dto.CdTemplateRequest;
import com.roome.domain.cdtemplate.dto.CdTemplateResponse;
import com.roome.domain.cdtemplate.service.CdTemplateService;
import com.roome.global.security.jwt.principal.CustomUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "CD 템플릿 API", description = "CD 템플릿 관련 API")
@RestController
@RequestMapping("/api/my-cd/{myCdId}/template")
@RequiredArgsConstructor
public class CdTemplateController {

  private final CdTemplateService cdTemplateService;

  @Operation(summary = "CD 템플릿 생성", description = "사용자가 특정 CD에 대한 템플릿을 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "CD 템플릿 생성 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (INVALID_REQUEST)"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)"),
      @ApiResponse(responseCode = "404", description = "CD를 찾을 수 없음 (CD_NOT_FOUND)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping
  public ResponseEntity<CdTemplateResponse> createTemplate(
          @AuthenticationPrincipal CustomUser user,
          @Parameter(description = "템플릿을 추가할 CD의 ID", example = "1") @PathVariable Long myCdId,
          @RequestBody CdTemplateRequest request
  ) {
    return ResponseEntity.ok(cdTemplateService.createTemplate(myCdId, user.getUserId(), request));
  }

  @Operation(summary = "CD 템플릿 조회", description = "특정 CD의 템플릿을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "CD 템플릿 조회 성공"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)"),
      @ApiResponse(responseCode = "404", description = "CD 또는 템플릿을 찾을 수 없음 (TEMPLATE_NOT_FOUND)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @GetMapping
  public ResponseEntity<CdTemplateResponse> getTemplate(
      @Parameter(description = "조회할 CD의 ID", example = "1") @PathVariable Long myCdId
  ) {
    return ResponseEntity.ok(cdTemplateService.getTemplate(myCdId));
  }

  @Operation(summary = "CD 템플릿 수정", description = "사용자가 특정 CD의 템플릿을 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "CD 템플릿 수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (INVALID_REQUEST)"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)"),
      @ApiResponse(responseCode = "404", description = "CD 또는 템플릿을 찾을 수 없음 (TEMPLATE_NOT_FOUND)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PatchMapping
  public ResponseEntity<CdTemplateResponse> updateTemplate(
          @AuthenticationPrincipal CustomUser user,
          @Parameter(description = "수정할 CD의 ID", example = "1") @PathVariable Long myCdId,
          @RequestBody CdTemplateRequest request
  ) {
    return ResponseEntity.ok(cdTemplateService.updateTemplate(myCdId, user.getUserId(), request));
  }

  @Operation(summary = "CD 템플릿 삭제", description = "사용자가 특정 CD의 템플릿을 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "CD 템플릿 삭제 성공"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)"),
      @ApiResponse(responseCode = "404", description = "CD 또는 템플릿을 찾을 수 없음 (TEMPLATE_NOT_FOUND)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @DeleteMapping
  public ResponseEntity<Void> deleteTemplate(
          @AuthenticationPrincipal CustomUser user,
          @Parameter(description = "삭제할 CD의 ID", example = "1") @PathVariable Long myCdId
  ) {
    cdTemplateService.deleteTemplate(myCdId, user.getUserId());
    return ResponseEntity.noContent().build();
  }
}
