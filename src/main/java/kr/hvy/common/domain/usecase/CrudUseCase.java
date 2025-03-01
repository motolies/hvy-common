package kr.hvy.common.domain.usecase;

import java.util.List;
import kr.hvy.common.domain.dto.DeleteResponse;

public interface CrudUseCase<T, R, C, U, ID, DR extends DeleteResponse<ID>> {

  default R create(C createDto) {
    throw new UnsupportedOperationException("Create operation not supported");
  }

  default R update(ID id, U updateDto) {
    throw new UnsupportedOperationException("Update operation not supported");
  }

  default DR delete(ID id) {
    throw new UnsupportedOperationException("Delete operation not supported");
  }

  default R findById(ID id) {
    throw new UnsupportedOperationException("FindById operation not supported");
  }

  default List<R> findAll() {
    throw new UnsupportedOperationException("FindAll operation not supported");
  }
}