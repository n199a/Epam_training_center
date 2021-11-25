package com.epam.esm.controller;

import com.epam.esm.model.dto.OrderDto;
import com.epam.esm.service.impl.OrderServiceImpl;
import com.epam.esm.repository.util.EsmPagination;
import com.epam.esm.model.util.MessagePropertyKey;
import com.epam.esm.model.util.UrlMapping;
import com.epam.esm.controller.util.hateoas.LinkBuilder;
import com.epam.esm.model.view.View;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Set;

import static com.epam.esm.model.util.MessagePropertyKey.VALIDATION_GIFT_CERTIFICATE_ID;
import static com.epam.esm.model.util.MessagePropertyKey.VALIDATION_GIFT_CERTIFICATE_ID_NOT_NULL;
import static com.epam.esm.model.util.MessagePropertyKey.VALIDATION_ORDER_ID;
import static com.epam.esm.model.util.MessagePropertyKey.VALIDATION_USER_ID;
import static com.epam.esm.model.util.MessagePropertyKey.VALIDATION_USER_ID_NOT_EMPTY;

@RestController
@RequestMapping(value = UrlMapping.ORDERS)
@Validated
public class OrderController {
    private final OrderServiceImpl service;
    private final LinkBuilder<OrderDto> linkBuilder;

    @Autowired
    public OrderController(OrderServiceImpl service, LinkBuilder<OrderDto> linkBuilder) {
        this.service = service;
        this.linkBuilder = linkBuilder;
    }

    /**
     * Create order for user by user ID.
     *
     * @param userId            User ID.
     * @param giftCertificateId gift certificate ID.
     * @return Created order DTO.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntityModel<OrderDto> createOrderForUser(@NotNull(message = VALIDATION_USER_ID_NOT_EMPTY)
                                                    @Min(value = 1, message = VALIDATION_USER_ID)
                                                    @RequestBody Long userId,
                                                    @NotNull(message = VALIDATION_GIFT_CERTIFICATE_ID_NOT_NULL)
                                                    @Min(value = 1, message = VALIDATION_GIFT_CERTIFICATE_ID)
                                                    @RequestBody Long giftCertificateId) {
        OrderDto order = service.create(userId, giftCertificateId);
        return EntityModel.of(linkBuilder.build(order));
    }


    /**
     * Find order by ID.
     *
     * @param id Order ID.
     * @return Order DTO.
     */
    @GetMapping(UrlMapping.ID)
    public EntityModel<OrderDto> findById(@Min(value = 1, message = MessagePropertyKey.VALIDATION_ID)
                                          @PathVariable long id) {
        OrderDto order = service.findById(id);
        linkBuilder.build(order);
        return EntityModel.of(order);
    }

    /**
     * Find all orders.
     *
     * @param pagination Pagination parameters.
     * @return Set of found orders DTO.
     */
    @GetMapping
    public CollectionModel<OrderDto> findAll(@Valid EsmPagination pagination) {
        Set<OrderDto> orders = service.findAll(pagination);
        linkBuilder.build(orders);
        return CollectionModel.of(orders);
    }

    /**
     * Find all orders by user ID.
     *
     * @param userId        User ID.
     * @param pagination Pagination parameters.
     * @return Set of found order DTO.
     */
    @GetMapping(UrlMapping.FIND_ALL_ORDERS_BY_USER_ID)
    public CollectionModel<OrderDto> findAllOrdersByUserId(@Min(value = 1, message = VALIDATION_USER_ID)
                                                           @PathVariable long userId,
                                                           @Valid EsmPagination pagination) {
        Set<OrderDto> orders = service.findAllByUserId(userId, pagination);
        return CollectionModel.of(linkBuilder.build(orders));
    }

    /**
     * Find order by ID for User by ID.
     *
     * @param userId  user ID.
     * @param orderId order ID.
     * @return Entity model of found order.
     */
    @GetMapping(UrlMapping.FIND_ORDER_BY_USER_ID)
    @JsonView(View.FindOrderForUser.class)
    public OrderDto findOrderForUser(@Min(value = 1, message = VALIDATION_USER_ID)
                                     @PathVariable long userId,
                                     @Min(value = 1, message = VALIDATION_ORDER_ID)
                                     @PathVariable long orderId) {
        OrderDto order = service.findOrderForUser(orderId, userId);
        return linkBuilder.build(order);
    }
}