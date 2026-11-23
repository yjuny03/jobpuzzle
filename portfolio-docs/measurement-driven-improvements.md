# 실측으로 찾은 병목과 개선

> 커밋 `f06a538`(자료 분석 병렬화) · `4e46b8f`(자격요건 없는 공고 처리) · 2026.11

비동기 전환 뒤 실제 API로 분석을 돌려 **단계별 시간**을 기록했고, 그 결과로 두 가지를 고쳤습니다.

## 1. 자료 분석(JSON-02) 분할 호출 병렬화

### 발견
| 단계 | 시간 | 비중 |
|---|---|---|
| 공고 분석 (JSON-01) | 27초 | 13% |
| **자료 분석 (JSON-02)** | **141초** | **68%** |
| 맞춤 종합 (JSON-05) | 36초 | 17% |

긴 이력서는 응답 잘림을 막기 위해 여러 조각(partition)으로 나눠 호출하는데, 이 호출들을 **하나씩 순서대로** 실행하고 있었습니다 (8쪽 이력서 → 4번 × 약 35초).

### 왜 병렬화해도 되는가
- 조각마다 자기 partition 기록과 AI 호출 로그를 따로 가짐 → 서로 공유하는 상태가 없음
- 결과는 조각별로 저장되고, 나중에 **순서 번호(ordinal) 기준으로** 합쳐짐 → 실행 순서와 무관

### 바꾼 것
```java
// CandidateMaterialPartitionExecutor
// 전: for 문 안에서 하나씩 호출·파싱·저장
// 후: 조각 1개 처리를 executePartition으로 분리하고 전용 풀에서 동시에 실행
for (...) {
    tasks.add(CompletableFuture.supplyAsync(
        () -> executePartition(partition, storedPartition, context, sources, prompt, selection), partitionExecutor));
}
boolean failed = tasks.stream().map(CompletableFuture::join).anyMatch(succeeded -> !succeeded);
```
```java
// AsyncConfig — json02PartitionExecutor
executor.setCorePoolSize(concurrency);   // app.ai.generation.json02-partition-concurrency (기본 3)
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
```

### 주의한 점
- Claude 응답의 완료 정보(토큰 등)는 **스레드별로 보관**됩니다 → 호출과 조회를 반드시 같은 작업(`executePartition`) 안에서 처리
- 풀을 모든 분석이 공유 → **자료 분석 조각 호출의 동시 실행 수 상한** (분석 4건 × 조각 3개 = 12개로 불어나지 않음). 공고 분석·맞춤 종합 호출은 분석 풀에서 따로 실행되므로 이 상한에 포함되지 않습니다.
- 풀의 스레드 3개가 모두 바쁘면 대기열(50)에서 기다리고, 대기열까지 가득 차면 거절하지 않고 호출한 스레드가 직접 실행 → 조각 결과를 잃지 않음

### 결과 — 같은 입력으로 1:1 비교 (실제 API)
같은 PDF 두 개(백엔드 공고 2,078자 + 이력서 12,516자)를 수정 전·후 코드로 각각 실행했습니다.

| 단계 | 수정 전 (순차) | 수정 후 (동시 3) |
|---|---|---|
| 공고 분석 | 27초 | 26초 |
| **자료 분석 (조각 4개)** | **141초** | **49초** |
| 맞춤 종합 | 36초 | 39초 |
| **분석 전체** | **209초** | **120초** |

- 바꾼 단계(자료 분석)만 줄고 나머지는 그대로 → 효과가 병렬화에서 나왔음
- 같은 입력 비교는 이 1쌍뿐이라, 수치는 평균이 아니라 한 번의 측정 결과입니다.
- 앞선 수정 후 실행(케이스 62, 다른 공고·이력서)에서는 조각 4개 중 **3개가 같은 순간에 시작**하고, 4번째는 첫 조각이 끝난 직후 시작했습니다(풀 크기 3이 의도대로 동작). 이 실행의 3개 동시 호출에서 요청 한도(429) 오류는 없었습니다.

**공정한 비교를 위해 확인한 것**
- 같은 이력서를 다시 넣으면 저장된 조각 결과를 **재사용**하는 구조 → 이 측정에서만 재사용을 막고 새로 호출
- 두 실행의 입력 토큰이 같음(자료 분석 24,927, 맞춤 종합 19,071) → 같은 입력임을 확인

Mock 측정(조각 4개, 호출당 1.5초 지연): 자료 분석 6,163ms → 3,114ms (`Json02PartitionConcurrencyMeasurementTest`)

## 2. 자격요건 없는 공고를 "AI 오류"가 아닌 입력 문제로 처리

### 발견
자격요건·우대사항이 없는 공고로 실제 분석을 돌렸을 때:
1. Claude는 규칙대로 **요구사항 0개**를 반환 (AI는 정상)
2. 파이프라인은 계속 진행해 **자료 분석 비용을 쓴 뒤**
3. 맞춤 종합 준비 단계에서 예외 → 화면·DB에 `AI provider request failed`로 잘못 표시

원래 예외가 일반 메시지로 바뀌면서 버려지고 있어, 진단 로그 한 줄을 추가해 원인(`JSON-01 requirements are invalid`)을 찾았습니다. **Mock은 항상 요구사항을 돌려주기 때문에 Mock 테스트로는 드러나지 않는 문제**였습니다.

### 바꾼 것
```java
// InitialAnalysisStageExecutor — 공고 분석 직후
JobPostingAnalysisResult result = aiResponseProcessor.parseJobPosting(...);
requireRequirements(result);   // 요구사항·우대사항이 모두 비면 REQUIREMENTS_NOT_FOUND
saveJobPostingSuccess(lease, result);
```
- 같은 공고로 다시 실행하면 결과가 같으므로 **AI를 다시 부르지 않고** 바로 실패
- `/status` 안내: "공고에서 자격요건이나 우대사항을 찾지 못했어요. 공고 원문에 자격요건이 포함되어 있는지 확인해 주세요."
- API 오류 코드: `ANALYSIS_019` (`JOB_POSTING_REQUIREMENTS_NOT_FOUND`)

| | 수정 전 | 수정 후 |
|---|---|---|
| 사용자에게 보이는 것 | "분석 결과를 생성하지 못했어요" | 공고에 자격요건이 없다는 안내 |
| 쓰인 AI 비용 | 공고 분석 + 자료 분석 전체 | 공고 분석만 |
| 같은 공고로 재실행 | 다시 전부 호출 | AI 호출 없이 바로 안내 |

테스트: `InitialAnalysisStageExecutorTest` 2개 (실패 유형·결과 미저장 / 재실행 시 AI 미호출)
