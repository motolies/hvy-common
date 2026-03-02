package kr.hvy.common.infrastructure.notification.slack;

import kr.hvy.common.infrastructure.notification.slack.message.SlackMessage;

public interface Notify {

  void sendMessage(NotifyRequest request);

  void sendMessage(SlackMessage message);
}
