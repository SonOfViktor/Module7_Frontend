package com.epam.esm.service.impl;

import com.epam.esm.dao.GiftCertificateDao;
import com.epam.esm.dao.PaymentDao;
import com.epam.esm.dao.UserDao;
import com.epam.esm.dao.UserOrderDao;
import com.epam.esm.dto.PaymentDto;
import com.epam.esm.entity.Payment;
import com.epam.esm.entity.User;
import com.epam.esm.entity.UserOrder;
import com.epam.esm.exception.ResourceNotFoundException;
import com.epam.esm.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final String DELETED = "DELETED";

    private final PaymentDao paymentDao;
    private final UserOrderDao userOrderDao;
    private final UserDao userDao;
    private final GiftCertificateDao giftCertificateDao;

    @Override
    public PaymentDto addPayment(int userId, List<Integer> giftCertificateIdList) {
        User user = userDao.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("There is no user with id " + userId + " in database"));

        Payment payment = Payment.builder()
                .createdDate(LocalDateTime.now())
                .user(user)
                .build();
        payment.setOrders(createUserOrders(giftCertificateIdList));

        Payment createdPayment = paymentDao.save(payment);

        return mapPaymentOnDto(createdPayment);
    }

    @Override
    public PaymentDto findPayment(int paymentId) {
        Payment payment = paymentDao.findById(paymentId).orElseThrow(() ->
                new ResourceNotFoundException("There is no payment with id " + paymentId + " in database"));

        return mapPaymentOnDto(payment);
    }

    @Override
    public Page<PaymentDto> findPaymentsByUserId(int userId, Pageable pageable) {

        return paymentDao.findByUserId(userId, pageable)
                .map(this::mapPaymentOnDto);
    }

    @Override
    public Page<PaymentDto.UserOrderDto> findUserOrderByPaymentId(int paymentId, Pageable pageable) {

        return userOrderDao.findAllByPaymentId(paymentId, pageable)
                .map(this::createUserOrderPage);
    }

    private PaymentDto mapPaymentOnDto(Payment payment) {

        return new PaymentDto(
                payment.getPaymentId(),
                (payment.getUser().getFirstName() + SPACE + payment.getUser().getLastName()),
                payment.getOrders().stream().map(this::createUserOrderPage).toList(),
                payment.getCreatedDate()
        );
    }

    private List<UserOrder> createUserOrders(List<Integer> giftCertificateIdList) {

        return giftCertificateIdList.stream()
                .map(id -> giftCertificateDao.findById(id).orElseThrow(() ->
                        new ResourceNotFoundException("There is no certificate with Id " + id + " in database")))
                .map(gc -> UserOrder.builder()
                        .cost(gc.getPrice())
                        .giftCertificate(gc)
                        .build())
                .toList();
    }

    private PaymentDto.UserOrderDto createUserOrderPage(UserOrder order) {

        return new PaymentDto.UserOrderDto(
                (order.getGiftCertificate() != null) ?
                        order.getGiftCertificate().getGiftCertificateId() :
                        INTEGER_ZERO,
                (order.getGiftCertificate() != null) ?
                        order.getGiftCertificate().getName() :
                        DELETED,
                order.getCost());
    }
}
