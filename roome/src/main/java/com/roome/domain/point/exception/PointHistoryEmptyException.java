package com.roome.domain.point.exception;

import com.roome.global.exception.BusinessException;
import com.roome.global.exception.ErrorCode;

public class PointHistoryEmptyException extends BusinessException {
	public PointHistoryEmptyException() {
		super(ErrorCode.POINT_HISTORY_EMPTY);
	}
}
