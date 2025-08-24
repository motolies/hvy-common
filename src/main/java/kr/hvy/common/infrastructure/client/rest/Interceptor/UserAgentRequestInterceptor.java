package kr.hvy.common.infrastructure.client.rest.Interceptor;

import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserAgentRequestInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    request.getHeaders().set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
    // Referer 값을 동적으로 설정
    URI currentUri = request.getURI();
    String refererUri = currentUri.getScheme() + "://" + currentUri.getHost() + (currentUri.getPort() != -1 ? ":" + currentUri.getPort() : "");
    request.getHeaders().set("Referer", refererUri);

    return execution.execute(request, body);
  }

}