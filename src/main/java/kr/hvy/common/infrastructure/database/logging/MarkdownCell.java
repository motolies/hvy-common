package kr.hvy.common.infrastructure.database.logging;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * SQL 결과 값 하나를 GFM 마크다운 표의 셀 텍스트로 바꾼다.
 *
 * <p>로그를 읽을 때의 디코딩 규칙:
 * <ul>
 *   <li>{@code null}: SQL NULL</li>
 *   <li>{@code "…"}: 문자열 값. 비었거나({@code ""}), null 이거나(대소문자 무시, {@code "null"}),
 *       따옴표로 시작하거나({@code ""abc""}), 앞뒤 공백이 있을 때({@code " z "})만 따옴표를 붙인다.
 *       안쪽 따옴표는 그대로이므로 바깥 한 쌍만 벗기면 원래 값이다</li>
 *   <li>{@code \\} {@code \|} {@code \*} {@code \`} {@code \~} {@code \_} {@code \<} {@code \]} {@code \&}: 그 문자 하나</li>
 *   <li>{@code \n} {@code \r}: 개행·캐리지리턴({@code \r\n}은 {@code \n} 하나)</li>
 *   <li>{@code 2026-11-03T09:04:50.856763+09:00}: 순간 값(Timestamp·Instant·OffsetDateTime·ZonedDateTime·java.util.Date·Calendar)은
 *       JVM 기본 시간대 오프셋을 붙인 ISO-8601({@code OffsetDateTime.toString()}, 소수부는 0·3·6·9자리).
 *       LocalDateTime·sql.Date·sql.Time 등 지역 값은 시간대 없는 ISO 그대로. 표현 범위를 벗어나는 순간 값
 *       (PostgreSQL {@code infinity}를 pgjdbc 가 {@code OffsetDateTime.MAX}로 돌려주는 경우)은 원래 {@code toString()} 표기</li>
 *   <li>앞부분 + {@code ...}: 최대 길이(UTF-16 단위) 초과 절단. 이스케이프 전 원문 기준이며 서로게이트 쌍을 가르지 않는다</li>
 * </ul>
 *
 * <p>이스케이프는 CommonMark + GFM(tables·strikethrough)에서 셀이 입력 그대로 보이는 것을 계약으로 하고,
 * 원시 로그 가독성을 위해 구문이 되는 자리에서만 건다. {@code \ | * ` ~}는 항상,
 * {@code _}는 양옆이 글자·숫자가 아닐 때만(단어 안 밑줄은 플랭킹 규칙상 강조가 되지 않는다),
 * {@code <}는 태그·URI autolink 시작(영문자·/·!·?)이거나 이메일 autolink({@code <id@host>}) 꼴일 때만,
 * {@code ]}는 링크 꼬리({@code ](}·{@code ][})일 때만, {@code &}는 엔티티 참조 꼴일 때만 건다.
 * 역슬래시를 두 배로 하는 것은 GFM 표 파서가 {@code \|}만 풀고 나머지 역슬래시는 인라인 파서로 넘기기 때문이며,
 * 그래야 {@code a\|b}와 {@code a|b}가 렌더링 뒤에도 구분된다.
 * 알려진 한계: 렌더링 뒤에는 실제 개행·캐리지리턴({@code \n}·{@code \r})과 데이터의 역슬래시+n/r({@code \\n}·{@code \\r})이
 * 같아 보인다(원시 로그는 역슬래시가 두 배라 구분된다).
 */
final class MarkdownCell {

  static final String NULL = "null";
  private static final String ELLIPSIS = "...";
  /** CommonMark 가 디코딩하는 엔티티 참조 꼴: {@code &#10; &#x1F; &amp;}(이름은 영문자로 시작, 최대 32자) */
  private static final Pattern ENTITY =
      Pattern.compile("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[A-Za-z][A-Za-z0-9]{0,31});");
  /** CommonMark 이메일 autolink {@code <id@host>}. 로컬파트는 숫자·기호로도 시작할 수 있어 '<' 다음 문자만으로는 못 거른다 */
  private static final Pattern EMAIL_AUTOLINK = Pattern.compile(
      "<[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
          + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*>");

  private MarkdownCell() {
  }

  /** SQL NULL 은 따옴표 없는 null. 그 외는 텍스트화 → 절단(maxLength) → 이스케이프 → 필요 시 따옴표. */
  static String of(Object value, int maxLength) {
    if (ObjectUtils.anyNull(value)) {
      return NULL;
    }
    String text = truncate(text(value), maxLength);
    String escaped = escape(text);
    return needsQuotes(text) ? "\"" + escaped + "\"" : escaped;
  }

  /**
   * 순간 값은 JVM 시간대 오프셋을 붙인 ISO-8601, 지역 값·그 외는 toString(java.time 은 이미 ISO).
   * toString 이 null 을 돌려주면 빈 문자열로, 시간대 변환이 범위를 벗어나면 원래 표기로 남겨 셀 하나 때문에 표를 잃지 않는다.
   */
  private static String text(Object value) {
    try {
      return StringUtils.defaultString(switch (value) {
        case Timestamp ts -> offset(ts.toInstant());
        case java.sql.Date d -> d.toString();        // java.util.Date 보다 먼저: toInstant() 가 예외를 던진다
        case java.sql.Time t -> t.toString();
        case java.util.Date d -> offset(d.toInstant());
        case Instant i -> offset(i);
        case OffsetDateTime o -> offset(o.toInstant());
        case ZonedDateTime z -> offset(z.toInstant()); // "[Asia/Seoul]" 꼬리를 떼고 오프셋만 남긴다
        case Calendar c -> offset(c.toInstant());
        default -> value.toString();
      });
    } catch (DateTimeException | ArithmeticException e) {
      // infinity 등 표현 범위 밖 순간 값(pgjdbc 는 OffsetDateTime.MAX/MIN 으로 돌려준다)은 원래 표기로
      return StringUtils.defaultString(value.toString());
    }
  }

  /** 순간을 JVM 기본 시간대의 OffsetDateTime 으로 바꿔 ISO-8601 로 찍는다. */
  private static String offset(Instant instant) {
    return instant.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
  }

  /** 비었거나 null 이거나 따옴표로 시작하거나 앞뒤 공백이면 따옴표로 감싼다(안쪽 따옴표는 그대로). */
  private static boolean needsQuotes(String text) {
    return text.isEmpty() || text.equalsIgnoreCase(NULL) || text.charAt(0) == '"'
        || Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(text.length() - 1));
  }

  /**
   * maxLength 초과는 (maxLength - 3)자 + "...". 절단점이 서로게이트 쌍 사이면 한 글자 덜 남긴다.
   * maxLength 가 3 이하이면 남길 글자가 없으므로 "..." 만 남겨 잘렸다는 사실은 드러낸다.
   */
  private static String truncate(String text, int maxLength) {
    if (text.length() <= maxLength) {
      return text;
    }
    int cut = Math.max(0, maxLength - ELLIPSIS.length());
    if (cut > 0 && Character.isHighSurrogate(text.charAt(cut - 1))) {
      cut--;
    }
    return text.substring(0, cut) + ELLIPSIS;
  }

  /** CommonMark/GFM 인라인 구문이 되는 자리만 역슬래시로 막는다(규칙은 클래스 Javadoc). */
  private static String escape(String s) {
    StringBuilder sb = new StringBuilder(s.length() + 8);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\', '|', '*', '`', '~' -> sb.append('\\').append(c);
        case '\n' -> sb.append("\\n");
        case '\r' -> {
          if (!nextIs(s, i, '\n')) {
            sb.append("\\r");
          }
        }
        case '_' -> sb.append(intraword(s, i) ? "_" : "\\_");
        case '<' -> sb.append(tagOrAutolink(s, i) ? "\\<" : "<");
        case ']' -> sb.append(nextIs(s, i, '(') || nextIs(s, i, '[') ? "\\]" : "]");
        case '&' -> sb.append(ENTITY.matcher(s).region(i, s.length()).lookingAt() ? "\\&" : "&");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }

  /** i 번째 문자 바로 뒤가 expected 인지(범위 검사 포함). */
  private static boolean nextIs(String s, int i, char expected) {
    return i + 1 < s.length() && s.charAt(i + 1) == expected;
  }

  /** 양옆이 모두 글자·숫자면 CommonMark 플랭킹 규칙상 '_' 는 강조를 열지도 닫지도 못한다. */
  private static boolean intraword(String s, int i) {
    return i > 0 && i + 1 < s.length()
        && Character.isLetterOrDigit(s.charAt(i - 1)) && Character.isLetterOrDigit(s.charAt(i + 1));
  }

  /**
   * '<' 뒤에 영문자·'/'·'!'·'?' 가 오면 HTML 태그 또는 URI autolink 의 시작이다.
   * 이메일 autolink 는 로컬파트가 숫자·기호(<123@x.y>, <=@x.y>)로도 시작하므로 원문에 정규식을 맞춰 본다.
   */
  private static boolean tagOrAutolink(String s, int i) {
    if (i + 1 >= s.length()) {
      return false;
    }
    char next = s.charAt(i + 1);
    boolean tag = (next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z')
        || next == '/' || next == '!' || next == '?';
    return tag || EMAIL_AUTOLINK.matcher(s).region(i, s.length()).lookingAt();
  }
}
