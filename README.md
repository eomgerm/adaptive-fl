# Adaptive-FL

**Design of an Adaptive Communication Protocol Adapter for Federated Learning in MSA Environments**

MSA(Microservice Architecture) 환경에서 연합학습(Federated Learning)을 수행할 때 발생하는 대규모 모델 파라미터 전송 문제를 해결하기 위해 개발한 적응형 통신 프로토콜 어댑터 프로젝트입니다.

본 프로젝트는 REST, gRPC, RSocket의 성능을 비교 분석하고, 데이터 크기와 전송 특성에 따라 최적의 프로토콜을 선택하는 Adaptive Communication Protocol Adapter를 설계 및 구현하는 것을 목표로 합니다.

## 프로젝트 배경

기존 연합학습 시스템은 대부분 gRPC 기반으로 구현되어 있습니다.

하지만 최근 AI 모델의 크기가 지속적으로 증가하면서 다음과 같은 문제가 발생합니다.

* 대용량 모델 파라미터 전송
* 네트워크 병목 현상
* 클라이언트 환경의 이질성
* 메모리 사용량 증가
* 통신 프로토콜별 성능 차이

특히 MSA 환경에서는 수많은 서비스가 서로 통신하므로 상황에 맞는 프로토콜 선택이 중요합니다.

본 프로젝트에서는 gRPC와 RSocket의 장점을 결합하여 상황에 따라 동적으로 프로토콜을 선택하는 적응형 통신 계층을 구현했습니다.

## 연구 목표

### 1. 통신 프로토콜 성능 비교

MSA 환경에서 다음 프로토콜의 성능을 비교합니다.

* REST
* gRPC
* RSocket

### 2. 연합학습 인프라 구축

Kubernetes 기반 MSA 환경에서 연합학습 서비스를 구현합니다.

### 3. 적응형 프로토콜 어댑터 구현

데이터 크기, 전송 빈도, 클라이언트 특성을 고려하여 적절한 프로토콜을 자동 선택하는 어댑터를 개발합니다.

## 기술 스택

### Backend

* Kotlin
* Spring Boot 3.3
* gRPC
* RSocket
* REST API

### Infrastructure

* Kubernetes
* AWS EKS
* Docker

### Monitoring & Testing

* Grafana K6

### Machine Learning

* Federated Learning Simulation

## 시스템 아키텍처

```text
                     +------------------+
                     | Federated Server |
                     +---------+--------+
                               |
               +---------------+---------------+
               |                               |
         Adaptive Client                 Adaptive Client
               |                               |
      +--------+--------+             +--------+--------+
      |                 |             |                 |
     gRPC           RSocket          gRPC           RSocket
      |                 |             |                 |
      +--------+--------+             +--------+--------+
               |                               |
      +--------+--------+             +--------+--------+
      | FL Client A     |             | FL Client B     |
      +-----------------+             +-----------------+
```

## 주요 기능

### Federated Learning Simulation

실제 딥러닝 모델 대신 모델 동작을 시뮬레이션하여 연합학습 워크플로우를 구현했습니다.

* Local Training
* Global Aggregation
* Model Evaluation
* Round Management

### Adaptive Communication Layer

데이터 특성에 따라 통신 프로토콜을 선택합니다.

#### gRPC 사용 조건

* 작은 데이터 크기
* 높은 요청 빈도
* 빠른 직렬화/역직렬화 필요

#### RSocket 사용 조건

* 대용량 데이터
* Back Pressure 필요
* 안정적인 스트리밍 전송 필요

### Protocol Benchmark

아래 시나리오를 기준으로 성능 측정을 수행했습니다.

#### 저용량 데이터

* 1MB
* 2MB
* 4MB

#### 대용량 데이터

* 100MB
* 200MB
* 400MB
* 800MB
* 1600MB

## 성능 분석 결과

### 저용량 데이터

동시 요청 환경에서는 다음 결과를 확인했습니다.

```text
gRPC ≈ RSocket > REST
```

* REST는 요청 수 증가에 따라 응답시간이 급격히 증가
* gRPC와 RSocket은 REST 대비 우수한 성능
* gRPC와 RSocket 간 차이는 크지 않음

### 대용량 데이터

```text
gRPC > RSocket
```

* 평균 응답시간 기준 gRPC가 더 빠름
* 데이터 크기가 증가할수록 차이가 커짐

### 안정성 테스트

100MB 데이터 다중 전송 실험 결과

| Protocol               | 64 Requests | 128 Requests | 256 Requests |
| ---------------------- | ----------- | ------------ | ------------ |
| RSocket (BackPressure) | 0% Error    | 0% Error     | 0% Error     |
| gRPC                   | 0% Error    | 57.81% Error | 100% Error   |

결과적으로 RSocket은 Back Pressure를 활용해 대규모 데이터 전송 상황에서 높은 안정성을 보였습니다.

## 프로토콜 선택 알고리즘

Adaptive Transport Decision Algorithm

선택 기준은 다음과 같습니다.

### 데이터 크기

큰 데이터

```text
→ RSocket
```

작은 데이터

```text
→ gRPC
```

### 전송 빈도

빈도가 높음

```text
→ gRPC
```

### Back Pressure 필요 여부

```text
→ RSocket
```

### 클라이언트 지원 프로토콜

지원 가능한 프로토콜만 선택

```text
gRPC Only
RSocket Only
gRPC + RSocket
```

## 서버 구조

```text
Workflow
   │
ClientManager
   │
AdaptiveClient
 ┌─┴──────────┐
 │            │
gRPC      RSocket
Client     Client
```

구성 요소

* Workflow
* ClientManager
* AdaptiveClient
* GrpcClient
* RSocketClient
* ClientMessageQueue
* Model Simulator

## 클라이언트 구조

```text
Model
  │
GrpcService
  │
RSocketController
```

구성 요소

* GrpcClient
* GrpcService
* RSocketClient
* RSocketController
* Model Simulator

## 실행 환경

### Kubernetes Cluster

| Component | Spec      |
| --------- | --------- |
| Platform  | AWS EKS   |
| Instance  | t3.medium |
| vCPU      | 2         |
| Memory    | 4GB       |

### Application

| Component   | Version |
| ----------- | ------- |
| Spring Boot | 3.3.3   |
| Java        | 21      |
| Kotlin      | Latest  |

## 연구 성과

* REST / gRPC / RSocket 성능 비교
* Kubernetes 기반 연합학습 환경 구축
* Adaptive Communication Adapter 설계
* gRPC + RSocket 하이브리드 통신 구조 구현
* 연합학습용 프로토콜 선택 알고리즘 구현

## 향후 연구

### 프로토콜 결정 알고리즘 고도화

현재는 규칙 기반으로 구현되어 있으나 향후에는 동적 네트워크 상태를 반영할 예정입니다.

### 실제 머신러닝 모델 적용

현재는 시뮬레이션 기반 구조이며 향후 실제 딥러닝 모델을 적용할 예정입니다.

### 추가 프로토콜 지원

* MQTT
* Kafka
* NATS
* WebTransport

### Flower Framework 연동

실제 연합학습 프레임워크와 연계하여 검증을 진행할 예정입니다.

## 논문

**MSA 환경에서 연합 학습을 위한 적응형 통신 프로토콜 어댑터 설계**

* 경희대학교 컴퓨터공학과 졸업논문
* 2024

## License

This project is intended for research and educational purposes only.
