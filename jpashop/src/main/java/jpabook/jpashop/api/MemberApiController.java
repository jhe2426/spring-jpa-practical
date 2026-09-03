package jpabook.jpashop.api;

import jakarta.validation.Valid;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /*
        조회V1: 응답 값으로 엔티티를직접 외부에 노출
        문제점
            - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
            - 기본적으로 엔티티의 모든 값이 노출된다.
            - 응답 스펙을 맞추기 위해 로직이 추가된다. (ex: @JsonIgnore, 별도의 뷰 로직 등등)
            - 실무에서는 같은 엔티티에 대해 API가 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각각의 API를 위한
                프레젠테이션 응답 로직을 담기는 어렵다.
            - 엔티티가 변경되면 API 스펙이 변한다.
            - 추가로 컬렉션을 직접 반환하면 향후 API 스펙을 변경하기 어렵다. (별도의 Result 클래스 생성으로 해결)
                - 컬렉션을 직접 반환하면 JSON의 최상위 구조가 배열로 고정이 됨
                - 이후 전체 데이터 개수, 페이지 정보 등 부가 필드를 추가하려면 최상위 구조를 객체로 변경해야 하므로
                    기존 API 응답 스펙이 깨질 수 있다.
                컬렉션을 직접 반환:
                    [
                      {
                        "id": 1,
                        "name": "사용자A"
                      },
                      {
                        "id": 2,
                        "name": "사용자B"
                      }
                    ]
                여기 구조에서 totalCount를 추가하려면 아래와 같이 구조 자체 변경:
                    {
                      "data": [
                        {
                          "id": 1,
                          "name": "사용자A"
                        },
                        {
                          "id": 2,
                          "name": "사용자B"
                        }
                      ],
                      "totalCount": 2
                    }
        결론: API 응답 스펙에 맞추어 별도의 DTO를 반환한다.
    */
    @GetMapping("/api/v1/members")
    public List<Member> memberV1() {
        return memberService.findMembers();
    }

    @GetMapping("/api/v2/members")
    public Result memberV2() {
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .toList();
        
        /*
            제네릭 클래스의 객체를 생성할 때 <>를 사용하면 컴파일러가 생성자에 전달된 값과 주변 문맥을 통해 타입을 추론하고 검사한다.
                반면 <>를 생략하면 원시 타입이 되어 타입 검사가 제대로 이루어지지 않는다.
                원시 타입: 제네릭이 도입되기 전의 오래된 자바 코드와 호환하기 위해 남아 있는 기능
                    원시 타입을 사용하면 제네릭의 타입 정보가 사라져 컴파일러가 값을 사실상 object로 취급한다.
                    따라서 잘못된 타입의 값이 저장되거나 잘못된 타입으로 형변환되어도 컴파일 단계에서 발견하지 못할 수 있으며,
                        실행 중 ClassCastException이 발생할 수 있으므로 사용하지 않는 것이 바람직하다.
        */
        return new Result<>(collect.size(), collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }

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
            - 응답을 만들기 위해 복잡한 조인이나 추가 조회가 필요하다면 해당 로직은 별도의 Query 메서드로 분리한다.

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
