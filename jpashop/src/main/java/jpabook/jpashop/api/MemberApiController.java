package jpabook.jpashop.api;

import jakarta.validation.Valid;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {

        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id, @RequestBody @Valid UpdateMemberRequest request) {
        /*
            CQS(Command Query Separation)
            - 메서드를 다음 두 가지 성격으로 명확하게 분리하는 원칙
            1. Command
                - 데이터를 저장, 수정, 삭제하여 상태를 변경한다.
            2. Query
                - 데이터를 변경하지 않고 조회한 값을 반환한다.
            엄격한 CQS에서는 Command가 상태 변경에만 집중하도록 반환값을 두지 않고, 필요한 응답 데이터를 조회할 수 있는 최소한의 데이터만 반환해주고
                이 데이터를 가지고 별도의 Query로 조회한다.
            따라서 아래의 코드에서는 update()가 회원 이름을 변경하는 Command를 담당하고 findOne()이 변경된 회원을 조회하는 Query를 담당한다.

            이렇게 분리하면 데이터 변경 문제가 발생했을 떄 Command 메서드만 확인하면 되므로 원인 추적 범위가 줄어든다.
            또한 API 응답 필드나 조회 방식이 변경되어도 회원을 수정하는 Command 로직에는 영향을 주지 않아 변경 로직과 조회 로직을 독립적으로
                유지보수할 수 있다.

            다만 실무에서는 CQS를 유연하게 적용하여 Command가 실행 과정에서 이미 얻은 ID, 상태, 점수, 수정된 값 등의 결과를 반환하기도 한다.
            유연하게 적용하더라도 다음 원칙은 지키는 것이 좋다.
            - Query 메서드에서는 데이터 변경이나 저장 같은 사이드 이펙트가 발생하지 않아야 한다.
            - Command의 반환값은 작업 과정에서 자연스럽게 얻어진 직접적인 실행 결과로 제한한다.
            - 응답을 만들기 위해 복자반 조인이나 추가 조회가 필요하다면 해당 로직은 별도의 Query 메서드로 분리한다.

            즉, 아래의 코드는 엄격한 CQS 스타일을 적용하여 수정 Command와 응답 조회 Query를 명확하게 분리한 예제이다.
        */
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    @Data
    static class CreateMemberRequest {
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

}
