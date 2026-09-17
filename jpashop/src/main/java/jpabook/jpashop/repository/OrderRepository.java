package jpabook.jpashop.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.QMember;
import jpabook.jpashop.domain.QOrder;
import jpabook.jpashop.domain.type.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    public OrderRepository(EntityManager em) {
        this.em = em;
        this.query = new JPAQueryFactory(em);
    }

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    public List<Order> findAllByString(OrderSearch orderSearch) {

        String jpql = "select o from Order o join o.member m";
        boolean isFirstCondition = true;

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000);// 최대 1000건

        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }

        return query.getResultList();
    }

    // JPA Criteria
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Object, Object> m = o.join("member", JoinType.INNER);

        List<Predicate> criteria = new ArrayList<>();

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
            criteria.add(status);
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name = cb.like(m.get("name"), "%" + orderSearch.getMemberName() + "%");
            criteria.add(name);
        }

        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
        return query.getResultList();
    }

    public List<Order> findAll(OrderSearch orderSearch) {
        QOrder order = QOrder.order; // static import를 통해서 해당 코드들을 더 깔끔하게 작성할 수 있음
        QMember member = QMember.member;

        return query
                .select(order)
                .from(order)
                .join(order.member, member)
                // 동적 쿼리 조건
                .where(statusEq(orderSearch.getOrderStatus()), nameLike(orderSearch.getMemberName()))
                // 정적 쿼리 조건
//                .where(order.status.eq(orderSearch.getOrderStatus()), member.name.like(orderSearch.getMemberName()))
                .limit(1000)
                .fetch();
    }

    private BooleanExpression nameLike(String memberName) {
        if (StringUtils.hasText(memberName)) {
            return null;
        }
        return QMember.member.name.like(memberName);
    }

    private BooleanExpression statusEq(OrderStatus statusCond) {
        if (statusCond == null) {
            return null;
        }
        return QOrder.order.status.eq(statusCond);
    }

    /*
        [Fetch Join 사용 시 주의사항]
        1. Fetch Join의 목적
        - Fetch Join은 LAZY로 설정된 연관관계를 현재 조회에서 실제로 사용할 것이 확실한 경우,
            연관 엔티티를 JOIN을 통해 한 번의 SQL로 함께 조회하여 N+1 문제를 방지하기 위한 조회 전략이다.


        2. To-One Fetch Join
        -  @ManyToOne, @OneToOne과 같은 To-One 연관관계는 Fetch Join을 하더라도
            일반적으로 루트 엔티티의 SQL 결과 행 수를 증가시키지 않는다.
            예)
                Member1 TeamA
                Member2 TeamA
                Member3 TeamB
                Member 하나당 Team은 최대 하나이므로 Member 3개 × Team 1개 = 3행이 됨
                따라서 현재 조회에서 필요한 To-One 연관관계는 같은 루트에서 여러 개를 병렬 Fetch Join하거나
                    To-One 경로를 중첩해서 Fetch Join하더라도 일반적으로 결과 행 수가 증가하지 않으므로 비교적 적극적으로 고려할 수 있다.
                To-One 경로 중첩 예)
                    Order
                     └─ member        // @ManyToOne
                          └─ company  // @ManyToOne
                    select o
                    from Order o
                    join fetch o.member m
                    join fetch m.company
                    단계가 To-One이므로 Order 1개 × Member 1개 × Company 1개 = SQL 1행으로 유지된다.


        3. To-Many 컬렉션 Fetch Join
        - @OneToMany, @ManyToMany와 같은 컬렉션을 Fetch Join하면 자식의 개수만큼 루트 엔티티의 SQL 결과 행이 증가한다.
        예)
            TeamA
            ├─ Member1
            ├─ Member2
            └─ Member3
            Fetch Join 결과:
                TeamA Member1
                TeamA Member2
                TeamA Member3
            객체 관점에서는 TeamA 하나이지만, DB의 JOIN 결과에서는 Member의 개수만큼 TeamA 행이 반복된다.
            따라서 컬렉션 Fetch Join은 쿼리 횟수는 줄어들지만 ResultSet의 행 수는 증가할 수 있다는 점을 반드시 고려해야 한다.


        4. DISTINCT가 사용되었던 이유
        - 과거 Hibernate에서는 컬렉션 Fetch Join으로 인해 다음과 같은 SQL 결과가 발생하면
            TeamA Member1
            TeamA Member2
            TeamA Member3
            최종 조회 결과 List에 동일한 TeamA 엔티티 참조가 여러 번 포함될 수 있었다.
            즉 같은 PK의 Team 객체를 여러 개 새로 만드는 것이 아니라, 영속성 컨텍스트에 존재하는 동일한 Team 인스턴스의 참조가
                결과 List에 여러 번 포함될 수 있었다는 의미
                distinct 없음 -> [TeamA, TeamA, TeamA]
            따라서 과거에는 다음과 같이 JPQL DISTINCT를 사용하였다. select distinct t from Team t join fetch t.members
                위의 쿼리문에 Distinct를 사용하여 최종 엔티티 조회 결과에서 동일한 루트 엔티티의 중복을 제거하였다.
                distinct 적용 -> [TeamA]
            주의: SQL의 DISTINCT가
                        TeamA Member1
                        TeamA Member2
                        TeamA Member3를 DB에서 TeamA 한 행으로 만들어 주는 것은 아니다.
                    Member 컬럼 값이 서로 다르므로 SQL 전체 행 기준으로는 각각 서로 다른 행이기 때문에 DB ResultSet에는 그대로 존재할 수 있다.
                즉, 과거 JPQL의 distinct는 DB의 SQL DISTINCT 처리뿐만 아니라, Hibernate가 DB 결과를 엔티티로 매핑한 이후
                    최종 Result List에서 동일한 루트 엔티티 참조를 제거하는 의미도 있었다.
            Hibernate 6부터는 Fetch Join으로 인해 발생한 동일 루트 엔티티의 중복을 Hibernate가 최종 결과 List에서 자동으로 제거한다.
            따라서 Hibernate 6 이상에서는 단순히 컬렉션 Fetch Join으로 발생한 루트 엔티티 중복 제거를 목적으로 DISTINCT를 사용할 필요가 없다.


        5. DISTINCT가 여전히 필요한 경우
        - Hibernate 6부터 DISTINCT 자체가 필요 없어진 것은 아니다. 일반 JOIN이나 Projection 등에서
            실제 조회 결과의 중복을 제거해야 하는 상황에서는 여전히 사용할 수 있다.
        - 단지 컬렉션 Fetch Join을 했더니 루트 엔티티가 결과 List에서 중복되므로 DISTINCT를 붙인다라는 용도로는
            Hibernate 6 이상에서 사용할 필요가 없어진 것이다.


        6. 컬렉션 Fetch Join + Pagination
        - DISTINCT는 컬렉션 Fetch Join의 페이징 문제를 해결하지 못한다.
        - To-Many Fetch Join은 자식 수만큼 SQL 결과 행이 증가한다.
            예)
                TeamA Member1
                TeamA Member2
                TeamA Member3
                TeamB Member4
            애플리케이션은 Team 엔티티 기준으로 페이징하고 싶지만, DB의 LIMIT/OFFSET은 JOIN 결과 행을 기준으로 처리한다.
            따라서 엔티티 개수 != SQL 결과 행 개수가 되어 컬렉션 Fetch Join과 페이징이 충돌할 수 있다.
            DISTINCT 역시 Member 값이 서로 다른 JOIN 행 자체를 하나로 합치지 못하므로 이 페이징 문제의 해결책이 아니다.
        - Hibernate 6 ~ 7.3버전에서는 컬렉션 Fetch Join + Pagination 시 메모리 페이징이 발생할 수 있으므로 특히 주의한다.
            메모리 페이징: 컬렉션 Fetch Join과 Pagination을 함께 사용할 때, DB에서 LIMIT/OFFSET으로 필요한 페이지 범위만 조회하지 못하고
                 조건에 해당하는 컬렉션 Fetch Join의 전체 결과를 먼저 조회한 뒤, Hibernate가 애플리케이션 메모리에서
                 루트 엔티티 기준으로 페이지를 잘라서 반환하는 방식이다.
                 즉 페이지 크기가 20이더라도 DB에서 20개만 조회하는 것이 아니라, 전체 JOIN 결과를 조회한 뒤 메모리에서 20개를 선택할 수 있다.
                 따라서 SQL 실행 횟수는 1번이어도 불필요한 전체 데이터 조회, 네트워크 전송량 증가, JVM 메모리 사용량 증가 등의 성능 문제가 발생할 수 있다.
        - Hibernate 7.4부터는 지원 DB에서 처리가 개선되었지만, 버전/DB에 의존하지 않는 설계가 필요하다면 부모를 먼저 페이징하고
            컬렉션은 Batch Fetching하는 방식을 고려한다.


        7. 여러 To-Many 컬렉션의 병렬 Fetch Join
        - 서로 다른 To-Many 컬렉션을 동시에 Fetch Join하면 각 컬렉션 크기가 곱해지는 Cartesian Product가 발생할 수 있다.
            예) OrderItems 10개 × Coupons 5개 = 50행
            - 따라서 SQL 횟수는 줄어도 DB 처리량, 네트워크 전송량, 객체 매핑 비용이 크게 증가할 수 있으므로
                여러 To-Many 컬렉션의 병렬 Fetch Join은 일반적으로 지양한다.
            - 특히 여러 Bag을 동시에 Fetch Join하면 MultipleBagFetchException이 발생할 수도 있다.
            - Set 등으로 변경하여 예외를 피하더라도 Cartesian Product에 따른 성능 문제는 그대로 남는다.

        8. Bag과 MultipleBagFetchException
        - Hibernate의 Bag은 중복을 허용하고 순서/index 정보는 의미가 없는 컬렉션 방식이다.
        - Bag 컬렉션 하나만 Fetch Join하는 것은 가능하다. 하지만 여러 Bag을 하나의 SQL에서 동시에 Fetch Join하면
            Cartesian Product로 인해 한 Bag의 원소가 다른 Bag의 원소 개수만큼 반복된다.
            이때 Bag은 원래 중복 자체를 허용하므로 원래 컬렉션에 존재하던 유의미한 중복과 다른 컬렉션과 JOIN하면서 발생한 인위적인 중복
                 을 Hibernate가 안전하게 구분하기 어렵다. 이러한 경우 MultipleBagFetchException이 발생할 수 있다.
        - 그래서 서로 다른 여러 To-Many 컬렉션을 병렬 Fetch Join하는 것 자체를 일반적으로 지양하는 방향으로 설계하는 것이 좋다.


        9. 중첩 Fetch Join
        다음과 같은 구조는 비교적 안전하게 사용할 수 있다.
            Order
            └─ orderItems : To-Many
                 └─ product : To-One
                      └─ category : To-One
            예) join fetch o.orderItems oi join fetch oi.product p join fetch p.category
             OrderItems가 10개라면 Order -> OrderItems 관계에서 SQL 결과는 10행으로 증가한다.
             하지만 각각의 OrderItem이 가지는 Product는 하나이고, Product가 가지는 Category도 하나이므로
             그 이후 To-One Fetch Join에서는 추가적인 행 증폭이 발생하지 않는다.
             10 × 1 × 1 = 10행 따라서 To-Many 하나 -> 그 아래 To-One -> 그 아래 To-One과 같이
                하나의 컬렉션 이후 To-One 방향으로 내려가는 Fetch Join은 비교적 안전하게 사용할 수 있다.
        주의: orderItems를 Fetch Join했다고 해서 OrderItem.product까지 자동으로 Fetch Join되는 것은 아니다.
            Product가 LAZY이고 현재 조회에서 Product까지 필요하다면 join fetch oi.product처럼 명시적으로 Fetch Join해야 한다.
            그렇지 않으면 Product에 접근하는 시점에 또 다른 N+1 문제가 발생할 수 있다.


        10. Batch Fetching을 사용하는 이유
        - Fetch Join은 연관관계를 JOIN하여 한 SQL에서 가져오는 방식이다.
        - 반면 Batch Fetching은 연관관계를 별도의 SELECT로 분리하되, 하나씩 조회하지 않고 여러 부모의 FK를 IN 절로 묶어서 한 번에 조회한다.
            예)
            Order 100개
            각 Order마다
                - OrderItems 10개
                - Coupons 5개
            두 컬렉션을 모두 Fetch Join하면 100 × 10 × 5 = 5,000행이 만들어질 수 있다.

            Batch Fetching으로 분리하면
            Order 조회      = 100행
            OrderItems 조회 = 1,000행
            Coupons 조회    = 500행
            총 1,600행 정도가 된다.

            즉 SQL 실행 횟수는 증가할 수 있지만 Fetch Join의 10 × 5와 같은 곱셈 구조를 10 + 5와 같은 합의 구조로 바꾸는 효과가 있다.

            따라서 여러 To-Many 컬렉션을 조회해야 하는 경우에는 불필요한 Cartesian Product를 피하기 위해 Batch Fetching을 고려할 수 있다.
        - 또한 Pagination이 필요한 목록 조회에서는
            1. 부모 엔티티를 먼저 정확한 개수만큼 페이징하고
            2. 해당 부모들의 컬렉션을 IN 절로 Batch Fetching하는 방식으로 처리할 수 있기 때문에
                컬렉션 Fetch Join보다 자연스럽게 처리할 수 있는 경우가 많다.


        11. 실무 Fetch Join 설계 기준
        [1] 현재 조회에서 필요한 To-One 연관관계
            @ManyToOne, @OneToOne → Fetch Join을 적극적으로 고려한다.
        [2] 상세 조회 + To-Many 컬렉션 하나 → 루트 엔티티가 보통 하나이고 페이징 문제가 없으며,
            컬렉션 크기가 과도하지 않다면 Fetch Join을 고려할 수 있다.
        [3] 서로 다른 To-Many 컬렉션 여러 개 → N × N 형태로 SQL 결과 행이 증가하므로 병렬 Fetch Join을 일반적으로 지양한다.
            Order
             ├─ OrderItems (N)
             └─ Coupons    (N)
                 → Batch Fetching
                 → 별도 조회
                 → DTO Projection 등의 다른 조회 전략을 고려한다.

        [4] Paging 목록 + To-Many → 컬렉션 Fetch Join은 Hibernate / DB 버전에 따라
            Pagination 처리 방식에 영향을 받을 수 있으므로 주의
            → 버전에 의존하지 않는 안정적인 설계를 원한다면 부모 엔티티를 먼저 페이징한 뒤  컬렉션을 Batch Fetching하는 방식을 고려


        즉, Fetch Join은 Fetch Join으로 줄어드는 SQL 횟수와 JOIN으로 인해 증가하는 SQL 결과 행 수를 함께 비교하여 가장 적절한 조회 전략을 선택한다.
    */
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
                "select o from Order o " +
                          "join fetch o.member m " +
                          "join fetch o.delivery d", Order.class
        ).getResultList();
    }

    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                "select o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Order> findAllWithItem() {
        return em.createQuery(
                      "select o from Order o " +
                                "join fetch o.member m " +
                                "join fetch o.delivery d " +
                                "join fetch o.orderItems oi " +
                                "join fetch oi.item i", Order.class)
                .getResultList();
    }

}
