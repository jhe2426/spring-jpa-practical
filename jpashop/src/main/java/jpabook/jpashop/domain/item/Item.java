package jpabook.jpashop.domain.item;

import jakarta.persistence.*;
import jpabook.jpashop.domain.Category;
import jpabook.jpashop.exception.NotEnoughStockException;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/*
    @ManyToOne 연관관계에서는 하나의 필드가 하나의 엔티티를 가리킨다.
    @ManyToOne(fetch = LAZY)
    private Item item;
        OrderItem 하나의 item 필드는 Item 하나만 가리킨다.

    여러 개의 OrderItem을 조회했다면 각각 다음과 같이 서로 다른 Item을 참조하고 있을 수 있다.
        OrderItem1 → Item1
        OrderItem2 → Item2
        OrderItem3 → Item3
        OrderItem4 → Item4
    item이 LAZY이기 때문에 처음 OrderItem을 조회했을 때는 Item의 실제 데이터를 바로 조회하지 않고, 각 Item을 대신하는 프록시가 준비된다.
        OrderItem1 → Item1 프록시
        OrderItem2 → Item2 프록시
        OrderItem3 → Item3 프록시
        OrderItem4 → Item4 프록시
    Batch Fetching을 적용하면 Hibernate는 Item1만 조회하지 않고,
        현재 영속성 컨텍스트에서 함께 조회할 수 있는 다른 Item 프록시들도 찾아 한 번에 조회한다.

    즉, Batch Fetching의 대상은 OrderItem.item이라는 필드 하나가 아니라, 각 item 필드가 가리키고 있는 여러 Item 엔티티이다.

    따라서 @ManyToOne과 같은 ToOne 연관관계에서는 연관관계 필드가 가리키는 대상 엔티티 타입을 기준으로
        Batch Fetching이 적용되기 때문에 대상 엔티티 클래스에 @BatchSize를 작성한다.

    BatchSize 사이즈는 100 ~ 1000 사이를 선택하는 것을 권장
    데이터베이스에 따라 IN절 파라미터를 1000으로 제한하기도 하기 때문
    1000으로 잡으면 한 번에 1000개를 DB에서 애플리케이션에 불러오므로 DB에 순간 부하가 증가할 수 있다.
    하지만 애플리케이션은 100이든 1000개이든 결국 전체 데이터를 로딩해야 하므로 메모리 사용량이 같다.
    1000으로 설정하는 것이 성능상 가장 좋지만, 결국 DB든 애플리케이션이든 순간 부하를 어디까지 견딜 수 있는지로 결정하면 된다.
*/
@BatchSize(size = 100)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Getter @Setter
public abstract class Item {

    @Id
    @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<>();

    // == 비즈니스 로직 == //

    // stock 증가
    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    // stock 감소
    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }
}
