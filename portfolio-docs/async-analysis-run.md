# 분석 실행 요청(/run) 비동기 전환

> 커밋 `ede7ddd` · 2026.11 (프로젝트 종료 후 개인 개선)

## 1. 문제

분석 화면은 `/run`을 보내 놓고 `/status`를 따로 조회해서 **비동기처럼 보였지만**, 서버의 `/run`은 분석이 끝날 때까지 응답하지 않는 **동기 방식**이었습니다.

```
POST /run ─▶ 소유권 확인 ─▶ 공고 분석 ─▶ 자료 분석 ─▶ 가이드 ─▶ 맞춤 종합 ─▶ 응답(200)
           └──────────────── HTTP 요청 스레드가 끝까지 대기 ────────────────┘
```

실제 분석 1건은 AI 호출 때문에 수 분이 걸립니다. 그동안 Tomcat 요청 스레드 하나와 HTTP 연결이 계속 묶여 있어, 요청이 몰리면 스레드가 고갈되고 프록시·로드밸런서의 시간 제한에 걸릴 수 있었습니다.

> 이미 있던 "선점 → AI 호출 → 확정" 3단계 트랜잭션은 **DB 연결·잠금**을 오래 잡지 않게 한 것이고, **요청 스레드** 문제는 그대로였습니다.

## 2. 해결 — 접수와 실행 분리

```
POST /run ─▶ 소유권 확인 + 빠른 사전검사 ─▶ 응답(202 Accepted)
                          └─▶ 분석 전용 풀: 공고 분석 ─▶ 자료 분석 ─▶ 가이드 ─▶ 맞춤 종합
GET /status (화면이 주기적으로 조회) ─▶ 진행 상황·실패 이유
```

| 파일 | 변경 |
|---|---|
| `AnalysisController` | `/run`이 분석을 맡기고 **202**로 바로 응답 |
| `AnalysisService` | `startCustomizedAnalysis`(요청 스레드: 소유권 + 사전검사) / `continueCustomizedAnalysis`(백그라운드: AI 분석)로 분리 |
| `AnalysisRunDispatcher` (신규) | 분석 전용 풀에 실행을 넘기고, 대기열이 가득 차면 상태를 실패로 정리한 뒤 503 |
| `AsyncConfig` | `analysisExecutor` (동시 4건, 대기열 20건) |
| `GuideContextService` | `requireAvailableGuide` — 활성 가이드 유무를 AI 호출 전에 확인 |

```java
// AnalysisRunDispatcher
if (!analysisService.startCustomizedAnalysis(userId, caseId)) return;   // 이미 분석 중이면 그대로 둠
try {
    analysisExecutor.execute(() -> runInBackground(userId, caseId));
} catch (TaskRejectedException rejected) {
    statusTransitionService.failIfAnalyzing(userId, caseId);           // ANALYZING으로 남지 않게
    throw new CustomException(ErrorCode.ANALYSIS_QUEUE_FULL);          // 503
}
```

### 설계 판단

- **빠른 사전검사는 요청 스레드에 남김** — `/status`는 AI 호출 기록에 남은 실패만 이유를 보여 줍니다. "활성 가이드 없음"처럼 AI 호출 전에 나는 실패를 백그라운드로 넘기면 화면에 이유 없이 FAILED만 보이므로, 벡터 색인 준비·가이드 유무는 요청 단계에서 확인해 지금처럼 **409를 바로 응답**합니다. 덕분에 가이드가 없으면 AI를 한 번도 부르지 않습니다.
- **`@Async` 대신 실행기를 직접 사용** — 대기열이 가득 찼을 때의 거절(`TaskRejectedException`)을 직접 받아 상태를 정리하기 위해서입니다.
- **동시 실행 4건 제한** — AI 제공자 요청 한도와 DB 연결을 보호합니다. 대신 요청이 많이 몰리면 완료가 늦어집니다(트레이드오프).
- **화면 수정 없음** — 원래부터 `/run` 응답 뒤 `/status`를 계속 조회해 COMPLETED/FAILED를 판단하는 구조였습니다.

## 3. 결과

### 실제 API 측정
실제 Claude(`claude-sonnet-5`) + OpenAI 임베딩 + Qdrant, 공고 PDF 2쪽 + 이력서 PDF 8쪽을 업로드부터 실제 사용자 흐름 그대로 실행했습니다.

| 항목 | 값 |
|---|---|
| 분석 완료까지 | **3분 29초** (수정 전 구조였다면 `/run`이 이 시간 동안 응답하지 않음) |
| `/run` 응답 | **164ms** (HTTP 202) |
| 분석 중 `/status` | 104회 호출, 최대 **63ms** |

### Mock 지연 측정 (반복 측정용)
비용 때문에 반복 측정은 Mock AI 호출마다 1.5초 지연을 넣어 재현했습니다 (분석 5건 동시 요청).

| 항목 | 수정 전 | 수정 후 |
|---|---|---|
| `/run` 응답 | 6,304ms (분석이 끝나야 응답) | **133ms** |
| 요청 스레드 점유 합계 | 31,519ms | **665ms** |
| 5건 모두 완료 | 6,448ms | 10,469ms (동시 4건 제한의 트레이드오프) |

## 4. 테스트
| 테스트 | 내용 | 실행 조건 |
|---|---|---|
| `AnalysisControllerTest` | 202 응답, 분배기 호출, 대기열 가득 참(503) | 항상 |
| `AnalysisPipelineHttpIntegrationTest` | `/run` 202 → `/status`로 완료 대기 | Docker(Testcontainers) |
| `AnalysisRunLatencyMeasurementTest` | Mock 지연 측정 | `LOCAL_DB_MEASURE=true` |
| `RealAnalysisRunMeasurementTest` · `RealUserFlowMeasurementTest` | 실제 API 측정 | `REAL_AI_MEASURE=true` |
