package com.zerozoa.skinner.global.exception;

import java.util.UUID;

public class MemberNotFoundException extends RuntimeException{
    public MemberNotFoundException(UUID uuid) {
        super("존재하지 않는 사용자입니다. UUID: " + uuid);
    }
}
