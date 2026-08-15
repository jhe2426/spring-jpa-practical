package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jpabook.jpashop.domain.Member;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MemberRepository {

    /*

        EntityManagerFactory
            - EntityManager를 생성하는 공장 역할을 하는 객체이다.
            - JPA 설정 정보를 읽고 Hibernate 내부 환경과 데이터베이스 관련 설정을 초기화하면서 생성되기 때문에 생성 비용이 크다.
            - 따라서 요청이나 트랜잭션마다 새로 생성하지 않고, 일반적으로 애플리케이션이 실행될 때 한 번 생성해서 공유한다.
            - EntityManagerFactory는 여러 스레드에서 동시에 사용해도 안전하게 설계되어 있다.

        EntityManager
            - 영속성 컨텍스트를 통해 Entity를 관리하는 객체이다.
            - Entity의 저장(persist), 조회(find), 삭제(remove) 등의 작업을 수행하며,
                영속 상태의 Entity가 변경되면 변경 감지를 통해 수정 SQL도 처리한다.
            - EntityManager는 여러 스레드에서 동시에 공유해서 사용하도록 설계되어 있지 않다.
            - 따라서 EntityManagerFactory는 애플리케이션 전체에서 하나를 공유하고, EntityManager는 일반적으로 요청이나 트랜잭션과 같은 작업 단위마다
                생성해서 사용한다.
            - 이렇게 역할을 분리한 이유는 생성 비용이 큰 JPA 실행 환경은 EntityManagerFactory가 한 번만 초기화해서 공유하고,
                실제 Entity 관리 작업은 상대적으로 가벼운 EntityManager를 필요한 작업마다 생성해서 처리하기 위해서이다.
    */
    @PersistenceContext
    private EntityManager em;

/*
    EntityManagerFactory 주입받는 방법
    @PersistenceUnit
    private EntityManagerFactory emf;
*/

    /*
        메서드는 크게 Command와 Query로 구분할 수 있다.

        1. Command 메서드
        - 객체, 영속성 컨텍스트, DB 등의 상태를 변경하는 메서드이다.
        - 예: save(), update(), delete()
        - 데이터를 반환해주는 것이 아닌 이 상태를 변경해줘라는 명령을 수행한다.

        2. Query 메서드
        - 상태를 변경하지 않고 필요한 데이터를 조회하여 반환하는 메서드이다.
        - 예: findOne(), findAll()
        - 조회 결과를 사용하는 것이 목적이므로 반환값이 존재한다.

        [Side Effect]
        - Side Effect(부수 효과)란 메서드가 단순히 값을 계산해서 반환하는 것에서 끝나지 않고, 메서드를 실행하기 전과 실행한 후에 프로그램이 가지고 있던
            상태가 달라지는 것을 의미한다.
        - 여기서 '상태가 변경된다'는 것은 예를 들어 다음과 같은 변화를 말한다.
            - 전달 받은 객체의 필드 값이 변경되는 것
            - 컬렉션에 데이터가 추가되거나 삭제되는 것
            - 영속성 컨텍스트에 엔티티가 저장되거나 제거되는 것
            - DB의 데이터가 INSERT, UPDATE, DELETE 되는 것
            - 파일의 내용이 변경되는 것
        - 예를 들어 단순 계산 메서드는 결과값만 반환하고 기존의 객체나 데이터는 변경하지 않으므로 일반적으로 Side Effect가 없다고 볼 수 있다.
            int add(int a, int b) {
                return a + b;
            }
        - 반면, member.setName("kim");을 실행하면 기존 Member 객체의 name 값이 변경되고, em.persist(member);를 실행하면 기존에 비영속 상태였던
            Member가 영속 상태가 되어 영속성 컨텍스트가 해당 객체를 관리하게 되며, 이후 flush 과정에서 DB에도 INSERT가 발생할 수 있다.
        - 즉, 메서드의 반환값과 별개로 '메서드를 실행하기 전과 후에 기존 상태가 달라진다면' 이를 Side Effect가 발생핬다고 표현한다.
        - 따라서  save()는 단순히 값을 계산해서 반환하는 메서드가 아니라, 내부의 em.persist(member) 호출을 통해 Member를 저장하는 메서드이다.
            이 과정에서 영속성 컨텍스트의 상태가 변경되고, 이후 flush/commit 시 DB에도 반영되므로 Side Effect가 발생한다.

        [Command가 반환값을 가지지 않은 이유]
        - 상태를 변경하는 Command와 데이터를 반환하는 Query의 책임을 명확하게 분리하기 위해 Command는 일반적으로 반환값을 가지지 않도록 설계한다.
        - Command에서 엔티티 자체를 반환하기 시작하면 호출하는 쪽에서 해당 메서드를 '상태 변경'의 목적인지 '값 조회/반환'의 목적인지 혼동할 수 있고,
            상태 변경과 조회의 책임이 하나의 메서드에 섞일 수 있다.
        - 따라서 엄격하게 적용한다면 save()와 같은 Command 메서드는 반환값 없이 상태 변경만 수행하도록 설계할 수 있다.
        - 다만 실제로는 엔티티를 저장한 직후 생성된 식별자를 호출부에서 바로 사용하는 경우가 많기 때문에, 사용 편의성을 위해 엔티티 전체가 아니라
            최소한의 결과인 id만 반환하도록 설계할 수 있다.
            - 이는 id를 반환하지 않으면 식별자를 알 수 없기 때문은 아니다.
            - persist 과정에서 결정된 식별자는 전달한 Member 객체 자체에도 설정되므로, save() 호출 이후 member.getId()를 통해서도 식별자를 확인할 수 있다.
            - 즉, id 반환은 JPA 동작상 반드시 필요한 것이 아니라, Command의 주된 책임인 상태 변경을 크게 확장하지 않으면서 저장 결과의 식별자를 호출부에서
                바로 사용할 수 있도록 제공하는 실용적인 편의라고 볼 수 있다.

            [식별자가 결정되는 시점]
            - @GeneratedValue를 사용하는 경우 엔티티 객체를 생성한 시점에는 아직 식별자가 정해지지 않았으며, persist 과정에서 식별자 생성 전략에 따라
                식별자가 결정된다.
            - IDENTITY 전략은 DB가 식별자를 생성하므로, Hibernate가 식별자를 알기 위해 실제 INSERT를 실행하고 DB가 생성한 식별자를 전달받아야 한다.
            - 따라서 IDENTITY 전략에서는 일반적인 쓰기 지연과 달리 보통 em.persist(member)를 호출하는 시점에 INSERT SQL이 실행되고,
                DB가 생성한 식별자를 전달받아야 한다.
            - 단, INSERT가 persist 시점에 실행되더라도 트랜잭션이 확정된 것은 아니다. 트랜잭션의 최정 확정은 commit 시점에 이루어지며,
                그 전에 rollback하면 실행된 INSERT 역시 롤백된다.
            - 반면 SEQUENCE 전략은 DB Sequence에서 식별자 값만 먼저 얻을 수 있으므로 persist 시점에 Member 객체에 id를 설정하고,
                실제 INSERT는 flush 또는 commit 시점까지 지연할 수 있다.
            - 어떤 전략을 사용하든 persist 과정에서 결정된 식별자는 전달한 Member 객체 자체에도 설정된다.
    */
    public Long save(Member member) {
        em.persist(member);
        return member.getId();
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }

}
