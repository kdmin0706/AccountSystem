package com.example.account.exception;

import com.example.account.type.ErrorCode;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AccountException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String errorMessage;

    public AccountException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }
}
