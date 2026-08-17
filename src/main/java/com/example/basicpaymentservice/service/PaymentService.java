package com.example.basicpaymentservice.service;

import com.example.basicpaymentservice.dto.TransferRequest;
import com.example.basicpaymentservice.dto.TransferResponse;

public interface PaymentService {

    TransferResponse transfer(TransferRequest request);
}
