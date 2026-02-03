# IndexingApplication.md

## 목적
`IndexingApplication`은 **색인만 실행**하기 위한 전용 엔트리 포인트입니다.  
웹 서버를 띄우지 않고, 데이터 색인 작업만 수행하도록 설계했습니다.

---

## 실행 흐름 (초보 개발자용)

아래 순서대로 동작합니다.

### 1) IndexingApplication 시작

```java
new SpringApplicationBuilder(AiSearchGptApplication.class)
    .properties(Map.of("ai-search.bootstrap-index", "true"))
    .run(args);
```

- `AiSearchGptApplication`을 기반으로 Spring Boot 애플리케이션을 실행합니다.
- 실행 시점에 `ai-search.bootstrap-index=true` 값을 **환경 설정으로 주입**합니다.

---

### 2) Spring Boot가 설정값을 읽음

Spring Boot는 애플리케이션 시작 시점에:

- `application.yml`
- `환경 변수`
- `properties(...)` 로 주입된 값

등을 모두 합쳐 **최종 설정(Environment)** 을 만듭니다.

따라서 `ai-search.bootstrap-index=true`가 **정상적으로 반영**됩니다.

---

### 3) BootstrapIndexer 실행 조건

`BootstrapIndexer`에는 다음 조건이 붙어 있습니다.

```java
@ConditionalOnProperty(prefix = "ai-search", name = "bootstrap-index", havingValue = "true")
```

뜻:
> 환경 설정에 `ai-search.bootstrap-index=true`가 있으면  
> BootstrapIndexer를 실행한다.

---

### 4) BootstrapIndexer가 색인 실행

```java
indexManagementService.recreateIndex();
long count = productIndexingService.reindexSampleData();
```

- 인덱스를 새로 만들고
- JSON 데이터를 읽어
- 벡터 임베딩을 생성해서
- Elasticsearch에 bulk 색인합니다.

---

## CommandLineRunner의 역할
`BootstrapIndexer`는 `CommandLineRunner`를 구현합니다.

```java
@Component
@ConditionalOnProperty(prefix = "ai-search", name = "bootstrap-index", havingValue = "true")
public class BootstrapIndexer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapIndexer.class);

    private final IndexManagementService indexManagementService;
    private final ProductIndexingService productIndexingService;

    public BootstrapIndexer(IndexManagementService indexManagementService,
                            ProductIndexingService productIndexingService) {
        this.indexManagementService = indexManagementService;
        this.productIndexingService = productIndexingService;
    }

    @Override
    public void run(String... args) {
        indexManagementService.recreateIndex();
        long count = productIndexingService.reindexSampleData();
        log.info("Indexed {} documents into Elasticsearch", count);
    }
}

```

### CommandLineRunner란?
- Spring Boot가 **애플리케이션 시작을 끝낸 직후** 자동으로 실행해주는 인터페이스입니다.
- `run(String... args)` 메서드가 딱 한 번 호출됩니다.

즉, `BootstrapIndexer`는
> "스프링 컨텍스트가 준비되면 바로 색인을 실행하라"  
라는 역할을 담당합니다.

---

## 요약 한 줄

**IndexingApplication은 "bootstrap-index=true"를 주입해서,  
조건이 맞으면 BootstrapIndexer가 자동 실행되도록 만든 구조입니다.**

---

## 참고
- 색인 전용 실행은 서버 없이도 가능합니다.
- 필요하면 실행 시 `--spring.main.web-application-type=none` 옵션을 추가하면 됩니다.
