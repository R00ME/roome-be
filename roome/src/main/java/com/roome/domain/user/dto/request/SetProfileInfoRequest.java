package com.roome.domain.user.dto.request;

import com.roome.domain.user.entity.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SetProfileInfoRequest {
    @NotBlank
    private Gender gender;

    @NotBlank
    @Min(1900)
    @Max(2025)
    private Integer birthYear;
}
