package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jpabook.jpashop.domain.value.Address;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
    [Lazy Loading과 LazyInitializationException]

    JPA에서 연관관계를 LAZY(지연 로딩)로 설정하면 연관된 엔티티를 처음부터 DB에서 조회하지 않고 프록시 객체로 가지고 있다가,
    실제로 해당 연관 객체의 데이터가 필요한 순간에 추가 SELECT 쿼리를 실행한다.

    예)
    @Entity
    public class Member {
        @ManyToOne(fetch = FetchType.LAZY)
        private Team team;
    }

    Member member = em.find(Member.class, 1L); 코드 실행 시 Member만 조회하고 Team은 실제 엔티티가 아니라 아직 초기화되지 않은 프록시
        객체가 들어갈 수 있다.

    Member
     └─ team -> Team Proxy (아직 Team SELECT 실행 X)
     이후 다음과 같이 Team의 실제 데이터에 접근하면 member.getTeam().getName(); 이 시점에서 Team 프록시를 초기화하기 위해 SELECT 쿼리가 실행된다.
        select * from team where team_id = ?;
      즉, LAZY 로딩은 연관 데이터를 지금 조회하지 않고, 실제로 필요한 순간에 DB에서 조회한다라는 방식이다.

    --------------------------------------------------
    1. LazyInitializationException이 발생하는 이유
    --------------------------------------------------

    Lazy Loading을 수행하려면 해당 엔티티를 관리하고 있는 Hibernate Session(영속성 컨텍스트)이 살아 있어야 한다.
    예)
    @Service
    public class MemberService {
        @Transactional
        public Member findMember(Long id) {
            return memberRepository.findById(id).orElseThrow();
        }
    }

    아래와 같이 트랜잭션이 존재하는 Service에서 지연 로딩인 연관관계를 조회하는 것이 아닌 트랜잭션이 끝난 Controller에서 해당 연관관계에 접근을 하게 되면
    LazyInitializationException 발생하게 된다.
        Member member = memberService.findMember(id);
        member.getTeam().getName();

    흐름
        @Transactional 시작 -> 영속성 컨텍스트 생성 -> Member 조회 -> Member.team은 LAZY이므로 Team Proxy만 존재
            -> Service 메서드 종료 -> Transaction 종료 -> 영속성 컨텍스트(Session) 종료 -> Controller에서 member.getTeam().getName();
            -> Team 데이터를 실제 DB에서 조회하려고 함 -> 하지만 Team Proxy를 초기화해 줄 Session이 이미 종료됨
            -> LazyInitializationException 발생
    즉, LazyInitializationException의 핵심 원인은 LAZY 설정 자체가 잘못된 것이 아니라, 아직 초기화되지 않은 LAZY 연관관계를 조회하려는 시점에
    해당 엔티티를 관리하던 영속성 컨텍스트가 이미 종료되어 추가 SELECT 쿼리를 실행할 수 없는 상태이기 때문이다.

    --------------------------------------------------
    2. 해결 방법1 - 트랜잭션 안에서 LAZY 객체 초기화
    --------------------------------------------------

    @Transactional
    public Member findMember(Long id) {

        Member member = memberRepository.findById(id)
                .orElseThrow();

        member.getTeam().getName();

        return member;
    }
    member.getTeam().getName()을 호출하는 시점에는 아직 트랜잭션과 영속성 컨텍스트가 살아 있으므로 Team SELECT가 실행되면서 프록시가 초기화된다.
    @Transactional -> Member 조회 -> Team Proxy -> member.getTeam().getName() -> Team SELECT -> Team Proxy 초기화 완료
        -> Transaction 종료
    이후 트랜잭션 밖에서 Team의 이미 조회된 데이터를 사용하는 것이 가능하다.

    단순히 LazyInitializationException을 피하기 위해 getter를 억지로 호출하여 초기화하기보다는, 실무에서는 필요한 데이터를 명확하게 조회하도록
    Fetch Join, EntityGraph, DTO 조회 등을 사용하는 것이 일반적으로 더 좋다.

    --------------------------------------------------
    3. 해결 방법 2 - Fetch Join
    --------------------------------------------------

    조회 시점부터 Member와 Team이 모두 필요하다는 것을 알고 있다면 Fetch Join을 사용하여 필요한 연관 엔티티를 한 번에 조회할 수 있다.
        @Query("""
        select m
        from Member m
        join fetch m.team
        where m.id = :id
        """)
        Optional<Member> findMemberWithTeam(Long id);

        이 경우 Member 조회 시 Team도 같이 조회되므로

        Member
         └─ Team -> 이미 조회 완료 상태가 된다.
        따라서 트랜잭션 종료 이후 Team 데이터에 접근하더라도 새로운 Lazy Loading이 필요하지 않으므로 LazyInitializationException이 발생하지 않는다.

    --------------------------------------------------
    4. 해결 방법 3 - 트랜잭션 내부에서 DTO로 변환
    --------------------------------------------------

    REST API에서는 Entity 자체를 Controller까지 전달하기보다 Service의 트랜잭션 안에서 필요한 LAZY 데이터를 조회하고
        DTO로 변환하여 반환하는 방법을 많이 사용한다.

    @Transactional(readOnly = true)
    public MemberResponse findMember(Long id) {

        Member member = memberRepository.findById(id).orElseThrow();

        return new MemberResponse(
                member.getId(),
                member.getUsername(),
                member.getTeam().getName()
        );
    }

    흐름) @Transactional -> Member 조회 -> Team Lazy Loading -> 필요한 데이터 추출 -> DTO 생성 -> Transaction 종료
        -> Controller에 DTO 반환
    DTO는 더 이상 JPA가 관리하는 Entity나 Proxy가 아니므로 Controller에서 Lazy Loading이 발생할 일이 없다.

    --------------------------------------------------
    5. 해결 방법 4 - OSIV(Open Session In View)
    --------------------------------------------------

    OSIV는 HTTP 요청이 들어온 순간부터 응답이 완료될 때까지 Hibernate Session(영속성 컨텍스트)을 열어두는 방법이다.

    OSIV OFF라고 가정하면) HTTP Request -> Controller -> @Transactional Service -> Member 조회 -> Transaction 종료
        -> 영속성 컨텍스트 종료 -> Controller -> member.getTeam().getName() -> LazyInitializationException

   반면 OSIV ON이면)
          HTTP Request
               ↓
          영속성 컨텍스트 OPEN
        ┌──────────────────────────────────┐
        │ Controller                       │
        │      ↓                           │
        │ @Transactional Service           │
        │      ↓                           │
        │ Member 조회                       │
        │      ↓                           │
        │ Transaction 종료                  │
        │                                  │
        │ Controller                       │
        │      ↓                           │
        │ member.getTeam().getName()       │
        │      ↓                           │
        │ Lazy Loading 가능                 │
        └──────────────────────────────────┘
               ↓
          HTTP Response
               ↓
          영속성 컨텍스트 CLOSE
    * OSIV는 Transaction을 HTTP 응답이 끝날 떄까지 유지하는 기능이 아니다. Transaction과 영속성 컨텍스트의 생명주기를 구분해야 한다.
        Service의 @Transactional은 정상적으로 종료되지만 Hibernate Session은 HTTP 응답이 끝날 때까지 살아 있기 때문에 Controller에서도
        Lazy Loading을 수행할 수 있다.

    --------------------------------------------------
    6. Spring Boot OSIV 설정
    --------------------------------------------------

    application.yml

    spring:
      jpa:
        open-in-view: true
    true
    -> HTTP 요청이 끝날 때까지 영속성 컨텍스트 유지
    -> Controller에서도 Lazy Loading 가능

    반대로
    spring:
      jpa:
        open-in-view: false
    false
    -> 일반적으로 Transaction이 종료된 이후에는 해당 영속성 컨텍스트를 이용한 Lazy Loading 불가능
    -> 필요한 연관 데이터는 Service의 Transaction 내부에서 Fetch Join, EntityGraph, DTO 조회 등을 통해 미리 준비해야 한다.

    --------------------------------------------------
    7. OSIV의 단점
    --------------------------------------------------

    OSIV를 사용하면 Service의 Transaction이 종료된 이후에도 Controller / View에서 Lazy Loading을 통해 DB 조회가 가능하다.

    단점
        1. Controller의 단순 getter 호출에서도 예상하지 못한 SQL이 발생할 수 있다.

        2. N+1 문제가 숨어서 발생하기 쉬워진다.

           예)
           Member 목록만 조회한 뒤 View에서 각 Member의 LAZY 연관관계를 반복 접근하는 경우

           for (Member member : members) {
               member.getTeam().getName();
           }

           Member 목록 조회 1번 + 각 Member의 Team 조회 N번이 발생하여 N+1 문제가 생길 수 있다.

           OSIV가 꺼져 있다면 Transaction 밖의 LAZY 접근에서 LazyInitializationException이 발생해 문제가 바로 드러날 수 있지만,
           OSIV가 켜져 있으면 추가 SELECT가 정상 실행되므로 N+1 문제가 눈에 띄지 않은 채 발생하기 쉽다.

        3. DB 접근 범위가 Service 계층을 넘어 Controller까지 퍼질 수 있다.

        4. 요청 처리 후반에도 DB Connection이 필요해져 Connection Pool에 부담을 줄 수 있다.

        ※ OSIV가 HTTP 요청 시작부터 끝까지 하나의 DB Connection을 무조건 계속 점유한다는 의미는 아니다.

        핵심
        - OSIV는 개발 편의성은 높지만, DB 조회 시점을 명확하게 통제하기 어려워질 수 있다.

    LazyInitializationException 해결 방법
        1. Transaction 안에서 필요한 LAZY 연관관계를 초기화한다.
        2. Fetch Join / EntityGraph 등으로 필요한 데이터를 미리 조회한다.
            EntityGraph Ex)
                @EntityGraph(attributePaths = {"team"}) // Member를 조회할 때 team도 같이 조회해 달라는 의미
                Optional<Member> findById(Long id);
        3. Transaction 안에서 필요한 데이터를 DTO로 변환하여 반환한다.
        4. OSIV를 사용하여 HTTP 응답까지 Session을 유지할 수도 있다.

    다만 REST API에서는 OSIV에 무조건 의존하기보다 필요한 DB 조회를 Service의 Transaction 범위 안에서 끝내고 DTO로 반환하는 구조를 많이 고려한다.
*/
@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

//    @NotEmpty
    private String name;

    @Embedded
    private Address address;

    /*
        컬렉션은 필드에서 초기화 하자
        - 컬렉션은 필드에서 바로 초기화하는 것이 안전하다.
        - 새 엔티티를 생성했을 때 컬렉션이 null이 되는 것을 방지할 수 있다.
        - getter 등에서 매번 null 여부를 확인하며 컬렉션을 생성하는 코드를 작성할 필요가 없어 코드가 단순해진다.
        - Hibernate는 엔티티를 영속 상태로 관리할 때 컬렉션을 PersistentBag, PersistentSet 등의 Hibernate 전용 컬렉션으로 관리한다.
            - 이 Hibernate 컬렉션은 단순한 List가 아니라 Lazy Loading, 변경 감지, 추가/삭제된 엔티티 추적 등의 기능을 담당한다.
            - 따라서 엔티티가 영속 상태가 된 이후 getter나 setter 등에서 컬렉션을 임의로 new ArrayList<>()로 교체하면
                Hibernate가 관리하던 컬렉션과의 연결이 끊겨 변경 감지 및 컬렉션 상태 추적에 문제가 생길 수 있다.
    */
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

}
