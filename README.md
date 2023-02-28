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
<br><br><br><br><br><br>


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

#### V3 
- Fetch Join으로 SQL이 1번만 실행됨
- distinct를 사용한 이유는 1대다 조인이 있으므로 데이터 베이스 row가 증가한다. 그 결과 같은 order Entity의 조회 수도 증가하게 된다. JPA의 distinct는 SQL에 distinct를 추가하고, 더해서 같은 Entity가 조회되면, 애플리케이션에서 중복을 걸러준다.
<br>
- 단점
  - 페이징이 불가능하다
<br><br>

:rotating_light: 컬렉션 Fetch Join을 사용하면 페이징이 불가능하다. Hibernate는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어오고, 메모리에서 페이징 해버린다.
<br><br>
:pushpin: 컬렉션 Fetch Join은 1개만 사용할 수 있다. 컬렉션 둘 이상에 Fetch Join을 사용하면 안된다. 데이터가 부정합하게 조회될 수 있다.