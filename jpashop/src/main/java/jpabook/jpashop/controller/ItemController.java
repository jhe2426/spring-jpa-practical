package jpabook.jpashop.controller;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.service.ItemService;
import jpabook.jpashop.service.dto.UpdateItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BookForm());
        return "items/createItemForm";
    }

    @PostMapping("/items/new")
    public String create(BookForm form) {
        Book book = new Book();
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);
        return "redirect:/";
    }

    @GetMapping("/items")
    public String list(Model model) {
        List<Item> items = itemService.findItems();
        model.addAttribute("items", items);
        return "items/itemList";
    }

    @GetMapping("/items/{itemId}/edit")
    public String updateItemForm(@PathVariable("itemId") Long itemId, Model model) {
        Book item = (Book) itemService.findOne(itemId);

        BookForm form = new BookForm();
        form.setId(item.getId());
        form.setName(item.getName());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());
        form.setAuthor(item.getAuthor());
        form.setIsbn(item.getIsbn());

        model.addAttribute("form", form);
        return "items/updateItemForm";
    }

    /*
        [웹 환경에서 엔티티를 수정하는 흐름]
        1. GET 요청에서 Item을 조회하면 영속 상태가 된다.
        2. 브라우저에는 Item 객체가 아니라 HTML에 담긴 값만 전달된다.
            GET 요청이 끝나면 기존 영속성 컨텍스트도 종료된다.
        3. 이때 아이템들의 항목을 수정을 하고 수정 버튼을 누르면 새로운 POST 요청이 발생한다.
            Spring MVC는 전달받은 값으로 새로운 Item 객체를 생성한다.
        4. 이 Item은 엄밀히 말하면 새로 생성된 비영속 객체이다.
            다만 기존 DB 식별자(id)를 가지고 있기 때문에 실무에서는 넓은 의미로 준영속 엔티티라고 표현하기도 한다.
        5. 현재 영속성 컨텍스트가 관리하지 않으므로 값을 변경해도 변경 감지가 작동하지 않는다.
        6. merge()를 사용하면 전달받은 값을 영속 엔티티에 복사할 수 있지만, 모든 필드가 복사되어 전달되지 않은 값까지 null로 변경될 수 있다.
        7. 따라서 JPA에서는 서비스 계층의 트랜잭션 안에서 엔티티를 다시 조회하고, 수정이 허용된 값만 변경하는 변경 감지 방식을 권장한다.
    */
/*
    @PostMapping("/items/{itemId}/edit")
    public String updateItem(@ModelAttribute("form") BookForm form) {

        Book book = new Book();
        book.setId(form.getId());
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);
        return "redirect:/items";
    }
*/
    /*
        BookForm 객체는 화면에서만 사용할 데이터 목적으로 생성을 했으므로 서비스 계층에 해당 객체를 전달하는 것은
        코드가 지저분해지고 좋지 않음 그래서 아래와 같이 해당 form 객체를 전달하는 것이 아니라 값만 서비스 계층에 전달하는 것이 좋은 설계

        화면용 객체는 화면 요구사항에 따라 변경될 수 있고 서비스에서 사용하지 않는 필드도 포함할 수 있으므로,
        이를 서비스 계층까지 전달하면 서비스가 웹 계층에 불필요하게 의존하게 된다.
        따라서 서비스에는 해당 기능을 수행하는 데 필요한 값만 전달하여 계층의 역할과 의존성을 분리한다.
    */
/*

    @PostMapping("/items/{itemId}/edit")
    public String updateItemV1(@PathVariable("itemId") Long itemId, @ModelAttribute("form") BookForm form) {
        itemService.updateItemV1(itemId, form.getName(), form.getPrice(), form.getStockQuantity());
        return "redirect:/items";
    }
*/
    @PostMapping("/items/{itemId}/edit")
    public String updateItemV2(@PathVariable("itemId") Long itemId, @ModelAttribute("form") BookForm form) {
        UpdateItemDto dto = new UpdateItemDto(form.getName(), form.getPrice(), form.getStockQuantity());
        itemService.updateItemV2(itemId, dto);
        return "redirect:/items";
    }


}
