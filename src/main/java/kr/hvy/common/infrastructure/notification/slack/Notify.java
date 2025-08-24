package kr.hvy.common.infrastructure.notification.slack;

public interface Notify {

  void sendMessage(NotifyRequest request);
}
