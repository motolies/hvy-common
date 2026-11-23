package kr.hvy.common.core.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class FileStoreUtils {

  /**
   * 업로드 파일을 {uploadPath}/{그룹}/{postId}/{UUID}.{확장자} 로 스트림 복사해 저장한다.
   * 파일 전체를 메모리에 올리지 않으므로 업로드 한도(max-file-size)가 힙보다 커도 안전하다.
   *
   * @return 생성된 파일 명(유일한 값)
   * @throws IllegalStateException
   * @throws IOException
   */
  public static String save(String uploadPath, MultipartFile file, Long postId) throws IllegalStateException, IOException {
    File uploadPathDir = new File(uploadPath);

    if (!uploadPathDir.exists()) {
      if (!uploadPathDir.mkdirs()) {
        throw new IOException("Failed to create directory");
      }
    }

    // 파일 중복명 처리
    String genId = UUID.randomUUID().toString();
    genId = genId.replace("-", "");

    String originalfileName = file.getOriginalFilename();
    String fileExtension = getExtension(originalfileName);
    String saveFileName = genId + "." + fileExtension;

    String savePath = calcPath(uploadPath, postId);

    File target = new File(uploadPath + savePath, saveFileName);

    // getBytes() 는 파일 크기만큼 byte[] 를 힙에 한 번에 할당한다(400MB 업로드 → 힙 OOM, 2026-11-23).
    // 컨테이너가 디스크에 받아 둔 임시 파일을 스트림으로 복사해 힙 사용을 버퍼 크기로 묶는다.
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    String returnPath = makeFilePath(uploadPath, savePath, saveFileName);
    return returnPath.replaceFirst("^/", "");
  }

  /**
   * 파일이름으로부터 확장자를 반환
   *
   * @param fileName 확장자를 포함한 파일 명
   * @return 파일 이름에서 뒤의 확장자 이름만을 반환
   */
  public static String getExtension(String fileName) {
    int dotPosition = fileName.lastIndexOf('.');

    if (-1 != dotPosition && fileName.length() - 1 > dotPosition) {
      return fileName.substring(dotPosition + 1);
    } else {
      return "";
    }
  }

  private static String calcPath(String uploadPath, Long contentId) {
    // 저장할 때 폴더
    String groupPath = File.separator + calcGroupPath(contentId);
    String contentPath = File.separator + contentId;

    makeDir(uploadPath, groupPath);
    makeDir(uploadPath + groupPath, contentPath);

    return groupPath + contentPath;
  }

  private static String calcGroupPath(Long numericId) {
    double result = Math.floor((double) (numericId + 1000) / 1000) * 1000;
    return String.valueOf(Math.round(result));
  }

  private static void makeDir(String uploadPath, String... paths) {

    if (new File(paths[paths.length - 1]).exists()) {
      return;
    }

    for (String path : paths) {
      File dirPath = new File(uploadPath + path);

      if (!dirPath.exists()) {
        if (!dirPath.mkdir()) {
          log.error("Failed to create directory");
          throw new IllegalStateException("Failed to create directory");
        }
      }
    }
  }

  private static String makeFilePath(String uploadPath, String path, String fileName) {
    String filePath = uploadPath + path + File.separator + fileName;
    return filePath.substring(uploadPath.length()).replace(File.separatorChar, '/');
  }


  public static void deleteFile(String uploadPath, String fileName) {
    File file = new File(uploadPath + File.separatorChar + fileName);
    log.debug("Attempting to delete file: {}", file.getAbsolutePath());

    if (!file.exists()) {
      log.info("File does not exist: {}", file.getAbsolutePath());
      return;
    }

    if (!file.delete()) {
      log.error("Failed to delete file: {}", file.getAbsolutePath());
      if (!file.canWrite()) {
        log.error("File is not writable");
      }
      throw new IllegalStateException("Failed to delete file: " + file.getAbsolutePath());
    } else {
      log.debug("File deleted successfully: {}", file.getAbsolutePath());
    }
  }

  public static void deleteFolder(String uploadPath, Long postId) {
    String targetFolder = (uploadPath + File.separatorChar + calcGroupPath(postId) + File.separatorChar + postId);
    FileSystemUtils.deleteRecursively(new File(targetFolder));
  }

}
