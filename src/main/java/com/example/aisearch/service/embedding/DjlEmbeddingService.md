# DjlEmbeddingService 설명서 (초보 개발자용)

이 문서는 `DjlEmbeddingService`가 **무엇을 하는지**, **왜 필요한지**, **어떻게 동작하는지**를 쉽게 설명합니다.

---

## ✅ 1) 이 클래스가 하는 일

`DjlEmbeddingService`는 텍스트를 **임베딩 벡터로 변환**하는 서비스입니다.
쉽게 말해, 사람이 입력한 문장을 **숫자 배열(벡터)**로 바꿔서
검색이나 유사도 계산에 사용할 수 있게 합니다.

---

## 🧩 2) 주요 역할 요약

- DJL 모델을 로드한다
- 텍스트를 임베딩 벡터로 변환한다
- 필요하면 L2 정규화로 벡터 크기를 맞춘다
- 애플리케이션 종료 시 모델 리소스를 정리한다

---

## 🧪 3) 핵심 흐름

### (1) 모델 로딩 준비
```java
Criteria.Builder<String, float[]> criteria = Criteria.builder()
        .setTypes(String.class, float[].class)
        .optApplication(Application.NLP.TEXT_EMBEDDING)
        .optProgress(new ProgressBar());
```
- DJL이 텍스트 → 벡터 변환 모델을 사용할 수 있게 설정한다.

---

### (2) 모델 소스 결정 (로컬 vs URL)
```java
EmbeddingModelSource modelSource = modelSourceResolver.load();
```
- 설정값을 보고 **로컬 경로** 또는 **URL 모델** 중 하나를 선택한다.
- 로컬이면 `TextEmbeddingTranslatorFactory`를 붙여준다.

---

### (3) 모델 로딩 및 Predictor 생성
```java
model = buildCriteria.loadModel();
predictor = model.newPredictor();
```
- 실제 모델을 메모리에 올리고
- 추론을 담당하는 `Predictor`를 만든다.

---

### (4) 차원 수 확인
```java
float[] probe = predictRaw("한글 식품 벡터 검색 테스트");
dimensions = probe.length;
```
- 임베딩 벡터의 차원 수를 1회 추론으로 확인한다.

---

### (5) 실제 임베딩 생성
```java
float[] raw = predictRaw(text);
return embeddingNormalizer.l2Normalize(raw);
```
- 입력 텍스트를 임베딩으로 변환
- 필요 시 정규화 수행

---

## 📌 4) 핵심 메서드 요약

| 메서드 | 역할 |
|--------|------|
| `init()` | DJL 모델 로딩 + Predictor 준비 |
| `embed(text)` | 텍스트 → 벡터 변환 |
| `dimensions()` | 벡터 차원 수 반환 |
| `close()` | 리소스 정리 |

---

## ❓ 5) 왜 분리된 컴포넌트를 쓰나?

- `EmbeddingModelSourceLoader` → 로컬/URL 모델 선택
- `EmbeddingNormalizer` → 정규화 로직 분리

이렇게 나누면
- 각 역할이 명확해지고
- 테스트/교체가 쉬워진다

---

## ⚠️ 6) 주의할 점

- 모델 로딩 실패 시 애플리케이션이 시작되지 않을 수 있다
- URL 모델은 네트워크/인증서 문제에 민감하다
- 로컬 모델은 파일 경로가 정확해야 한다

---

## 🔗 7) 관련 파일

- `src/main/java/com/example/aisearch/service/embedding/DjlEmbeddingService.java`
- `src/main/java/com/example/aisearch/service/embedding/model/EmbeddingModelSourceLoader.java`
- `src/main/java/com/example/aisearch/service/embedding/model/EmbeddingNormalizer.java`
- `src/main/java/com/example/aisearch/service/embedding/model/EmbeddingModelSource.java`

