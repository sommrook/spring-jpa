package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;
    
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        // 1. 엔티티 직접 노출
        List<Order> all = orderRepository.findAllString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    @GetMapping("/api/v2/orders")
    public List<OrderDto> orderV2() {
        // 2. DTO 로 변환 후 노출
        List<Order> orders = orderRepository.findAllString(new OrderSearch());
        return orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
    }

    @GetMapping("/api/v3/orders")
    public List<OrderDto> orderV3() {
        // 3. fetch join 사용 -> paging 사용 X (collection 도 fetch join 함)
        List<Order> orders = orderRepository.findAllWithItem();

        for (Order order : orders){
            System.out.println("order ref = " + order + " id=" + order.getId());
        }
        return orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
    }

    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> orderV3_page(@RequestParam(value="offset", defaultValue = "0") int offset,
                                       @RequestParam(value = "limit", defaultValue = "100") int limit) {
        // 3.1. fetch join 사용 -> paging O (collection 은 fetch join 하지 않고 지연 로딩함 => "default_batch_fetch_size")
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        return orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
    }

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4(){
        // 4. DTO로 직접 조회 -> Order 갯수 만큼 OrderItem 조회 쿼리 날라감
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5(){
        // 5. DTO로 직접 조회 -> Order 구한 후 in 절로 OrderItem 한번에 조회
        return orderQueryRepository.findAllByDto_optimization();
    }

    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6(){
        // 6. 쿼리 한번으로 조회 -> 대신 페이징에서는 사용할 수 없다.
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order){
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getMember().getAddress();
            order.getOrderItems().stream().forEach(o -> o.getItem().getName());
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;
        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
