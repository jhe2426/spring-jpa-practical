package jpabook.jpashop.domain;

import jakarta.persistence.*;
import jpabook.jpashop.domain.type.DeliveryStatus;
import jpabook.jpashop.domain.value.Address;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(mappedBy = "delivery")
    private Order order;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status; // READY, COMP
}
