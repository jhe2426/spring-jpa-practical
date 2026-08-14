package jpabook.jpashop.domain;

import jakarta.persistence.*;
import jpabook.jpashop.domain.item.Item;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Category {

    @Id @GeneratedValue
    @Column(name = "category_id")
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
            // name: 중간 테이블의 이름을 지정
            name = "category_item",
            // joinColumns: @JoinTable을 선언한 현재 엔티티(Category)를 참조하는 FK 컬럼
            joinColumns = @JoinColumn(name = "category_id"),
            // inverseJoinColumns: 연관된 상대방 엔티티(Item)를 참조하는 FK 컬럼
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private List<Item> items = new ArrayList<>();

    /*
        여러 개의 자식 Category 객체(Many)가 하나의 부모 Category(One)를 참조하는 관계
        현재 Category(엔티티)가 다른 Category의 자식이라면 parent에 부모 Category가 저장된다.
        최상위 Category처럼 부모가 없는 경우 parent는 null이다.
        하나의 Category는 누군가의 자식이면서 동시에 다른 Category의 부모가 될 수 있다.
        여러 자식 Category가 하나의 부모 Category를 참조하므로 자식 Category 쪽이 N, 부모 Category 쪽이 1에 해당한다.
        현재 parent 필드는 자식 Category에서 부모 Category를 참조하는 필드이므로 이 필드가 N:1 관계의 N쪽에 해당한다.
        양방향 1:N 관계에서는 N쪽이 FK를 관리하므로 parent 필드에 @JoinColumn을 사용해 parent_id FK를 매핑한다.
    */
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    /*
        하나의 부모 Category(One)는 여러 자식 Category(Many)를 가질 수 있는 관계
        현재 Category를 부모로 저장한 Category들이 children 컬렉션에 담긴다.
        자식이 없는 최하위 Category라면 children 빈 컬렉션이다.
        mappedBy = "parent"는 children 컬렉션이 FK를 관리하지 않는다는 의미이다.
        단, children 컬렉션 자체의 add/remove 같은 자바 객체 변경을 막는 것은 아니다.
        따라서 children에 add/remove를 해도 자바 객체의 컬렉션 값 자체는 변경되지만, 그 변경만으로는 DB의 parent_id FK가 변경되지 않는다.
        주의)
            부모 Category 객체의 children 컬렉션만 변경하면, 부모 객체에서는 해당 Category를 자식으로 가지고 있다고 보이지만,
            자식 Category 객체의 parent 필드에서는 부모가 설정되지 않을 수 있다.
            예)
                신발.children.add(운동화);
                -> 신발.children에는 운동화가 추가됨
                -> 하지만 운동화.parent는 여전히 null일 수 있음
            이렇게 되면 부모와 자식 객체가 서로 다른 관계 상태를 가지게 되므로, 편의 메서드를 사용해 children과 parent를 함께 변경하는 것이 좋다.
    */
    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();
}
