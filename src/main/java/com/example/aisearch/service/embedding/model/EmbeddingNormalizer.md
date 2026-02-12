# EmbeddingNormalizer 설명서 (초보 개발자용)

이 문서는 `EmbeddingNormalizer`의 L2 정규화 로직이 **왜 맞는지**, **왜 필요한지**를 쉽게 설명합니다.

---

## ✅ 1) 이 클래스가 하는 일

`EmbeddingNormalizer`는 임베딩 벡터를 **L2 정규화**합니다.  
쉽게 말해, **벡터의 크기(세기)는 같게 맞추고 방향만 남기는 처리**입니다.

---

## 📌 2) L2 정규화란?

### 🧭 벡터가 담는 정보: 크기 + 방향
- **크기(노름)**: 벡터가 얼마나 “강한지/큰지”를 나타냄
- **방향**: 벡터가 어떤 의미적 방향을 가리키는지 나타냄

L2 정규화는 **크기 정보를 제거**하고, **방향 정보만 남기기 위한 처리**입니다.
그래서 “크기를 1로 맞춘다”는 표현은
**방향 비교에 집중하기 위해 크기를 동일하게 만든다**는 뜻입니다.


벡터 `v`가 있을 때:

- 벡터 길이(노름)

```
||v|| = sqrt(v1^2 + v2^2 + ... + vn^2)
```

- 정규화된 벡터

```
vi' = vi / ||v||
```

이렇게 하면 **벡터의 노름(크기)이 1로 맞춰집니다.**
중요한 점은 **벡터 값이 1이 되는 것이 아니라**,
**방향은 유지하고 크기만 1이 되도록 비율을 조정**한다는 것입니다.

---

## 🧪 3) 실제 코드와 대응

```java
public float[] l2Normalize(float[] vector) {
    double sum = 0.0;
    for (float value : vector) {
        sum += value * value;
    }
    double norm = Math.sqrt(sum);
    if (norm == 0.0) {
        return vector;
    }

    float[] normalized = new float[vector.length];
    for (int i = 0; i < vector.length; i++) {
        normalized[i] = (float) (vector[i] / norm);
    }
    return normalized;
}
```

위 코드는 수식과 그대로 매칭됩니다:

1. `sum += value * value` → 각 원소 제곱 합
2. `norm = sqrt(sum)` → 벡터 길이
3. `vector[i] / norm` → 정규화

---

## ❓ 4) 왜 정규화를 해야 하나요?

### ✅ 코사인 유사도를 쓰기 때문
프로젝트에서는 Elasticsearch의 `similarity: "cosine"`을 사용합니다.

코사인 유사도는 **벡터의 방향**만 비교하고, 크기는 무시해야 합니다.
그래서 **노름 기준으로 정규화**하면 계산이 안정적입니다.

---

## ⚠️ 5) 0 벡터 처리

```java
if (norm == 0.0) {
    return vector;
}
```

- 모든 값이 0이면 노름도 0
- 0으로 나누면 NaN(오류)이 생김
- 그래서 **그냥 원본을 반환**해서 오류를 막습니다.

---

## ✅ 6) 요약

- 이 로직은 **표준 L2 정규화 방식**이다.
- 코사인 유사도 검색을 위해 **필수적인 전처리**다.
- 0 벡터 케이스도 안전하게 처리한다.

---

## 🔗 관련 파일

- `src/main/java/com/example/aisearch/service/embedding/model/EmbeddingNormalizer.java`
