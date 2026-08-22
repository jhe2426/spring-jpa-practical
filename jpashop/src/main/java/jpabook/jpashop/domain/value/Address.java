package jpabook.jpashop.domain.value;

import jakarta.persistence.Embeddable;
import lombok.Getter;

/*
    값 타입은 여러 엔티티에서 같은 객체 인스턴스를 공유하면 한쪽의 변경이 다른 쪽에도 영향을 줄 수 있다.
    따라서 Setter등 값 변경 수단을 제거한 불변 객체로 만들어, 값을 변경할 때는
    기존 객체를 수정하지 않고 새로운 값 타입 객체로 교체하도록 하는 것이 안전하다.
*/
@Embeddable
@Getter
public class Address {

    private String city;
    private String street;
    private String zipcode;

    /*
        Address는 값 타입이므로 생성된 이후 내부 값을 변경하기보다 생성 시점에 필요한 값을 모두 전달하여 완전한 상태의 객체로 사용하는 것이 안전하다.

        하지만 JPA는 DB 조회 결과를 엔티티/값 타입 객체로 생성하기 위해 기본 생성자를 필요하며,
        JPA 명세상 기본 생성자는 public 또는 protected로 선언해야 한다.

        값 타입의 기본 생성자를 public으로 두면 애플리케이션 코드에서도 new Address()처럼 값이 하나도 없는 불완전한 값 타입 객체를 생성할 수 있으므로,
        JPA는 사용할 수 있으면서 외부의 불필요한 생성을 제한할 수 있도록 protected로 선언한다.
    */
    protected Address() {
    }

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
