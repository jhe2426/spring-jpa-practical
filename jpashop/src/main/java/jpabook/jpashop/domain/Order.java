package jpabook.jpashop.domain;

import jakarta.persistence.*;
import jpabook.jpashop.domain.type.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "orders")
@Getter @Setter
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    /*
        cascade = CascadeType.ALL
            Order에 수행한 영속성 작업(persist, remove, merge 등)을 연관된 OrderItem에도 함께 전파한다.
             따라서 Order를 저장하거나 삭제할 때 OrderItem을 각각 따로 처리하지 않아도 Order와 함께 저장되거나 삭제되도록 생명주기를 같이 관리할 수 있다.
    */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    /*
        일대일 양방향 연관관계 주인 결정
        - 알대일 관계는 외래 키를 어느 테이블에 둘지 선택할 수 있다.
        - 그리고 실제 외래 키를 가진 엔티티가 연관관계의 주인이 된다.

        - 실무에서는 보퉁 두 엔티티 중 관계를 주도하고 상대 엔티티를 자주 참조하는 쪽에 외래 키를 두는 방식을 우선 고려한다.
        - 다만 조회가 많은 쪽을 무조건 주인으로 둔다는 규칙은 아니다.

        - 실제 설계에서는 아래와 같은 것들을 함께 고려하여 외래 키의 위치를 결정한다.
            - 어느 엔티티가 관계를 주도하는지
            - 주로 어느 방향으로 객체를 탐색하는지
            - 두 엔티티의 생성/삭제 생명주기
            - 외래 키의 NULL 허용 여부
            - 향후 1:1 관계가 1:N으로 확장될 가능성이 있는가

        - 주 테이블 / 대상 테이블
            주 테이블과 대상 테이블은 JPA가 정하는 개념이 아니라 도메인에서 어떤 엔티티를 관계의 중심으로 볼 것인지에 따라 구분한다.

            예) Order - Delivery
            Order    = 주 테이블
            Delivery = 대상 테이블

            주문이 비즈니스의 중심이고, 일반적으로 Order를 조회한 후 Delivery를 참조하는 흐름이 자연스럽기 때문에
                Order를 주 테이블, Delivery를 대상 테이블로 볼 수 있다.

            주의: 주 테이블과 "연관관계의 주인"은 다른 개념이다.
                주 테이블       : 도메인/업무 관계의 중심
                연관관계의 주인 : 실제 DB 외래 키를 관리하는 엔티티

            따라서 Order가 주 테이블이더라도 외래 키를 Delivery에 두면 JPA 연관관계의 주인은 Delivery가 될 수 있다.

        - 주 테이블(Order)에 외래 키를 두는 방식
            ORDER
            --------------------------------
            ORDER_ID(PK) | DELIVERY_ID(FK)
                 1       |       100

            장점
                - Order를 조회하면 DELIVERY_ID를 바로 알 수 있으므로 연결된 Delivery의 존재 여부와 식별자를 쉽게 확인할 수 있다.
                - order.getDelivery()처럼 Order -> Delivery 방향으로 자주 탐색하는 경우 객체 구조와 DB 구조가 자연스럽게 대응된다.
                - Order 조회 시 Delivery의 식별자를 이미 알고 있으므로 Hibernate는 Delivery의 실제 데이터를 바로 조회하지 않고
                    해당 식별자를 가진 프록시 객체를 만들어 둘 수 있다.
            단점
                - 향후 Order 1 : N Delivery로 관계가 변경되면 ORDER.DELIVERY_ID 하나만으로 여러 Delivery를 표현할 수 없다.
                - 따라서 외래 키를 Delivery 쪽으로 옮기는 등 테이블 구조 변경이 필요할 수 있다.

                ORDER_ID는 PK이므로 같은 ORDER_ID를 가진 행을 여러 개 만들 수 없다.
                ORDER_ID | DELIVERY_ID
                  1    |    100
                  1    |    101   // 불가능: ORDER_ID(PK) 중복
                  1    |    102   // 불가능

                또한 하나의 DELIVERY_ID 컬럼에 DELIVERY_ID = 100, 101, 102 처럼 여러 FK 값을 저장할 수도 없다.
                따라서 하나의 Order에 여러 Delivery를 연결해야 한다면 FK를 Delivery 쪽으로 옮기는 테이블 구조 변경이 필요하다.


        - 대상 테이블(Delivery)에 외래 키를 두는 경우
            DELIVERY
            --------------------------------
            DELIVERY_ID(PK) | ORDER_ID(FK)
                 100       |       1

            장점
                - 향후 Order 1 : N Delivery로 확장되더라도 기존 DELIVERY.ORDER_ID 외래 키 구조를 그대로 사용할 수 있다.
                - 같은 ORDER_ID를 가진 Delivery 행을 여러 개 허용하면 되므로 1 : N 관계로 확장하기 쉽다.
                DELIVERY_ID는 각 Delivery를 식별하는 PK이므로 각각 다른 값을 가지는 여러 행을 만들 수 있고,
                ORDER_ID는 FK이므로 서로 다른 Delivery 행에서 동일한 Order의 ID를 반복해서 가질 수 있다.
                DELIVERY_ID | ORDER_ID
                  100    |    1
                  101    |    1
                  102    |    1
                즉, Order 1
                    ├── Delivery 100
                    ├── Delivery 101
                    └── Delivery 102
                와 같은 1 : N 관계를 기존 FK 위치를 변경하지 않고 표현할 수 있다.
                단, 기존 1 : 1 관계를 보장하기 위해 DELIVERY.ORDER_ID에 UNIQUE 제약조건을 설정했다면
                    1 : N으로 변경할 때에는 해당 UNIQUE 제약조건을 제거해야 한다.

            단점
                - Order 테이블에는 DELIVERY_ID가 없으므로 Order만 조회해서는 연결된 Delivery가 존재하는지,
                    존재한다면 Delivery의 식별자가 무엇인지 바로 알 수 없다.
                - 관계 정보가 Delivery 테이블에 있기 때문에 Delivery의 존재 여부를 확인하려면 Delivery 테이블 조회가 필요할 수 있다.
                - 따라서 FK가 없는 Order 쪽에서는 Delivery의 존재 여부와 식별자를 알 수 없어 프록시를 바로 만들 수 없다.
                    따라서 일반적인 프록시 방식에서는 LAZY로 설정해도 Delivery 확인을 위한 조회가 발생하여 사실상 즉시 로딩처럼 동작할 수 있다.
    */
    @OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    /*
        java.util.Date는 하나의 Date 타입으로 날짜와 시간을 모두 표현하기 때문에
        JPA 입장에서는 해당 값을 DB의 DATE, TIME, TIMESTAMP 중
        어떤 타입으로 저장할지 알기 위해 @Temporal 설정이 필요했다.
        @Temporal(TemporalType.DATE / TIME / TIMESTAMP)을 지정해야 한다.

        하지만 Java 8 부터 제공되는 java.time 패키지의 LocalDate, LocalTime, LocalDateTime은 타입 자체가
        날짜/시간 정보를 명확하게 표현하므로 JPA가 별도의 @Temporal 없이 적절한 DB 타입으로 자동 매핑한다.

        LocalDate -> DATE
        LocalTime -> TIME
        LocalDateTime -> TIMESTAMP

        따라서 LocalDateTime을 사용하는 경우 @Temporal을 지정할 필요가 없다.

    */
    private LocalDateTime orderDate; // 주문 시간

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문 상태 [ORDER, CANCEL]

    // == 연관관계 편의 메서드 == //
    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

}
