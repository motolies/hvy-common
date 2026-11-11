package kr.hvy.common.infrastructure.notification.slack;

import kr.hvy.common.infrastructure.notification.slack.message.SlackMessage;

/**
 * 알림 전송 창구. 두 메서드 모두 <b>전송 결과를 돌려주고 전송 실패로는 예외를 던지지 않는다</b>(0.4.1 부터 — 이전에는 void 였고 실패를 알 방법이 없었다).
 * 반환값을 쓰지 않는 기존 호출부는 그대로 동작한다. 실패했을 때 다시 보내거나 "발행됨" 표시를 미뤄야 하는 호출부만 결과를 본다.
 */
public interface Notify {

  /**
   * 텍스트 알림(예외가 실려 있으면 오류 알림)을 보낸다.
   *
   * @return 상대(Slack)가 메시지를 받았는가. 전송 오류나 거부는 구현이 로그로 남기고 false 를 돌려준다
   */
  boolean sendMessage(NotifyRequest request);

  /**
   * 조립된 메시지(Block Kit)를 보낸다.
   *
   * @return 상대(Slack)가 메시지를 받았는가. 전송 오류나 거부는 구현이 로그로 남기고 false 를 돌려준다
   */
  boolean sendMessage(SlackMessage message);
}
