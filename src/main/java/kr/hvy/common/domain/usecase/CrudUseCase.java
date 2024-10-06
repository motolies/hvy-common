package kr.hvy.common.domain.usecase;

import java.util.List;

public interface CrudUseCase<T, R, C, U, ID> {

  default R create(C createDto) {
    throw new UnsupportedOperationException("Create operation not supported");
  }

  default R update(U updateDto) {
    throw new UnsupportedOperationException("Update operation not supported");
  }

  default void delete(ID id) {
    throw new UnsupportedOperationException("Delete operation not supported");
  }

  default R findById(ID id) {
    throw new UnsupportedOperationException("FindById operation not supported");
  }

  default List<R> findAll() {
    throw new UnsupportedOperationException("FindAll operation not supported");
  }
}