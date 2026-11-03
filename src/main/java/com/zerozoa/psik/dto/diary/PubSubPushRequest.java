package com.zerozoa.psik.dto.diary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Pub/Sub push 요청 body. data는 base64로 인코딩된 메시지 본문 */
public record PubSubPushRequest(@NotNull @Valid Message message) {
    public record Message(@NotBlank String data, String messageId) {}
}
