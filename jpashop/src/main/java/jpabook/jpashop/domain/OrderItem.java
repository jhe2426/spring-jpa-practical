package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jpabook.jpashop.domain.item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @JsonIgnore
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice; // 주문 가격
    private int count; // 주문 수량

    // == 생성 메서드 == //
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        item.removeStock(count);
        return orderItem;
    }

    // == 비즈니스 로직 == //
    // 주문 취소
    public void cancel() {
        getItem().addStock(count);
    }

    // == 조회 로직 == //
    // 주문 상품 전체 가격 조회
    /*
        엔티티 내부에서 자신의 필드(this.name 등)에 직접 접근하는 것은 문제가 없다.
        하지만 매개변수로 전달받은 다른 엔티티 인스턴스의 값을 읽을 때는 필드에 직접 접근하기보다 getter를 사용하는 것이 안전하다.
        예)
            this.name // 자기 자신의 필드이므로 직접 접근 가능
            other.getName() // 다른 인스턴스는 getter를 통해 접근
        이유:
            other에는 실제 엔티티가 아니라 JPA(Hibernate) 프록시 객체가 전달될 수 있다.
            프록시가 아직 초기화되지 않은 상태에서 other.name처럼 필드에 직접 접근하면 일반적인 프록시의 메서드 인터셉트 과정을 거치지 않으므로
                필요한 DB 조회 및 프록시 초기화가 발생하지 않을 수 있다.
            반면 other.getName()처럼 getter를 통해 호출하면 Hibernate가 해당 메서드 호출을 가로챌 수 있고, 프록시가 초기화되지 않았다면
                필요한 경우 DB에서 실제 엔티티를 조회한 뒤 값을 반환할 수 있다.
            단, 프록시 초기화가 필요한 시점에 영속성 컨텍스트(Session)가 이미 종료되어 있다면 getter를 호출하더라도
                LazyInitializationException이 발생할 수 있다.
            또한 필드를 private으로 감추고 getter를 통해 접근하게 하면 나중에 getter에 값 가공이나 공통 규칙이 추가되어도 외부 코드의 수정 없이
                동일한 규칙을 일관되게 적용할 수 있다.
            따라서
                - 엔티티 내부에서 자기 자신의 필드 -> 직접 접근 가능
                - 다른 엔티티 인스턴스의 필드 - getter 사용 권장
                - 엔티티 외부 -> 필드를 직접 공개하지 않고 getter 또는 비즈니스 메서드로 접근
            하는 방식이 안전하다.
    */
    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }
}
