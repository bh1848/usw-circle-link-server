package com.USWCicrcleLink.server.global.exception.errortype;

import com.USWCicrcleLink.server.global.exception.ExceptionType;

public class ClubApplicationException extends BaseException {
    public ClubApplicationException(ExceptionType exceptionType) {
        super(exceptionType);
    }
}
