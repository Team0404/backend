package com.sparta.slack.slack.service;

import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;

/**
 * 슬랙 발송 진입점. AI 쪽(A1/A4)이 슬랙 도메인에 의존하는 유일한 통로다.
 *
 * <p>AI 담당자는 이 인터페이스만 바라보고 개발하고, 슬랙 담당자가 구현체를 채운다.
 * 두 사람의 작업이 겹치는 지점이 여기 하나뿐이므로 시그니처는 임의로 바꾸지 말 것.
 *
 * <p>구현 시 주의: 발송에 실패해도 예외를 던지지 말고 이력을 FAILED로 저장한 뒤
 * 그대로 반환한다. AI 로그와 슬랙 이력이 모두 남아야 A4/M2 재시도가 가능하다.
 */
public interface SlackSender {

    SlackMessageCreateResponseDto send(SlackSendCommand command);
}
