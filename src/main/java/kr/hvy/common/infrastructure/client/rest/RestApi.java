package kr.hvy.common.infrastructure.client.rest;

import java.text.MessageFormat;
import java.util.Optional;
import kr.hvy.common.core.exception.RestApiException;
import kr.hvy.common.infrastructure.notification.slack.Notify;
import kr.hvy.common.infrastructure.notification.slack.NotifyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
public class RestApi {

  private final RestClient restClient;
  protected final Optional<Notify> notify;
  protected final Optional<String> defaultErrorChannel;

  public <T> T get(String uri, Class<T> responseType) {
    return exchange(uri, HttpMethod.GET, null, null, responseType, (Object) null);
  }

  public <T> T get(String uri, MultiValueMap<String, String> urlParameters, Class<T> responseType) {
    return exchange(uri, HttpMethod.GET, null, urlParameters, responseType, (Object) null);
  }

  public <T> T get(String uri, MultiValueMap<String, String> urlParameters, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.GET, null, urlParameters, responseType, uriVariables);
  }

  public <T> T get(String uri, MultiValueMap<String, String> headers, MultiValueMap<String, String> urlParameters, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.GET, createHttpEntity(headers, null), urlParameters, responseType, uriVariables);
  }

  public <T, P> T post(String uri, P body, Class<T> responseType) {
    return exchange(uri, HttpMethod.POST, createHttpEntity(null, body), null, responseType, (Object) null);
  }

  public <T, P> T post(String uri, P body, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.POST, createHttpEntity(null, body), null, responseType, uriVariables);
  }

  public <T, P> T post(String uri, MultiValueMap<String, String> headers, P body, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.POST, createHttpEntity(headers, body), null, responseType, uriVariables);
  }

  public <T, P> T post(String uri, MultiValueMap<String, String> headers, MultiValueMap<String, String> urlParameters, P body, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.POST, createHttpEntity(headers, body), urlParameters, responseType, uriVariables);
  }

  public <T, P> T put(String uri, P body, Class<T> responseType) {
    return exchange(uri, HttpMethod.PUT, createHttpEntity(null, body), null, responseType, (Object) null);
  }

  public <T, P> T put(String uri, P body, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.PUT, createHttpEntity(null, body), null, responseType, uriVariables);
  }

  public <T, P> T put(String uri, MultiValueMap<String, String> headers, P body, Class<T> responseType) {
    return exchange(uri, HttpMethod.PUT, createHttpEntity(headers, body), null, responseType, (Object) null);
  }

  public <T, P> T put(String uri, MultiValueMap<String, String> headers, P body, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.PUT, createHttpEntity(headers, body), null, responseType, uriVariables);
  }

  public <T> T delete(String uri, Class<T> responseType) {
    return exchange(uri, HttpMethod.DELETE, null, null, responseType, (Object) null);
  }

  public <T> T delete(String uri, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.DELETE, null, null, responseType, uriVariables);
  }

  public <T> T delete(String uri, MultiValueMap<String, String> headers, Class<T> responseType) {
    return exchange(uri, HttpMethod.DELETE, createHttpEntity(headers, null), null, responseType, (Object) null);
  }

  public <T> T delete(String uri, MultiValueMap<String, String> headers, Class<T> responseType, Object... uriVariables) {
    return exchange(uri, HttpMethod.DELETE, createHttpEntity(headers, null), null, responseType, uriVariables);
  }


  private HttpEntity<?> createHttpEntity(MultiValueMap<String, String> headers, Object body) {
    HttpEntity<?> requestEntity = null;
    if (MapUtils.isNotEmpty(headers) && ObjectUtils.isEmpty(body)) {
      requestEntity = new HttpEntity<>(headers);
    } else if (ObjectUtils.isEmpty(headers) && ObjectUtils.isNotEmpty(body)) {
      requestEntity = new HttpEntity<>(body);
    } else if (MapUtils.isNotEmpty(headers) && ObjectUtils.isNotEmpty(body)) {
      requestEntity = new HttpEntity<>(body, headers);
    }
    return requestEntity;
  }

  private <R, P> R exchange(String uri, HttpMethod method, HttpEntity<P> requestEntity, MultiValueMap<String, String> urlParameters, Class<R> responseType, Object... uriVariables) {

    if (MapUtils.isNotEmpty(urlParameters)) {
      UriComponents uriComponents = UriComponentsBuilder.fromUriString(uri)
          .queryParams(urlParameters)
          .build();
      uri = uriComponents.toUriString();
    }

    // tip : uriVariables 사용하려면 http://example.com?param={param} 형태로 uri를 만들어야 한다.
    RequestBodySpec requestBodySpec = restClient
        .method(method)
        .uri(uri, uriVariables)
        .accept(MediaType.APPLICATION_JSON);

    if (ObjectUtils.isNotEmpty(requestEntity) && ObjectUtils.isNotEmpty(requestEntity.getHeaders())) {
      requestBodySpec.headers(httpHeaders -> httpHeaders.addAll(requestEntity.getHeaders()));
    }

    if (ObjectUtils.isNotEmpty(requestEntity) && ObjectUtils.isNotEmpty(requestEntity.getBody())) {
      requestBodySpec.body(requestEntity.getBody());
    }

    return requestBodySpec.exchange((request, response) -> {
      if (response.getStatusCode().is2xxSuccessful()) {
        return response.bodyTo(responseType);
      }

      String errorMessage = MessageFormat.format("RestApi Error \n uri: {} \n status: {} \n response: {}", request.getURI(), response.getStatusCode(), response.getBody());
      RestApiException ex = new RestApiException(errorMessage);
      log.error(ex.getMessage(), ex);

      notify.ifPresent(value -> value.sendMessage(NotifyRequest.builder()
          .channel(defaultErrorChannel.orElse("#hvy-error"))
          .exception(ex)
          .build()));

      throw ex;
    });
  }

}
