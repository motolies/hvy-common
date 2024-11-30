package kr.hvy.common.security;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {

  /**
   * 현재 인증된 사용자의 사용자 이름을 반환합니다.
   *
   * @return 사용자 이름, 익명 사용자의 경우 null, 인증되지 않은 경우 null
   */
  public static String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated() ||
        authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof UserDetails) {
      return ((UserDetails) principal).getUsername();
    } else if (principal instanceof String) {
      return (String) principal;
    }

    return null;
  }

  /**
   * 현재 인증된 사용자의 역할 목록을 반환합니다.
   *
   * @return 역할 목록 또는 인증되지 않은 경우 빈 리스트
   */
  public static Collection<String> getRoles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || authentication.getAuthorities() == null || authentication instanceof AnonymousAuthenticationToken) {
      return Collections.emptyList();
    }

    return authentication.getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());
  }

  /**
   * 현재 사용자가 인증되었는지 확인합니다.
   *
   * @return 인증된 사용자이면 true, 익명 사용자 또는 인증되지 않은 경우 false
   */
  public static boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    return authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
  }

  /**
   * 현재 사용자가 특정 역할을 가지고 있는지 확인합니다.
   *
   * @param role 확인할 역할
   * @return 사용자가 역할을 가지고 있으면 true, 그렇지 않으면 false
   */
  public static boolean hasRole(String role) {
    return getRoles().contains(role);
  }

  /**
   * 현재 사용자가 ROLE_ADMIN 역할을 가지고 있는지 확인합니다.
   *
   * @return 사용자가 ROLE_ADMIN 역할을 가지고 있으면 true, 그렇지 않으면 false
   */
  public static boolean hasAdminRole() {
    return hasRole("ROLE_ADMIN");
  }
}