# 실전! 스프링 부트와 JPA 활용2 - API 개발과 성능 최적화
김영한님의 강의를 보며 공부한 내용을 정리하는 파일입니다
<br>
<br>
<br>
***
<br>

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

:pushpin: Entity를 외부에 노출하지 마세요!<br>
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

:rotating_light: 지연 로딩(LAZY)을 피하기 위해 즉시 로딩(EAGER)으로 설정하면 안된다! 즉시 로딩 때문에 연관관계가 필요 없는 경우에도 데이터를 항상 조회해서 성능 문제가 발생할 수 있다.<br>
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
- Entity를 Fetch Join을 사용해서 쿼리 1번에 조회<br>
- Fetch Join으로 order -> member, order -> delivery는 이미 조회 된 상태이므로 지연로딩X<br>

<br>
<br>

#### V4 (JPA에서 DTO로 바로 조회)
- 일반적인 SQL을 사용할 때 처럼 원하는 값을 선택해서 조회
- new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
- SELECT 절에서 원하는데이터를 직접 선택하므로 DB -> 애플리케이션 네트웍 용량 최적화(생각보다 미비)
- Repository 재사용성 떨어짐, API 스펙에 맞춘 코드가 Repository에 들어가는 단점<br>
<br>
<br>
<br>


:memo: 쿼리 방식 선택 권장 순서
1. 우선 Entity를 DTO로 변환하는 방법을 선택한다.
2. 필요하면 Fetch Join으로 성능을 최적화한다. -> 대부분의 성능 이슈가 해결된다.
3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접 사용한다.
<br>
<br><br><br><br>
***
<br><br>


### 컬렉션 조회 최적화
#### V1 Entity 직접 노출
- orderItem, item 관계를 직접 초기화 하면 hibernate5Module설정에 의해 Entity를 JSON으로 생성
- 양방향 연관관계면 무한 루프에 걸리지 않게 한곳에 @JsonIgnore를 추기해야 한다.
- Entity를 직접 노출하므로 좋은 방법은 아니다.
<br><br><br>

#### V2 Entity를 DTO로 변환
- 지연로딩으로 많은 SQL 실행
  - order 1번
  - member, address n번(order 조회 수 만큼)
  - orderItem N번(order 조회 수 만큼)
- item N번(orderItem 조회 수 만큼)
<br>
<br>
:pushpin: 지연 로딩은 영속성 컨텍스트에 있으면 영속성 컨텍스트에 있는 Entity를 사용하고 없으면 SQL을 실행한다. 따라서 같은 영속성 컨텍스트에서 이미 로딩한 회원 Entity를 추가로 조회하면 SQL을 실행하지 않는다.
<br><br><br>

#### V3 Fetch Join 최적화
- Fetch Join으로 SQL이 1번만 실행됨
- distinct를 사용한 이유는 1대다 조인이 있으므로 데이터 베이스 row가 증가한다. 그 결과 같은 order Entity의 조회 수도 증가하게 된다. JPA의 distinct는 SQL에 distinct를 추가하고, 더해서 같은 Entity가 조회되면, 애플리케이션에서 중복을 걸러준다.
<br><br>
- 단점
  - 페이징이 불가능하다
<br><br>

:rotating_light: 컬렉션 Fetch Join을 사용하면 페이징이 불가능하다. Hibernate는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어오고, 메모리에서 페이징 해버린다.
<br><br>
:pushpin: 컬렉션 Fetch Join은 1개만 사용할 수 있다. 컬렉션 둘 이상에 Fetch Join을 사용하면 안된다. 데이터가 부정합하게 조회될 수 있다.
<br><br><br>

#### V3.1 페이징과 한계 돌파
- 장점
  - 쿼리 호출 수가 1+N -> 1+1로 최적화 된다.
  - Join보다 DB 데이터 전송량이 최적화 된다.(Order와 Orderitem을 조인하면 Order가 OrderItem 만큼 중복해서 조회된다. 이 방법은 각각 조회하므로 전송해야할 중복 데이터가 없다.)
  - Fetch Join 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소한다.
  - 컬렉션 Fetch Join은 페이징이 불가능 하지만 이 방법은 페이징이 가능하다.
- 결론
  - ToOne 관계는 Fetch Join해도 페이징에 영향을 주지 않는다. 따라서 ToOne관계는 Fetch Join으로 쿼리 수를 줄이고 해결하고, 나머지는 hibernate.default_batch_fetch_size 로 최적화 하자.
<br><br>

:pushpin: default_batch_fetch_size 의 크기는 100~1000사이를 선택하는 것을 권장한다. 
이 전략을 SQL IN 절을 사용하는데, 데이터베이스에 따라서 IN 절 파라미터를 1000으로 제한하기도 한다. 
1000개로 잡으면 한번에 1000개를 DB에서 애플리케이션에 불러오므로 DB에 순간 부하가 증가할 수 있다. 하지만 애플리케이션은 100이든 1000이든 결국 전체 데이터를 로딩해야 하므로 메모리 사용량이 같다.
1000으로 설정하는 것이 성능상 가장 좋지만, 결국 DB든 애플리케이션이든 순간 부하를 어디까지 견딜 수 있는지로 결정하면 된다.
<br>
<br>

#### V4 JPA에서 DTO 직접 조회
- Query : 루트 1번, 컬렉션 N번 실행
- ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N)관계는 각각 별도로 처리한다.
  - ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
  - ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
- row 수가 증가하지 않는 ToOne 관계는 Join으로 최적화 하기 쉬으므로 한번에 조회하고, ToMany 관계는 최적화하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다.
<br><br>

#### V5 컬렉션 조회 최적화
- Query : 루트 1번, 컬렉션 1번
- ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderid로 ToMany관계인 OrderItem을 한꺼번에 조회
- Map을 사용해서 매칭 성능 향상(O(1))

<br><br>

#### V6 플랫 데이터 최적화
- Query 1번에 조회 가능하다
- 단점
  - 쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복데이터가 추가되므로 상황에 따라 V5보다 더 느릴 수 있다.
  - 애플리케이션에서 추가 작업이 크다.
  - 페이징 불가능

<br><br><br><br>

### 정리
#### 권장 순서
1. Entity 조회 방식으로 우선 접근
   1. Fetch Join으로 쿼리 수를 최적화
   2. 컬렉션 최적화
      1. 페이징 필요 -> hibernate.default_batch_fetch_size, @BatchSize로 최적화
      2. 페이징 필요 X -> Fetch Join 사용
2. Entity 조회 방식으로 해결이 안되면 DTO 조회 방식 사용
3. DTO 조회 방식으로 해결이 안되면 NativeSQL or 스프링 Jdbctemplate
<br><br>
:memo: 엔티티 조회 방식은 페치 조인이나, hibernate.default_batch_fetch_size , @BatchSize 같이 
 코드를 거의 수정하지 않고, 옵션만 약간 변경해서, 다양한 성능 최적화를 시도할 수 있다. 반면에 DTO를
 직접 조회하는 방식은 성능을 최적화 하거나 성능 최적화 방식을 변경할 때 많은 코드를 변경해야 한다.
<br><br>

#### DTO 조회 방식의 선택지
- DTO로 조회하는 방법도 각각 장단이 있다. V4, V5, V6에서 단순하게 쿼리가 1번 실행된다고 V6이 항상 좋은 방법인 것은 아니다.
- V4는 코드가 단순하다. 특정 주문 한거만 조회하면 이 방식을 사용해도 성능이 잘 나온다. 예를 들어 조회한 Order 데이터가 1건이면 OrderItem을 찾기 위한 쿼리도 1번만 실행하면 된다.
- V5는 코드가 복잡하다. 여러 주문을 한꺼번에 조회하는 경우에는 V4 대신에 이것을 최적화한 V5방식을 사용해야 한다.  예를 들어서 조회한 Order 데이터가 1000건인데, V4 방식을 그대로 사용하면, 쿼리가 총 1 + 1000번 실행된다.
 여기서 1은 Order 를 조회한 쿼리고, 1000은 조회된 Order의 row 수다. V5 방식으로 최적화 하면 쿼리가 총 1 + 1번만 실행된다.
 상황에 따라 다르겠지만 운영 환경에서 100배 이상의 성능 차이가 날 수 있다.
- V6는 완전히 다른 접근방식이다. 쿼리 한번으로 최적화 되어서 상당히 좋아보이지만, Order를 기준으로 페이징이 불가능하다.
 실무에서는 이정도 데이터면 수백이나, 수천건 단위로 페이징 처리가 꼭 필요하므로, 이 경우 선택하기 어려운 방법이다.
 그리고 데이터가 많으면 중복 전송이 증가해서 V5와 비교해서 성능 차이도 미비하다.