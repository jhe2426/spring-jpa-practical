package jpabook.jpashop;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Delivery;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.value.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    /*
        [JVM -> Java 객체 -> Spring Bean -> @PostConstruct -> Proxy -> @Transactional 전체 흐름 정리]
        1. JVM(Java Virtual Machine)이란?
        - Java 코드는 CPU가 직접 이해할 수 있는 기계어로 바로 컴파일하지 않는다.
        - 개발자가 작성한 Java 코드
                        Hello.java

                            ↓ javac 컴파일

                Hello.class (JVM이 이해하는 Bytecode)

                            ↓ 실행

                           JVM

                            ↓

                   현재 OS / CPU 환경에서 실행
       - Java는 특정 CPU나 OS의 기계어가 아니라 JVM이 공통으로 이해할 수 있는 Bytecode(.class)로 먼저 컴파일한다.
       - 실제 프로그램을 실행할 때에는 각 OS / CPU 환경에 맞게 구현한 JVM이 Bytecode를 해석하거나 JIT 컴파일 등을 통해 현재 컴퓨터에서 실행
            될 수 있도록 처리한다.
            JIT 컴파일: JVM이 Java 바이트코드를 실행하면서, 자주 실행되는 코드를 실제 CPU가 이해하는 기계어로 바꿔서 더 빠르게 실행하는 기능
                미리 전부 기계어로 바꾸는 것이 아니라, 프로그램이 실제로 실행되는 도중 필요한 부분을 기계어로 컴파일한다는 의미
        - 따라서 JVM은 바이트코드를 실행하면서, 필요에 따라 인터프리터로 실행하거나 자주 사용되는 부분을 JIT 컴파일해서
            현재 CPU용 기계어로 최적화하는 실행 환경

        - 즉, JVM은 Java Bytecode와 실제 OS/CPU 사이의 환경 차이를 감춰주는 추상화된 실행 환경
        - 단, JVM은 단순한 '기계어 변환기'만은 아니다. JVM은 Java 프로그램을 실행하면 다음과 같은 역할도 담당함
            - 클래스 로딩
            - Bytecode 실행
            - JIT 컴파일
            - Heap / Stack 등의 메모리 관리
            - 객체 메모리 관리
            - Garbage Collection
            - Thread 실행 및 관리
            - 예외 처리 지원


        2. JVM의 클래스 로딩과 Java 객체 생성
        - Java 프로그램을 실행하기 위해 JVM은 필요한 클래스 정보를 로딩한다.
        - 클래스 로딩과 객체 생성은 서로 다른 개념이다.
        - 클래스 로딩 / 준비:
            - JVM이 .class 파일을 읽고 해당 클래스를 실행에 사용할 수 있도록 클래스의 필드, 메서드, 생성자, 상속 관계, 메서드 Bytecode 등의
                런타임 정보를 준비하는 과정이다.
            - static 필드는 클래스 준비 과정에서 실제 필드가 생성되고 우선 기본값으로 초기화된다.
            - instance 필드는 클래스 로딩 시 실제 값 저장 공간이 생기는 것이 아니라, 이 클래스의 객체는 이런 필드를 가진다라는 구조 정보만
                준비된다. 실제 instance 필드와 저장 공간은 new로 객체를 생성할 때 객체마다 생성된다.
            - 메서드 역시 객체마다 코드를 새로 만든 것이 아니라, 클래스 로딩 과정에서 메서드 정보와 Bytecode가 준비되고 생성된 여러 객체가 해당 메서드
                코드를 사용한다.
        - 객체 생성: Member member = new Member(); -> Heap 등에 Member 객체를 위한 메모리 공간 확보 -> 인스턴스 필드 기본값 설정
                        -> 명시적인 필드 초기화 실행 -> 생성자 실행 -> Member 객체 생성 완료


        3. Spring Bean이란?
        - Java 객체와 Spring Bean은 완전히 같은 개념은 아니다.
        - Java 객체: JVM에서 생성된 일반적인 Java 객체
        - Spring Bean: Spring Container가 생성하고 관리하는 Java 객체
            생성 흐름: Java 객체 생성 -> 필요한 의존성 주입 -> 초기화 작업 -> 필요한 Spring 후처리 -> 최종적으로 Spring Bean으로 사용


        4. @PostConstruct란?
        - @PostConstruct가 붙은 메서드는 Spring이 Bean 객체를 생성하고 필요한 의존성 주입을 완료한 뒤, Bean의 초기화를 마무리하는 과정에서
            Spring이 자동으로 한 번 호출해주는 메서드
        - 해당 Bean을 본격적으로 사용하기 전에 한 번 자동으로 실행해주는 "초기화 콜백 메서드"이다.
            초기화 콜백 메서드: Bean 객체가 생성되고 필요한 의존성 주입이 모두 끝난 뒤, Bean이 최종적으로 사용되기 전에
                Spring이 자동으로 한 번 호출해주는 메서드
                콜백 메서드: 개발자가 직접 호출하는 것이 아니라 특정 조건이나 시점이 되었을 때 Framework가 자동으로 호출해주는 메서드
        - 흐름: TestService 객체 생성 -> 필요한 의존성 주입 -> @PostConstruct 메서드 실행 -> Bean 초기화 완료 -> 필요한 Spring 후처리 -> 최종 Bean 사용
        - 중요한 점은 @PostConstruct는 Bean 초기화가 모든 끝난 다음 실행되는 일반 메서드가 아니라 Bean 초기화를 완료하기 위한 과정 중 실행되는 메서드이다.


        5. Spring에서 말하는 Bean 초기화란?
        - Java 객체 생성:
            - 객체 메모리 확보
            - 인스턴스 필드 초기화
            - 생성자 실행
        - Spring Bean 초기화: Java 객체를 만든 뒤 Spring이 해당 객체를 Spring 환경에서 사용할 수 있도록 필요한 의존성을 주입하고,
            @PostConstruct 등의 초기화 작업을 수행하는 과정
        - 즉, Bean 초기화는 메서드를 메모리에 올리는 것을 의미하지 않는다. Spring 환경에서 해당 객체를 정상적으로 사용할 수 있도록 필요한 준비를 끝내는 과정


        6. Spring Bean의 기본적인 생성 흐름
        1. Java 객체 생성
        2. 필요한 의존성 주입
        3. @PostConstruct 실행
        4. Bean 초기화 완료
        5. 필요한 경우 Spring이 Proxy 등의 부가 기능 적용
        6. 최종 Bean 사용


        7. Transaction Proxy란?
        - @Transactional이 붙어 있다고 해서 해당 애노테이션 자체가 트랜잭션을 직접 시작하지 않음
        - Spring은 @Transactional이 적용된 Bean에 트랜잭션 기능이 추가된 Proxy 객체를 사용
            Proxy: 실제 객체 바로 앞에서 메서드 호출을 대신 받아서 추가 기능을 수행한 뒤 실제 객체의 메서드를 호출해주는 중간 객체이다.
        - Transaction Proxy는 메서드 호출을 가로채서 트랜잭션 시작 / commit / rollback 처리를 추가해줌
        - MemberService에 @Transactional이 선언되어 있을 때 실행 흐름:
            Controller -> Transaction Proxy -> 트랜잭션 시작 -> 실제 MemberService 메서드 실행 -> 정상 종료 -> commit
                                                                                           예외 발생 -> rollback
        - 즉, 개념적으로는 다음과 같은 역할을 수행
            transaction.begin():
            try {
                실제 객체의 메서드 실행;
                transaction.commit();
            } catch(...) {
                transaction.rollback();
            }
        - 실제로는 Spring의 TransactionInterceptor, TransactionManager 등의 기능이 함께 동작됨

        - Proxy는 메서드를 호출할 때마다 새로 만들어지는 것이 아니라 Bean 생성 과정에서 필요한 경우 Transaction Proxy가 적용된 Bean 준비
            -> 이후 메서드를 호출할 때 기존 proxy를 통과
            - @Transactional이 특정 메서드 하나에만 선언되어 있어도 해당 메서드를 실행할 때마다 Proxy를 새로 생성하지 않음
            - Spring은 Bean을 준비하는 과정에서 트랜잭션 적용 대상이 존재하면 해당 Bean에 Proxy를 적용하여 사용
            - 이후 외부에서 해당 Bean의 메서드를 호추하면 먼저 Proxy를 거치고, Proxy는 호출된 메서드가 트랜잭션 적용 대상인지 판단


        8. @PostConstruct + @Transactional이 문제가 되는 이유
            @PostConstruct
            @Transactional
            public void init() {
                entityManager.persist(member);
            }
            - 겉으로 보면 @Transactional -> 트랜잭션 시작 -> persist() -> 정상 종료 시 commit이 될 것처럼 보임
                - 하지만, @PostConstruct는 일반적인 외부 메서드 호출이 아님
                - Bean 객체가 생성되고 필요한 의존성 주입이 끝난 뒤, 해당 Bean의 초기화를 마무리하는 과정에서 Spring이 자동으로 한 번 실행해주는 메서드
                - 즉, Spring이 현재 Bean을 만드는 과정 중에 @PostConstruct 메서드를 직접 실행
                    현재 Bean 객체 생성 -> 필요한 의존성 주입 -> @PostConstruct 실행 -> Bean 초기화 완료 -> 필요한 경우 proxy 등의 부가 기능이 적용된 최종 Bean 사용
           - 즉, @PostConstruct가 실행되는 시점에는 현재 Bean이 아직 초기화 과정에 있기 때문에 자기 자신의 Transaction Proxy를 통과하는 호출이 아니다.
           - @Transactional은 메서드 호출이 Transaction Proxy를 통과해야 트랜잭션 시작 -> 메서드 실행 -> commit / rollback 기능이 적용되므로,
                해당 트랜잭션이 정상 적용될 것을 기대하면 안 된다.


        9. @PostConstruct 내부에서 호출한 Repository.save()는 정상적이 트랜잭션이 동작되는 이유
        - @PostConstruct 메서드 자체에 @Transactional을 선언하는 경우와, @PostConstruct 내부에서 주입받은 Repository의 save()를 호출하는 경우는
            서로 다르게 동작한다.
        - 예)
            @Component
            public class InitData {
                private final MemberRepository memberRepository;

                public InitData(MemberRepository memberRepository) {
                    this.memberRepository = memberRepository;
                }

                 @PostConstruct
                 public void init() {
                    memberRepository.save(new Member());
                 }
            }
        - 여기서 중요한 포인트는 현재 초기화 중인 InitData Bean고 주입받은 MemberRepository Bean은 서로 다른 Bean
        -  InitData를 생성하려면 생성자에 MemberRepository가 필요하기 때문에 Spring은 InitData에 MemberRepository를 주입하기 전에
            MemberRepository를 먼저 사용할 수 있는 Bean 상태로 준비해야 함
            MemberRepository Bean 생성 및 준비 -> Repository 기능 / Proxy 구성 -> 사용할 수 있는 MemberRepository Bean 준비 완료
                -> InitData 생성 시 MemberRepository 주입 -> InitData의 @PostConstruct 실행
        - 즉 InitData가 @PostConstruct를 실행하고 있는 시점에는 InitData 자신은 아직 초기화 과정에 있지만,
            이미 주입받은 MemberRepository는 별도의 Bean으로서 사용할 수 있도록 준비된 상태이므로 memberRepository.save(member)를 호출하면
            InitData 자신의 Proxy를 거치는 것이 아니라, 이미 주입되어 있는 MemberRepository의 Proxy를 호출하게 되면서 트랜잭션이 적용되어
            save() 내부의 persist/merge가 정상적으로 수행될 수 있다.
    */
    @PostConstruct
    public void init() {
        initService.dbInit1();
        initService.dbInit2();
    }


    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {
        private final EntityManager em;
        public void dbInit1() {
            Member member = createMember("userA", new Address("서울", "1", "111"));
            em.persist(member);

            Book book1 = createBook("JPA1 BOOK", 10000, 100);
            em.persist(book1);

            Book book2 = createBook("JPA2 BOOK", 20000, 200);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);


            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);

        }

        public void dbInit2() {
            Member member = createMember("userB", new Address("진주", "2", "222"));
            em.persist(member);

            Book book1 = createBook("Spring1 BOOK", 20000, 200);
            em.persist(book1);

            Book book2 = createBook("Spring2 BOOK", 40000, 300);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 20000, 3);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 40000, 4);

            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);

        }

        private Member createMember(String name, Address address) {
            Member member = new Member();
            member.setName(name);
            member.setAddress(address);
            return member;
        }

        private Book createBook(String name, int price, int stockQuantity) {
            Book book = new Book();
            book.setName(name);
            book.setPrice(price);
            book.setStockQuantity(stockQuantity);
            return book;
        }

        private Delivery createDelivery(Member member) {
            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());
            return delivery;
        }
    }
}

