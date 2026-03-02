package kr.hvy.common.infrastructure.notification.slack.message;

import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import java.util.Collections;
import java.util.List;

public interface SlackMessage {

  String getChannel();

  boolean isNotify();

  String getFallbackText();

  List<LayoutBlock> toBlocks();

  default List<Attachment> toAttachments() {
    return Collections.emptyList();
  }
}
