package kr.hvy.common.application.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteResponse<T> {

  private T id;
}
