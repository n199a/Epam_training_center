package com.epam.esm.service.impl;

import com.epam.esm.repository.dao.GiftCertificateDao;
import com.epam.esm.repository.dao.OrderDao;
import com.epam.esm.repository.dao.UserDao;
import com.epam.esm.model.dto.GiftCertificateDto;
import com.epam.esm.model.dto.OrderDto;
import com.epam.esm.model.dto.UserDto;
import com.epam.esm.model.entity.GiftCertificate;
import com.epam.esm.model.entity.Order;
import com.epam.esm.model.entity.User;
import com.epam.esm.service.exception.EntityNonExistentException;
import com.epam.esm.service.OrderService;
import com.epam.esm.repository.util.EsmPagination;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.esm.model.util.MessagePropertyKey.EXCEPTION_GIFT_CERTIFICATE_ID_NOT_FOUND;
import static com.epam.esm.model.util.MessagePropertyKey.EXCEPTION_ORDER_FOR_USER_NOT_FOUND;
import static com.epam.esm.model.util.MessagePropertyKey.EXCEPTION_ORDER_ID_NOT_FOUND;
import static com.epam.esm.model.util.MessagePropertyKey.EXCEPTION_UNSUPPORTED_OPERATION;
import static com.epam.esm.model.util.MessagePropertyKey.EXCEPTION_USER_ID_NOT_FOUND;

/**
 * Order service implementation.
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private final ModelMapper modelMapper;
    private final OrderDao orderDao;
    private final UserDao userDao;
    private final GiftCertificateDao certificateDao;

    /**
     * Instantiates a new tag service.
     *
     * @param modelMapper Model mapper.
     * @param orderDao    Tag DAO layer.
     * @param userDao     User DAO layer.
     * @param certificateDao       Gift certificate DAO layer.
     */
    public OrderServiceImpl(ModelMapper modelMapper, OrderDao orderDao, UserDao userDao, GiftCertificateDao certificateDao) {
        this.modelMapper = modelMapper;
        this.orderDao = orderDao;
        this.userDao = userDao;
        this.certificateDao = certificateDao;
    }

    @Override
    public OrderDto create(long userId, long giftCertificateId) {
        // Find requested User in the database
        User user = getUserOrElseThrow(userId);
        UserDto userDto = modelMapper.map(user, UserDto.class);

        // Find requested Gift certificate in the database
        GiftCertificate certificate = getGiftCertificateOrElseThrow(giftCertificateId);
        GiftCertificateDto certificateDto = modelMapper.map(certificate, GiftCertificateDto.class);

        // Create OrderDto
        OrderDto orderDto = new OrderDto();
        orderDto.setUser(userDto);
        orderDto.setGiftCertificate(certificateDto);

        // Create Order entity with User and Gift certificate
        Order order = modelMapper.map(orderDto, Order.class);
        BigDecimal gcPrice = certificate.getPrice();
        order.setPrice(gcPrice);
        order.setUser(user);
        order.setGiftCertificate(certificate);
        return modelMapper.map(orderDao.create(order), OrderDto.class);
    }

    @Override
    public Set<OrderDto> findAll(EsmPagination pagination) {
        return orderDao.findAll(pagination, Order.class).stream()
                .map(order -> modelMapper.map(order, OrderDto.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<OrderDto> findAllByUserId(long userId, EsmPagination pagination) {
        User user = getUserOrElseThrow(userId);
        return orderDao.findAllBy(user, pagination).stream()
                .map(order -> modelMapper.map(order, OrderDto.class))
                .collect(Collectors.toSet());
    }

    @Override
    public OrderDto findOrderForUser(long orderId, long userId) {
        User user = getUserOrElseThrow(userId);
        Order order = getOrderOrElseThrow(orderId);

        if (Objects.equals(user, order.getUser())) {
            return modelMapper.map(order, OrderDto.class);
        } else {
            throw new EntityNonExistentException(EXCEPTION_ORDER_FOR_USER_NOT_FOUND, userId);
        }
    }

    @Override
    public OrderDto findById(long id) {
        Order order = getOrderOrElseThrow(id);
        return modelMapper.map(order, OrderDto.class);
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException(EXCEPTION_UNSUPPORTED_OPERATION);
    }

    private User getUserOrElseThrow(long userId) {
        return userDao.findById(userId).orElseThrow(() -> new EntityNonExistentException(EXCEPTION_USER_ID_NOT_FOUND, userId));
    }

    private Order getOrderOrElseThrow(long orderId) {
        return orderDao.findById(orderId).orElseThrow(() -> new EntityNonExistentException(EXCEPTION_ORDER_ID_NOT_FOUND, orderId));
    }

    private GiftCertificate getGiftCertificateOrElseThrow(long gcId) {
        return certificateDao.findById(gcId).orElseThrow(() -> new EntityNonExistentException(EXCEPTION_GIFT_CERTIFICATE_ID_NOT_FOUND, gcId));
    }
}
