package com.roome.domain.point.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PointHistoryGroupedDto {

	private String date;
	private List<PointHistoryDto> items;  // 해당 날짜의 포인트 내역 목록
}
