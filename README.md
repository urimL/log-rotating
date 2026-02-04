## Log Rotating System

### 1. 프로젝트 목적

로그는 **기록 자체보다 관리가 더 중요**하다. 무제한으로 쌓이는 로그는 디스크 고갈과 장애 대응 속도 저하를 야기하므로, 이를 해결하기 위한 자동화 시스템을 설계·구현하는 것을 목표로 한다.

<br />

### 2. 로깅 라이브러리

- **SLF4J** : Logging Facade (Interface)
- **Log4j2** : Logging Implementation

<br />

### 3. 설계

> #### 3-1. 관심사 분리

로그의 성격에 따라 **물리적 저장 위치와 책임을 분리**하여 관리 효율성을 높인다.

| 구분  | 역할                    | 분리 기준   |
| ----- | ----------------------- | ----------- |
| APP   | 일반 비즈니스 로직 로그 | Log Level   |
| ERROR | 장애 추적용 예외 로그   | Log Level   |
| AUDIT | 보안·감사 증적 로그     | Logger Name |

---

##### 일반 어플리케이션 로그 (APP / ERROR)

- **Root Logger**에서 수신된 모든 로그 중 Appender + Filter 조합을 통해 로그 레벨 기준으로 분리 저장

```xml
<Root level="debug">
    <AppenderRef ref="Console" />
    <AppenderRef ref="APP_FILE" />
    <AppenderRef ref="ERROR_FILE" />
</Root>

<!-- ERROR file filter -->
<Filters>
	<ThresholdFilter level="WARN" onMatch="ACCEPT" onMismatch="DENY" />
</Filters>

<!-- APP file filter -->
<Filters>
	<ThresholdFilter level="WARN" onMatch="DENY" onMismatch="NEUTRAL" />
</Filters>
```

##### AUDIT 로그

- 전용 audit logger 활용하여 log level이 아닌 로거의 이름으로 분리

```xml
<Logger name="audit" level="info" additivity="false">
	<AppenderRef ref="AUDIT_FILE"/>
</Logger>
```

> #### 3-2. 용량 기반 롤링 및 압축

##### 정책

- 각 로그 파일이 50KB 도달 시 즉시 롤링 및 Gzip(.gz) 압축 수행

##### 검증

- 압축 프로세스가 1초 이내에 완료되는지 실시간 측정.
  - (현재 로그 파일 크기 < 이전 측정 파일 크기)인 순간(롤링 포착 시간)을 찾아 (압축 파일 생성 시간 - 롤링 포착 시간) 차이 값이 1000ms(1초) 이내면 PASS로 판단.
  ```java
  if (currentSize > 0 && lastSize > 0 && currentSize < lastSize) {
      long rotationTime = System.currentTimeMillis();
  }
  ```
  ![alt text](image.png)

> #### 3-3. 데이터 Archive

##### 단순 삭제가 아닌, 데이터 중요도에 따른 계층형 관리

- Hot (Active): 현재 기록 중인 `.log` 파일.
- Warm (Archived): 최근 발생한 `.gz` 압축 파일.
- Cold (Deleted): 생성 후 2초(시연 설정) 경과 시 별도의 `deleted/` 폴더로 자동 이관.

  ![alt text](image-1.png)

<br />

### 4. 프로젝트 구조

```java
log-rotating/
├── src/main/java/log_rotating/
│   ├── LogRotateTest.java (검증 및 이관 로직)
│   └── RollingTarget.java (로그 타겟 객체)
└── logs/
    ├── app/
    │   ├── app.log (Active)
    │   ├── archived/ (Warm)
    │   └── deleted/ (Cold: 2초 경과 로그 이관 폴더)
    ├── audit/ ...
    └── error/ ...
```

<br />

### 5. 기대 효과

> #### 운영 안정성

디스크 풀(Full) 장애 방지 및 스토리지 가용성 확보

> #### 보안성

AUDIT 로그를 별도 관리하여 보안 사고 시 무결성 있는 증적 확보

> #### 비용 최적화 운영

운영 서버 자원을 효율적으로 사용하면서도 과거 데이터 보존 가능
