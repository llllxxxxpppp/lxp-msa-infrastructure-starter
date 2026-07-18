package com.lcs.subscription.domain.model.vo;

/**
 * 결제/환불 요청에 대한 응답 결과를 표현한다.
 * NOT_REQUESTED(요청 전, default) / SUCCESS(성공 응답) / FAILED(실패 응답).
 */
public enum ResponseResult {
    NOT_REQUESTED,
    SUCCESS,
    FAILED
}
