package jpabook.jpashop.controller;

import jakarta.validation.Valid;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.value.Address;
import jpabook.jpashop.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members/new")
    public String createForm(Model model) {
        /*
            빈 객체가 없더라도 HTML, form의 name 속성을 통해 입력값을 받을 수 있다.
            하지만 MemberForm 빈 객체를 Model에 전달하면 Thymeleaf가 입력 폼을 MemberForm과 연결할 수 있다.
            이를 통해 다음 기능을 편리하게 처리할 수 있다.
            1. th:field를 통한 입력값 자동 바인딩
            2. 검증 실패 시 사용자가 입력한 값 유지
            3. th:errors를 통한 검증 오류 표시
            4. 수정 화면에서 기존 값 자동 표시

            즉, 값을 받기 위해 반드시 필요한 것은 아니며 Spring과 Thymeleaf의 폼 바인딩 기능을 편리하게 사용하기 위한 객체이다.
            MemberForm을 Model에 전달하여 입력값, 기존 값, 검증 결과를 하나의 객체를 기준으로 관리한다.
        */
        model.addAttribute("memberForm", new MemberForm());
        return "members/createMemberForm";
    }

    /*
        BindingResult: 사용자 입력값을 객체에 담는 과정과 @Valid 검증 과정에서 발생한 오류 및 거절된 입력값을 보관하여,
            컨트롤러와 Thymeleaf에서 처리할 수 있게 해주는 객체
    */
    @PostMapping("/members/new")
    public String create(@Valid MemberForm form, BindingResult result) {

        if (result.hasErrors()) {
            return "members/createMemberForm";
        }

        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

        Member member = new Member();
        member.setName(form.getName());
        member.setAddress(address);

        memberService.join(member);
        return "redirect:/";
    }
}
