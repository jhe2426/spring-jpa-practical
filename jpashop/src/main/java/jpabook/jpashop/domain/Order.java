package jpabook.jpashop.domain;

import jakarta.persistence.*;
import jpabook.jpashop.domain.type.DeliveryStatus;
import jpabook.jpashop.domain.type.OrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    // == 생성 메서드 == //
    /*
        생성 메서드인데 왜 정적 팩토리 메서드로 생성했을까?
        - 객체 생성에 필요한 초기화 작업은 생성자에서도 처리할 수 있다.
        - 하지만 생성자는 이름이 클래스명으로 고정되기 때문에, 해당 생성자가 어떤 목적과 규칙으로 객체를 생성하는지 명확하게 표현하기 어렵다.
        예)
            new Order(member, delivery, orderItems)
            위 코드만 보면 단순히 Order 객체를 생성하는 것인지, 주문 생성에 필요한 연관관계 설정, 주문 상태 초기화, 주문 시간 설정 등의 모든 규칙을
                수행하는 생성자인지 바로 알기 어렵다.
            반면 정적 팩토리 메서드를 사용하면 Order.create(memer, delivery, orderItems)처럼 메서드 이름을 통해
                정상적인 주문 생성 절차를 수행한다는 의도를 명확하게 표현할 수 있다.
            또한 주문 생성에 필요한 연관관계 설정과 초기값 설정 등의 생성 규칙을 이 메서드 한 곳에 모아 관리할 수 있다.
            static으로 선언한 이유:
                - createOrder()의 역할은 새로운 Order 인스턴스를 생성하는 것이다.
                - 인스턴스 메서드는 호출하기 위해 이미 Order 객체가 존재해야 하지만, 이 메서드는 그 Order 객체 자체를 처음 생성하기 위한 메서드이므로
                    인스턴스 메서드로 만들 수 없다.
                - 따라서 기존 객체 없이 Order.createOrder()로 호출할 수 있도록 static 메서드로 선언한 것이다.
            생성자의 접근 범위를 제한하고 정적 팩토리 메서드만 외부에 공개하면 애플리케이션 코드가 반드시 정해진 생성 절차를 거쳐 완전한 상태의
                Order 객체를 생성하도록 강제할 수도 있다.
        즉, 정적 팩토리 메서드는 단순히 생성자를 대체하기 위한 것이 아니라 객체 생성의 의도를 명확히 표현하고,
            객체 생성 규칙과 생성 경로를 한 곳에서 관리하기 위해 사용한다.

        [정적 팩토리 메서드를 사용하는 기준]
        정적 팩토리 메서드는 단순히 생성자를 메서드로 바꾼 것이라고 보기보다, 이 타입의 객체를 하나 만들어 주거나, 적절한 인스턴스를 선택해서 반환하는 진입점이라고
            이해하는 것이 좋다.
        1. 새로운 객체를 생성할 때
            예)
                Order.createOrder(member, delivery, orderItems)
                Money.of(1000)
            객체 생성에 필요한 초기화 규칙, 연관관계 설정, 기본값 설정 등을 한 곳에 모아 관리하고 싶을 때 사용한다.
            생성자도 동일한 초기화 작업을 수행할 수 있지만, 생성자는 이름이 클래스명으로 고정되어 있어 생성 목적을 표현하기 어렵다.
            new Order(...)보다 Order.createOrder(...)가 주문을 생성한다는 의도를 더 명확하게 표현할 수 있다.

        2. 기존 객체를 사용하거나 캐시에서 반환할 때
            예)
                Boolean.valueOf(true)를 통해 true를 나타내는 Boolean 객체 하나를 달라고 요청할 뿐, 새 객체가 생성되는지
                    기존 객체가 재사용되는지는 알 필요가 없다.

       3. 입력값에 따라 적절한 구현체 또는 하위 타입을 선택해서 반환할 때
            예)
                Payment.create(PaymentType.CARD) 내부에서 조건에 따라 CardPayment, CashPayment등 서로 다른 구현 객체를 선택해서 반환할 수 있다.
                즉, 호출자는 구체적인 객체 생성 방법을 몰라도 Payment 타입의 적절한 객체를 얻을 수 있다.

       4. 다른 값이나 객체로부터 현재 타입의 객체를 만들 때
            예)
                Member.from(memberDto)
                MemberDto를 기반으로 member를 생성한다는 의미를 from이라는 이름으로 명확하게 표현할 수 있다.
                이 경우 dto.toMember()처럼 인스턴스 메서드로 설계하는 것도 가능하므로, 반드시 정적 팩토리 메서드여야 하는 것은 아니다.
                Member의 생성 규칙을 Member 클래스 내부에서 관리하고 싶다면 Member.from(dto) 형태가 자연스럽다.

        [static으로 만드는 이유]
        - 인스턴스 메서드는 order.cancel(), member.changeName()처럼 이미 존재하는 "특정 객체"가 있어야 호출할 수 있다.
        - 반면 정적 팩토리 메서드는 Order.createOrder(...), Money.of(...), Payment.create(...)처럼 아직 사용할 객체가 정해지지 않은 상태에서
            이 타입의 객체 하나를 만들어 주거나 적절한 객체를 선택해서 반환해 달라는 역할을 한다.
        - 따라서 특정 인스턴스에 속하는 행동이 아니라 클래스 자체에 객체 생성/선택 책임을 두기 위해 static으로 선언한다.

        [판단 기준]
        - 이미 존재하는 이 객체가 어떤 행동을 해야 하는가? -> 인스턴스 메서드
            예)
                order.cancel()
                account.withdraw()
                member.changeName()
        - 아직 객체가 없거나 어떤 객체를 사용할지 정해지지 않았고, 이 타입의 객체 하나를 얻고 싶은가? -> 정적 팩토리 메서드를 고려
            예)
                Order.createOrder(...)
                Money.of(...)
                Member.from(...)
                Boolean.valueOf(...)
                Payment.create(...)
        즉, 정적 팩토리 메서드는 새 객체 생성뿐 아니라 기존 객체 재사용, 캐싱, 변환, 구현체 선택 등 객체를 어떻게 얻을지를 클래스 내부에 숨길 수 있다.
        외부에서는 해당 타입의 객체를 요청하기만 하고, 실제로 새 객체를 생성할지, 기존 객체를 재사용할지, 어떤 구현체를 반환할지는 정적 팩토리 메서드 내부에서 결정한다.
    */
    public static Order createOrder(Member member, Delivery delivery, OrderItem... orderItems) {
        Order order = new Order();
        order.setMember(member);
        order.setDelivery(delivery);
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        order.setStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    // == 비즈니스 로직 == //
    // 주문 취소
    public void cancel() {
        if (delivery.getStatus() == DeliveryStatus.COMP) {
            throw new IllegalStateException("이미 배송 완료한 상품은 취소가 불가능합니다.");
        }

        this.setStatus(OrderStatus.CANCEL);
        for (OrderItem orderItem : orderItems) {
            orderItem.cancel();
        }
    }

    // == 조회 로직 == //
    // 전체 주문 가격 조회
    public int getTotalPrice() {
        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
    }
}
