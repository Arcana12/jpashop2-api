# 실전! 스프링 부트와 JPA 활용2 - API 개발과 성능 최적화
김영한님의 강의를 보며 공부한 내용을 정리하는 파일입니다
<br>
<br>
<br>
***
<br>

## API 개발 기본
### 회원 등록 API (Initial commit)
V1->V2<br>
API를 만들때 Entity를 파리미터로 받으면 안된다.<br>
-> Entity가 변경되면 API스펙자체가 변경되기 때문<br>
API요청 스펙에 맞춰서 별도의 DTO를 만들어 파라미터로 받는게 좋다.<br>
<br>
### 회원 수정 API
데이터를 수정할때 변경감지를 이용<br>

### 회원 조회 API
#### V1 문제점
- Entity에 프레젠테이션 계층을 위한 로직이 추가된다.
- 기본적으로 Entity의 모든 값이 노출된다.
- 응답 스펙을 맞추기 위해 로직이 추가된다. (@JsonIgnore, 별도의 뷰 로직 등등)
- 실무에서는 같은 Entity에 대해 API가 용도에 따라 다양하게 만들어지는데, 한 Entity에 각각의 API를 위한 프레젠테이션 응답 로직을 담기는 어렵다.
- Entity가 변경되면 API 스펙이 변한다.
- 추가로 컬렉션을 직접 반환하면 항후 API 스펙을 변경하기 어렵다.(별도의 Result 클래스 생성으로 해결)
<br>

#### 결론
API 응답 스펙에 맞추어 별도의 DTO를 반환한다.<br>
<br>

:memo: Entity를 외부에 노출하지 마세요!<br>
실무에서는 member Entity의 데이터가 필요한 API가 계속 증가하게 된다. 어떤 API는 name 필드가 필요하지만, 어떤 API는 name 필드가 필요없을 수 있다.<br>
결론적으로 Entity 대신에 API 스펙에 맞는 별도의 DTO를 노출해야 한다.<br>
<br>
<br>
***
<br>

### 지연 로딩과 조회 성능 최적화
#### V1 (Entity를 직접 노출)
Entity를 직접 노출할 때는 양방향 연관관계가 걸린 곳은 꼭 한곳을 @JsonIgnore처리 해야 한다. 그러지 않으면 양쪽을 서로 호출하면서 무한 루프가 걸린다.<br>
<br>

:memo: 지연 로딩(LAZY)을 피하기 위해 즉시 로딩(EAGER)으로 설정하면 안된다! 즉시 로딩 때문에 연관관계가 필요 없는 경우에도 데이터를 항상 조회해서 성능 문제가 발생할 수 있다.<br>
항상 지연 로딩을 기본으로 하고, 성능 최적화가 필요한 경우에는 페치 조인(fetch join)을 사용해라!(V3)
<br>
<br>
<br>

#### V2 (Entity를 DTO로 변환)
쿼리가 총 1 + N + N번 실행된다.(V1과 쿼리수 결과는 같다.)
- order 조회 1번(order 조회 결과 수가 N이 된다.)
- order -> member 지연 로딩 조회 N 번
- order -> delivery 지연 로딩 조회 N 번
- order의 결과가 2개면 최악의 경우 1 + 2 + 2번 실행된다.(member수와 delivery수에 따라 증가한다.)
  - 지연로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 경우 쿼리를 생략한다.<br>
<br>
<br>

#### V3 (Fetch Join 최적화)
Entity를 Fetch Join을 사용해서 쿼리 1번에 조회<br>
Fetch Join으로 order -> member, order -> delivery는 이미 조회 된 상태이므로 지연로딩X<br>

<br>
<br>
