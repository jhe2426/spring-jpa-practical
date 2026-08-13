package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemberRepositoryTest {

    @Autowired MemberRepository memberRepository;

    @Test
    @Transactional
    @Rollback(value = false)
    public void testMember() throws Exception {
        // given
        Member member = new Member();
        member.setName("memberA");

        // when
        Long savedId = memberRepository.save(member);
        Member findMember = memberRepository.find(savedId);

        // then
        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
        Assertions.assertThat(findMember.getName()).isEqualTo(member.getName());

        /*
            AssertJ의 isEqualTo()는 기본적으로 equals()와 같은 논리적 동등성(Equality)을 기준으로 값을 비교한다.
            반면 isSameAs()는 두 참조기 실제로 동일한 객체 인스턴스를 가리키는지, 즉 == 와 같은 동일성(Identity)을 비교한다.
        */
        Assertions.assertThat(findMember).isEqualTo(member);
        Assertions.assertThat(findMember).isSameAs(member);

        /*
            영속성 컨텍스트의 동일성 보장
            - 같은 영속성 컨텍스트에서는 동일한 식별자를 가진 엔티티를 하나의 객체 인스턴스로만 관리한다.
            - persist(member) 이후 동일한 id로 find()를 호출하면 1차 캐시에 이미 Member가 존재하므로 새로운 객체를 생성하지 않고
                기존에 관리하던 Member 인스턴스를 그대로 반환한다.
            - 따라서 member == findMember는 true가 된다.
        */
        System.out.println("findMember == member: " + (findMember == member));
    }

}