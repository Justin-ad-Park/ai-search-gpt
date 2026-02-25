## AS-IS / TO-BE 비교

### AS-IS (Supplier 사용)

경로 값을 직접 들고 있지 않고, `Supplier<String>`를 통해 필요 시 꺼내 쓰는 구조였습니다.

```java
private final Supplier<String> ruleFilePathSupplier;

public JsonCategoryBoostRules(
        ResourceLoader resourceLoader,
        ObjectMapper objectMapper,
        String ruleFilePath,
        long cacheTtlSeconds
) {
    this(resourceLoader, objectMapper, () -> ruleFilePath, cacheTtlSeconds);
}

private String currentRulePath() {
    String path = ruleFilePathSupplier.get();
    if (path == null || path.isBlank()) {
        throw new IllegalStateException("카테고리 부스팅 룰 파일 경로가 비어 있습니다.");
    }
    return path;
}
```

특징:

- 테스트에서 `AtomicReference<String>` + `pathRef::get`으로 경로 변경 시나리오를 만들기 쉬움
- 반면 코드만 읽으면 "왜 함수형 인터페이스를 썼는지"를 추가로 해석해야 함

### TO-BE (String + setter 사용)

경로를 문자열 필드로 직접 관리하고, 필요 시 setter로 변경하는 구조입니다.

```java
private volatile String ruleFilePath;

public JsonCategoryBoostRules(
        ResourceLoader resourceLoader,
        ObjectMapper objectMapper,
        String ruleFilePath,
        long cacheTtlSeconds
) {
    this.resourceLoader = resourceLoader;
    this.objectMapper = objectMapper;
    this.ruleFilePath = ruleFilePath;
    this.versionCheckGate = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(Duration.ofSeconds(Math.max(1L, cacheTtlSeconds)))
            .build();
    this.currentEntry = new AtomicReference<>(CategoryBoostCacheEntry.empty());
    loadInitialRules();
}

void setRuleFilePath(String ruleFilePath) {
    this.ruleFilePath = ruleFilePath;
    this.versionCheckGate.invalidate(VERSION_CHECK_GATE_KEY);
}

private String currentRulePath() {
    String path = ruleFilePath;
    if (path == null || path.isBlank()) {
        throw new IllegalStateException("카테고리 부스팅 룰 파일 경로가 비어 있습니다.");
    }
    return path;
}
```

특징:

- 생성자와 필드 의미가 직관적이라 읽기 쉬움
- 함수형 인터페이스 개념이 없어도 이해 가능
- 경로 변경 시 `versionCheckGate`를 비워 다음 조회/재로딩에서 즉시 새 경로 반영

## 1) 함수형 인터페이스(Supplier) 기초 설명

함수형 인터페이스는 "메서드가 1개인 인터페이스"입니다.  
대표 예시:

```java
Supplier<String> s = () -> "value";
String v = s.get();
```

- `Supplier<T>`: 값을 "필요할 때 꺼내주는" 함수
- `Function<T, R>`: 입력을 받아 출력으로 바꾸는 함수
- `Consumer<T>`: 입력을 받아 처리만 하고 반환값 없음

과거 `JsonCategoryBoostRules`는 경로를 동적으로 바꾸는 테스트를 위해 `Supplier<String>`을 사용했습니다.




## 2) 지금 구조가 더 단순한 이유

현재는 `Supplier<String>` 대신 문자열 필드 + setter를 사용합니다.

```java
private volatile String ruleFilePath;

void setRuleFilePath(String ruleFilePath) {
    this.ruleFilePath = ruleFilePath;
    this.versionCheckGate.invalidate(VERSION_CHECK_GATE_KEY);
}
```

장점:

- 생성자 시그니처가 단순해짐
- "경로 값"이라는 의도가 직관적임
- 함수형 개념을 몰라도 읽기 쉬움

## 3) 테스트 코드 비교 (AS-IS vs TO-BE)

### AS-IS 테스트 (Supplier + AtomicReference)

```java
@Test
void shouldReloadRulesWhenVersionChangesByPathSwitching() {
    AtomicReference<String> pathRef = new AtomicReference<>("classpath:data/category_boosting_v1.json");
    JsonCategoryBoostRules rules = new JsonCategoryBoostRules(
            new DefaultResourceLoader(),
            new ObjectMapper(),
            pathRef::get,
            300
    );

    assertEquals(0.20, rules.findByKeyword("사과").orElseThrow().get("4"));

    pathRef.set("classpath:data/category_boosting_v2.json");
    rules.reload();

    assertEquals(0.30, rules.findByKeyword("사과").orElseThrow().get("4"));
}
```

특징:

- `pathRef::get`으로 경로를 지연 조회
- 람다 캡처 제약 때문에 `AtomicReference`를 같이 알아야 이해가 쉬움

AtomicReference를 사용한 이유:

- Java 람다는 지역 변수를 캡처할 때 해당 변수가 사실상 final(`effectively final`)이어야 합니다.
- 즉, 아래처럼 단순 `String path`를 람다에서 참조하면 나중에 `path = "..."`로 값을 바꿀 수 없습니다.
- 그래서 참조 자체는 final로 두고, 내부 값만 바꿀 수 있는 `AtomicReference<String>`를 사용했습니다.

```java
// 이 패턴은 컴파일 에러가 발생한다(람다에서 캡처한 지역 변수 재할당)
String path = "classpath:data/category_boosting_v1.json";
Supplier<String> supplier = () -> path;
path = "classpath:data/category_boosting_v2.json";
```

```java
// 참조는 고정하고 내부 값만 바꾸는 방식
AtomicReference<String> pathRef = new AtomicReference<>("classpath:data/category_boosting_v1.json");
Supplier<String> supplier = pathRef::get;
pathRef.set("classpath:data/category_boosting_v2.json");
```

### TO-BE 테스트 (String + setter)

```java
@Test
void shouldReloadRulesWhenVersionChangesByPathSwitching() {
    JsonCategoryBoostRules rules = new JsonCategoryBoostRules(
            new DefaultResourceLoader(),
            new ObjectMapper(),
            "classpath:data/category_boosting_v1.json",
            300
    );

    assertEquals(0.20, rules.findByKeyword("사과").orElseThrow().get("4"));

    rules.setRuleFilePath("classpath:data/category_boosting_v2.json");
    rules.reload();

    assertEquals(0.30, rules.findByKeyword("사과").orElseThrow().get("4"));
}
```

특징:

- 경로 변경 의도가 메서드 이름으로 직접 드러남
- 함수형 인터페이스/람다 캡처 지식 없이도 흐름 파악이 쉬움

## 4) 종합 결론: 어느 방식이 더 나은가?

현재 요구사항(룰 파일 경로 1개를 관리하고, 테스트에서 가끔 경로를 바꿔 재로딩 검증)에서는  
`String + setter` 방식이 더 적합합니다.

이유:

- 코드 의도가 직접적이다: "경로를 설정하고 reload 한다"
- 팀 온보딩이 쉽다: 함수형 인터페이스와 람다 캡처 규칙을 몰라도 이해 가능
- 유지보수 포인트가 줄어든다: 생성자/필드 구조가 단순해진다

`Supplier` 방식이 유리한 경우:

- 경로를 외부 상태에 따라 매번 동적으로 계산해야 하거나
- 값 공급 전략 자체를 주입해서 바꿔야 하는 확장 요구가 있을 때

이 프로젝트의 현재 문맥에서는 그런 요구가 강하지 않으므로,  
`String + setter`가 가독성과 실용성 측면에서 더 좋은 선택입니다.
