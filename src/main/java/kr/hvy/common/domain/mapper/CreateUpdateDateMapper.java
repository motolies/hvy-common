package kr.hvy.common.domain.mapper;

import kr.hvy.common.domain.embeddable.CreateUpdateDateEntity;
import kr.hvy.common.domain.vo.CreateUpdateDate;

public class CreateUpdateDateMapper {

  // Entity -> VO 변환
  public static CreateUpdateDate toDomain(CreateUpdateDateEntity entity) {
    if (entity == null) {
      return null;
    }

    return CreateUpdateDate.builder()
        .createDate(entity.getCreateDate())
        .updateDate(entity.getUpdateDate())
        .build();
  }

  // VO -> Entity 변환
  public static CreateUpdateDateEntity toEntity(CreateUpdateDate vo) {
    if (vo == null) {
      return null;
    }

    return CreateUpdateDateEntity.builder()
        .createDate(vo.getCreateDate())
        .updateDate(vo.getUpdateDate())
        .build();
  }
}