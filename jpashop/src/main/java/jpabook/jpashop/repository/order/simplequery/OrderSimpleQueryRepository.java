package jpabook.jpashop.repository.order.simplequery;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
    [화면 전용 조회 Repository를 분리하는 이유]
    - 핵심 비즈니스 로직에서 사용하는 OrderRepository는 Order 엔티티를 중심으로 조회하고 변경하는 역할에 집중하는 것이 좋다.
    - 반면 화면에 데이터를 출력하기 위한 조회 쿼리는 특정 화면에서 요구하는 데이터 구조에 강하게 의존하는 경우가 많다.
    - 예를 들어 화면에서 Order의 일부 필드뿐만 아니라 Member, OrderItem, Item 등 여러 엔티티의 데이터를 조합해서
        보여줘야 한다면 해당 화면에 맞는 DTO를 반환하는 조회 쿼리가 필요할 수 있다.
    - 이런 화면 전용 DTO 조회 메서드를 OrderRepository에 함께 작성하면 Repository의 역할이 핵심 비즈니스 조회와 화면 전용 조회로 섞이게 된다.
    - 또한 화면은 비즈니스 도메인에 비해 변경이 자주 발생할 수 있다. 화면에서 필요한 데이터가 변경되면 DTO의 필드나 조회 쿼리도 함께 변경될 수 있는데,
        이러한 화면 전용 메서드를 다른 비즈니스 로직에서도 재사용하고 있다면 화면 요구사항의 변경이 비즈니스 로직의 수정까지 전파될 수 있다.
    - 따라서
        - 핵심 비즈니스 로직에서 사용하는 Repository → 엔티티 중심의 조회 및 변경 책임
        - 화면에 필요한 데이터를 조회하는 Query Repository → DTO Projection, 여러 엔티티 JOIN 등 화면 전용 조회 책임 으로 분리하는 것이
            각 Repository의 역할을 명확하게 하고, 화면 변경이 핵심 비즈니스 로직에 미치는 영향을 줄여 유지보수성과 재사용성을 높일 수 있다.
    - 특히 화면에서 엔티티의 일부 필드만 필요하거나, 여러 엔티티를 조인하여 하나의 화면용 데이터로 만들어야 하는 경우에는
        별도의 Query Repository에서 DTO로 직접 조회하는 방식이 유용하다.
*/
@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    /*
        일반적인 SQL을 사용할 때 처럼 원하는 값을 선택해서 조회
        new 명령어를 사용해서 JPQL의 결과물 DTO로 즉시 변환
        SELECT 절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트워크 용량 최적화(생각보다 미비)
        리포지토리 재사용성 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점

        엔티티를 DTO로 변환하거나, DTO로 바로 조회하는 두가지 방법은 각각 장단점이 존재
        둘중 상황에 따라서 더 나은 방법을 선택하면 됨
        엔티티로 조회하면 리포지토리 재사용성도 좋고, 개발도 단순해짐, 따라서 권장하는 방법은 다음과 같음
        쿼리 방식 선택 권장 순서
        1. 우선 엔티티를 DTO로 변환하는 방법을 선택
        2. 필요하면 페치 조인으로 성능을 최적화 한다. -> 대부분의 성능 이슈가 해결됨
        3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용
        4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template를 사용해서 SQL를 직접 사용한다.
    */
    public List<OrderSimpleQueryDto> findOrderDtos() {

        /*
            select new jpabook.jpashop.repository.OrderSimpleQueryDto(o)
            이런식으로 엔티티 o를 직접 생성자로 넘겨주면 해당 엔티티가 넘어가는 것이 아니라 엔티티의 식별자 Id값만 넘어가기 때문에
            직접 일일이 엔티티의 필드 값으로 생성자에 대입을 해줘야지 원하는데로 dto에 매핑이 됨
        */
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                                "from Order o " +
                                "join o.member m " +
                                "join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();

    }

}
