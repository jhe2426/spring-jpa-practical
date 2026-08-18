package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em;

/*
    @Test
    @Rollback(value = false)
    public void 회원가입() throws Exception {
        // given
        Member member = new Member();
        member.setName("kim");

        // when
        Long savedId = memberService.join(member);

        // then
        Assertions.assertEquals(member, memberRepository.findOne(savedId));
    }
*/
/*
    // 실제 디비에 반영
    @Test
    @Rollback(value = false)
    public void 회원가입() throws Exception {
        // given
        Member member = new Member();
        member.setName("kim");

        // when
        Long savedId = memberService.join(member);
        em.flush();

        // then
        Assertions.assertEquals(member, memberRepository.findOne(savedId));
    }
*/

    @Test
    public void 회원가입() throws Exception {
        // given
        Member member = new Member();
        member.setName("kim");

        // when
        Long savedId = memberService.join(member);
        em.flush();

        // then
        Assertions.assertEquals(member, memberRepository.findOne(savedId));
    }

    /*
        [왜 같은 테스트 안에서 저장한 회원을 findByName()으로 바로 조회할 수 있을까?]
        - em.persist(member)로 회원을 저장해도 트랜잭션이 커밋되기 전까지는 최종적으로 DB에 확정된 상태가 아니다.
        - 하지만 이후 findByName()처럼 JPQL을 실해아현, 기본 FlushModeType.AUTO에서는 JPQL 실행 전에 영속성 컨텍스트의 변경사항을 DB와
            동기화하기 위해 자동으로 flush가 발생할 수 있다.
        - 즉, 첫 번째 회원을 persist한 뒤 두 번째의 회원의 중복 검사를 위해 findByName("kim")을 실행하면 다음과 같은 흐름으로 동작한다.
            1. 첫 번째 Member를 persist -> 영속성 컨텍스트에 저장
            2. 두 번째 Member의 중복 검사를 위해 JPQL 실행
            3. JPQL 실행 전에 AUTO Flush 발생 -> 첫 번째 Member의 INSERT SQL이 DB에 실행됨
            4. 그 다음 SELECT ... WHERE name = 'kim' 실행
            5. 같은 트랜잭션에서는 아직 commit되지 않은 자신의 INSERT 결과도 조회할 수 있으므로 첫 번째 Member가 조회됨
            6. 중복 회원으로 판단하여 예외 발생
        - 중요:
            JPQL은 DB를 조회하며, 조회 전에 AUTO flush가 발생하여 영속성 컨텍스트의 변경사항이 먼저 DB에 반영된 뒤 SELECT가 실행되는 것이다.
            flush는 SQL을 DB에 실행하여 동기화하는 것이고, commit은 그 변경사항을 최종 확장하는 것
            따라서 테스트가 마지막에 rollback되면 INSERT SQL이 실행됐더라도 최종적으로 DB에는 데이터가 남지 않는다.
    */
/*
    @Test()
    public void 중복_회원_예외() throws Exception {
        // given
        Member member1 = new Member();
        member1.setName("kim");


        Member member2 = new Member();
        member2.setName("kim");

        // when
        memberService.join(member1);
        try {
            memberService.join(member2);
        } catch (IllegalStateException e) {
            return;
        }

        // then
        // fail()은 테스트 코드가 절대로 도달하면 안 되는 지점에 도달했을 떄, 의도적으로 테스트를 실패시키는 Assertions 메서드이다.
        Assertions.fail("예외가 발생해야 한다.");
    }
*/

    @Test()
    public void 중복_회원_예외() throws Exception {
        // given
        Member member1 = new Member();
        member1.setName("kim");


        Member member2 = new Member();
        member2.setName("kim");

        // when
        memberService.join(member1);

        // then
        assertThrows(IllegalStateException.class, () -> {
            memberService.join(member2);
        });
    }

}