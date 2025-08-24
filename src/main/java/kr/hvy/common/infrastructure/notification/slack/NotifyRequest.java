package kr.hvy.common.infrastructure.notification.slack;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@Data
public class NotifyRequest {

  private boolean isNotify;
  private String message;
  private Exception exception;
  private String channel;

  @Builder
  public NotifyRequest(boolean isNotify, String message, Exception exception, String channel) {
    this.isNotify = isNotify;
    this.message = message;
    this.exception = exception;
    this.channel = channel;
  }

  public static class NotifyRequestBuilder {

    public NotifyRequest build() {
      if (ObjectUtils.allNull(message, exception)) {
        throw new IllegalArgumentException("message or exception is required");
      }
      if (StringUtils.isBlank(channel)) {
        throw new IllegalArgumentException("channel is required");
      }

      return new NotifyRequest(isNotify, message, exception, channel);
    }
  }

}
