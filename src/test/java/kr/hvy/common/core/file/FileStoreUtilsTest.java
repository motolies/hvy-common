package kr.hvy.common.core.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStoreUtilsTest {

  @TempDir
  Path uploadRoot;

  /**
   * 업로드 파일을 힙에 통째로 올리지 않고(getBytes 미호출) 스트림으로 디스크에 복사하는지 검증한다.
   * 2026-11-23 400MB 첨부 업로드가 getBytes() 전체 적재로 힙 OOM 을 낸 회귀를 막는다.
   */
  @Test
  void savesByStreamingWithoutLoadingWholeFileIntoHeap() throws IOException {
    byte[] content = "스트리밍 저장 확인".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new StreamOnlyMultipartFile("file", "manual.pdf", content);

    String relativePath = FileStoreUtils.save(uploadRoot.toString(), file, 1246L);

    Path saved = uploadRoot.resolve(relativePath);
    assertTrue(relativePath.startsWith("2000/1246/") && relativePath.endsWith(".pdf"), relativePath);
    assertArrayEquals(content, Files.readAllBytes(saved));
  }

  /**
   * getBytes() 호출 시 즉시 실패하는 MultipartFile — 스트림 경로만 허용한다.
   */
  private static final class StreamOnlyMultipartFile extends MockMultipartFile {

    StreamOnlyMultipartFile(String name, String originalFilename, byte[] content) {
      super(name, originalFilename, "application/pdf", content);
    }

    @Override
    public byte[] getBytes() {
      throw new AssertionError("업로드 파일 전체를 getBytes() 로 힙에 올리면 안 된다");
    }
  }
}
