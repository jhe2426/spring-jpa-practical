package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.type.OrderStatus;
import jpabook.jpashop.domain.value.Address;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    /*
        V1. 엔티티 직접 노출
        [엔티티를 API 응답으로 직접 반환할 때 발생할 수 있는 문제]
        1. LAZY 연관관계의 Hibernate Proxy 직렬화 문제
        - Order의 member, delivery 등의 지연 로딩 연관관계에는 실제 엔티티 대신 Hibernate가 생성한 Proxy 객체가 들어갈 수 있다.
        - Jackson은 Java 객체를 JSON으로 직렬화할 때 런타임 객체의 getter를 확인한다.
            따라서 Hibernate Proxy가 가지고 있는 getHibernateLazyInitializer()도 일반 getter처럼 직렬화 대상으로 인식할 수 있다.
        - getHibernateLazyInitializer()가 반환하는 실제 객체는 ByBuddyInterceptor와 같은 Hibernate 내부 Proxy 관리 객체일 수 있다.
        - 과거 Jackson2에서는 FAIL_ON_EMPTY_BEANS의 기본값이 true이기 때문에, ByteBuddyInterceptor에서 JSON으로 직렬화할 프로퍼티(속성)를 찾지 못하면
            InvalidDefinitionException(Type definition error)이 발생할 수 있다.
        - Spring Boot 4부터 기본 Jackson이 Jackson3으로 변경되었으며, Jackson3에서는 FAIL_ON_EMPTY_BEANS의 기본값이 false이다.
            따라서 직렬화 가능한 프로퍼티가 없는 객체를 만나도 예외를 발생시키지 않고 {}로 직렬화할 수 있으므로 과거 강의와 동일한 예외가 발생하지 않을 수 있다.
            예) "hibernateLazyInitializer": {}

        2. Hibernate 전용 Jackson Module을 사용하는 이유
        - 단순히 FAIL_ON_EMPTY_BEANS=false로 설정하는 것도 위 예외를 피할 수 있지만, 이는 Jackson이 직렬화하지 못하는 객체를 전역적으로 {}로 허용하는 방식일 뿐이다.
            따라서 hibernateLazyInitializer 같은 Hibernate 내부 구현 정보가 API 응답에 그대로 노출될 수 있다.
        - Hibernate5Module은 Jackson에게 Hibernate Proxy와 PersistentCollection을 일반 Java 객체가 아닌 Hibernate 전용 객체로 처리하는 직렬화 규칙을 제공한다.
        - 따라서 getHibernateLazyInitializer(), ByteBuddyInterceptor 같은 Hibernate 내부 구현을 일반 API 데이터처럼 직렬화하지 않으며, 초기화되지 않은 LAZY Proxy는
            기본 설정(FORCE_LAZY_LOADING=false)에서 강제로 조회하지 않고 null로 처리할 수 있다.
        - 즉 FAIL_ON_EMPTY_BEANS=false는 직렬화할 값이 없어도 {}로 허용하는 설정이고, Hibernate5Module은 Hibernate Proxy 자체를 이해하고
            적절한 방식으로 직렬화하는 전용 모듈이므로 더 의미 있는 해결 방법이다.

        3. LAZY 강제 초기화
        - 아래 getName(), getAddress() 호출은 LAZY Proxy의 실제 데이터가 필요하다는 것을 Hibernate에 알려 SELECT를 수행하게 하고 Proxy를 강제로 초기화한다.
        - OSIV=true라면 Controller/Jackson 직렬화 시점까지 영속성 컨텍스트가 열러 있어 LAZY 로딩이 가능하지만, OSIV=false 상태에서 초기화되지 않은 Proxy를 Jackson이 접근하면
            Could not initialize proxy - no session과 같은 LazyInitializationException이 발생할 수 있다.

        4. 양방향 연관관계 무한 직렬화 문제
        - Order -> Member -> orders -> Member -> order .. 처럼 양방향 연관관계를 양쪽 모두 JSON 직렬화 대상으로 두면
            Jackson이 두 엔티티를 계속 왕복하며 객체 그래프를 탐색할 수 있다.
        - 이 경우 응답 JSON이 반복적으로 중첩되다가 StackOverflowError 등의 예외가 발생할 수 있다.
        - LAZY + OSIV 환경에서는 객체 그래프를 탐색하는 과정에서 예상하지 못한 추가 SELECT가 발생할 수 있다.
        - 따라서 한쪽 연관관계에 @JsonIgnore를 적용하여 JSON 탐색을 끊을 수 있다.

        * 해당 예제는 엔티티를 직접 반환의 문제를 설명하기 위한 예제이다.
            실무 REST API에서는 엔티티를 직접 노출하지 않고 DTO로 변환하여 필요한 데이터와 API 스펙을 명확하게 정의한느 것이 권장한다.
    */
    /*
        [OSIV(Open Session/EntityManager In View)]
        - Spring Boot의 웹 애플리케이션에서는 기본적으로 OSIV가 true로 설정된다. (spring.jpa.open-in-view=true)
        - OSIV를 사용하면 HTTP 요청이 끝날 때까지 EntityManager(Session)를 열어두기 때문에, Service의 @Transactional 범위가 끝난 이후에도 Controller나
            JSP/Thymeleaf/Jackson 직렬화 과정에서 초기화되지 않은 LAZY 연관관계에 접근하여 추가 조회를 할 수 있다.

        [Spring Boot가 OSIV를 기본 true로 두는 이유]
        - 전통적인 Spring MVC 애플리케이션에서는 Repository/Service에서 엔티티를 조회한 뒤 JSP나 Thymeleaf 같은 View에서 연관관계의 값을 사용하는 경우가 많았다.
        - 만약 Service 트랜잭션이 끝나는 순간 EntityManager까지 닫히면, View에서 아직 초기화되지 않은 LAZY 연관관계를 사용하는 순간 LazyInitializationException이 발생한다.
        - 따라서 View가 완전히 렌더링될 때까지 EntityManager를 유지하면 View 계층에서 필요한 LAZY 데이터를 자연스럽게 조회할 수 있으므로
            개발 편의성이 높아지고 LazyInitializationException도 줄어든다.

        [OSIV=true의 장점]
        1. View / Controller에서 LAZY 연관관계 접근 가능
        2. Service에서 모든 연관관계를 미리 초기화하지 않아도 됨
        3. 단순한 MVC 애플리케이션에서는 개발이 편리함
        4. 트랜잭션 종료 이후의 LAZY 접근으로 인한 LazyInitializationException을 줄일 수 있음

        [OSIV=true의 단점]
        - Controller, View, Jackson 직렬화 과정에서도 DB 쿼리가 발생할 수 있다.
        - 어떤 SQL이 Service에서 발생하고 어떤 SQL이 응답 직렬화 과정에서 발생하는지 파악하기 어려워질 수 있다.
        - 엔티티를 API 응답으로 직접 반환하면 Jackson이 연관관계를 따라가면서 예상하지 못한 LAZY SELECT가 발생할 수 있고,
            N+1 문제를 숨길 가능성이 있다.
        - 즉 OSIV는 LAZY 문제 자체를 해결하는 것이 아니라 LAZY 로딩이 가능한 시간을 HTTP 요청 종료 시점까지 연장하는 것이다.

        [REST API에서는]
        - REST API 중심 애플리케이션에서는 OSIV를 false로 설정하고, Service의 트랜잭션 안에서 Fetch Join, EntityGraph, DTO Projection 등을 이용해
            API에 필요한 데이터를 명시적으로 조회하는 방식을 선택하기도 한다.
        - 이렇게 하면 Controller/Jackson 직렬화 단계에서 예상하지 못한 추가 SQL이 발생하는 것을 방지하고, 어느 시점에 DB를 조회하는지 명확하게 관리할 수 있다.
            spring.jpa.open-in-view=false

        * OSIV는 엔티티를 View까지 가져가서 사용하는 전통적인 MVC 구조를 편하게 해주는 기능이고,
            DTO를 트랜잭션 안에서 완성해서 View/API에 넘기는 구조라면 OSIV의 필요성이 크게 줄어든다.
        * 규모가 커질수록 엔티티 직접 반환은 계층 간 결합도를 높여 유지보수에 불리하므로,
            DTO를 통해 외부에 노출할 데이터 구조를 명확하게 분리하는 것이 더 좋다.
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // Lazy 강제 초기화
            order.getDelivery().getAddress();
        }
        return all;
    }

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        /*
           ORDER 2개
           N + 1 -> 1(주문) + 회원 N + 배송 N
           - 쿼리가 총 1 + N + N번 실행이 됨
           - order 조회 1번(order 조회 결과 수가 N이 됨)
           - order -> member 지연 로딩 조회 N번
           - order -> delivery 지연 로딩 조회 N번
           - order의 결과가 2개이면 최악의 경우 쿼리 수가 1 + 2 + 2번이 실행이 된다.
                - 지연로딩은 영속성 컨텍스트에서 조회하므로 이미 조회된 member, delivery가 영속성 컨텍스트에 존재한다면 조회 쿼리를 생략하기 때문에
                    최악의 경우라는 것은 member나 delivery에 같은 식별자를 갖는 값들이 존재하지 않은 경우가 된다.
        */
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        List<SimpleOrderDto> result = orders.stream()
                .map(SimpleOrderDto::new)
                .toList();
        return result;
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }


}
