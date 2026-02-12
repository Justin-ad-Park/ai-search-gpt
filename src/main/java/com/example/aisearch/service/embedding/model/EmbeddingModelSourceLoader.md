# 📘 EmbeddingModelSourceLoader 설명서 (초보 개발자용)

이 문서는 `EmbeddingModelSourceLoader`가 **무엇을 하는지**, **왜 필요한지**, **어떻게 동작하는지**를 쉽게 설명합니다.

---

## ✅ 1) 이 클래스는 왜 필요한가?

임베딩 모델은 두 가지 방식으로 사용할 수 있습니다.

1. **로컬 모델** 🗂️
   - 컴퓨터에 미리 내려받은 모델을 파일로 보관
   - 장점: 빠르고 안정적 (네트워크 의존 X)

2. **원격 모델(URL)** 🌐
   - 실행 시 인터넷에서 모델 다운로드
   - 장점: 설치가 간편 (파일 관리 X)

`EmbeddingModelSourceLoader`는 이 두 방식 중에서 **무엇을 쓸지 결정**해주는 역할을 합니다.

---

## 🧩 2) 주요 역할 요약

- 설정값을 읽어서
- **로컬 경로가 있으면 로컬 모델 사용**
- 없으면 **원격 URL 모델 사용**
- 최종적으로 `EmbeddingModelSource` 객체를 만들어 반환

---

## 🧪 3) 핵심 코드와 설명

### 코드

```java
public EmbeddingModelSource load() throws IOException {
    String modelPath = properties.embeddingModelPath();
    if (modelPath != null && !modelPath.isBlank() && !"__NONE__".equalsIgnoreCase(modelPath.trim())) {
        Resource resource = resourceLoader.getResource(modelPath);
        Path resolvedPath = resource.getFile().toPath();
        log.info("[EMBED_MODEL] using model path: {} -> {}", modelPath, resolvedPath);
        return new EmbeddingModelSource(resolvedPath, null, true);
    }

    String modelUrl = properties.embeddingModelUrl();
    log.info("[EMBED_MODEL] using model url: {}", modelUrl);
    return new EmbeddingModelSource(null, modelUrl, false);
}
```

---

### 🔍 (1) 로컬 모델 경로 확인

```java
String modelPath = properties.embeddingModelPath();
```
- 설정에서 **로컬 모델 경로**를 읽습니다.


```java
if (modelPath != null && !modelPath.isBlank() && !"__NONE__".equalsIgnoreCase(modelPath.trim())) {
```
- 경로가 비어 있지 않고
- `__NONE__` 같은 “사용 안 함” 값도 아니라면
- **로컬 모델을 우선 사용합니다.**

---

### 📁 (2) 로컬 모델 파일 실제 위치 확인

```java
Resource resource = resourceLoader.getResource(modelPath);
Path resolvedPath = resource.getFile().toPath();
```
- Spring의 `ResourceLoader`를 이용해
- classpath 경로를 실제 파일 경로로 바꿉니다.

예시:
- `classpath:/model/multilingual-e5-small-ko-v2`
- → `/Users/.../build/resources/main/model/multilingual-e5-small-ko-v2`

---

### 🧱 (3) EmbeddingModelSource 생성

```java
return new EmbeddingModelSource(resolvedPath, null, true);
```

`EmbeddingModelSource`는 아래 정보를 담습니다.

| 필드 | 의미 |
|------|------|
| `modelPath` | 로컬 모델의 실제 경로 |
| `modelUrl`  | 원격 모델 URL (로컬이면 null) |
| `requiresTranslatorFactory` | 로컬 모델이면 true |


#### ❓ `requiresTranslatorFactory`는 왜 `true`인가?
- 로컬 모델 로딩 시 DJL이 자동으로 Translator를 잘 못 찾는 경우가 있음
- 그래서 **TextEmbeddingTranslatorFactory를 강제로 지정**해야 안정적으로 동작

### 🧠 추가 설명

DJL은 모델을 로드할 때 **번역기(Translator)**를 선택해야 하는데,  
로컬 디렉터리에서 로딩할 때는 **자동으로 적절한 Translator를 찾지 못하는 경우**가 있습니다.

특히 HuggingFace 기반 텍스트 임베딩 모델은  
`TextEmbeddingTranslatorFactory`가 필요합니다.  
이를 명시하지 않으면 다음과 같은 문제가 생길 수 있어요:

- 모델은 로드되지만 추론이 실패
- 또는 다른 타입 Translator가 선택돼서 결과가 이상함

### ✅ 그래서 왜 true 인가?
- 로컬 모델 경로(`embedding-model-path`)를 쓰는 경우  
  → Translator를 명시해줘야 안정적으로 동작  
  → 그래서 `true`
- 원격 URL 모델(`embedding-model-url`)을 쓰는 경우  
  → DJL이 모델 메타데이터를 보고 Translator를 자동으로 고를 수 있음  
  → 그래서 `false`

### ✨ 요약
- `requiresTranslatorFactory = true`  
  → “로컬 모델일 때는 TranslatorFactory를 강제로 붙여야 함”
- 그 덕분에 로컬 모델도 안정적으로 임베딩 추론이 가능해진다.

### 📦 메타데이터는 어디에 있나?
- 로컬 모델 기준으로 메타데이터는 모델 디렉터리 안의 파일들에 있습니다.

대표적으로:
- `config.json` (모델 구조/타입 정보)
- `tokenizer.json` / vocab 관련 파일들 (토크나이저 정보)
- DJL 메타데이터가 있으면 `model.properties` 또는 `serving.properties`

`TextEmbeddingTranslatorFactory`는 이런 파일들을 읽어서  
“텍스트 임베딩에 맞는 처리 방식”을 결정합니다.

예를 들어 `multilingual-e5-small-ko-v2.pt`의 경우  
`serving.properties` 파일에 아래와 같이 `translatorFactory`가 명시되어 있습니다.

```
translatorFactory=ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
```

---

### 🌐 (4) 로컬 모델이 없으면 URL 모델 사용

```java
String modelUrl = properties.embeddingModelUrl();
return new EmbeddingModelSource(null, modelUrl, false);
```

- 로컬 경로가 없으면 **원격 모델 URL**을 사용
- false : DJL 모델의 경우 URL 에서 제공되는 메타데이터를 보고 적절한 Translator를 자동 선택 가능

---

## 🧭 4) 동작 흐름 한 줄 요약

**"로컬 경로가 있으면 로컬 모델, 없으면 URL 모델을 선택한다."**

---

## 🙋 5) FAQ

### Q. 로컬 모델 경로가 있는데도 URL이 사용될 수 있나요?
A. 경로가 비어 있거나 `__NONE__`로 설정되어 있으면 URL로 넘어갑니다.

### Q. 왜 `__NONE__` 같은 값을 쓰나요?
A. 환경 변수로 경로를 쉽게 끄고 켤 수 있도록 만든 장치입니다.

### Q. URL 모델은 왜 느릴 수 있나요?
A. 첫 실행 시 인터넷에서 모델을 내려받아야 하기 때문입니다.

---

## 🔗 6) 관련 파일

- `src/main/java/com/example/aisearch/service/embedding/model/EmbeddingModelSourceLoader.java`
- `src/main/java/com/example/aisearch/service/embedding/DjlEmbeddingService.java`
- `src/main/java/com/example/aisearch/service/embedding/model/EmbeddingModelSource.java`
