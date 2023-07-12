package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    @Embedded
    private Address address;

    // Order class 의 member 에 mapping
    // 1:N => 한 member 는 여러 order 를 할 수 있다.
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();
}
