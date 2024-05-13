package com.pax.pay.trans.model;

import com.pax.pay.base.Acquirer;

public class EcrSettlementModel {

    public byte[] captureSaleCount = new byte[3];
    public byte[] captureSaleAmount = new byte[12];
    public byte[] captureRefundCount = new byte[3];
    public byte[] captureRefundAmount = new byte[12];
    public byte[] debitSaleCount = new byte[3];
    public byte[] debitSaleAmount = new byte[12];
    public byte[] debitRefundCount = new byte[3];
    public byte[] debitRefundAmount = new byte[12];
    public byte[] AuthorizeSaleCount = new byte[3];
    public byte[] AuthorizeSaleAmount = new byte[12];
    public byte[] AuthorizeRefundCount = new byte[3];
    public byte[] AuthorizeRefundAmount = new byte[12];

    public String HostIndex = "000";
    public String HostNII = "000";
    public byte[] HostSettlementResult = new byte[2];
    public Acquirer _acquirer = null;
}


