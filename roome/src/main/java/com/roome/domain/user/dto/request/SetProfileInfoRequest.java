package com.roome.domain.user.dto.request;

import com.roome.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SetProfileInfoRequest {
    @NotBlank
    private String gender;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "생년월일은 yyyy-MM-dd 형식이어야 합니다."
    )
    private String birthDate;
}
