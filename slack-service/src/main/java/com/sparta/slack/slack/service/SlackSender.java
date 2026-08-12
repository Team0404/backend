package com.sparta.slack.slack.service;

import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;

/**
 * 슬랙 발송 진입점. AI 쪽(A1/A4)이 슬랙 도메인에 의존하는 유일한 통로다.
 */
public interface SlackSender {

    SlackMessageCreateResponseDto send(SlackSendCommand command);
}
