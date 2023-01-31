## 실전! 스프링 부트와 JPA 활용2 - API 개발과 성능 최적화
김영한님의 강의를 보며 공부한 내용을 정리하는 파일입니다


### 회원 등록 API (Initial commit)
v1->v2<br>
API를 만들때 Entity를 파리미터로 받으면 x<br>
-> Entity를 손대서 API스펙자체가 변경되기 때문<br>
API요청 스펙에 맞춰서 별도의 DTO를 만들어 파라미터로 받는게 좋다.<br>
<br>
