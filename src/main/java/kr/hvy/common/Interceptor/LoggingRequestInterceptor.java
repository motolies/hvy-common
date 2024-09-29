package kr.hvy.common.Interceptor;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    log.debug("URI                 : {}", request.getURI());
    log.debug("Method          : {}", request.getMethod());
    log.debug("Headers         : {}", request.getHeaders());
    log.debug("Request body: {}", new String(body));
    return execution.execute(request, body);
  }

}