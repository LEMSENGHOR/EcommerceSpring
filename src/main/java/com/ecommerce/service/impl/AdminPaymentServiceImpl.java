package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.payment.AdminPaymentFilterRequest;
import com.ecommerce.dto.payment.PaymentResponse;
import com.ecommerce.entity.Payment;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.PaymentMapper;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.repository.specification.PaymentSpecification;
import com.ecommerce.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public PagedResponse<PaymentResponse> getAllPayments(AdminPaymentFilterRequest filter, Pageable pageable) {
        Page<Payment> page = paymentRepository.findAll(PaymentSpecification.withFilters(filter), pageable);
        return PagedResponse.from(page.map(PaymentMapper::toResponse));
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return PaymentMapper.toResponse(payment);
    }
}
