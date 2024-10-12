package kr.hvy.common.domain.mapper;

import kr.hvy.common.domain.embeddable.CreateUpdateDateEntity;
import kr.hvy.common.domain.vo.CreateUpdateDate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreateUpdateDateMapper extends BaseMapper {

  CreateUpdateDate toDomain(CreateUpdateDateEntity entity);

  CreateUpdateDateEntity toEntity(CreateUpdateDate vo);

}