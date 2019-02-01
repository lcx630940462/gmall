package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    public void savePaymentInfo(PaymentInfo paymentInfo);

    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);
}
