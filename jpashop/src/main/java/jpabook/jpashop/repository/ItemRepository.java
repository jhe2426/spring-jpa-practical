package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.item.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    /*
        [엔티티 수정 방법]
        - JPA에서 기존 엔티티를 수정하는 방법은 크게 두 가지가 있다.
        1. 변경 감지(Dirty Checking)
            - 현재 트랜잭션에서 기존 엔티티를 조회하여 영속 상태로 만든 뒤, 수정이 필요한 값만 직접 변경하는 방식이다.
            예)
                Item item = em.find(Item.class,itemId);
                item.changeName("변경된 상품명");
                item.changePrice(10000);
                영속 엔티티를 처음 조회할 때 JPA는 해당 엔티티의 최초 상태를 스냅샷으로 저장해둔다.

                최초 스냅샷 name = "나이키" price = 10000 stock = 50 이후 price만 변경하면
                현재 엔티티 name = "나이키" price = 80000 stock = 50
                flush 시점(보통 트랜잭션 commit 직전)에 최초 스냅샷과 현재 엔티티의 값을 비교하여 어떤 값이 변경되었는지 감지한다.
                name: 변경 없음 price: 변경 stock: 변경 없음, 변경된 값이 존재하면 UPDATE SQL을 실행한다.

            [UPDATE SQL 생성 방식]
                하나의 엔티티에서 여러 필드가 변경되더라도 일반적으로 필드마다 UPDATE SQL이 따로 실행되는 것이 아니라, 해당 엔티티에 대한 한 번의 UPDATE SQL로 처리한다.
                예)
                    name, price, stock을 모두 변경한 경우
                    UPDATE item SET name = ?, price = ?, stock_quantity = ? WHERE item_id = ?;

                    단, Hibernate는 기본적으로 @DynamicUpdate를 사용하지 않으면 실제로 변경되지 않은 컬럼도 UPDATE문의 SET 절에 포함할 수 있다.
                    예를 들어 price만 변경했더라도 다음과 같은 SQL이 만들어 질 수 있다.
                        UPDATE item SET name = ?, price = ?, stock_quantity = ? WHERE item_id = ?;
                   이때 변경되지 않은 컬럼에 잘못된 값이 들어가는 것은 아니다. 현재 영속 엔티티가 가지고 있는 기존 값을 그대로 다시 넣는다.
                   예)
                    DB에 원래 저장된 값 name = "나이키" price = 10000 stock = 50, price만 80000으로 변경했다면 UPDATE에 전달되는 값은
                    name = "나이키" // 기존 값 그대로, price = 80000 // 변경된 값, stock = 50 // 기존 값 그대로
                    따라서 UPDATE SQL에 모든 컬럼이 포함되더라도 변경하지 않은 값은 동일한 값으로 다시 저장되므로 결과적으로 값은 변경되지 않는다.

                    Hibernate가 기본적으로 이렇게 동작하는 이유 중 하나는 엔티티마다 일정한 형태의 UPDATE SQL을 사용할 수 있기 때문이다.
                    price만 변경
                        -> UPDATE item SET name = ?, price = ?, stock =? ....
                    name만 변경
                        -> UPDATE item SET name = ?, price = ?, stock =? ....
                    처럼 동일한 SQL 형태를 사용할 수 있다.
                    만약 실제로 변경된 컬럼만 UPDATE SQL에 포함하고 싶다면 Hibernate의 @DynamicUpdate를 사용할 수 있디.
                    예)
                        @Entity
                        @DynamicUpdate
                        public class Item {}

                        price만 변경했다면 UPDATE SET price = ? WHERE item_id = ?;처럼 실제 변경된 컬럼을 기준으로 UPDATE SQL을 생성한다.
                즉, Dirty Checking은 어떤 값이 변경되었는지를 감지하는 기능
                @DynamicUpdate는 변경된 컬럼만 UPDATE문의 SET절에 포함하도록 UPDATE SQL을 동적으로 생성하는 Hibernate 기능
                이 둘은 서로 다른 개념

            장점
            - 수정하려는 필드만 명확하게 변경할 수 있다.
            - 전달되지 않은 값이 null이나 0으로 덮어써지는 문제를 방지할 수 있다.
            - 어떤 값이 변경되는지 코드에서 명확하게 확인할 수 있다.
            - 일반적인 웹 애플리케이션의 수정 로직에서는 이 방식을 주로 사용한다.

            단점
            - 기존 엔티티를 조회한 뒤 변경할 값을 직접 하나씩 적용해야 한다.

        2. merge() 사용
            - 현재 영속성 컨텍스트가 관리하지 않는 엔티티가 가지고 있는 상태를 현재 영속성 컨텍스트가 관리하는 엔티티에 병합하는 방식이다.
            - merge()에 전달한 엔티티 인스턴스 자체가 영속 상태가 되는 것은 아니다.
            [merge() 동작 과정]
                1. 전달받은 엔티티의 식별자(ID)를 확인한다.
                2. 현재 영속성 컨텍스트에서 같은 ID의 엔티티가 존재하는지 확인한다.
                3. 영속성 컨텍스트에 존재하지 않으면 DB에서 해당 엔티티를 조회하여 영속 상태의 엔티티를 준비한다.
                4. 전달받은 엔티티가 가지고 있는 상태(필드 값)를 영속성 컨텍스트가 관리하는 엔티티에 복사한다.
                    이미 영속성 컨텍스트의 기존 인스턴스를 여러 객체가 참조하고 있을 수 있으므로,
                        그 인스턴스를 다른 객체로 교체하지 않고 기존 영속 인스턴스에 상태만 복사하는 방식으로 merge()가 설계된 것이다.
                5. merge()는 값이 복사된 영속 상태의 엔티티를 반환한다.
                예)
                    Item item = ...;
                    Item mergedItem = em.merge(item);
                        item -> merge()에 값을 제공한 객체, 이 인스턴스 자체는 영속 상태가 되지 않는다.
                        mergedItem -> item의 갑이 복사된 객체, 영속성 컨텍스트가 관리하는 영속 상태의 객체
                    즉, merge()는 전달받은 객체 자체를 영속성 컨텍스트에서 관리해라가 아니라 이 객체가 가지고 있는 상태를 현재 영속성 컨테스트가
                        관리하는 엔티티에 복사해라라는 의미이다.
            장점
                - 현재 영속성 컨텍스트가 관리하지 않는 객체라도 해당 객체가 가지고 있는 상태를 영속 엔티티에 병합할 수 있다.
                - 수정된 값들을 이미 가지고 있는 엔티티가 완성되어 있다면 각 필드를 직접 하나씩 영속 엔티티에 옮기지 않고 merge()
                    한 번으로 상태를 병합할 수 있어 코드가 간단해질 수 있다.
                - 단, 이것은 편의성 측면의 장점이며, 일반적인 수정 로직에서는 의도하지 않은 값까지 복사될 위험 떄문에
                    조회 후 필요한 값만 변경하는 변경 감지 방식을 더 많이 사용한다.
            단점
                - 전달받은 엔티티의 상태를 병합하기 때문에 의도하지 않은 null, 0 등의 값까지 복사될 수 있다.
                - 어떤 필드가 수정되는지 코드만 보고 명확하게 파악하기 어렵다.
                - merge()에 전달한 객체 자체가 영속 상태가 되는 것이 아니므로 전달한 객체와 merge()가 반환한 영속 객체를 혼동하기 쉽다.

            [왜 전달받은 객체 자체를 다시 영속 상태로 만들지 않고 이러한 merge() 메서드 기능을 제공하는 걸까?]
            - 가장 중요한 이유는 현재 영속성 컨텍스트에 같은 식별자(ID)를 가진 다른 영속 엔티티가 이미 존재할 수 있기 때문이다.
            - 그럼 현재 영속성 컨텍스트에 같은 ID의 엔티티가 아무것도 없다면, 전달받은 준영속 객체를 다시 영속 상태로 만들어도 겉보기에는 문제가 없어 보일 수 있다.
            예)
                Item item;
                item.id = 1;
                item.name = "나이키";
                item.price = 80000;
                현재 영속성 컨텍스트 (비어 있음)
                이런 상황이라면 개념적으로 item 객체 자체 -> 영속성 컨텍스트에 다시 등록 -> Item(id=1) 영속 상태처럼 처리해도 문제가 없어 보인다.
                하지만 문제는 현재 영속성 컨텍스트가 이미 같은 ID의 다른 엔티티 인스턴스를 관리하고 있을 수 있다는 것이다.
                예)
                    Item findItem = em.find(Item.class, 1L);
                    현재 영속성 컨텍스트 (id = 1 -> findItem)
                    findItem
                        id = 1
                        name = "나이키"
                        price = 100000
                    그런데 현재 영속성 컨텍스트가 관리하지 않는 또 다른 Item 객체가 들어왔다고 가정
                    item
                        id = 1
                        name = "나이키"
                        price = 80000
                    findItem과 item은 같은 ID를 가지고 있지만 서로 다른 Java 인스턴스이다.
                    findItem -> 메모리상의 객체 A
                    item -> 메모리상의 객체 B
                    여기서 전달받은 item 객체 자체를 그대로 다시 영속 상태로 만들어버리면 영속성 컨텍스트에 id = 1 -> findItem, id = 1 -> item처럼 같은
                        ID를 가진 서로 다른 영속 객체가 두 개 존재하게 된다.
                    그리고 두 객체가 서로 다른 값으로 변경될 수도 있다.
                    findItem.setPrice(90000);
                    item.setPrice(80000);
                    두 객체 모두 Item(id=1)을 의미하는데 findItem의 price = 90000 item의 price = 80000 이라면 DB의 Item(id=1)을
                        어떤 객체의 값으로 UPDATE해야 하는지 문제가 생긴다.
                    따라서 영속성 컨텍스트에서는 동일한 ID를 가진 엔티티를 하나의 인스턴스로 관리한다.
                    즉 하나의 영속성 컨텍스트 안에서는 id = 1 → 하나의 Item 인스턴스만 존재하도록 관리해야 한다.

                    [그렇다면 같은 ID의 영속 객체가 없을 때만 전달받은 객체 자체를 영속 상태로 만들면 되지 않을까?]
                        개념적으로는 가능할 수 있다.
                        예)
                            같은 ID의 영속 객체가 없음 → 전달한 item 자체를 영속 상태로 만듦
                            같은 ID의 영속 객체가 있음 → 기존 영속 객체에 item의 값을 복사

                            하지만 이렇게 설계하면 merge()의 동작 방식이 영속성 컨텍스트의 현재 상태에 따라 달라지게 된다.

                            Item result = em.merge(item); 같은 ID의 영속 객체가 없으면
                                result == item → true가 될 수 있고

                            같은 ID의 영속 객체가 이미 있으면
                                result == item → false가 될 수 있다.

                            즉 개발자는 merge()를 호출할 때마다 전달한 객체 자체가 영속 객체가 된 것인지, 새로운 다른 영속 객체가 반환된 것인지
                                상황에 따라 구분해야 하게 된다.

                            JPA는 이런 식으로 상황에 따라 동작을 바꾸지 않고 merge()의 규칙을 일관되게 만들었다.
                            [merge()의 일관된 규칙]
                                전달받은 객체 자체는 영속 상태로 만들지 않는다. -> 현재 영속성 컨텍스트에서 동일한 ID의 영속 엔티티를 하나 확보한다.
                                    -> 전달받은 객체가 가지고 있는 값을 해당 영속 엔티티에 복사한다. -> 그 영속 엔티티를 반환한다.

                                따라서 Item mergedItem = em.merge(item);
                                    item
                                    → 상태를 전달하기 위한 객체
                                    → 이 객체 자체는 영속 상태가 되지 않는다.

                                    mergedItem
                                    → item의 값이 복사된 객체
                                    → 실제 영속성 컨텍스트가 관리하는 영속 엔티티이다.

        [변경 감지와 merge 비교]
            변경 감지
                기존 엔티티 조회 -> 영속 상태 엔티티 확보 -> 필요한 값만 변경 -> flush -> 스냅샷과 현재 값 비교
                    -> 변경 감지 -> UPDATE

            merge()
                관리하지 않은 엔티티 전달 -> 동일한 ID의 영속 엔티티 준비 -> 전달받은 엔티티의 상태를 복사
                    -> 영속 엔티티 반환 -> UPDATE

        [정리]
        - 변경 감지
            - 기존 엔티티를 조회한 뒤 필요한 값만 수정한다.
            - 수정 범위를 직접 통제할 수 있어 안전하다.
        - merge()
            - 전달받은 엔티티의 상태를 영속 엔티티에 복사한다.
            - 편리하지만 원하지 않는 값까지 병합될 위험이 있다.
        - 따라서 일반적인 수정 로직에서는 merge()보다 조회 -> 필요한 값 변경 -> 변경 감지 방식을 주로 사용한다.
    */
    public void save(Item item) {
        if (item.getId() == null) {
            em.persist(item);
        } else {
            em.merge(item);
        }
    }

    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i", Item.class)
                .getResultList();
    }
}
