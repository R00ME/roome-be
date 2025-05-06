package com.roome.global.security.jwt.exception;

import com.roome.global.exception.BusinessException;
import com.roome.global.exception.ErrorCode;

public class MissingAuthorityException extends BusinessException {
    public MissingAuthorityException() {
        super(ErrorCode.MISSING_AUTHORITY);
    }
}
