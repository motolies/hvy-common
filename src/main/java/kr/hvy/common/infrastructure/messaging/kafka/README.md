# Spring Cloud Stream Kafka Default DLQ

## 사용하지 않을 때 
```yaml
spring:
  cloud:
    stream:
      kafka:
        enabled: false # kafka 사용 여부
```
## 사용할 때
```yaml
spring:
  application.name: dlq
  kafka:
    bootstrap-servers: localhost:9092 # spring cloud stream 사용시 dlq가 사용하는 kafkaTemplate 설정, stream 설정과 동일하게 설정해야함
    default-dlq: # default dlq 설정 
      enableDlq: true
      dlqName: custom-dlq-name
      maxAttempts: 0 # 재시도 횟수
      retryInterval: 1000 # 재시도간 딜레이 (ms)
  cloud:
    stream:
      bindings:
        defaultDlq-in-0:
          destination: defaultDlq-topic-template
          group: defaultDlq-group
        statusUpdate-in-0:
          destination: statusUpdate-topic
          group: statusUpdate-group
        status-out-0:
          destination: statusUpdate-topic
      kafka:
        bindings: 
          statusUpdate-in-0: # 별도 토픽에 dlq 작성하더라도 default 적용시 동작하지 않음
            consumer:
              enableDlq: true
              dlqName: statusUpdate-dlq-only
```

---
## ListenerContainerWithDlqAndRetryCustomizer 생성 후 달라지는 부분
- dlq 헤더에서 추가 데이터의 keyName 변경 

기존
```json
{
	"x-original-offset": "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0013",
	"x-original-partition": "\u0000\u0000\u0000\u0001",
	"spring_json_header_types": "{\"contentType\":\"java.lang.String\",\"target-protocol\":\"java.lang.String\"}",
	"x-exception-message": "error occurred in message handler [org.springframework.cloud.stream.function.FunctionConfiguration$FunctionToDestinationBinder$1@43d5bb59]; Error occurred",
	"x-exception-fqcn": "org.springframework.messaging.MessageHandlingException",
	"x-original-topic": "statusUpdate-topic",
	"x-original-timestamp-type": "CreateTime",
	"contentType": "application/json",
	"x-original-timestamp": "\u0000\u0000\u0001���BX",
	"target-protocol": "kafka",
	"x-exception-stacktrace": "org.springframework.messaging.MessageHandlingException: error occurred in message handler ... 71 more\n"
}
```
<br>
적용 후
<br>

```json
{
	"kafka_dlt-original-consumer-group": "statusUpdate-group",
	"x-failed-consumer-group-name": "statusUpdate-group",
	"kafka_dlt-original-topic": "statusUpdate-topic",
	"kafka_dlt-original-offset": "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0018",
	"target-protocol": "kafka",
	"spring_json_header_types": "{\"contentType\":\"java.lang.String\",\"target-protocol\":\"java.lang.String\"}",
	"kafka_dlt-original-partition": "\u0000\u0000\u0000\u0001",
	"x-exception-message": "RuntimeException: Error occurred",
	"x-failed-consumer-app": "dlq",
	"kafka_dlt-original-timestamp-type": "CreateTime",
	"contentType": "application/json",
	"x-exception-stacktrace": "org.springframework.kafka.listener.ListenerExecutionFailedException: Listener failed... 66 more\n",
	"kafka_dlt-original-timestamp": "\u0000\u0000\u0001��:��"
}
```